package net.spookly.kodama.brain.plugin;

import java.util.Map;

public record BrainPrepareInstanceVariables(Map<String, String> variables, String variablesJson) {
  public BrainPrepareInstanceVariables {
    variables = variables == null ? null : Map.copyOf(variables);
  }
}
