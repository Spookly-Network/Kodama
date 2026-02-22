package net.spookly.kodama.nodeagent.config;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NodeConfigTest {

  @TempDir Path tempDir;

  @Test
  void validateFailsWhenRequiredConfigMissing() {
    NodeConfig config = new NodeConfig();

    assertThatThrownBy(config::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("node-agent.node-name is required")
        .hasMessageContaining("node-agent.region is required")
        .hasMessageContaining("node-agent.brain-base-url is required")
        .hasMessageContaining("node-agent.cache-dir is required")
        .hasMessageContaining("node-agent.capacity-slots must be at least 1");
  }

  @Test
  void validateAcceptsRequiredConfig() {
    NodeConfig config = new NodeConfig();
    config.setNodeName("Node 1");
    config.setRegion("local");
    config.setCapacitySlots(4);
    config.setBrainBaseUrl("http://brain:8080");
    config.setCacheDir("./cache");

    assertThatNoException().isThrownBy(config::validate);
  }

  @Test
  void validateRejectsNegativeHeartbeatInterval() {
    NodeConfig config = new NodeConfig();
    config.setNodeName("Node 1");
    config.setRegion("local");
    config.setCapacitySlots(4);
    config.setBrainBaseUrl("http://brain:8080");
    config.setCacheDir("./cache");
    config.setHeartbeatIntervalSeconds(-1);

    assertThatThrownBy(config::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("node-agent.heartbeat-interval-seconds must be 0 or greater");
  }

  @Test
  void validateRejectsTemplateCacheCheckWhenMissingFields() {
    NodeConfig config = new NodeConfig();
    config.setNodeName("Node 1");
    config.setRegion("local");
    config.setCapacitySlots(4);
    config.setBrainBaseUrl("http://brain:8080");
    config.setCacheDir("./cache");
    config.getTemplateCacheCheck().setEnabled(true);

    assertThatThrownBy(config::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("node-agent.template-cache-check.template-id is required")
        .hasMessageContaining("node-agent.template-cache-check.version is required")
        .hasMessageContaining("node-agent.template-cache-check.checksum is required");
  }

  @Test
  void validateRejectsNegativeVariableSubstitutionLimit() {
    NodeConfig config = new NodeConfig();
    config.setNodeName("Node 1");
    config.setRegion("local");
    config.setCapacitySlots(4);
    config.setBrainBaseUrl("http://brain:8080");
    config.setCacheDir("./cache");
    config.getVariableSubstitution().setMaxFileBytes(-1);

    assertThatThrownBy(config::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "node-agent.variable-substitution.max-file-bytes must be 0 or greater");
  }

  @Test
  void validateAcceptsTemplateCacheCheckWhenConfigured() {
    NodeConfig config = new NodeConfig();
    config.setNodeName("Node 1");
    config.setRegion("local");
    config.setCapacitySlots(4);
    config.setBrainBaseUrl("http://brain:8080");
    config.setCacheDir("./cache");
    config.getTemplateCacheCheck().setEnabled(true);
    config.getTemplateCacheCheck().setTemplateId("starter");
    config.getTemplateCacheCheck().setVersion("1.2.3");
    config.getTemplateCacheCheck().setChecksum("abc123");

    assertThatNoException().isThrownBy(config::validate);
  }

  @Test
  void validateRejectsDockerTimeoutsWhenNonPositive() {
    NodeConfig config = new NodeConfig();
    config.setNodeName("Node 1");
    config.setRegion("local");
    config.setCapacitySlots(4);
    config.setBrainBaseUrl("http://brain:8080");
    config.setCacheDir("./cache");
    config.getDocker().setConnectionTimeoutSeconds(0);
    config.getDocker().setResponseTimeoutSeconds(0);

    assertThatThrownBy(config::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("node-agent.docker.connection-timeout-seconds must be greater than 0")
        .hasMessageContaining("node-agent.docker.response-timeout-seconds must be greater than 0");
  }

  @Test
  void validateAcceptsDockerTlsVerifyWhenCertConfigMissing() {
    NodeConfig config = new NodeConfig();
    config.setNodeName("Node 1");
    config.setRegion("local");
    config.setCapacitySlots(4);
    config.setBrainBaseUrl("http://brain:8080");
    config.setCacheDir("./cache");
    config.getDocker().setTlsVerify(true);
    config.getDocker().setCertPath("");
    config.getDocker().setConfigDir("");

    assertThatNoException().isThrownBy(config::validate);
  }

  @Test
  void validateAcceptsBrainTlsWithHttpsBaseUrlAndTrustStore() throws Exception {
    NodeConfig config = validConfig();
    Path trustStore = createPkcs12Store("truststore.p12", "secret");
    config.setBrainBaseUrl("https://brain:8443");
    config.getBrainTls().setEnabled(true);
    config.getBrainTls().setTrustStorePath(trustStore.toString());
    config.getBrainTls().setTrustStorePassword("secret");

    assertThatNoException().isThrownBy(config::validate);
  }

  @Test
  void validateRejectsBrainTlsWhenBaseUrlIsNotHttps() throws Exception {
    NodeConfig config = validConfig();
    Path trustStore = createPkcs12Store("truststore-http.p12", "secret");
    config.setBrainBaseUrl("http://brain:8080");
    config.getBrainTls().setEnabled(true);
    config.getBrainTls().setTrustStorePath(trustStore.toString());
    config.getBrainTls().setTrustStorePassword("secret");

    assertThatThrownBy(config::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "node-agent.brain-base-url must use https:// when node-agent.brain-tls.enabled=true");
  }

  @Test
  void validateRejectsBrainTlsWhenTrustStorePathMissing() {
    NodeConfig config = validConfig();
    config.setBrainBaseUrl("https://brain:8443");
    config.getBrainTls().setEnabled(true);
    config.getBrainTls().setTrustStorePassword("secret");

    assertThatThrownBy(config::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("node-agent.brain-tls.trust-store-path is required");
  }

  @Test
  void validateRejectsBrainTlsWhenTrustStorePasswordMissing() throws Exception {
    NodeConfig config = validConfig();
    Path trustStore = createPkcs12Store("truststore-missing-password.p12", "secret");
    config.setBrainBaseUrl("https://brain:8443");
    config.getBrainTls().setEnabled(true);
    config.getBrainTls().setTrustStorePath(trustStore.toString());

    assertThatThrownBy(config::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("node-agent.brain-tls.trust-store-password is required");
  }

  @Test
  void validateRejectsBrainTlsWhenTrustStorePathInvalid() {
    NodeConfig config = validConfig();
    config.setBrainBaseUrl("https://brain:8443");
    config.getBrainTls().setEnabled(true);
    config.getBrainTls().setTrustStorePath(tempDir.resolve("missing-truststore.p12").toString());
    config.getBrainTls().setTrustStorePassword("secret");

    assertThatThrownBy(config::validate)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(
            "node-agent.brain-tls.trust-store-path must point to a readable file");
  }

  private NodeConfig validConfig() {
    NodeConfig config = new NodeConfig();
    config.setNodeName("Node 1");
    config.setRegion("local");
    config.setCapacitySlots(4);
    config.setBrainBaseUrl("http://brain:8080");
    config.setCacheDir("./cache");
    return config;
  }

  private Path createPkcs12Store(String fileName, String password) throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(null, password.toCharArray());
    Path path = tempDir.resolve(fileName);
    try (OutputStream outputStream = Files.newOutputStream(path)) {
      keyStore.store(outputStream, password.toCharArray());
    }
    return path;
  }
}
