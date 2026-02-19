package net.spookly.kodama.brain.service;

import java.util.List;
import java.util.UUID;

import net.spookly.kodama.brain.domain.blueprint.Blueprint;
import net.spookly.kodama.brain.domain.blueprint.BlueprintTemplateAssignment;
import net.spookly.kodama.brain.domain.instance.GroupTemplateAssignment;
import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.instance.InstanceGroup;
import net.spookly.kodama.brain.domain.instance.InstanceTemplateAssignment;
import net.spookly.kodama.brain.domain.template.Template;
import net.spookly.kodama.brain.domain.template.TemplateVersion;
import net.spookly.kodama.brain.dto.TemplateAssignmentDto;
import net.spookly.kodama.brain.dto.TemplateAssignmentRequest;
import net.spookly.kodama.brain.repository.BlueprintRepository;
import net.spookly.kodama.brain.repository.BlueprintTemplateAssignmentRepository;
import net.spookly.kodama.brain.repository.GroupTemplateAssignmentRepository;
import net.spookly.kodama.brain.repository.InstanceGroupRepository;
import net.spookly.kodama.brain.repository.InstanceRepository;
import net.spookly.kodama.brain.repository.InstanceTemplateAssignmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TemplateAssignmentService {

    private final BlueprintTemplateAssignmentRepository blueprintTemplateAssignmentRepository;
    private final InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository;
    private final GroupTemplateAssignmentRepository groupTemplateAssignmentRepository;
    private final BlueprintRepository blueprintRepository;
    private final InstanceRepository instanceRepository;
    private final InstanceGroupRepository instanceGroupRepository;
    private final TemplateService templateService;

    public TemplateAssignmentService(
            BlueprintTemplateAssignmentRepository blueprintTemplateAssignmentRepository,
            InstanceTemplateAssignmentRepository instanceTemplateAssignmentRepository,
            GroupTemplateAssignmentRepository groupTemplateAssignmentRepository,
            BlueprintRepository blueprintRepository,
            InstanceRepository instanceRepository,
            InstanceGroupRepository instanceGroupRepository,
            TemplateService templateService
    ) {
        this.blueprintTemplateAssignmentRepository = blueprintTemplateAssignmentRepository;
        this.instanceTemplateAssignmentRepository = instanceTemplateAssignmentRepository;
        this.groupTemplateAssignmentRepository = groupTemplateAssignmentRepository;
        this.blueprintRepository = blueprintRepository;
        this.instanceRepository = instanceRepository;
        this.instanceGroupRepository = instanceGroupRepository;
        this.templateService = templateService;
    }

    @Transactional(readOnly = true)
    public List<TemplateAssignmentDto> listBlueprintAssignments(UUID blueprintId) {
        ensureBlueprintExists(blueprintId);
        return blueprintTemplateAssignmentRepository.findAllByBlueprintId(blueprintId).stream()
                .map(TemplateAssignmentDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TemplateAssignmentReference> listBlueprintAssignmentReferences(UUID blueprintId) {
        ensureBlueprintExists(blueprintId);
        return blueprintTemplateAssignmentRepository.findAllByBlueprintId(blueprintId).stream()
                .map(assignment -> new TemplateAssignmentReference(
                        assignment.getTemplate().getId(),
                        assignment.getTemplateVersion() == null ? null : assignment.getTemplateVersion().getId(),
                        assignment.getPriority()
                ))
                .toList();
    }

    public TemplateAssignmentDto addBlueprintAssignment(UUID blueprintId, TemplateAssignmentRequest request) {
        Blueprint blueprint = loadBlueprint(blueprintId);
        AssignmentTarget target = resolveAssignmentTarget(request);
        BlueprintTemplateAssignment assignment = new BlueprintTemplateAssignment(
                blueprint,
                target.template(),
                target.templateVersion(),
                target.priority()
        );
        BlueprintTemplateAssignment saved = blueprintTemplateAssignmentRepository.save(assignment);
        return TemplateAssignmentDto.fromEntity(saved);
    }

    public void removeBlueprintAssignment(UUID blueprintId, UUID assignmentId) {
        ensureBlueprintExists(blueprintId);
        BlueprintTemplateAssignment assignment = blueprintTemplateAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!assignment.getBlueprint().getId().equals(blueprintId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }
        blueprintTemplateAssignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<TemplateAssignmentDto> listInstanceAssignments(UUID instanceId) {
        ensureInstanceExists(instanceId);
        return instanceTemplateAssignmentRepository.findAllByInstanceId(instanceId).stream()
                .map(TemplateAssignmentDto::fromEntity)
                .toList();
    }

    public TemplateAssignmentDto addInstanceAssignment(UUID instanceId, TemplateAssignmentRequest request) {
        Instance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found"));
        AssignmentTarget target = resolveAssignmentTarget(request);
        InstanceTemplateAssignment assignment = new InstanceTemplateAssignment(
                instance,
                target.template(),
                target.templateVersion(),
                target.priority()
        );
        InstanceTemplateAssignment saved = instanceTemplateAssignmentRepository.save(assignment);
        return TemplateAssignmentDto.fromEntity(saved);
    }

    public void removeInstanceAssignment(UUID instanceId, UUID assignmentId) {
        ensureInstanceExists(instanceId);
        InstanceTemplateAssignment assignment = instanceTemplateAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!assignment.getInstance().getId().equals(instanceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }
        instanceTemplateAssignmentRepository.delete(assignment);
    }

    @Transactional(readOnly = true)
    public List<TemplateAssignmentDto> listGroupAssignments(UUID groupId) {
        ensureGroupExists(groupId);
        return groupTemplateAssignmentRepository.findAllByGroupId(groupId).stream()
                .map(TemplateAssignmentDto::fromEntity)
                .toList();
    }

    public TemplateAssignmentDto addGroupAssignment(UUID groupId, TemplateAssignmentRequest request) {
        InstanceGroup group = instanceGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        AssignmentTarget target = resolveAssignmentTarget(request);
        GroupTemplateAssignment assignment = new GroupTemplateAssignment(
                group,
                target.template(),
                target.templateVersion(),
                target.priority()
        );
        GroupTemplateAssignment saved = groupTemplateAssignmentRepository.save(assignment);
        return TemplateAssignmentDto.fromEntity(saved);
    }

    public void removeGroupAssignment(UUID groupId, UUID assignmentId) {
        ensureGroupExists(groupId);
        GroupTemplateAssignment assignment = groupTemplateAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!assignment.getGroup().getId().equals(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found");
        }
        groupTemplateAssignmentRepository.delete(assignment);
    }

    private AssignmentTarget resolveAssignmentTarget(TemplateAssignmentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment request is required");
        }
        if (request.getTemplateId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateId is required");
        }
        int priority = request.getPriority() != null ? request.getPriority() : 0;
        if (priority < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Priority must be >= 0");
        }

        Template template = templateService.loadTemplatesById(List.of(request.getTemplateId()))
                .get(request.getTemplateId());

        TemplateVersion templateVersion = null;
        if (request.getTemplateVersionId() != null) {
            templateVersion = templateService.loadTemplateVersionsById(List.of(request.getTemplateVersionId()))
                    .get(request.getTemplateVersionId());
            if (!template.getId().equals(templateVersion.getTemplate().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "templateVersionId does not belong to templateId");
            }
        } else {
            templateService.ensureTemplatesHaveVersions(List.of(template.getId()));
        }

        return new AssignmentTarget(template, templateVersion, priority);
    }

    private void ensureInstanceExists(UUID instanceId) {
        if (!instanceRepository.existsById(instanceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found");
        }
    }

    private Blueprint loadBlueprint(UUID blueprintId) {
        return blueprintRepository.findByIdAndDeletedAtIsNull(blueprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found"));
    }

    private void ensureBlueprintExists(UUID blueprintId) {
        if (blueprintRepository.findByIdAndDeletedAtIsNull(blueprintId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blueprint not found");
        }
    }

    private void ensureGroupExists(UUID groupId) {
        if (!instanceGroupRepository.existsById(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
    }

    private record AssignmentTarget(Template template, TemplateVersion templateVersion, int priority) {
    }

    public record TemplateAssignmentReference(
            UUID templateId,
            UUID templateVersionId,
            int priority
    ) {
    }
}
