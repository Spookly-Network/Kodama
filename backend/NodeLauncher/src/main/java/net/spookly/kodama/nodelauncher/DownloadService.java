package net.spookly.kodama.nodelauncher;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

public final class DownloadService {

  private final HttpClient httpClient;
  private final Duration requestTimeout;

  public DownloadService(HttpClient httpClient, Duration requestTimeout) {
    this.httpClient = httpClient;
    this.requestTimeout = requestTimeout;
  }

  public Path downloadToTempFile(URI sourceUri, Path directory, String prefix, String suffix)
      throws IOException, InterruptedException {
    Files.createDirectories(directory);
    Path tempFile = Files.createTempFile(directory, prefix, suffix);
    boolean completed = false;
    try {
      HttpRequest request = HttpRequest.newBuilder(sourceUri).timeout(requestTimeout).GET().build();
      HttpResponse<Path> response =
          httpClient.send(
              request,
              HttpResponse.BodyHandlers.ofFile(
                  tempFile,
                  StandardOpenOption.WRITE,
                  StandardOpenOption.TRUNCATE_EXISTING,
                  StandardOpenOption.CREATE));
      validateSuccessfulStatus(sourceUri, response.statusCode());
      completed = true;
      return tempFile;
    } catch (HttpTimeoutException exception) {
      throw new IOException("Timed out while downloading " + sourceUri, exception);
    } finally {
      if (!completed) {
        Files.deleteIfExists(tempFile);
      }
    }
  }

  public String downloadString(URI sourceUri) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(sourceUri).timeout(requestTimeout).GET().build();
    HttpResponse<String> response;
    try {
      response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (HttpTimeoutException exception) {
      throw new IOException("Timed out while downloading " + sourceUri, exception);
    }
    validateSuccessfulStatus(sourceUri, response.statusCode());
    return response.body();
  }

  private void validateSuccessfulStatus(URI sourceUri, int statusCode) throws IOException {
    if (statusCode < 200 || statusCode >= 300) {
      throw new IOException(
          "Download failed for " + sourceUri + " with unexpected status code " + statusCode);
    }
  }
}
