package net.spookly.kodama.nodeagent.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.registration.NodeAuthTokenReader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BrainAuthFilter extends OncePerRequestFilter {

    private static final String CERTIFICATE_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";
    private static final String LEGACY_CERTIFICATE_ATTRIBUTE = "javax.servlet.request.X509Certificate";

    private final NodeConfig config;
    private final NodeAuthTokenReader tokenReader;
    private final BrainAuthCertificateReader certificateReader;

    public BrainAuthFilter(
            NodeConfig config,
            NodeAuthTokenReader tokenReader,
            BrainAuthCertificateReader certificateReader
    ) {
        this.config = config;
        this.tokenReader = tokenReader;
        this.certificateReader = certificateReader;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !BrainAuthRequestMatcher.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        NodeConfig.Auth auth = config.getAuth();
        String certPath = auth == null ? null : auth.getCertPath();
        if (certPath != null && !certPath.isBlank()) {
            handleCertificateAuth(request, response, filterChain);
            return;
        }
        handleTokenAuth(request, response, filterChain);
    }

    private void handleCertificateAuth(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {
        X509Certificate expectedCertificate;
        try {
            expectedCertificate = certificateReader.readCertificate();
        } catch (BrainAuthException ex) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
            return;
        }
        if (expectedCertificate == null) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Brain authentication certificate is not configured");
            return;
        }
        X509Certificate providedCertificate = resolveClientCertificate(request);
        if (providedCertificate == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing brain client certificate");
            return;
        }
        if (!certificatesMatch(expectedCertificate, providedCertificate)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid brain client certificate");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void handleTokenAuth(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {
        String expectedToken;
        try {
            expectedToken = tokenReader.readToken();
        } catch (RuntimeException ex) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
            return;
        }
        if (expectedToken == null || expectedToken.isBlank()) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Brain authentication token is not configured");
            return;
        }
        NodeConfig.Auth auth = config.getAuth();
        String headerName = auth == null ? null : auth.getHeaderName();
        if (headerName == null || headerName.isBlank()) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Brain authentication header is not configured");
            return;
        }
        String providedToken = request.getHeader(headerName);
        if (providedToken == null || providedToken.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing brain authentication token");
            return;
        }
        if (!tokensMatch(expectedToken, providedToken)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid brain authentication token");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private X509Certificate resolveClientCertificate(HttpServletRequest request) {
        Object candidate = request.getAttribute(CERTIFICATE_ATTRIBUTE);
        if (candidate == null) {
            candidate = request.getAttribute(LEGACY_CERTIFICATE_ATTRIBUTE);
        }
        if (candidate instanceof X509Certificate[] certificates && certificates.length > 0) {
            return certificates[0];
        }
        return null;
    }

    private boolean tokensMatch(String expectedToken, String providedToken) {
        byte[] expectedBytes = expectedToken.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = providedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }

    private boolean certificatesMatch(X509Certificate expected, X509Certificate provided) {
        try {
            return MessageDigest.isEqual(expected.getEncoded(), provided.getEncoded());
        } catch (CertificateEncodingException ex) {
            return false;
        }
    }
}
