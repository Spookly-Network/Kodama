package net.spookly.kodama.nodelauncher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AgentProcessManager {

  public Process start(LauncherConfig config, Path agentJar) throws IOException {
    return prepareProcessBuilder(config, agentJar).start();
  }

  ProcessBuilder prepareProcessBuilder(LauncherConfig config, Path agentJar) {
    List<String> command = new ArrayList<>();
    command.add(config.javaBin());
    command.add("-jar");
    command.add(agentJar.toString());
    command.addAll(config.agentArgs());

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    Path agentWorkingDirectory = resolveAgentWorkingDirectory(config, agentJar);
    processBuilder.directory(agentWorkingDirectory.toFile());
    processBuilder.inheritIO();
    return processBuilder;
  }

  private Path resolveAgentWorkingDirectory(LauncherConfig config, Path agentJar) {
    Path normalizedAgentPath = agentJar.toAbsolutePath().normalize();
    Path agentDirectory = normalizedAgentPath.getParent();
    if (agentDirectory != null) {
      return agentDirectory;
    }
    return config.installDirPath().toAbsolutePath().normalize();
  }
}
