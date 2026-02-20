package net.spookly.kodama.brain.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class WebConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource(BrainCorsProperties corsProperties) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    List<String> allowedOrigins = sanitize(corsProperties.getAllowedOrigins());
    if (allowedOrigins.isEmpty()) {
      return source;
    }
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(allowedOrigins);
    configuration.setAllowedMethods(sanitize(corsProperties.getAllowedMethods()));
    configuration.setAllowedHeaders(sanitize(corsProperties.getAllowedHeaders()));
    List<String> exposedHeaders = sanitize(corsProperties.getExposedHeaders());
    if (!exposedHeaders.isEmpty()) {
      configuration.setExposedHeaders(exposedHeaders);
    }
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());
    configuration.setMaxAge(corsProperties.getMaxAgeSeconds());
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private List<String> sanitize(List<String> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    List<String> sanitized = new ArrayList<>(values.size());
    for (String value : values) {
      if (value == null) {
        continue;
      }
      String trimmed = value.trim();
      if (!trimmed.isEmpty()) {
        sanitized.add(trimmed);
      }
    }
    return sanitized;
  }
}
