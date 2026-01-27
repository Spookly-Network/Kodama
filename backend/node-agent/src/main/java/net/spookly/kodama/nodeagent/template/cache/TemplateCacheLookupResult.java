package net.spookly.kodama.nodeagent.template.cache;

import java.nio.file.Path;

public record TemplateCacheLookupResult(
        String templateId,
        String version,
        String expectedChecksum,
        boolean isCacheHit,
        TemplateCacheMissReason missReason,
        Path contentsDir,
        String cachedChecksum
) {

    public static TemplateCacheLookupResult hit(
            String templateId,
            String version,
            String expectedChecksum,
            Path contentsDir,
            String cachedChecksum
    ) {
        return new TemplateCacheLookupResult(
                templateId,
                version,
                expectedChecksum,
                true,
                null,
                contentsDir,
                cachedChecksum
        );
    }

    public static TemplateCacheLookupResult miss(
            String templateId,
            String version,
            String expectedChecksum,
            TemplateCacheMissReason reason,
            Path contentsDir,
            String cachedChecksum
    ) {
        if (reason == null) {
            throw new IllegalArgumentException("reason is required for a cache miss");
        }
        return new TemplateCacheLookupResult(
                templateId,
                version,
                expectedChecksum,
                false,
                reason,
                contentsDir,
                cachedChecksum
        );
    }
}
