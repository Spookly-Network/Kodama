package net.spookly.kodama.nodeagent.instance.workspace;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstanceVariableSubstitutionService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceVariableSubstitutionService.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    private final long maxFileBytes;

    public InstanceVariableSubstitutionService(NodeConfig config) {
        NodeConfig.VariableSubstitution settings = Objects.requireNonNull(config, "config").getVariableSubstitution();
        this.maxFileBytes = settings == null ? 0L : settings.getMaxFileBytes();
    }

    public VariableSubstitutionResult substituteVariables(
            String instanceId,
            Path workspaceRoot,
            Map<String, String> variables
    ) {
        String normalizedInstanceId = requireValue("instanceId", instanceId);
        Path root = Objects.requireNonNull(workspaceRoot, "workspaceRoot");
        Map<String, String> normalizedVariables = normalizeVariables(variables);
        if (normalizedVariables.isEmpty()) {
            return VariableSubstitutionResult.empty();
        }
        if (!Files.isDirectory(root)) {
            throw new InstanceWorkspaceException("Workspace path is not a directory: " + root);
        }

        int scanned = 0;
        int updated = 0;
        int skippedBinary = 0;
        int skippedLarge = 0;
        int unchanged = 0;

        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                scanned++;
                SubstitutionOutcome outcome = substituteFile(path, normalizedVariables);
                switch (outcome) {
                    case UPDATED -> updated++;
                    case UNCHANGED -> unchanged++;
                    case SKIPPED_BINARY -> skippedBinary++;
                    case SKIPPED_LARGE -> skippedLarge++;
                }
            }
        } catch (IOException ex) {
            throw new InstanceWorkspaceException(
                    "Failed to substitute variables for instance " + normalizedInstanceId + " at " + root,
                    ex
            );
        }

        VariableSubstitutionResult result = new VariableSubstitutionResult(
                scanned,
                updated,
                skippedBinary,
                skippedLarge,
                unchanged
        );
        logger.info(
                "Variable substitution complete. instanceId={} scanned={} updated={} skippedBinary={} skippedLarge={} unchanged={}",
                normalizedInstanceId,
                result.filesScanned(),
                result.filesUpdated(),
                result.filesSkippedBinary(),
                result.filesSkippedLarge(),
                result.filesUnchanged()
        );
        return result;
    }

    private SubstitutionOutcome substituteFile(Path path, Map<String, String> variables) {
        long size = readFileSize(path);
        if (maxFileBytes > 0 && size > maxFileBytes) {
            return SubstitutionOutcome.SKIPPED_LARGE;
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new InstanceWorkspaceException("Failed to read file for substitution: " + path, ex);
        }
        if (looksBinary(bytes)) {
            return SubstitutionOutcome.SKIPPED_BINARY;
        }

        String content = decodeUtf8(bytes);
        if (content == null) {
            return SubstitutionOutcome.SKIPPED_BINARY;
        }

        String substituted = substituteContent(content, variables);
        if (content.equals(substituted)) {
            return SubstitutionOutcome.UNCHANGED;
        }

        try {
            Files.write(
                    path,
                    substituted.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException ex) {
            throw new InstanceWorkspaceException("Failed to write substituted file: " + path, ex);
        }
        return SubstitutionOutcome.UPDATED;
    }

    private long readFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            throw new InstanceWorkspaceException("Failed to read file size for substitution: " + path, ex);
        }
    }

    private boolean looksBinary(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        for (byte value : bytes) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    private String decodeUtf8(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer buffer = decoder.decode(ByteBuffer.wrap(bytes));
            return buffer.toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    private String substituteContent(String content, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer builder = new StringBuffer(content.length());
        boolean changed = false;
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = variables.get(key);
            if (replacement == null) {
                matcher.appendReplacement(builder, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
                changed = true;
            }
        }
        if (!changed) {
            return content;
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    private Map<String, String> normalizeVariables(Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return Map.of();
        }
        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank() || value == null) {
                continue;
            }
            normalized.put(key.trim(), value);
        }
        return normalized;
    }

    private String requireValue(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new InstanceWorkspaceException(label + " is required");
        }
        return value.trim();
    }

    private enum SubstitutionOutcome {
        UPDATED,
        UNCHANGED,
        SKIPPED_BINARY,
        SKIPPED_LARGE
    }
}
