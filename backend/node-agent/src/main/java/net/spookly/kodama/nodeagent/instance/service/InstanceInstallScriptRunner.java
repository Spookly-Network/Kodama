package net.spookly.kodama.nodeagent.instance.service;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.spookly.kodama.nodeagent.config.InstanceProperties;
import org.springframework.stereotype.Component;

@Component
public class InstanceInstallScriptRunner {

  private static final Path POSIX_SHELL = Path.of("/bin/sh");
  private static final long TERMINATION_GRACE_SECONDS = 5L;

  private final int installScriptTimeoutSeconds;

  public InstanceInstallScriptRunner(InstanceProperties instanceProperties) {
    this.installScriptTimeoutSeconds =
        instanceProperties.getInstanceRuntime().getInstallScriptTimeoutSeconds();
  }

  public int runScript(Path workingDir, String script, Map<String, String> env)
      throws IOException, InterruptedException {
    if (!Files.isExecutable(POSIX_SHELL)) {
      throw new IOException(
          "Install script execution requires /bin/sh, but it is not available on this node");
    }
    ProcessBuilder processBuilder =
        new ProcessBuilder(POSIX_SHELL.toString(), "-c", script)
            .directory(workingDir.toFile())
            .redirectOutput(Redirect.INHERIT)
            .redirectError(Redirect.INHERIT);

    if (env != null) {
      for (Map.Entry<String, String> entry : env.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        if (key == null || key.isBlank() || value == null) {
          continue;
        }
        processBuilder.environment().put(key, value);
      }
    }

    Process process = processBuilder.start();
    if (process.waitFor(installScriptTimeoutSeconds, TimeUnit.SECONDS)) {
      return process.exitValue();
    }

    terminateProcess(process);
    throw new IOException(
        "Install script timed out after " + installScriptTimeoutSeconds + " seconds");
  }

  private void terminateProcess(Process process) throws IOException, InterruptedException {
    process.destroy();
    if (process.waitFor(TERMINATION_GRACE_SECONDS, TimeUnit.SECONDS)) {
      return;
    }

    process.destroyForcibly();
    if (!process.waitFor(TERMINATION_GRACE_SECONDS, TimeUnit.SECONDS)) {
      throw new IOException("Install script process did not terminate after timeout");
    }
  }
}
