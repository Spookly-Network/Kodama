package net.spookly.kodama.nodeagent.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.util.AntPathMatcher;

public final class BrainAuthRequestMatcher {

  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  private static final List<String> BRAIN_AUTH_PATHS =
      List.of("/api/instances/**", "/api/cache/**", "/api/node/dev-mode", "/api/node/dev-mode/**");

  private BrainAuthRequestMatcher() {}

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
    for (String pattern : BRAIN_AUTH_PATHS) {
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
