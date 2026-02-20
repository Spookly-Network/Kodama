package net.spookly.kodama.nodeagent.instance.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NodePrepareInstanceRequest(
    UUID instanceId,
    String name,
    String displayName,
    String containerImage,
    String installScript,
    List<String> startCommand,
    Integer slotsRequired,
    List<NodePreparePortDefinition> portDefinitions,
    String portsJson,
    Map<String, String> variables,
    String variablesJson,
    List<NodePrepareInstanceLayer> layers) {
  public NodePrepareInstanceRequest(
      UUID instanceId,
      String name,
      String displayName,
      String portsJson,
      Map<String, String> variables,
      String variablesJson,
      List<NodePrepareInstanceLayer> layers) {
    this(
        instanceId,
        name,
        displayName,
        null,
        null,
        null,
        null,
        null,
        portsJson,
        variables,
        variablesJson,
        layers);
  }
}
