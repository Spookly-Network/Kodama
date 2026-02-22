package net.spookly.kodama.nodelauncher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AgentProcessManager {

  public Process start(LauncherConfig config, Path agentJar) throws IOException {
    List<String> command = new ArrayList<>();
    command.add(config.javaBin());
    command.add("-jar");
    command.add(agentJar.toString());
    command.addAll(config.agentArgs());

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(config.installDirPath().toFile());
    processBuilder.inheritIO();
    return processBuilder.start();
  }
}
