package net.spookly.kodama.nodeagent.config;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "node-agent")
public class NodeConfig {

  private static final String HTTPS_SCHEME = "https";
  private static final String BRAIN_TLS_HTTPS_REQUIRED_MESSAGE =
      "node-agent.brain-base-url must use https:// when node-agent.brain-tls.enabled=true";
  private static final String DEFAULT_STORE_TYPE = "PKCS12";

  @Setter private String nodeId;
  @Setter private String nodeName;
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
  private BrainTls brainTls = new BrainTls();
  private S3 s3 = new S3();
  private TemplateCacheCheck templateCacheCheck = new TemplateCacheCheck();
  private TemplateCacheLimits templateCacheLimits = new TemplateCacheLimits();
  private VariableSubstitution variableSubstitution = new VariableSubstitution();

  public void validate() {
    List<String> errors = new ArrayList<>();
    addIfBlank(errors, nodeName, "node-agent.node-name is required");
    addIfBlank(errors, region, "node-agent.region is required");
    addIfBlank(errors, brainBaseUrl, "node-agent.brain-base-url is required");
    addIfBlank(errors, cacheDir, "node-agent.cache-dir is required");
    if (capacitySlots < 1) {
      errors.add("node-agent.capacity-slots must be at least 1");
    }
    if (heartbeatIntervalSeconds < 0) {
      errors.add("node-agent.heartbeat-interval-seconds must be 0 or greater");
    }
    validateBrainTls(errors);
    if (templateCacheCheck != null && templateCacheCheck.isEnabled()) {
      addIfBlank(
          errors,
          templateCacheCheck.getTemplateId(),
          "node-agent.template-cache-check.template-id is required");
      addIfBlank(
          errors,
          templateCacheCheck.getVersion(),
          "node-agent.template-cache-check.version is required");
      addIfBlank(
          errors,
          templateCacheCheck.getChecksum(),
          "node-agent.template-cache-check.checksum is required");
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
      throw new IllegalStateException(
          "Invalid node-agent configuration:\n- " + String.join("\n- ", errors));
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private void addIfBlank(List<String> errors, String value, String message) {
    if (isBlank(value)) {
      errors.add(message);
    }
  }

  private void validateBrainTls(List<String> errors) {
    if (!isBrainTlsEnabled()) {
      return;
    }
    BrainTls configuredBrainTls = brainTls;
    addIfBlank(
        errors,
        configuredBrainTls.getTrustStorePath(),
        "node-agent.brain-tls.trust-store-path is required when node-agent.brain-tls.enabled=true");
    addIfBlank(
        errors,
        configuredBrainTls.getTrustStorePassword(),
        "node-agent.brain-tls.trust-store-password is required when node-agent.brain-tls.enabled=true");
    validateStorePath(
        errors,
        configuredBrainTls.getTrustStorePath(),
        "node-agent.brain-tls.trust-store-path must point to a readable file");

    validatePairedValues(
        errors,
        configuredBrainTls.getKeyStorePath(),
        configuredBrainTls.getKeyStorePassword(),
        "node-agent.brain-tls.key-store-path is required when node-agent.brain-tls.key-store-password is set",
        "node-agent.brain-tls.key-store-password is required when node-agent.brain-tls.key-store-path is set");
    validateStorePath(
        errors,
        configuredBrainTls.getKeyStorePath(),
        "node-agent.brain-tls.key-store-path must point to a readable file");

    if (!isBlank(brainBaseUrl)) {
      try {
        requireHttpsBrainUri(URI.create(brainBaseUrl));
      } catch (IllegalArgumentException ex) {
        errors.add("node-agent.brain-base-url is invalid: " + brainBaseUrl);
      } catch (IllegalStateException ex) {
        errors.add(ex.getMessage());
      }
    }
  }

  private void validateStorePath(List<String> errors, String value, String message) {
    if (isBlank(value)) {
      return;
    }
    try {
      Path path = Path.of(value);
      if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
        errors.add(message + ": " + value);
      }
    } catch (RuntimeException ex) {
      errors.add(message + ": " + value);
    }
  }

  private void validatePairedValues(
      List<String> errors,
      String firstValue,
      String secondValue,
      String firstMissingMessage,
      String secondMissingMessage) {
    boolean hasFirstValue = !isBlank(firstValue);
    boolean hasSecondValue = !isBlank(secondValue);
    if (hasFirstValue == hasSecondValue) {
      return;
    }
    errors.add(hasFirstValue ? secondMissingMessage : firstMissingMessage);
  }

  public boolean isBrainTlsEnabled() {
    return brainTls != null && brainTls.isEnabled();
  }

  public void requireHttpsBrainUri(URI uri) {
    if (!isBrainTlsEnabled()) {
      return;
    }
    String scheme = uri.getScheme();
    if (isBlank(scheme) || !HTTPS_SCHEME.equalsIgnoreCase(scheme)) {
      throw new IllegalStateException(BRAIN_TLS_HTTPS_REQUIRED_MESSAGE);
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

  public void setBrainTls(BrainTls brainTls) {
    this.brainTls = brainTls == null ? new BrainTls() : brainTls;
  }

  public void setS3(S3 s3) {
    this.s3 = s3 == null ? new S3() : s3;
  }

  public void setTemplateCacheCheck(TemplateCacheCheck templateCacheCheck) {
    this.templateCacheCheck =
        templateCacheCheck == null ? new TemplateCacheCheck() : templateCacheCheck;
  }

  public void setTemplateCacheLimits(TemplateCacheLimits templateCacheLimits) {
    this.templateCacheLimits =
        templateCacheLimits == null ? new TemplateCacheLimits() : templateCacheLimits;
  }

  public void setVariableSubstitution(VariableSubstitution variableSubstitution) {
    this.variableSubstitution =
        variableSubstitution == null ? new VariableSubstitution() : variableSubstitution;
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
  public static class BrainTls {

    private boolean enabled;
    private String trustStorePath;
    private String trustStorePassword;
    private String trustStoreType = DEFAULT_STORE_TYPE;
    private String keyStorePath;
    private String keyStorePassword;
    private String keyStoreType = DEFAULT_STORE_TYPE;
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
