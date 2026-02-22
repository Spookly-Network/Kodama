package net.spookly.kodama.brain.config;

import jakarta.validation.constraints.Min;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "node")
public class NodeProperties {

  private static final String DEFAULT_STORE_TYPE = "PKCS12";
  private static final String HTTPS_SCHEME = "https";

  @Min(1)
  private int heartbeatIntervalSeconds = 30;

  @Min(1)
  private int heartbeatTimeoutSeconds = 90;

  @Min(1)
  private int heartbeatMonitorIntervalSeconds = 60;

  @Min(1)
  private int commandTimeoutSeconds = 10;

  @Min(1)
  private int commandMaxAttempts = 2;

  @Min(0)
  private long commandRetryBackoffMillis = 500;

  private Tls tls = new Tls();

  public int getHeartbeatIntervalSeconds() {
    return heartbeatIntervalSeconds;
  }

  public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
    this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
  }

  public int getHeartbeatTimeoutSeconds() {
    return heartbeatTimeoutSeconds;
  }

  public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
    this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
  }

  public int getHeartbeatMonitorIntervalSeconds() {
    return heartbeatMonitorIntervalSeconds;
  }

  public void setHeartbeatMonitorIntervalSeconds(int heartbeatMonitorIntervalSeconds) {
    this.heartbeatMonitorIntervalSeconds = heartbeatMonitorIntervalSeconds;
  }

  public int getCommandTimeoutSeconds() {
    return commandTimeoutSeconds;
  }

  public void setCommandTimeoutSeconds(int commandTimeoutSeconds) {
    this.commandTimeoutSeconds = commandTimeoutSeconds;
  }

  public int getCommandMaxAttempts() {
    return commandMaxAttempts;
  }

  public void setCommandMaxAttempts(int commandMaxAttempts) {
    this.commandMaxAttempts = commandMaxAttempts;
  }

  public long getCommandRetryBackoffMillis() {
    return commandRetryBackoffMillis;
  }

  public void setCommandRetryBackoffMillis(long commandRetryBackoffMillis) {
    this.commandRetryBackoffMillis = commandRetryBackoffMillis;
  }

  public Tls getTls() {
    return tls;
  }

  public void setTls(Tls tls) {
    this.tls = tls == null ? new Tls() : tls;
  }

  public void validateTlsConfiguration() {
    Tls configuredTls = getTls();
    if (!configuredTls.isEnabled()) {
      return;
    }

    List<String> errors = new ArrayList<>();
    addIfBlank(errors, configuredTls.getTrustStorePath(), "node.tls.trust-store-path is required");
    addIfBlank(
        errors, configuredTls.getTrustStorePassword(), "node.tls.trust-store-password is required");
    validateFilePath(
        errors,
        configuredTls.getTrustStorePath(),
        "node.tls.trust-store-path must point to a file");

    validatePairedValues(
        errors,
        configuredTls.getKeyStorePath(),
        configuredTls.getKeyStorePassword(),
        "node.tls.key-store-path is required when node.tls.key-store-password is set",
        "node.tls.key-store-password is required when node.tls.key-store-path is set");
    validateFilePath(
        errors, configuredTls.getKeyStorePath(), "node.tls.key-store-path must point to a file");

    if (!errors.isEmpty()) {
      throw new IllegalStateException(
          "Invalid node TLS configuration:\n- " + String.join("\n- ", errors));
    }
  }

  public void requireHttpsBaseUrl(String baseUrl, String propertyName) {
    if (!getTls().isEnabled() || !hasText(baseUrl)) {
      return;
    }
    URI uri;
    try {
      uri = URI.create(baseUrl);
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("Invalid " + propertyName + ": " + baseUrl, ex);
    }
    String scheme = uri.getScheme();
    if (scheme == null || !HTTPS_SCHEME.equalsIgnoreCase(scheme)) {
      throw new IllegalStateException(
          propertyName + " must use https:// when node.tls.enabled=true");
    }
  }

  private void addIfBlank(List<String> errors, String value, String message) {
    if (!hasText(value)) {
      errors.add(message);
    }
  }

  private void validateFilePath(List<String> errors, String value, String message) {
    if (!hasText(value)) {
      return;
    }
    try {
      Path path = Path.of(value);
      if (!Files.isRegularFile(path)) {
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
    boolean hasFirstValue = hasText(firstValue);
    boolean hasSecondValue = hasText(secondValue);
    if (hasFirstValue == hasSecondValue) {
      return;
    }
    errors.add(hasFirstValue ? secondMissingMessage : firstMissingMessage);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public static class Tls {

    private boolean enabled;
    private String trustStorePath;
    private String trustStorePassword;
    private String trustStoreType = DEFAULT_STORE_TYPE;
    private String keyStorePath;
    private String keyStorePassword;
    private String keyStoreType = DEFAULT_STORE_TYPE;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getTrustStorePath() {
      return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
      this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
      return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
      this.trustStorePassword = trustStorePassword;
    }

    public String getTrustStoreType() {
      return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) {
      this.trustStoreType = trustStoreType;
    }

    public String getKeyStorePath() {
      return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
      this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
      return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStoreType() {
      return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
      this.keyStoreType = keyStoreType;
    }
  }
}
