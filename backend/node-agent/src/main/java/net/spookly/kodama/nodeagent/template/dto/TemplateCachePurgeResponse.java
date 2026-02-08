package net.spookly.kodama.nodeagent.template.dto;

import net.spookly.kodama.nodeagent.template.cache.TemplateCachePurgeResult;

public record TemplateCachePurgeResponse(
        String scope,
        String templateId,
        long deletedFiles,
        long deletedDirectories,
        long deletedBytes
) {

    public static TemplateCachePurgeResponse forAll(TemplateCachePurgeResult result) {
        return fromResult("all", null, result);
    }

    public static TemplateCachePurgeResponse forTemplate(String templateId, TemplateCachePurgeResult result) {
        return fromResult("template", templateId, result);
    }

    private static TemplateCachePurgeResponse fromResult(
            String scope,
            String templateId,
            TemplateCachePurgeResult result
    ) {
        TemplateCachePurgeResult safeResult = result == null
                ? new TemplateCachePurgeResult(0, 0, 0)
                : result;
        return new TemplateCachePurgeResponse(
                scope,
                templateId,
                safeResult.deletedFiles(),
                safeResult.deletedDirectories(),
                safeResult.deletedBytes()
        );
    }
}
