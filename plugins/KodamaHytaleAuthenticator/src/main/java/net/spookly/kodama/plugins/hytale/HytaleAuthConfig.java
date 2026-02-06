package net.spookly.kodama.plugins.hytale;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class HytaleAuthConfig {

    public static final String ENV_CONFIG_PATH = "HYTALE_AUTH_CONFIG_PATH";
    private static final String DEFAULT_CONFIG_PATH = "./plugins/hytale-auth.json";
    private static final String DEFAULT_CLIENT_ID = "hytale-server";
    private static final String DEFAULT_SCOPES = "openid offline auth:server";
    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

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

    public static HytaleAuthConfig load() {
        Path configPath = resolveConfigPath();
        return fromConfigFile(configPath);
    }

    static HytaleAuthConfig fromConfigFile(Path configPath) {
        Objects.requireNonNull(configPath, "configPath");
        if (!Files.exists(configPath)) {
            throw new IllegalStateException("Hytale auth config file does not exist at " + configPath);
        }
        if (!Files.isRegularFile(configPath)) {
            throw new IllegalStateException("Hytale auth config path is not a file: " + configPath);
        }

        ObjectMapper mapper = new ObjectMapper();
        HytaleAuthFileConfig fileConfig;
        try {
            fileConfig = mapper.readValue(configPath.toFile(), HytaleAuthFileConfig.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read Hytale auth config at " + configPath, ex);
        }
        if (fileConfig == null) {
            throw new IllegalStateException("Hytale auth config is empty at " + configPath);
        }

        String refreshToken = requireText(fileConfig.refreshToken, "refreshToken");
        URI tokenUrl = parseUri(requireText(fileConfig.tokenUrl, "tokenUrl"), "tokenUrl");
        URI profilesUrl = parseUri(requireText(fileConfig.profilesUrl, "profilesUrl"), "profilesUrl");
        URI sessionUrl = parseUri(requireText(fileConfig.sessionUrl, "sessionUrl"), "sessionUrl");

        String clientId = readOptional(fileConfig.clientId);
        if (clientId == null) {
            clientId = DEFAULT_CLIENT_ID;
        }

        String scopes = readOptional(fileConfig.scopes);
        if (scopes == null) {
            scopes = DEFAULT_SCOPES;
        }

        UUID profileUuid = null;
        String rawProfileUuid = readOptional(fileConfig.profileUuid);
        if (rawProfileUuid != null) {
            profileUuid = parseUuid("profileUuid", rawProfileUuid);
        }

        String profileUsername = readOptional(fileConfig.profileUsername);

        Duration timeout = parseTimeoutSeconds(fileConfig.timeoutSeconds);

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

    private static Duration parseTimeoutSeconds(Long timeoutSeconds) {
        if (timeoutSeconds == null) {
            return Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalStateException("timeoutSeconds must be greater than 0");
        }
        return Duration.ofSeconds(timeoutSeconds);
    }

    private static String requireText(String value, String label) {
        String normalized = readOptional(value);
        if (normalized == null) {
            throw new IllegalStateException(label + " is required");
        }
        return normalized;
    }

    private static String readOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static URI parseUri(String rawValue, String label) {
        try {
            return URI.create(rawValue);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(label + " must be a valid URI", ex);
        }
    }

    private static UUID parseUuid(String label, String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(label + " must be a valid UUID", ex);
        }
    }

    private static Path resolveConfigPath() {
        String override = readOptional(System.getenv(ENV_CONFIG_PATH));
        if (override != null) {
            return Path.of(override).toAbsolutePath().normalize();
        }
        return Path.of(DEFAULT_CONFIG_PATH).toAbsolutePath().normalize();
    }

    private static final class HytaleAuthFileConfig {
        public String refreshToken;
        public String tokenUrl;
        public String profilesUrl;
        public String sessionUrl;
        public String clientId;
        public String scopes;
        public String profileUuid;
        public String profileUsername;
        public Long timeoutSeconds;
    }
}
