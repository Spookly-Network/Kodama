package net.spookly.kodama.nodeagent.template.cache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TemplateCacheLookupService {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCacheLookupService.class);

    private final TemplateCacheLayout layout;

    public TemplateCacheLookupService(TemplateCacheLayout layout) {
        this.layout = layout;
    }

    public TemplateCacheLookupResult findCachedTemplate(String templateId, String version, String expectedChecksum) {
        String normalizedExpected = requireChecksum(expectedChecksum);
        TemplateCachePaths paths = layout.resolveTemplateVersion(templateId, version);

        if (!Files.isDirectory(paths.contentsDir()) || !Files.isRegularFile(paths.checksumFile())) {
            TemplateCacheLookupResult result = TemplateCacheLookupResult.miss(
                    paths.templateId(),
                    paths.version(),
                    normalizedExpected,
                    TemplateCacheMissReason.NOT_FOUND,
                    paths.contentsDir(),
                    null
            );
            logger.info(
                    "Template cache miss. templateId={}, version={}, reason={}",
                    paths.templateId(),
                    paths.version(),
                    result.missReason()
            );
            return result;
        }

        String cachedChecksum = readChecksum(paths.checksumFile());
        if (!cachedChecksum.equals(normalizedExpected)) {
            TemplateCacheLookupResult result = TemplateCacheLookupResult.miss(
                    paths.templateId(),
                    paths.version(),
                    normalizedExpected,
                    TemplateCacheMissReason.CHECKSUM_MISMATCH,
                    paths.contentsDir(),
                    cachedChecksum
            );
            logger.info(
                    "Template cache miss. templateId={}, version={}, reason={}, cachedChecksum={}",
                    paths.templateId(),
                    paths.version(),
                    result.missReason(),
                    cachedChecksum
            );
            return result;
        }

        TemplateCacheLookupResult result = TemplateCacheLookupResult.hit(
                paths.templateId(),
                paths.version(),
                normalizedExpected,
                paths.contentsDir(),
                cachedChecksum
        );
        logger.info(
                "Template cache hit. templateId={}, version={}",
                paths.templateId(),
                paths.version()
        );
        return result;
    }

    private String requireChecksum(String expectedChecksum) {
        if (expectedChecksum == null || expectedChecksum.isBlank()) {
            throw new IllegalArgumentException("expectedChecksum is required");
        }
        return expectedChecksum.trim();
    }

    private String readChecksum(Path checksumFile) {
        try {
            return Files.readString(checksumFile, StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new TemplateCacheException("Failed to read checksum file at " + checksumFile, ex);
        }
    }
}
