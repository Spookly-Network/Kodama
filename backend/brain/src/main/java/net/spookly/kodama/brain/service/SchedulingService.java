package net.spookly.kodama.brain.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.spookly.kodama.brain.domain.instance.Instance;
import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import net.spookly.kodama.brain.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchedulingService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingService.class);
    private static final Comparator<Node> NODE_ORDERING = Comparator
            .comparingInt(Node::getUsedSlots)
            .thenComparing(Node::getName)
            .thenComparing(Node::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final NodeRepository nodeRepository;

    public SchedulingService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Transactional(readOnly = true)
    public Node selectNode(Instance instance) {
        if (instance == null) {
            throw new IllegalArgumentException("instance");
        }
        Node node = selectNodeFromCandidates(
                nodeRepository.findAll(),
                instance.getRegion(),
                instance.getTags(),
                instance.getDevModeAllowed()
        );
        if (node == null) {
            logger.warn(
                    "No eligible nodes found for instance {} (region={}, tags={}, devModeAllowed={})",
                    instance.getId(),
                    instance.getRegion(),
                    instance.getTags(),
                    instance.getDevModeAllowed()
            );
        }
        return node;
    }

    @Transactional(readOnly = true)
    public Node selectNode(String region, String tags, Boolean devModeAllowed) {
        return selectNodeFromCandidates(nodeRepository.findAll(), region, tags, devModeAllowed);
    }

    Node selectNodeFromCandidates(
            Collection<Node> nodes,
            String region,
            String tags,
            Boolean devModeAllowed
    ) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        String normalizedRegion = normalizeRegion(region);
        Set<String> requestedTags = parseTags(tags);

        return nodes.stream()
                .filter(node -> node.getStatus() == NodeStatus.ONLINE)
                .filter(node -> normalizedRegion == null || normalizedRegion.equals(node.getRegion()))
                .filter(node -> devModeAllowed == null || node.isDevMode() == devModeAllowed)
                .filter(node -> node.getUsedSlots() < node.getCapacitySlots())
                .filter(node -> hasRequiredTags(node, requestedTags))
                .sorted(NODE_ORDERING)
                .findFirst()
                .orElse(null);
    }

    private boolean hasRequiredTags(Node node, Set<String> requestedTags) {
        if (requestedTags.isEmpty()) {
            return true;
        }
        Set<String> nodeTags = parseTags(node.getTags());
        return nodeTags.containsAll(requestedTags);
    }

    private String normalizeRegion(String region) {
        if (region == null) {
            return null;
        }
        String trimmed = region.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Set<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Set.of();
        }
        Stream<String> tokens = Stream.of(rawTags.split(","));
        return tokens
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}
