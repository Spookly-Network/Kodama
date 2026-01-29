package net.spookly.kodama.nodeagent.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.registration.NodeAuthTokenReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
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
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void missingTokenOnCommandEndpointIsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/instances/11111111-1111-1111-1111-111111111111/start"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    @Test
    void invalidTokenOnCommandEndpointIsUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/instances/11111111-1111-1111-1111-111111111111/start"
        );
        request.addHeader("X-Node-Token", "invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    @Test
    void nonCommandEndpointSkipsAuthChecks() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
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
        MockFilterChain chain = new MockFilterChain();

        missingConfigFilter.doFilter(request, response, chain);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatus());
    }
}
