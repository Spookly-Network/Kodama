package net.spookly.kodama.nodelauncher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LauncherConfig(
    GitHubConfig github,
    VerifyConfig verify,
    String installDir,
    String javaBin,
    List<String> agentArgs,
    UpdateMode updateMode) {

  public LauncherConfig {
    github = Objects.requireNonNull(github, "github configuration is required");
    verify = verify == null ? VerifyConfig.defaults() : verify;
    installDir = isBlank(installDir) ? "/opt/kodama-node" : installDir.trim();
    javaBin = isBlank(javaBin) ? "java" : javaBin.trim();
    agentArgs = agentArgs == null ? List.of() : List.copyOf(agentArgs);
    updateMode = updateMode == null ? UpdateMode.NEXT_START : updateMode;
  }

  public Path installDirPath() {
    return Path.of(installDir);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record GitHubConfig(String owner, String repo, Channel channel, String assetRegex) {

    public GitHubConfig {
      if (isBlank(owner)) {
        throw new IllegalArgumentException("github.owner is required");
      }
      if (isBlank(repo)) {
        throw new IllegalArgumentException("github.repo is required");
      }
      channel = channel == null ? Channel.STABLE : channel;
      if (isBlank(assetRegex)) {
        throw new IllegalArgumentException("github.assetRegex is required");
      }
      owner = owner.trim();
      repo = repo.trim();
      assetRegex = assetRegex.trim();
    }

    public Pattern assetPattern() {
      try {
        return Pattern.compile(assetRegex);
      } catch (PatternSyntaxException exception) {
        throw new IllegalArgumentException(
            "github.assetRegex is invalid: " + exception.getMessage(), exception);
      }
    }
  }

  public enum Channel {
    STABLE,
    BETA;

    @JsonCreator
    public static Channel fromValue(String value) {
      if (isBlank(value)) {
        return STABLE;
      }
      return Channel.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record VerifyConfig(boolean sha256Required, String sha256Suffix) {

    public VerifyConfig {
      sha256Suffix = isBlank(sha256Suffix) ? ".sha256" : sha256Suffix.trim();
    }

    public static VerifyConfig defaults() {
      return new VerifyConfig(true, ".sha256");
    }
  }

  public enum UpdateMode {
    NEXT_START;

    @JsonCreator
    public static UpdateMode fromValue(String value) {
      if (isBlank(value)) {
        return NEXT_START;
      }
      return UpdateMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
