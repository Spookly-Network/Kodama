package net.spookly.kodama.brain.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.spookly.kodama.brain.config.BrainSecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class NodeAuthFilter extends OncePerRequestFilter {

    private static final String NODE_AUTHORITY = "ROLE_NODE";

    private final BrainSecurityProperties securityProperties;

    public NodeAuthFilter(BrainSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!securityProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!NodeAuthRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        BrainSecurityProperties.NodeAuth nodeAuth = securityProperties.getNode();
        String expectedToken = nodeAuth == null ? null : nodeAuth.getToken();
        if (expectedToken == null || expectedToken.isBlank()) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Node authentication is not configured");
            return;
        }
        String headerName = nodeAuth.getHeaderName();
        if (headerName == null || headerName.isBlank()) {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Node authentication header is not configured");
            return;
        }
        String providedToken = request.getHeader(headerName);
        if (providedToken == null || providedToken.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing node authentication token");
            return;
        }
        if (!tokensMatch(expectedToken, providedToken)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid node authentication token");
            return;
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "node",
                null,
                AuthorityUtils.createAuthorityList(NODE_AUTHORITY)
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean tokensMatch(String expectedToken, String providedToken) {
        byte[] expectedBytes = expectedToken.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = providedToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }
}
