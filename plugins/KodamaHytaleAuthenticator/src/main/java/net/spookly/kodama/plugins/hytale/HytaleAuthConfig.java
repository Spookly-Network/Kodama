package net.spookly.kodama.plugins.hytale;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class HytaleAuthConfig {

    public static final String ENV_REFRESH_TOKEN = "HYTALE_AUTH_REFRESH_TOKEN";
    public static final String ENV_TOKEN_URL = "HYTALE_AUTH_TOKEN_URL";
    public static final String ENV_PROFILES_URL = "HYTALE_AUTH_PROFILES_URL";
    public static final String ENV_SESSION_URL = "HYTALE_AUTH_SESSION_URL";
    public static final String ENV_CLIENT_ID = "HYTALE_AUTH_CLIENT_ID";
    public static final String ENV_SCOPES = "HYTALE_AUTH_SCOPES";
    public static final String ENV_PROFILE_UUID = "HYTALE_AUTH_PROFILE_UUID";
    public static final String ENV_PROFILE_USERNAME = "HYTALE_AUTH_PROFILE_USERNAME";
    public static final String ENV_TIMEOUT_SECONDS = "HYTALE_AUTH_TIMEOUT_SECONDS";

    private final String refreshToken;
    private final URI tokenUrl;
    private final URI profilesUrl;
    private final URI sessionUrl;
    private final String clientId;
    private final String scopes;
    private final UUID profileUuid;
    private final String profileUsername;
    private final Duration timeout;

    private HytaleAuthConfig(
            String refreshToken,
            URI tokenUrl,
            URI profilesUrl,
            URI sessionUrl,
            String clientId,
            String scopes,
            UUID profileUuid,
            String profileUsername,
            Duration timeout
    ) {
        this.refreshToken = refreshToken;
        this.tokenUrl = tokenUrl;
        this.profilesUrl = profilesUrl;
        this.sessionUrl = sessionUrl;
        this.clientId = clientId;
        this.scopes = scopes;
        this.profileUuid = profileUuid;
        this.profileUsername = profileUsername;
        this.timeout = timeout;
    }

    public static HytaleAuthConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static HytaleAuthConfig fromEnvironment(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");

        String refreshToken = readRequired(environment, ENV_REFRESH_TOKEN);
        URI tokenUrl = readRequiredUri(environment, ENV_TOKEN_URL);
        URI profilesUrl = readRequiredUri(environment, ENV_PROFILES_URL);
        URI sessionUrl = readRequiredUri(environment, ENV_SESSION_URL);

        String clientId = readOptional(environment, ENV_CLIENT_ID);
        if (clientId == null) {
            clientId = "hytale-server";
        }

        String scopes = readOptional(environment, ENV_SCOPES);
        if (scopes == null) {
            scopes = "openid offline auth:server";
        }

        UUID profileUuid = null;
        String rawProfileUuid = readOptional(environment, ENV_PROFILE_UUID);
        if (rawProfileUuid != null) {
            profileUuid = parseUuid(ENV_PROFILE_UUID, rawProfileUuid);
        }

        String profileUsername = readOptional(environment, ENV_PROFILE_USERNAME);

        Duration timeout = parseTimeout(environment);

        return new HytaleAuthConfig(
                refreshToken,
                tokenUrl,
                profilesUrl,
                sessionUrl,
                clientId,
                scopes,
                profileUuid,
                profileUsername,
                timeout
        );
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public URI getTokenUrl() {
        return tokenUrl;
    }

    public URI getProfilesUrl() {
        return profilesUrl;
    }

    public URI getSessionUrl() {
        return sessionUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getScopes() {
        return scopes;
    }

    public UUID getProfileUuid() {
        return profileUuid;
    }

    public String getProfileUsername() {
        return profileUsername;
    }

    public Duration getTimeout() {
        return timeout;
    }

    private static Duration parseTimeout(Map<String, String> environment) {
        String rawTimeout = readOptional(environment, ENV_TIMEOUT_SECONDS);
        if (rawTimeout == null) {
            return Duration.ofSeconds(10);
        }
        try {
            long value = Long.parseLong(rawTimeout);
            if (value <= 0) {
                throw new IllegalStateException(ENV_TIMEOUT_SECONDS + " must be greater than 0");
            }
            return Duration.ofSeconds(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException(ENV_TIMEOUT_SECONDS + " must be a number", ex);
        }
    }

    private static String readRequired(Map<String, String> environment, String key) {
        String value = readOptional(environment, key);
        if (value == null) {
            throw new IllegalStateException(key + " is required");
        }
        return value;
    }

    private static URI readRequiredUri(Map<String, String> environment, String key) {
        String value = readOptional(environment, key);
        if (value == null) {
            throw new IllegalStateException(key + " is required");
        }
        try {
            return URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(key + " must be a valid URI", ex);
        }
    }

    private static String readOptional(Map<String, String> environment, String key) {
        String value = environment.get(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static UUID parseUuid(String label, String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(label + " must be a valid UUID", ex);
        }
    }
}
