package net.spookly.kodama.nodeagent.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class JsonHttpRequestSupport {

  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String ACCEPT_HEADER = "Accept";
  private static final String JSON_MEDIA_TYPE = "application/json";

  private JsonHttpRequestSupport() {}

  public static HttpRequest.Builder newJsonPostRequestBuilder(
      URI endpoint, Duration timeout, String authHeaderName, String authToken) {
    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
            .header(ACCEPT_HEADER, JSON_MEDIA_TYPE);
    if (hasText(authToken)) {
      requestBuilder.header(authHeaderName, authToken);
    }
    return requestBuilder;
  }

  public static HttpRequest.BodyPublisher jsonBody(String body) {
    return HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
  }

  public static HttpResponse<String> sendUtf8(HttpClient client, HttpRequest request)
      throws IOException, InterruptedException {
    return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  public static String normalizeBody(HttpResponse<String> response) {
    return normalizeBody(response.body());
  }

  public static String normalizeBody(String body) {
    return body == null ? "" : body.trim();
  }

  public static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
