package net.spookly.kodama.nodelauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentProcessManagerTest {

  @TempDir Path tempDir;

  @Test
  void shouldUseAgentDirectoryAsWorkingDirectory() {
    Path installDir = tempDir.resolve("install");
    Path agentLink = installDir.resolve("agent").resolve("current");
    LauncherConfig config = createConfig(installDir);
    AgentProcessManager manager = new AgentProcessManager();

    ProcessBuilder processBuilder = manager.prepareProcessBuilder(config, agentLink);

    assertEquals(installDir.resolve("agent").toFile(), processBuilder.directory());
    assertEquals(
        List.of("java", "-jar", agentLink.toString(), "--brainUrl=http://localhost:8080"),
        processBuilder.command());
  }

  private LauncherConfig createConfig(Path installDir) {
    return new LauncherConfig(
        new LauncherConfig.GitHubConfig(
            "example-owner", "example-repo", LauncherConfig.Channel.STABLE, "agent-(.*)\\.jar"),
        LauncherConfig.VerifyConfig.defaults(),
        installDir.toString(),
        "java",
        List.of("--brainUrl=http://localhost:8080"),
        LauncherConfig.UpdateMode.NEXT_START);
  }
}
