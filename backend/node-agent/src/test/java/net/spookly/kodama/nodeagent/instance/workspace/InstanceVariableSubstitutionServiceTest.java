package net.spookly.kodama.nodeagent.instance.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceVariableSubstitutionServiceTest {

    @TempDir
    Path tempDir;

    private final InstanceVariableSubstitutionService service = new InstanceVariableSubstitutionService();

    @Test
    void substitutesVariablesInTextFiles() throws Exception {
        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        Path config = workspace.resolve("server.properties");
        Files.writeString(config, "id=${INSTANCE_ID}\nname=${SERVER_NAME}");

        VariableSubstitutionResult result = service.substituteVariables(
                "instance-1",
                workspace,
                Map.of("INSTANCE_ID", "abc-123", "SERVER_NAME", "alpha")
        );

        assertThat(Files.readString(config)).isEqualTo("id=abc-123\nname=alpha");
        assertThat(result.filesScanned()).isEqualTo(1);
        assertThat(result.filesUpdated()).isEqualTo(1);
    }

    @Test
    void skipsBinaryFiles() throws Exception {
        Path workspace = tempDir.resolve("workspace-bin");
        Files.createDirectories(workspace);
        Path binary = workspace.resolve("world.dat");
        byte[] payload = new byte[] {0x00, 0x10, 0x20, 0x30, '$', '{', 'I', 'D', '}'};
        Files.write(binary, payload);

        VariableSubstitutionResult result = service.substituteVariables(
                "instance-2",
                workspace,
                Map.of("ID", "value")
        );

        assertThat(Files.readAllBytes(binary)).isEqualTo(payload);
        assertThat(result.filesSkippedBinary()).isEqualTo(1);
        assertThat(result.filesUpdated()).isEqualTo(0);
    }

    @Test
    void leavesUnknownVariablesUntouched() throws Exception {
        Path workspace = tempDir.resolve("workspace-unknown");
        Files.createDirectories(workspace);
        Path config = workspace.resolve("config.yml");
        Files.writeString(config, "value=${UNKNOWN}", StandardCharsets.UTF_8);

        VariableSubstitutionResult result = service.substituteVariables(
                "instance-3",
                workspace,
                Map.of("KNOWN", "value")
        );

        assertThat(Files.readString(config)).isEqualTo("value=${UNKNOWN}");
        assertThat(result.filesUpdated()).isEqualTo(0);
        assertThat(result.filesUnchanged()).isEqualTo(1);
    }
}
