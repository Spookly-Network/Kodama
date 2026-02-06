package net.spookly.kodama.nodeagent.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.springframework.stereotype.Component;

@Component
public class BrainAuthCertificateReader {

    private final NodeConfig config;

    public BrainAuthCertificateReader(NodeConfig config) {
        this.config = config;
    }

    public X509Certificate readCertificate() {
        String certPath = config.getAuth().getCertPath();
        if (certPath == null || certPath.isBlank()) {
            return null;
        }
        Path path = Path.of(certPath);
        if (!Files.exists(path)) {
            throw new BrainAuthException("Brain auth certificate file does not exist: " + certPath);
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate certificate = factory.generateCertificate(inputStream);
            if (certificate instanceof X509Certificate x509Certificate) {
                return x509Certificate;
            }
            throw new BrainAuthException("Brain auth certificate is not an X.509 certificate: " + certPath);
        } catch (IOException | CertificateException ex) {
            throw new BrainAuthException("Failed to read brain auth certificate file: " + certPath, ex);
        }
    }
}
