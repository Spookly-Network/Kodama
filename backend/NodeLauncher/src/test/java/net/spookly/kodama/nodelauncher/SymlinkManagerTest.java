package net.spookly.kodama.nodelauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SymlinkManagerTest {

  @TempDir Path tempDir;

  @Test
  void shouldSwitchCurrentAndKeepPreviousLink() throws IOException {
    SymlinkManager symlinkManager = new SymlinkManager(tempDir);
    symlinkManager.ensureDirectoryStructure();

    Path firstJar = Files.writeString(tempDir.resolve("agent").resolve("agent-1.0.0.jar"), "1");
    Path secondJar = Files.writeString(tempDir.resolve("agent").resolve("agent-2.0.0.jar"), "2");

    symlinkManager.switchCurrent(firstJar);
    symlinkManager.switchCurrent(secondJar);

    assertEquals(
        secondJar.toAbsolutePath().normalize(),
        symlinkManager.resolveCurrentTarget().orElseThrow());
    assertEquals(
        firstJar.toAbsolutePath().normalize(),
        symlinkManager.resolvePreviousTarget().orElseThrow());
  }

  @Test
  void rollbackShouldReturnFalseWhenNoPreviousExists() throws IOException {
    SymlinkManager symlinkManager = new SymlinkManager(tempDir);
    symlinkManager.ensureDirectoryStructure();

    assertFalse(symlinkManager.rollbackToPrevious());
  }

  @Test
  void rollbackShouldRestorePreviousLink() throws IOException {
    SymlinkManager symlinkManager = new SymlinkManager(tempDir);
    symlinkManager.ensureDirectoryStructure();

    Path firstJar = Files.writeString(tempDir.resolve("agent").resolve("agent-1.0.0.jar"), "1");
    Path secondJar = Files.writeString(tempDir.resolve("agent").resolve("agent-2.0.0.jar"), "2");

    symlinkManager.switchCurrent(firstJar);
    symlinkManager.switchCurrent(secondJar);
    assertTrue(symlinkManager.rollbackToPrevious());

    assertEquals(
        firstJar.toAbsolutePath().normalize(), symlinkManager.resolveCurrentTarget().orElseThrow());
  }
}
