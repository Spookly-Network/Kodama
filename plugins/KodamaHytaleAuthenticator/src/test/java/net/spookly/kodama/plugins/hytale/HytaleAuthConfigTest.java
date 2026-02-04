package net.spookly.kodama.plugins.hytale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HytaleAuthConfigTest {

    @Test
    void fromEnvironmentRequiresRefreshToken() {
        Map<String, String> env = baseEnv();
        env.remove(HytaleAuthConfig.ENV_REFRESH_TOKEN);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> HytaleAuthConfig.fromEnvironment(env)
        );

        assertEquals(HytaleAuthConfig.ENV_REFRESH_TOKEN + " is required", ex.getMessage());
    }

    @Test
    void fromEnvironmentParsesProfileUuid() {
        Map<String, String> env = baseEnv();
        UUID profileUuid = UUID.randomUUID();
        env.put(HytaleAuthConfig.ENV_PROFILE_UUID, profileUuid.toString());

        HytaleAuthConfig config = HytaleAuthConfig.fromEnvironment(env);

        assertEquals(profileUuid, config.getProfileUuid());
    }

    @Test
    void fromEnvironmentParsesTimeout() {
        Map<String, String> env = baseEnv();
        env.put(HytaleAuthConfig.ENV_TIMEOUT_SECONDS, "15");

        HytaleAuthConfig config = HytaleAuthConfig.fromEnvironment(env);

        assertEquals(Duration.ofSeconds(15), config.getTimeout());
    }

    @Test
    void fromEnvironmentDefaultsClientIdAndScopes() {
        Map<String, String> env = baseEnv();

        HytaleAuthConfig config = HytaleAuthConfig.fromEnvironment(env);

        assertNotNull(config.getClientId());
        assertNotNull(config.getScopes());
    }

    private Map<String, String> baseEnv() {
        Map<String, String> env = new HashMap<>();
        env.put(HytaleAuthConfig.ENV_REFRESH_TOKEN, "refresh-token");
        env.put(HytaleAuthConfig.ENV_TOKEN_URL, "https://auth.invalid/token");
        env.put(HytaleAuthConfig.ENV_PROFILES_URL, "https://auth.invalid/profiles");
        env.put(HytaleAuthConfig.ENV_SESSION_URL, "https://auth.invalid/session");
        return env;
    }
}
