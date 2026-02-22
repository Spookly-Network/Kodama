package net.spookly.kodama.nodelauncher;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class SymlinkManager {

  private final Path installDir;
  private final Path launcherDir;
  private final Path agentDir;
  private final Path logsDir;
  private final Path currentLink;
  private final Path previousLink;

  public SymlinkManager(Path installDir) {
    this.installDir = installDir.toAbsolutePath().normalize();
    this.launcherDir = this.installDir.resolve("launcher");
    this.agentDir = this.installDir.resolve("agent");
    this.logsDir = this.installDir.resolve("logs");
    this.currentLink = this.agentDir.resolve("current");
    this.previousLink = this.agentDir.resolve("previous");
  }

  public void ensureDirectoryStructure() throws IOException {
    Files.createDirectories(installDir);
    Files.createDirectories(launcherDir);
    Files.createDirectories(agentDir);
    Files.createDirectories(logsDir);
  }

  public Path agentDirectory() {
    return agentDir;
  }

  public Path currentLinkPath() {
    return currentLink;
  }

  public Optional<Path> resolveCurrentTarget() throws IOException {
    return resolveLinkTarget(currentLink);
  }

  public Optional<Path> resolvePreviousTarget() throws IOException {
    return resolveLinkTarget(previousLink);
  }

  public Path installAgentJar(Path downloadedArtifact, String version) throws IOException {
    Path installedJar = agentDir.resolve("agent-" + version + ".jar");
    moveAtomically(downloadedArtifact, installedJar);
    return installedJar;
  }

  public void switchCurrent(Path newAgentJar) throws IOException {
    Path absoluteTarget = newAgentJar.toAbsolutePath().normalize();
    if (!Files.isRegularFile(absoluteTarget)) {
      throw new IOException("Agent jar does not exist: " + absoluteTarget);
    }

    Optional<Path> previousCurrentTarget = resolveCurrentTarget();
    if (previousCurrentTarget.isPresent()) {
      replaceLink(previousLink, previousCurrentTarget.get());
    }
    replaceLink(currentLink, absoluteTarget);
  }

  public boolean rollbackToPrevious() throws IOException {
    Optional<Path> previousTarget = resolvePreviousTarget();
    if (previousTarget.isEmpty()) {
      return false;
    }
    replaceLink(currentLink, previousTarget.get());
    return true;
  }

  private Optional<Path> resolveLinkTarget(Path link) throws IOException {
    if (!Files.exists(link)) {
      return Optional.empty();
    }
    if (!Files.isSymbolicLink(link)) {
      throw new IOException("Expected symlink at " + link + " but found a non-symlink");
    }
    Path rawTarget = Files.readSymbolicLink(link);
    Path resolvedTarget =
        rawTarget.isAbsolute()
            ? rawTarget.toAbsolutePath().normalize()
            : link.getParent().resolve(rawTarget).toAbsolutePath().normalize();
    return Optional.of(resolvedTarget);
  }

  private void replaceLink(Path linkPath, Path absoluteTarget) throws IOException {
    Files.createDirectories(linkPath.getParent());

    Path tempLink =
        linkPath.resolveSibling(
            linkPath.getFileName() + ".tmp-" + Long.toUnsignedString(System.nanoTime()));
    Files.deleteIfExists(tempLink);

    Path relativeTarget = relativeTarget(linkPath.getParent(), absoluteTarget);
    boolean created = false;
    try {
      Files.createSymbolicLink(tempLink, relativeTarget);
      created = true;
      moveAtomically(tempLink, linkPath);
    } finally {
      if (created && Files.exists(tempLink)) {
        Files.deleteIfExists(tempLink);
      }
    }
  }

  private Path relativeTarget(Path linkParent, Path absoluteTarget) {
    try {
      return linkParent.toAbsolutePath().normalize().relativize(absoluteTarget);
    } catch (IllegalArgumentException exception) {
      return absoluteTarget;
    }
  }

  private void moveAtomically(Path source, Path target) throws IOException {
    try {
      Files.move(
          source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException ignored) {
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
