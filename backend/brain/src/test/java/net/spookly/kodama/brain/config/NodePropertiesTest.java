package net.spookly.kodama.brain.config;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NodePropertiesTest {

  @TempDir Path tempDir;

  @Test
  void validateTlsConfigurationAcceptsDisabledTls() {
    NodeProperties properties = new NodeProperties();

    assertThatNoException().isThrownBy(properties::validateTlsConfiguration);
  }

  @Test
  void validateTlsConfigurationRejectsMissingTrustStore() {
    NodeProperties properties = new NodeProperties();
    properties.getTls().setEnabled(true);

    assertThatThrownBy(properties::validateTlsConfiguration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("node.tls.trust-store-path is required")
        .hasMessageContaining("node.tls.trust-store-password is required");
  }

  @Test
  void validateTlsConfigurationAcceptsConfiguredTrustStore() throws Exception {
    Path trustStore = createPkcs12Store("brain-node-truststore.p12", "secret");
    NodeProperties properties = new NodeProperties();
    properties.getTls().setEnabled(true);
    properties.getTls().setTrustStorePath(trustStore.toString());
    properties.getTls().setTrustStorePassword("secret");

    assertThatNoException().isThrownBy(properties::validateTlsConfiguration);
  }

  @Test
  void requireHttpsBaseUrlRejectsHttpWhenTlsEnabled() {
    NodeProperties properties = new NodeProperties();
    properties.getTls().setEnabled(true);

    assertThatThrownBy(() -> properties.requireHttpsBaseUrl("http://node-1.internal", "baseUrl"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("baseUrl must use https:// when node.tls.enabled=true");
  }

  @Test
  void requireHttpsBaseUrlAllowsHttpsWhenTlsEnabled() {
    NodeProperties properties = new NodeProperties();
    properties.getTls().setEnabled(true);

    assertThatNoException()
        .isThrownBy(() -> properties.requireHttpsBaseUrl("https://node-1.internal", "baseUrl"));
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
