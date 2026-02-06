package net.spookly.kodama.brain.security;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

public final class NodeAuthRequestMatcher {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> NODE_AUTH_PATHS = List.of(
            "/api/nodes/register",
            "/api/nodes/*/heartbeat",
            "/api/nodes/*/instances/*/prepared",
            "/api/nodes/*/instances/*/running",
            "/api/nodes/*/instances/*/stopped",
            "/api/nodes/*/instances/*/destroyed",
            "/api/nodes/*/instances/*/failed"
    );

    private NodeAuthRequestMatcher() {
    }

    public static boolean matches(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String requestUri = request.getRequestURI();
        if (matchesPath(requestUri)) {
            return true;
        }
        String normalized = normalizePath(requestUri, request.getContextPath());
        return matchesPath(normalized);
    }

    public static boolean matchesPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String pattern : NODE_AUTH_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizePath(String path, String contextPath) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            String normalized = path.substring(contextPath.length());
            return normalized.isEmpty() ? "/" : normalized;
        }
        return path;
    }
}
