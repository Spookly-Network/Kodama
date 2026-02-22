package net.spookly.kodama.nodelauncher;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public record ReleaseInfo(String version, List<ReleaseAsset> assets) {

  public ReleaseInfo {
    if (version == null || version.isBlank()) {
      throw new IllegalArgumentException("Release version is required");
    }
    assets = List.copyOf(Objects.requireNonNull(assets, "Release assets are required"));
  }

  public record ReleaseAsset(String name, URI downloadUrl) {
    public ReleaseAsset {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Asset name is required");
      }
      downloadUrl = Objects.requireNonNull(downloadUrl, "Asset download URL is required");
    }
  }
}
