package net.spookly.kodama.brain.plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BrainPrepareInstanceContext(
    UUID instanceId,
    String name,
    String displayName,
    String portsJson,
    Map<String, String> variables,
    String variablesJson,
    List<BrainPrepareInstanceLayer> layers) {
  public BrainPrepareInstanceContext {
    variables = variables == null ? Map.of() : Map.copyOf(variables);
    layers = layers == null ? List.of() : List.copyOf(layers);
  }
}
