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
  private static final Comparator<Node> NODE_ORDERING =
      Comparator.comparingInt(Node::getUsedSlots)
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
    int slotsRequired = normalizeSlotsRequired(instance.getSlotsRequired());
    Node node =
        selectNodeFromCandidates(
            nodeRepository.findAll(),
            instance.getRegion(),
            instance.getTags(),
            instance.getDevModeAllowed(),
            slotsRequired);
    if (node == null) {
      logger.warn(
          "No eligible nodes found for instance {} (region={}, tags={}, devModeAllowed={}, slotsRequired={})",
          instance.getId(),
          instance.getRegion(),
          instance.getTags(),
          instance.getDevModeAllowed(),
          slotsRequired);
    }
    return node;
  }

  @Transactional(readOnly = true)
  public Node selectNode(String region, String tags, Boolean devModeAllowed) {
    return selectNode(region, tags, devModeAllowed, 1);
  }

  @Transactional(readOnly = true)
  public Node selectNode(String region, String tags, Boolean devModeAllowed, int slotsRequired) {
    return selectNodeFromCandidates(
        nodeRepository.findAll(), region, tags, devModeAllowed, slotsRequired);
  }

  @Transactional(readOnly = true)
  public boolean hasEligibleNodes(String region, String tags, Boolean devModeAllowed) {
    return hasEligibleNodesFromCandidates(nodeRepository.findAll(), region, tags, devModeAllowed);
  }

  Node selectNodeFromCandidates(
      Collection<Node> nodes,
      String region,
      String tags,
      Boolean devModeAllowed,
      int slotsRequired) {
    if (nodes == null || nodes.isEmpty()) {
      return null;
    }

    int normalizedSlotsRequired = normalizeSlotsRequired(slotsRequired);
    String normalizedRegion = normalizeRegion(region);
    Set<String> requestedTags = parseTags(tags);

    return nodes.stream()
        .filter(node -> matchesFilters(node, normalizedRegion, requestedTags, devModeAllowed))
        .filter(node -> hasCapacity(node, normalizedSlotsRequired))
        .sorted(NODE_ORDERING)
        .findFirst()
        .orElse(null);
  }

  boolean hasEligibleNodesFromCandidates(
      Collection<Node> nodes, String region, String tags, Boolean devModeAllowed) {
    if (nodes == null || nodes.isEmpty()) {
      return false;
    }
    String normalizedRegion = normalizeRegion(region);
    Set<String> requestedTags = parseTags(tags);
    return nodes.stream()
        .anyMatch(node -> matchesFilters(node, normalizedRegion, requestedTags, devModeAllowed));
  }

  private boolean matchesFilters(
      Node node, String normalizedRegion, Set<String> requestedTags, Boolean devModeAllowed) {
    return node.getStatus() == NodeStatus.ONLINE
        && (normalizedRegion == null || normalizedRegion.equals(node.getRegion()))
        && (devModeAllowed == null || node.isDevMode() == devModeAllowed)
        && hasRequiredTags(node, requestedTags);
  }

  private boolean hasCapacity(Node node, int slotsRequired) {
    return ((long) node.getUsedSlots() + slotsRequired) <= node.getCapacitySlots();
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

  private int normalizeSlotsRequired(Integer slotsRequired) {
    if (slotsRequired == null) {
      return 1;
    }
    if (slotsRequired < 1) {
      throw new IllegalArgumentException("slotsRequired must be at least 1");
    }
    return slotsRequired;
  }
}
