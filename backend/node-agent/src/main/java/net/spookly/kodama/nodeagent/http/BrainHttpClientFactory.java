package net.spookly.kodama.nodeagent.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import net.spookly.kodama.nodeagent.config.NodeConfig;

public final class BrainHttpClientFactory {

  private static final String DEFAULT_STORE_TYPE = "PKCS12";
  private static final String TLS_PROTOCOL = "TLS";

  private BrainHttpClientFactory() {}

  public static HttpClient create(NodeConfig config, Duration connectTimeout) {
    HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(connectTimeout);
    NodeConfig.BrainTls brainTls = config.getBrainTls();
    if (brainTls != null && brainTls.isEnabled()) {
      builder.sslContext(createSslContext(brainTls));
    }
    return builder.build();
  }

  private static SSLContext createSslContext(NodeConfig.BrainTls tls) {
    try {
      KeyStore trustStore =
          loadKeyStore(
              tls.getTrustStorePath(),
              tls.getTrustStorePassword(),
              resolveStoreType(tls.getTrustStoreType()));
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);

      KeyManagerFactory keyManagerFactory = null;
      if (hasText(tls.getKeyStorePath())) {
        KeyStore keyStore =
            loadKeyStore(
                tls.getKeyStorePath(),
                tls.getKeyStorePassword(),
                resolveStoreType(tls.getKeyStoreType()));
        keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, tls.getKeyStorePassword().toCharArray());
      }

      SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
      sslContext.init(
          keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(),
          trustManagerFactory.getTrustManagers(),
          null);
      return sslContext;
    } catch (GeneralSecurityException | IOException ex) {
      throw new IllegalStateException(
          "Failed to initialize node-agent.brain-tls SSL context for Brain outbound calls", ex);
    }
  }

  private static KeyStore loadKeyStore(String path, String password, String type)
      throws GeneralSecurityException, IOException {
    try (InputStream inputStream = Files.newInputStream(Path.of(path))) {
      KeyStore keyStore = KeyStore.getInstance(type);
      keyStore.load(inputStream, password.toCharArray());
      return keyStore;
    }
  }

  private static String resolveStoreType(String configuredType) {
    if (!hasText(configuredType)) {
      return DEFAULT_STORE_TYPE;
    }
    return configuredType;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
