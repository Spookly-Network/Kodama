package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import net.spookly.kodama.nodeagent.config.InstanceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceInstallScriptRunnerTest {

  @TempDir Path tempDir;

  @Test
  void runScriptReturnsExitCodeForSuccessfulScript() throws Exception {
    InstanceInstallScriptRunner runner = runnerWithTimeoutSeconds(10);

    int exitCode =
        runner.runScript(tempDir, "test \"$TEST_FLAG\" = \"ok\"", Map.of("TEST_FLAG", "ok"));

    assertThat(exitCode).isZero();
  }

  @Test
  void runScriptCanCreateSentinelFileInWorkingDirectory() throws Exception {
    InstanceInstallScriptRunner runner = runnerWithTimeoutSeconds(10);
    Path sentinelFile = tempDir.resolve(".install-sentinel");

    int exitCode = runner.runScript(tempDir, "printf 'ready' > .install-sentinel", Map.of());

    assertThat(exitCode).isZero();
    assertThat(Files.exists(sentinelFile)).isTrue();
    assertThat(Files.readString(sentinelFile)).isEqualTo("ready");
  }

  @Test
  void runScriptTimesOutAndTerminatesHungScript() {
    InstanceInstallScriptRunner runner = runnerWithTimeoutSeconds(1);

    assertTimeoutPreemptively(
        Duration.ofSeconds(8),
        () ->
            assertThatThrownBy(
                    () -> runner.runScript(tempDir, "while true; do sleep 1; done", Map.of()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Install script timed out after 1 seconds"));
  }

  private InstanceInstallScriptRunner runnerWithTimeoutSeconds(int timeoutSeconds) {
    InstanceProperties properties = new InstanceProperties();
    properties.getInstanceRuntime().setInstallScriptTimeoutSeconds(timeoutSeconds);
    return new InstanceInstallScriptRunner(properties);
  }
}
