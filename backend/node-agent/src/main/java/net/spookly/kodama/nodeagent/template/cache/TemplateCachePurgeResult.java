package net.spookly.kodama.nodeagent.template.cache;

public record TemplateCachePurgeResult(long deletedFiles, long deletedDirectories, long deletedBytes) {

    public TemplateCachePurgeResult add(TemplateCachePurgeResult other) {
        if (other == null) {
            return this;
        }
        return new TemplateCachePurgeResult(
                deletedFiles + other.deletedFiles(),
                deletedDirectories + other.deletedDirectories(),
                deletedBytes + other.deletedBytes()
        );
    }
}
