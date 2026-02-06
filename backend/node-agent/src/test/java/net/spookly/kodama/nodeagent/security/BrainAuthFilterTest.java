package net.spookly.kodama.nodeagent.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.registration.NodeAuthTokenReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class BrainAuthFilterTest {

    @TempDir
    Path tempDir;

    private BrainAuthFilter filter;
    private String token;

    @BeforeEach
    void setUp() throws IOException {
        token = "test-brain-token";
        NodeConfig config = new NodeConfig();
        NodeConfig.Auth auth = new NodeConfig.Auth();
        auth.setHeaderName("X-Node-Token");
        Path tokenPath = tempDir.resolve("token.txt");
        Files.writeString(tokenPath, token, StandardCharsets.UTF_8);
        auth.setTokenPath(tokenPath.toString());
        config.setAuth(auth);
        filter = new BrainAuthFilter(
                config,
                new NodeAuthTokenReader(config),
                new BrainAuthCertificateReader(config)
        );
    }

    @Test
    void validTokenOnCommandEndpointAllowsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/instances/11111111-1111-1111-1111-111111111111/start"
        );
        request.addHeader("X-Node-Token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertTrue(chain.wasInvoked());
    }

    @Test
    void missingTokenOnCommandEndpointIsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/instances/11111111-1111-1111-1111-111111111111/start"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertFalse(chain.wasInvoked());
    }

    @Test
    void invalidTokenOnCommandEndpointIsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/instances/11111111-1111-1111-1111-111111111111/start"
        );
        request.addHeader("X-Node-Token", "invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertFalse(chain.wasInvoked());
    }

    @Test
    void nonCommandEndpointSkipsAuthChecks() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertTrue(chain.wasInvoked());
    }

    @Test
    void missingAuthConfigurationReturnsServerError() throws Exception {
        NodeConfig config = new NodeConfig();
        config.setAuth(new NodeConfig.Auth());
        BrainAuthFilter missingConfigFilter = new BrainAuthFilter(
                config,
                new NodeAuthTokenReader(config),
                new BrainAuthCertificateReader(config)
        );
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/instances/11111111-1111-1111-1111-111111111111/start"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        missingConfigFilter.doFilter(request, response, chain);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
        assertFalse(chain.wasInvoked());
    }

    @Test
    void validClientCertificateAllowsCommand() throws Exception {
        NodeConfig config = new NodeConfig();
        NodeConfig.Auth auth = new NodeConfig.Auth();
        auth.setCertPath("in-memory");
        config.setAuth(auth);

        X509Certificate expected = Mockito.mock(X509Certificate.class);
        X509Certificate provided = Mockito.mock(X509Certificate.class);
        byte[] certBytes = "cert-bytes".getBytes(StandardCharsets.UTF_8);
        Mockito.when(expected.getEncoded()).thenReturn(certBytes);
        Mockito.when(provided.getEncoded()).thenReturn(certBytes);

        BrainAuthFilter mtlsFilter = new BrainAuthFilter(
                config,
                new NodeAuthTokenReader(config),
                new BrainAuthCertificateReader(config) {
                    @Override
                    public X509Certificate readCertificate() {
                        return expected;
                    }
                }
        );

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/cache/purge"
        );
        request.setAttribute("jakarta.servlet.request.X509Certificate", new X509Certificate[]{provided});
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        mtlsFilter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertTrue(chain.wasInvoked());
    }

    @Test
    void invalidClientCertificateIsUnauthorized() throws Exception {
        NodeConfig config = new NodeConfig();
        NodeConfig.Auth auth = new NodeConfig.Auth();
        auth.setCertPath("in-memory");
        config.setAuth(auth);

        X509Certificate expected = Mockito.mock(X509Certificate.class);
        X509Certificate provided = Mockito.mock(X509Certificate.class);
        Mockito.when(expected.getEncoded()).thenReturn("expected".getBytes(StandardCharsets.UTF_8));
        Mockito.when(provided.getEncoded()).thenReturn("provided".getBytes(StandardCharsets.UTF_8));

        BrainAuthFilter mtlsFilter = new BrainAuthFilter(
                config,
                new NodeAuthTokenReader(config),
                new BrainAuthCertificateReader(config) {
                    @Override
                    public X509Certificate readCertificate() {
                        return expected;
                    }
                }
        );

        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/node/dev-mode"
        );
        request.setAttribute("jakarta.servlet.request.X509Certificate", new X509Certificate[]{provided});
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        mtlsFilter.doFilter(request, response, chain);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertFalse(chain.wasInvoked());
    }

    private static class RecordingFilterChain implements FilterChain {

        private boolean invoked;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            invoked = true;
        }

        private boolean wasInvoked() {
            return invoked;
        }
    }
}
