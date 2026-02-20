package net.spookly.kodama.plugins.hytale;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class HytaleAuthClient {

    private static final Logger logger = LoggerFactory.getLogger(HytaleAuthClient.class);

    private final HytaleAuthConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String refreshToken;

    HytaleAuthClient(HytaleAuthConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.getTimeout())
                .build();
        this.objectMapper = new ObjectMapper();
        this.refreshToken = config.getRefreshToken();
    }

    HytaleSession createSession() {
        String accessToken = refreshAccessToken();
        UUID profileUuid = resolveProfileUuid(accessToken);
        GameSessionResponse session = createGameSession(accessToken, profileUuid);
        if (session.sessionToken() == null || session.sessionToken().isBlank()) {
            throw new IllegalStateException("Game session response did not include sessionToken");
        }
        if (session.identityToken() == null || session.identityToken().isBlank()) {
            throw new IllegalStateException("Game session response did not include identityToken");
        }
        return new HytaleSession(session.sessionToken(), session.identityToken());
    }

    private String refreshAccessToken() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", config.getClientId());
        form.put("refresh_token", refreshToken);
//        form.put("scope", config.getScopes());

        HttpRequest request = HttpRequest.newBuilder(config.getTokenUrl())
                .timeout(config.getTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(form)))
                .build();

        logger.info("Sending OAuth token request to {}: {}; {}", config.getTokenUrl(), request, form);
        OAuthTokenResponse tokenResponse = send(request, OAuthTokenResponse.class, "oauth token");
        if (tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {
            logger.error("OAuth token response did not include access_token");
            throw new IllegalStateException("OAuth token response did not include access_token");
        }
        persistRefreshTokenIfRotated(tokenResponse.refreshToken());
        return tokenResponse.accessToken();
    }

    private UUID resolveProfileUuid(String accessToken) {
        if (config.getProfileUuid() != null) {
            return config.getProfileUuid();
        }
        ProfilesResponse response = fetchProfiles(accessToken);
        List<Profile> profiles = response.profiles();
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalStateException("No profiles returned by Hytale profile endpoint");
        }

        String username = config.getProfileUsername();
        if (username != null) {
            for (Profile profile : profiles) {
                if (profile.username() != null && profile.username().equals(username)) {
                    return parseProfileUuid(profile.uuid(), "profile username " + username);
                }
            }
            throw new IllegalStateException("No profile matched username " + username);
        }

        if (profiles.size() > 1) {
            logger.warn("Multiple Hytale profiles returned; selecting the first profile by default.");
        }
        return parseProfileUuid(profiles.get(0).uuid(), "first profile");
    }

    private ProfilesResponse fetchProfiles(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(config.getProfilesUrl())
                .timeout(config.getTimeout())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return send(request, ProfilesResponse.class, "profiles");
    }

    private GameSessionResponse createGameSession(String accessToken, UUID profileUuid) {
        String payload = writeJson(Map.of("uuid", profileUuid.toString()));
        HttpRequest request = HttpRequest.newBuilder(config.getSessionUrl())
                .timeout(config.getTimeout())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return send(request, GameSessionResponse.class, "game session");
    }

    private String formEncode(Map<String, String> form) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize JSON payload", ex);
        }
    }

    private void persistRefreshTokenIfRotated(String refreshToken) {
        String normalized = normalizeToken(refreshToken);
        if (normalized == null) {
            return;
        }
        if (normalized.equals(this.refreshToken)) {
            return;
        }
        config.persistRefreshToken(normalized);
        this.refreshToken = normalized;
        logger.info("Updated Hytale refresh token in config file");
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> T send(HttpRequest request, Class<T> responseType, String label) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to reach Hytale " + label + " endpoint", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Hytale " + label + " endpoint", ex);
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException(
                    "Hytale " + label + " endpoint returned status " + status
            );
        }

        try {
            return objectMapper.readValue(response.body(), responseType);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse Hytale " + label + " response", ex);
        }
    }

    private UUID parseProfileUuid(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Missing profile UUID for " + label);
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid profile UUID for " + label, ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OAuthTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") Integer expiresIn,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("scope") String scope,
            @JsonProperty("id_token") String idToken
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProfilesResponse(
            @JsonProperty("owner") String owner,
            @JsonProperty("profiles") List<Profile> profiles
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Profile(
            @JsonProperty("uuid") String uuid,
            @JsonProperty("username") String username,
            @JsonProperty("createdAt") String createdAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GameSessionResponse(
            @JsonProperty("sessionToken") String sessionToken,
            @JsonProperty("identityToken") String identityToken,
            @JsonProperty("expiresAt") String expiresAt
    ) {
    }

    record HytaleSession(String sessionToken, String identityToken) {
    }
}
