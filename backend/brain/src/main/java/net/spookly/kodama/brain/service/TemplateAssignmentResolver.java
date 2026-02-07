package net.spookly.kodama.brain.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.spookly.kodama.brain.domain.instance.GroupTemplateAssignment;
import net.spookly.kodama.brain.domain.instance.InstanceGroupMembership;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateAssignment;
import net.spookly.kodama.brain.domain.instance.TemplateAssignmentSource;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.repository.GroupTemplateAssignmentRepository;
import net.spookly.kodama.brain.repository.InstanceGroupMembershipRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateAssignmentRepository;
import net.spookly.kodama.brain.repository.TemplateRepository;
import net.spookly.kodama.brain.repository.TemplateVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TemplateAssignmentResolver {

    private static final Comparator<TemplateAssignmentCandidate> GROUP_DEDUP_ORDER = Comparator
            .comparingInt(TemplateAssignmentCandidate::priority)
            .thenComparing(candidate -> candidate.groupId() == null ? null : candidate.groupId(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TemplateAssignmentCandidate::assignmentId, Comparator.nullsLast(Comparator.naturalOrder()));

    private static final Comparator<TemplateAssignmentCandidate> EFFECTIVE_ORDER = Comparator
            .comparingInt(TemplateAssignmentCandidate::priority)
            .thenComparing(TemplateAssignmentCandidate::source)
            .thenComparing(TemplateAssignmentCandidate::templateId)
            .thenComparing(TemplateAssignmentCandidate::assignmentId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository;
    private final GroupTemplateAssignmentRepository groupTemplateAssignmentRepository;
    private final InstanceGroupMembershipRepository instanceGroupMembershipRepository;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;

    public TemplateAssignmentResolver(
            InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository,
            GroupTemplateAssignmentRepository groupTemplateAssignmentRepository,
            InstanceGroupMembershipRepository instanceGroupMembershipRepository,
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository
    ) {
        this.instanceTemplateAssignmentRepository = instanceTemplateAssignmentRepository;
        this.groupTemplateAssignmentRepository = groupTemplateAssignmentRepository;
        this.instanceGroupMembershipRepository = instanceGroupMembershipRepository;
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
    }

    public List<ResolvedTemplateLayer> resolveForInstance(UUID instanceId) {
        if (instanceId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instanceId is required");
        }
        List<InstanceTemplateAssignment> instanceAssignments =
                instanceTemplateAssignmentRepository.findAllByInstanceId(instanceId);
        List<UUID> groupIds = instanceGroupMembershipRepository.findGroupIdsByInstanceId(instanceId);
        List<GroupTemplateAssignment> groupAssignments = groupIds.isEmpty()
                ? List.of()
                : groupTemplateAssignmentRepository.findAllByGroupIds(groupIds);
        return resolveEffectiveLayers(instanceAssignments, groupAssignments);
    }

    public Map<UUID, List<ResolvedTemplateLayer>> resolveForInstances(Collection<UUID> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Map.of();
        }
        List<InstanceTemplateAssignment> instanceAssignments =
                instanceTemplateAssignmentRepository.findAllByInstanceIds(instanceIds);
        Map<UUID, List<InstanceTemplateAssignment>> instanceAssignmentsByInstance = instanceAssignments.stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getInstance().getId()));

        List<InstanceGroupMembership> memberships =
                instanceGroupMembershipRepository.findAllByInstanceIds(instanceIds);
        Map<UUID, List<UUID>> groupIdsByInstance = new HashMap<>();
        Set<UUID> groupIds = new HashSet<>();
        for (InstanceGroupMembership membership : memberships) {
            UUID instanceId = membership.getInstance().getId();
            UUID groupId = membership.getGroup().getId();
            groupIdsByInstance.computeIfAbsent(instanceId, key -> new ArrayList<>()).add(groupId);
            groupIds.add(groupId);
        }

        Map<UUID, List<GroupTemplateAssignment>> groupAssignmentsByGroup = groupIds.isEmpty()
                ? Map.of()
                : groupTemplateAssignmentRepository.findAllByGroupIds(groupIds).stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getGroup().getId()));

        Map<UUID, List<TemplateAssignmentCandidate>> effectiveCandidatesByInstance = new HashMap<>();
        List<TemplateAssignmentCandidate> allEffectiveCandidates = new ArrayList<>();
        for (UUID instanceId : instanceIds) {
            List<InstanceTemplateAssignment> directAssignments =
                    instanceAssignmentsByInstance.getOrDefault(instanceId, List.of());
            List<UUID> instanceGroupIds = groupIdsByInstance.getOrDefault(instanceId, List.of());
            List<GroupTemplateAssignment> groupedAssignments = new ArrayList<>();
            for (UUID groupId : instanceGroupIds) {
                groupedAssignments.addAll(groupAssignmentsByGroup.getOrDefault(groupId, List.of()));
            }
            List<TemplateAssignmentCandidate> ordered = resolveEffectiveCandidates(directAssignments, groupedAssignments);
            effectiveCandidatesByInstance.put(instanceId, ordered);
            allEffectiveCandidates.addAll(ordered);
        }

        Map<UUID, TemplateVersion> versionsByAssignmentId = allEffectiveCandidates.isEmpty()
                ? Map.of()
                : resolveTemplateVersions(allEffectiveCandidates);

        Map<UUID, List<ResolvedTemplateLayer>> resolved = new HashMap<>();
        for (UUID instanceId : instanceIds) {
            List<TemplateAssignmentCandidate> ordered =
                    effectiveCandidatesByInstance.getOrDefault(instanceId, List.of());
            resolved.put(instanceId, buildResolvedLayers(ordered, versionsByAssignmentId));
        }

        return resolved;
    }

    private List<ResolvedTemplateLayer> resolveEffectiveLayers(
            List<InstanceTemplateAssignment> instanceAssignments,
            List<GroupTemplateAssignment> groupAssignments
    ) {
        List<TemplateAssignmentCandidate> ordered = resolveEffectiveCandidates(instanceAssignments, groupAssignments);
        if (ordered.isEmpty()) {
            return List.of();
        }

        Map<UUID, TemplateVersion> versionsByAssignmentId = resolveTemplateVersions(ordered);
        return buildResolvedLayers(ordered, versionsByAssignmentId);
    }

    private List<TemplateAssignmentCandidate> resolveEffectiveCandidates(
            List<InstanceTemplateAssignment> instanceAssignments,
            List<GroupTemplateAssignment> groupAssignments
    ) {
        List<TemplateAssignmentCandidate> instanceCandidates = new ArrayList<>(instanceAssignments.size());
        for (InstanceTemplateAssignment assignment : instanceAssignments) {
            instanceCandidates.add(toCandidate(assignment));
        }

        List<TemplateAssignmentCandidate> groupCandidates = new ArrayList<>(groupAssignments.size());
        for (GroupTemplateAssignment assignment : groupAssignments) {
            groupCandidates.add(toCandidate(assignment));
        }

        if (instanceCandidates.isEmpty() && groupCandidates.isEmpty()) {
            return List.of();
        }

        Set<UUID> instanceTemplateIds = instanceCandidates.stream()
                .map(TemplateAssignmentCandidate::templateId)
                .collect(Collectors.toSet());

        List<TemplateAssignmentCandidate> eligibleGroupCandidates = groupCandidates.stream()
                .filter(candidate -> !instanceTemplateIds.contains(candidate.templateId()))
                .toList();

        Map<UUID, TemplateAssignmentCandidate> bestGroupAssignments =
                selectBest(eligibleGroupCandidates, GROUP_DEDUP_ORDER);

        List<TemplateAssignmentCandidate> ordered = new ArrayList<>(instanceCandidates.size()
                + bestGroupAssignments.size());
        ordered.addAll(instanceCandidates);
        ordered.addAll(bestGroupAssignments.values());
        ordered.sort(EFFECTIVE_ORDER);
        return ordered;
    }

    private List<ResolvedTemplateLayer> buildResolvedLayers(
            List<TemplateAssignmentCandidate> ordered,
            Map<UUID, TemplateVersion> versionsByAssignmentId
    ) {
        if (ordered.isEmpty()) {
            return List.of();
        }

        List<ResolvedTemplateLayer> resolved = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            TemplateAssignmentCandidate candidate = ordered.get(index);
            TemplateVersion version = versionsByAssignmentId.get(candidate.assignmentId());
            resolved.add(new ResolvedTemplateLayer(
                    candidate.assignmentId(),
                    candidate.templateId(),
                    version,
                    candidate.priority(),
                    index,
                    candidate.source()
            ));
        }

        return resolved;
    }

    private Map<UUID, TemplateAssignmentCandidate> selectBest(
            List<TemplateAssignmentCandidate> candidates,
            Comparator<TemplateAssignmentCandidate> ordering
    ) {
        Map<UUID, TemplateAssignmentCandidate> bestByTemplate = new HashMap<>();
        for (TemplateAssignmentCandidate candidate : candidates) {
            UUID templateId = candidate.templateId();
            TemplateAssignmentCandidate current = bestByTemplate.get(templateId);
            if (current == null || ordering.compare(candidate, current) < 0) {
                bestByTemplate.put(templateId, candidate);
            }
        }
        return bestByTemplate;
    }

    private Map<UUID, TemplateVersion> resolveTemplateVersions(List<TemplateAssignmentCandidate> candidates) {
        Set<UUID> templateVersionIds = candidates.stream()
                .map(TemplateAssignmentCandidate::templateVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, TemplateVersion> versionsById = templateVersionRepository.findAllById(templateVersionIds).stream()
                .collect(Collectors.toMap(TemplateVersion::getId, version -> version));

        if (versionsById.size() != templateVersionIds.size()) {
            Set<UUID> missingIds = new HashSet<>(templateVersionIds);
            missingIds.removeAll(versionsById.keySet());
            UUID missing = missingIds.stream().findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Template version not found" + (missing == null ? "" : ": " + missing));
        }

        Set<UUID> templateIdsWithoutVersion = candidates.stream()
                .filter(candidate -> candidate.templateVersionId() == null)
                .map(TemplateAssignmentCandidate::templateId)
                .collect(Collectors.toSet());

        Map<UUID, Template> templatesById = templateRepository.findAllById(templateIdsWithoutVersion).stream()
                .collect(Collectors.toMap(Template::getId, template -> template));

        if (templatesById.size() != templateIdsWithoutVersion.size()) {
            Set<UUID> missingIds = new HashSet<>(templateIdsWithoutVersion);
            missingIds.removeAll(templatesById.keySet());
            UUID missing = missingIds.stream().findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Template not found" + (missing == null ? "" : ": " + missing));
        }

        Map<UUID, TemplateVersion> latestVersions = templateIdsWithoutVersion.isEmpty()
                ? Map.of()
                : templateVersionRepository.findLatestForTemplateIds(templateIdsWithoutVersion).stream()
                .collect(Collectors.toMap(
                        version -> version.getTemplate().getId(),
                        version -> version,
                        (left, right) -> left
                ));

        if (!templateIdsWithoutVersion.isEmpty() && latestVersions.size() != templateIdsWithoutVersion.size()) {
            Set<UUID> missingIds = new HashSet<>(templateIdsWithoutVersion);
            missingIds.removeAll(latestVersions.keySet());
            UUID missing = missingIds.stream().findFirst().orElse(null);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Template has no versions" + (missing == null ? "" : ": " + missing));
        }

        Map<UUID, TemplateVersion> resolved = new HashMap<>();
        for (TemplateAssignmentCandidate candidate : candidates) {
            TemplateVersion version;
            if (candidate.templateVersionId() != null) {
                version = versionsById.get(candidate.templateVersionId());
                if (!candidate.templateId().equals(version.getTemplate().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "templateVersionId does not belong to templateId");
                }
            } else {
                version = latestVersions.get(candidate.templateId());
            }
            resolved.put(candidate.assignmentId(), version);
        }
        return resolved;
    }

    private TemplateAssignmentCandidate toCandidate(InstanceTemplateAssignment assignment) {
        return new TemplateAssignmentCandidate(
                requireAssignmentId(assignment.getId()),
                requireTemplateId(assignment.getTemplate().getId()),
                assignment.getTemplateVersion() == null ? null : assignment.getTemplateVersion().getId(),
                assignment.getPriority(),
                TemplateAssignmentSource.INSTANCE,
                null
        );
    }

    private TemplateAssignmentCandidate toCandidate(GroupTemplateAssignment assignment) {
        return new TemplateAssignmentCandidate(
                requireAssignmentId(assignment.getId()),
                requireTemplateId(assignment.getTemplate().getId()),
                assignment.getTemplateVersion() == null ? null : assignment.getTemplateVersion().getId(),
                assignment.getPriority(),
                TemplateAssignmentSource.GROUP,
                requireGroupId(assignment.getGroup().getId())
        );
    }

    private UUID requireAssignmentId(UUID assignmentId) {
        if (assignmentId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Assignment id is required");
        }
        return assignmentId;
    }

    private UUID requireTemplateId(UUID templateId) {
        if (templateId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Template id is required");
        }
        return templateId;
    }

    private UUID requireGroupId(UUID groupId) {
        if (groupId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Group id is required");
        }
        return groupId;
    }

    private record TemplateAssignmentCandidate(
            UUID assignmentId,
            UUID templateId,
            UUID templateVersionId,
            int priority,
            TemplateAssignmentSource source,
            UUID groupId
    ) {
    }
}
