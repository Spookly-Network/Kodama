package net.spookly.kodama.nodeagent.plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record NodeInstanceStartMutation(
        Map<String, String> envToSet,
        Set<String> envToRemove,
        Map<String, String> labelsToSet,
        Set<String> labelsToRemove,
        List<String> commandOverride
) {

    public NodeInstanceStartMutation {
        envToSet = envToSet == null ? Map.of() : Map.copyOf(envToSet);
        envToRemove = envToRemove == null ? Set.of() : Set.copyOf(envToRemove);
        labelsToSet = labelsToSet == null ? Map.of() : Map.copyOf(labelsToSet);
        labelsToRemove = labelsToRemove == null ? Set.of() : Set.copyOf(labelsToRemove);
        commandOverride = commandOverride == null ? null : List.copyOf(commandOverride);
    }

    public static NodeInstanceStartMutation empty() {
        return new NodeInstanceStartMutation(Map.of(), Set.of(), Map.of(), Set.of(), null);
    }

    public boolean isEmpty() {
        return envToSet.isEmpty()
                && envToRemove.isEmpty()
                && labelsToSet.isEmpty()
                && labelsToRemove.isEmpty()
                && commandOverride == null;
    }
}
