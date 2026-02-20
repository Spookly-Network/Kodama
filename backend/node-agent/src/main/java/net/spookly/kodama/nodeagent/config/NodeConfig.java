package net.spookly.kodama.nodeagent.config;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "node-agent")
public class NodeConfig {

    @Setter private String nodeId;
    @Setter private String nodeName;
    @Setter private String nodeVersion;
    @Setter private String region;
    @Setter private int capacitySlots;
    @Setter private boolean devMode;
    @Setter private String tags;
    @Setter private String baseUrl;
    @Setter private String brainBaseUrl;
    @Setter private String dockerHost;
    private Docker docker = new Docker();
    @Setter private String workspaceDir = "./data";
    @Setter private String cacheDir;
    @Setter private boolean registrationEnabled = true;
    @Setter private int heartbeatIntervalSeconds;
    private Auth auth = new Auth();
    private S3 s3 = new S3();
    private TemplateCacheCheck templateCacheCheck = new TemplateCacheCheck();
    private TemplateCacheLimits templateCacheLimits = new TemplateCacheLimits();
    private VariableSubstitution variableSubstitution = new VariableSubstitution();

    public void validate() {
        List<String> errors = new ArrayList<>();
        addIfBlank(errors, nodeName, "node-agent.node-name is required");
        addIfBlank(errors, nodeVersion, "node-agent.node-version is required");
        addIfBlank(errors, region, "node-agent.region is required");
        addIfBlank(errors, brainBaseUrl, "node-agent.brain-base-url is required");
        addIfBlank(errors, cacheDir, "node-agent.cache-dir is required");
        if (capacitySlots < 1) {
            errors.add("node-agent.capacity-slots must be at least 1");
        }
        if (heartbeatIntervalSeconds < 0) {
            errors.add("node-agent.heartbeat-interval-seconds must be 0 or greater");
        }
        if (templateCacheCheck != null && templateCacheCheck.isEnabled()) {
            addIfBlank(errors, templateCacheCheck.getTemplateId(), "node-agent.template-cache-check.template-id is required");
            addIfBlank(errors, templateCacheCheck.getVersion(), "node-agent.template-cache-check.version is required");
            addIfBlank(errors, templateCacheCheck.getChecksum(), "node-agent.template-cache-check.checksum is required");
        }
        if (templateCacheLimits == null) {
            errors.add("node-agent.template-cache-limits is required");
        } else {
            if (templateCacheLimits.getMaxExtractedBytes() <= 0) {
                errors.add("node-agent.template-cache-limits.max-extracted-bytes must be greater than 0");
            }
            if (templateCacheLimits.getMaxEntries() <= 0) {
                errors.add("node-agent.template-cache-limits.max-entries must be greater than 0");
            }
        }
        if (variableSubstitution == null) {
            errors.add("node-agent.variable-substitution is required");
        } else if (variableSubstitution.getMaxFileBytes() < 0) {
            errors.add("node-agent.variable-substitution.max-file-bytes must be 0 or greater");
        }
        if (docker == null) {
            errors.add("node-agent.docker is required");
        } else {
            if (docker.getConnectionTimeoutSeconds() <= 0) {
                errors.add("node-agent.docker.connection-timeout-seconds must be greater than 0");
            }
            if (docker.getResponseTimeoutSeconds() <= 0) {
                errors.add("node-agent.docker.response-timeout-seconds must be greater than 0");
            }
            Integer maxConnections = docker.getMaxConnections();
            if (maxConnections != null && maxConnections <= 0) {
                errors.add("node-agent.docker.max-connections must be greater than 0");
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid node-agent configuration:\n- " + String.join("\n- ", errors));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void addIfBlank(List<String> errors, String value, String message) {
        if (value == null || value.isBlank()) {
            errors.add(message);
        }
    }

    public void setDocker(Docker docker) {
        this.docker = docker == null ? new Docker() : docker;
    }

    public String getEffectiveDockerHost() {
        if (docker != null && docker.getHost() != null && !docker.getHost().isBlank()) {
            return docker.getHost();
        }
        return dockerHost;
    }

    public void setAuth(Auth auth) {
        this.auth = auth == null ? new Auth() : auth;
    }

    public void setS3(S3 s3) {
        this.s3 = s3 == null ? new S3() : s3;
    }

    public void setTemplateCacheCheck(TemplateCacheCheck templateCacheCheck) {
        this.templateCacheCheck = templateCacheCheck == null ? new TemplateCacheCheck() : templateCacheCheck;
    }

    public void setTemplateCacheLimits(TemplateCacheLimits templateCacheLimits) {
        this.templateCacheLimits = templateCacheLimits == null ? new TemplateCacheLimits() : templateCacheLimits;
    }

    public void setVariableSubstitution(VariableSubstitution variableSubstitution) {
        this.variableSubstitution = variableSubstitution == null ? new VariableSubstitution() : variableSubstitution;
    }

    @Setter
    @Getter
    public static class Auth {

        private String tokenPath;
        private String certPath;
        private String headerName = "X-Node-Token";

    }

    @Setter
    @Getter
    public static class S3 {

        private String endpoint;
        private String region;
        private String bucket;
        private String accessKey;
        private String secretKey;

    }

    @Setter
    @Getter
    public static class TemplateCacheCheck {

        private boolean enabled;
        private String templateId;
        private String version;
        private String checksum;

    }

    @Setter
    @Getter
    public static class TemplateCacheLimits {

        private long maxExtractedBytes = 10L * 1024 * 1024 * 1024;
        private int maxEntries = 100_000;

    }

    @Setter
    @Getter
    public static class VariableSubstitution {

        private long maxFileBytes = 1024 * 1024;

    }

    @Setter
    @Getter
    public static class Docker {

        private String host;
        private Boolean tlsVerify;
        private String certPath;
        private String apiVersion;
        private String configDir;
        private String context;
        private Integer maxConnections;
        private int connectionTimeoutSeconds = 5;
        private int responseTimeoutSeconds = 30;

    }
}
