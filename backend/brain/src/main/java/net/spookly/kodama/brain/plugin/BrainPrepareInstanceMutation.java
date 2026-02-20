package net.spookly.kodama.brain.plugin;

import java.util.Map;
import java.util.Set;

public record BrainPrepareInstanceMutation(
    Map<String, String> variablesToSet, Set<String> variablesToRemove) {

  public BrainPrepareInstanceMutation {
    variablesToSet = variablesToSet == null ? Map.of() : Map.copyOf(variablesToSet);
    variablesToRemove = variablesToRemove == null ? Set.of() : Set.copyOf(variablesToRemove);
  }

  public static BrainPrepareInstanceMutation empty() {
    return new BrainPrepareInstanceMutation(Map.of(), Set.of());
  }

  public boolean isEmpty() {
    return variablesToSet.isEmpty() && variablesToRemove.isEmpty();
  }
}
