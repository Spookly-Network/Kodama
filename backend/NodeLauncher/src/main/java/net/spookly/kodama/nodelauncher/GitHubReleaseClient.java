package net.spookly.kodama.nodelauncher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GitHubReleaseClient {

  private static final String GITHUB_ACCEPT_HEADER = "application/vnd.github+json";
  private static final String GITHUB_USER_AGENT = "kodama-node-launcher";
  private static final String RELEASES_URL_TEMPLATE = "https://api.github.com/repos/%s/%s/releases";

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final Duration requestTimeout;

  public GitHubReleaseClient(HttpClient httpClient, Duration requestTimeout) {
    this.httpClient = httpClient;
    this.objectMapper = new ObjectMapper().findAndRegisterModules();
    this.requestTimeout = requestTimeout;
  }

  public Optional<ReleaseInfo> fetchRelease(LauncherConfig.GitHubConfig githubConfig)
      throws IOException, InterruptedException {
    URI releasesUri =
        URI.create(RELEASES_URL_TEMPLATE.formatted(githubConfig.owner(), githubConfig.repo()));

    HttpRequest request =
        HttpRequest.newBuilder(releasesUri)
            .timeout(requestTimeout)
            .GET()
            .header("Accept", GITHUB_ACCEPT_HEADER)
            .header("User-Agent", GITHUB_USER_AGENT)
            .build();

    HttpResponse<String> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (HttpTimeoutException exception) {
      throw new IOException(
          "Timed out while requesting GitHub releases: " + releasesUri, exception);
    }

    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException(
          "GitHub releases request failed with status "
              + response.statusCode()
              + " for "
              + releasesUri);
    }

    JsonNode releasesNode = objectMapper.readTree(response.body());
    if (!releasesNode.isArray()) {
      throw new IOException("Unexpected GitHub releases payload: expected array");
    }

    for (JsonNode releaseNode : releasesNode) {
      boolean prerelease = releaseNode.path("prerelease").asBoolean(false);
      if (githubConfig.channel() == LauncherConfig.Channel.STABLE && prerelease) {
        continue;
      }
      String tagName = releaseNode.path("tag_name").asText("");
      List<ReleaseInfo.ReleaseAsset> assets = parseAssets(releaseNode.path("assets"));
      if (assets.isEmpty()) {
        continue;
      }
      return Optional.of(new ReleaseInfo(normalizeVersion(tagName), assets));
    }

    return Optional.empty();
  }

  public Optional<ResolvedReleaseAsset> resolveAgentAsset(
      ReleaseInfo releaseInfo, Pattern assetRegex, String sha256Suffix) {
    for (ReleaseInfo.ReleaseAsset asset : releaseInfo.assets()) {
      Matcher matcher = assetRegex.matcher(asset.name());
      if (!matcher.matches()) {
        continue;
      }
      String version = releaseInfo.version();
      if (matcher.groupCount() >= 1) {
        String capturedVersion = matcher.group(1);
        if (capturedVersion != null && !capturedVersion.isBlank()) {
          version = capturedVersion.trim();
        }
      }

      ReleaseInfo.ReleaseAsset shaAsset = null;
      if (sha256Suffix != null && !sha256Suffix.isBlank()) {
        String shaAssetName = asset.name() + sha256Suffix;
        shaAsset =
            releaseInfo.assets().stream()
                .filter(candidate -> shaAssetName.equals(candidate.name()))
                .findFirst()
                .orElse(null);
      }

      return Optional.of(new ResolvedReleaseAsset(version, asset, shaAsset));
    }
    return Optional.empty();
  }

  private List<ReleaseInfo.ReleaseAsset> parseAssets(JsonNode assetsNode) {
    if (!assetsNode.isArray()) {
      return List.of();
    }
    List<ReleaseInfo.ReleaseAsset> assets = new ArrayList<>();
    for (JsonNode assetNode : assetsNode) {
      String name = assetNode.path("name").asText("");
      String url = assetNode.path("browser_download_url").asText("");
      if (name.isBlank() || url.isBlank()) {
        continue;
      }
      assets.add(new ReleaseInfo.ReleaseAsset(name, URI.create(url)));
    }
    return assets;
  }

  private String normalizeVersion(String rawTagName) {
    if (rawTagName == null || rawTagName.isBlank()) {
      return "0";
    }
    String normalizedTag = rawTagName.trim();
    if (normalizedTag.toLowerCase(Locale.ROOT).startsWith("v") && normalizedTag.length() > 1) {
      return normalizedTag.substring(1);
    }
    return normalizedTag;
  }

  public record ResolvedReleaseAsset(
      String version, ReleaseInfo.ReleaseAsset agentAsset, ReleaseInfo.ReleaseAsset sha256Asset) {}
}
