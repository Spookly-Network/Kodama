package net.spookly.kodama.plugins.hytale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HytaleAuthConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void fromConfigFileRequiresRefreshToken() throws IOException {
        Path configPath = writeConfig("{\"tokenUrl\":\"https://auth.invalid/token\","
                + "\"profilesUrl\":\"https://auth.invalid/profiles\","
                + "\"sessionUrl\":\"https://auth.invalid/session\"}");
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> HytaleAuthConfig.fromConfigFile(configPath)
        );

        assertEquals("refreshToken is required", ex.getMessage());
    }

    @Test
    void fromConfigFileParsesTimeout() throws IOException {
        Path configPath = writeConfig("{\"refreshToken\":\"refresh-token\","
                + "\"tokenUrl\":\"https://auth.invalid/token\","
                + "\"profilesUrl\":\"https://auth.invalid/profiles\","
                + "\"sessionUrl\":\"https://auth.invalid/session\","
                + "\"timeoutSeconds\":15}");

        HytaleAuthConfig config = HytaleAuthConfig.fromConfigFile(configPath);

        assertEquals(Duration.ofSeconds(15), config.getTimeout());
    }

    @Test
    void fromConfigFileDefaultsClientIdAndScopes() throws IOException {
        Path configPath = writeConfig("{\"refreshToken\":\"refresh-token\","
                + "\"tokenUrl\":\"https://auth.invalid/token\","
                + "\"profilesUrl\":\"https://auth.invalid/profiles\","
                + "\"sessionUrl\":\"https://auth.invalid/session\"}");

        HytaleAuthConfig config = HytaleAuthConfig.fromConfigFile(configPath);

        assertEquals("hytale-server", config.getClientId());
        assertEquals("openid offline auth:server", config.getScopes());
    }

    @Test
    void persistRefreshTokenUpdatesConfigFile() throws IOException {
        Path configPath = writeConfig("{\"refreshToken\":\"refresh-token\","
                + "\"tokenUrl\":\"https://auth.invalid/token\","
                + "\"profilesUrl\":\"https://auth.invalid/profiles\","
                + "\"sessionUrl\":\"https://auth.invalid/session\"}");

        HytaleAuthConfig config = HytaleAuthConfig.fromConfigFile(configPath);
        config.persistRefreshToken("rotated-token");

        HytaleAuthConfig reloaded = HytaleAuthConfig.fromConfigFile(configPath);
        assertEquals("rotated-token", reloaded.getRefreshToken());
    }

    private Path writeConfig(String json) throws IOException {
        Path configPath = tempDir.resolve("hytale-auth.json");
        Files.writeString(configPath, json);
        return configPath;
    }
}
