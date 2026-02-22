package net.spookly.kodama.nodelauncher;

import java.io.IOException;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LauncherApplication {

  private static final Logger logger = Logger.getLogger(LauncherApplication.class.getName());
  private static final Pattern INSTALLED_AGENT_PATTERN = Pattern.compile("^agent-(.+)\\.jar$");
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration RELEASE_REQUEST_TIMEOUT = Duration.ofSeconds(20);
  private static final Duration DOWNLOAD_REQUEST_TIMEOUT = Duration.ofMinutes(2);
  private static final Duration CRASH_THRESHOLD = Duration.ofSeconds(30);
  private static final int CRASH_LIMIT = 3;

  public static void main(String[] args) {
    int exitCode = new LauncherApplication().run();
    System.exit(exitCode);
  }

  int run() {
    try {
      LauncherConfig config = new ConfigLoader().load();
      SymlinkManager symlinkManager = new SymlinkManager(config.installDirPath());
      symlinkManager.ensureDirectoryStructure();

      HttpClient httpClient =
          HttpClient.newBuilder()
              .connectTimeout(CONNECT_TIMEOUT)
              .followRedirects(HttpClient.Redirect.NORMAL)
              .build();
      GitHubReleaseClient releaseClient =
          new GitHubReleaseClient(httpClient, RELEASE_REQUEST_TIMEOUT);
      DownloadService downloadService = new DownloadService(httpClient, DOWNLOAD_REQUEST_TIMEOUT);
      ChecksumVerifier checksumVerifier = new ChecksumVerifier();

      checkForUpdates(config, symlinkManager, releaseClient, downloadService, checksumVerifier);
      return runAgent(config, symlinkManager, new AgentProcessManager());
    } catch (Exception exception) {
      logger.log(Level.SEVERE, "Launcher failed with an unrecoverable error", exception);
      return 1;
    }
  }

  private void checkForUpdates(
      LauncherConfig config,
      SymlinkManager symlinkManager,
      GitHubReleaseClient releaseClient,
      DownloadService downloadService,
      ChecksumVerifier checksumVerifier)
      throws IOException {
    Optional<Path> currentTarget = symlinkManager.resolveCurrentTarget();
    Optional<String> currentVersion = currentTarget.flatMap(this::extractInstalledVersion);
    currentVersion.ifPresent(
        version -> logger.info("Current installed node-agent version: " + version));

    Optional<ReleaseInfo> releaseInfo;
    try {
      releaseInfo = releaseClient.fetchRelease(config.github());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while checking GitHub releases", exception);
    } catch (IOException exception) {
      logger.log(
          Level.WARNING,
          "Failed to fetch GitHub release information. Keeping current agent.",
          exception);
      return;
    }

    if (releaseInfo.isEmpty()) {
      logger.info("No release available in selected GitHub channel.");
      return;
    }

    Optional<GitHubReleaseClient.ResolvedReleaseAsset> resolvedAsset =
        releaseClient.resolveAgentAsset(
            releaseInfo.get(), config.github().assetPattern(), config.verify().sha256Suffix());
    if (resolvedAsset.isEmpty()) {
      logger.warning("No release asset matched github.assetRegex. Keeping current agent.");
      return;
    }

    GitHubReleaseClient.ResolvedReleaseAsset updateCandidate = resolvedAsset.get();
    if (currentVersion.isPresent()
        && compareVersions(updateCandidate.version(), currentVersion.get()) <= 0) {
      logger.info(
          "No update required. Candidate version "
              + updateCandidate.version()
              + " is not newer than "
              + currentVersion.get());
      return;
    }

    logger.info("Update candidate found: " + updateCandidate.version());
    Path downloadedJar = null;
    try {
      downloadedJar =
          downloadService.downloadToTempFile(
              updateCandidate.agentAsset().downloadUrl(),
              symlinkManager.agentDirectory(),
              "agent-",
              ".jar");

      if (config.verify().sha256Required()) {
        ReleaseInfo.ReleaseAsset checksumAsset = updateCandidate.sha256Asset();
        if (checksumAsset == null) {
          throw new IOException(
              "SHA256 verification is required but no checksum asset was found for "
                  + updateCandidate.agentAsset().name());
        }
        String checksumContent = downloadService.downloadString(checksumAsset.downloadUrl());
        checksumVerifier.verifySha256(downloadedJar, checksumContent);
      } else if (updateCandidate.sha256Asset() != null) {
        String checksumContent =
            downloadService.downloadString(updateCandidate.sha256Asset().downloadUrl());
        checksumVerifier.verifySha256(downloadedJar, checksumContent);
      }

      Path installedJar = symlinkManager.installAgentJar(downloadedJar, updateCandidate.version());
      downloadedJar = null;
      symlinkManager.switchCurrent(installedJar);
      logger.info("Installed node-agent version " + updateCandidate.version());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while downloading release assets", exception);
    } catch (IOException exception) {
      logger.log(Level.WARNING, "Update failed; keeping current agent version.", exception);
    } finally {
      if (downloadedJar != null) {
        Files.deleteIfExists(downloadedJar);
      }
    }
  }

  private int runAgent(
      LauncherConfig config, SymlinkManager symlinkManager, AgentProcessManager processManager)
      throws IOException, InterruptedException {
    CrashMonitor crashMonitor = new CrashMonitor(CRASH_THRESHOLD, CRASH_LIMIT);
    boolean rollbackAttempted = false;
    boolean postRollbackRun = false;

    while (true) {
      Path agentJar =
          symlinkManager
              .resolveCurrentTarget()
              .orElseThrow(() -> new IOException("No current node-agent symlink configured."));
      if (!Files.isRegularFile(agentJar)) {
        throw new IOException("Current node-agent jar does not exist: " + agentJar);
      }

      Path currentLink = symlinkManager.currentLinkPath();
      logger.info("Starting node-agent from " + currentLink + " -> " + agentJar);
      Instant startTime = Instant.now();
      Process process = processManager.start(config, currentLink);
      int exitCode = process.waitFor();
      Duration runtime = Duration.between(startTime, Instant.now());
      logger.warning(
          "Node-agent exited with code "
              + exitCode
              + " after "
              + runtime.toSeconds()
              + " seconds.");

      if (postRollbackRun) {
        return exitCode;
      }

      if (runtime.compareTo(crashMonitor.crashThreshold()) >= 0) {
        return exitCode;
      }

      if (!crashMonitor.recordExit(runtime)) {
        continue;
      }

      if (rollbackAttempted) {
        return exitCode;
      }

      logger.severe(
          "Detected repeated startup crashes. Rolling back to previous node-agent version.");
      if (!symlinkManager.rollbackToPrevious()) {
        logger.severe("Rollback failed because no previous symlink target was available.");
        return exitCode;
      }

      rollbackAttempted = true;
      postRollbackRun = true;
      crashMonitor.reset();
    }
  }

  private Optional<String> extractInstalledVersion(Path agentJar) {
    String fileName = agentJar.getFileName().toString();
    Matcher matcher = INSTALLED_AGENT_PATTERN.matcher(fileName);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of(matcher.group(1));
  }

  static int compareVersions(String candidateVersion, String currentVersion) {
    List<String> candidateTokens = tokenizeVersion(candidateVersion);
    List<String> currentTokens = tokenizeVersion(currentVersion);

    int minSize = Math.min(candidateTokens.size(), currentTokens.size());
    for (int index = 0; index < minSize; index++) {
      int comparison = compareVersionToken(candidateTokens.get(index), currentTokens.get(index));
      if (comparison != 0) {
        return comparison;
      }
    }

    if (candidateTokens.size() == currentTokens.size()) {
      return 0;
    }

    if (candidateTokens.size() > currentTokens.size()) {
      return containsSignificantToken(candidateTokens, minSize) ? 1 : 0;
    }
    return containsSignificantToken(currentTokens, minSize) ? -1 : 0;
  }

  private static List<String> tokenizeVersion(String version) {
    if (version == null || version.isBlank()) {
      return List.of("0");
    }

    String[] rawTokens = version.trim().split("[^0-9A-Za-z]+");
    List<String> tokens = new ArrayList<>();
    for (String token : rawTokens) {
      if (!token.isBlank()) {
        tokens.add(token.toLowerCase(Locale.ROOT));
      }
    }
    return tokens.isEmpty() ? List.of("0") : List.copyOf(tokens);
  }

  private static int compareVersionToken(String left, String right) {
    boolean leftNumeric = left.chars().allMatch(Character::isDigit);
    boolean rightNumeric = right.chars().allMatch(Character::isDigit);

    if (leftNumeric && rightNumeric) {
      return new BigInteger(left).compareTo(new BigInteger(right));
    }
    if (leftNumeric) {
      return 1;
    }
    if (rightNumeric) {
      return -1;
    }
    return left.compareTo(right);
  }

  private static boolean containsSignificantToken(List<String> tokens, int startIndex) {
    for (int index = startIndex; index < tokens.size(); index++) {
      String token = tokens.get(index);
      boolean numeric = token.chars().allMatch(Character::isDigit);
      if (!numeric) {
        return true;
      }
      if (!new BigInteger(token).equals(BigInteger.ZERO)) {
        return true;
      }
    }
    return false;
  }
}
