package net.spookly.kodama.nodeagent.instance.workspace;

public record VariableSubstitutionResult(
        int filesScanned,
        int filesUpdated,
        int filesSkippedBinary,
        int filesSkippedLarge,
        int filesUnchanged
) {

    public static VariableSubstitutionResult empty() {
        return new VariableSubstitutionResult(0, 0, 0, 0, 0);
    }
}
