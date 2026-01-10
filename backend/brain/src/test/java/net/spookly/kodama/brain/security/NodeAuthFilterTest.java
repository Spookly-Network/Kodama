package net.spookly.kodama.brain.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.ServletException;
import java.io.IOException;

import net.spookly.kodama.brain.config.BrainSecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class NodeAuthFilterTest {

    private NodeAuthFilter filter;

    @BeforeEach
    void setUp() {
        BrainSecurityProperties securityProperties = new BrainSecurityProperties();
        securityProperties.setEnabled(true);
        BrainSecurityProperties.NodeAuth nodeAuth = new BrainSecurityProperties.NodeAuth();
        nodeAuth.setToken("test-node-token");
        nodeAuth.setHeaderName("X-Node-Token");
        securityProperties.setNode(nodeAuth);
        filter = new NodeAuthFilter(securityProperties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesNode() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/nodes/register");
        request.addHeader("X-Node-Token", "test-node-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_NODE")));
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }

    @Test
    void missingTokenOnNodeEndpointIsUnauthorized() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/nodes/register");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    @Test
    void invalidTokenOnNodeEndpointIsUnauthorized() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/nodes/register");
        request.addHeader("X-Node-Token", "invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    @Test
    void nonNodeEndpointSkipsTokenCheck() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/instances");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }
}
