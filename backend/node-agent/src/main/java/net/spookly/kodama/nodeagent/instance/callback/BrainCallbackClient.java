package net.spookly.kodama.nodeagent.instance.callback;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.http.BrainHttpClientFactory;
import net.spookly.kodama.nodeagent.http.JsonHttpRequestSupport;
import net.spookly.kodama.nodeagent.instance.service.InstancePrepareException;
import org.springframework.stereotype.Component;

@Component
public class BrainCallbackClient {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);

  private final HttpClient httpClient;

  public BrainCallbackClient(NodeConfig config) {
    this.httpClient = BrainHttpClientFactory.create(config, CONNECT_TIMEOUT);
  }

  public void sendCallback(URI endpoint, String authHeaderName, String authToken) {
    sendCallback(endpoint, authHeaderName, authToken, null);
  }

  public void sendCallback(
      URI endpoint, String authHeaderName, String authToken, String requestBody) {
    HttpRequest.Builder requestBuilder =
        JsonHttpRequestSupport.newJsonPostRequestBuilder(
            endpoint, RESPONSE_TIMEOUT, authHeaderName, authToken);
    if (JsonHttpRequestSupport.hasText(requestBody)) {
      requestBuilder.POST(JsonHttpRequestSupport.jsonBody(requestBody));
    } else {
      requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
    }
    try {
      HttpResponse<String> response =
          JsonHttpRequestSupport.sendUtf8(httpClient, requestBuilder.build());
      int status = response.statusCode();
      if (status < 200 || status >= 300) {
        String body = JsonHttpRequestSupport.normalizeBody(response);
        throw new InstancePrepareException("Callback failed with status " + status + ": " + body);
      }
    } catch (IOException ex) {
      throw new InstancePrepareException("Failed to send callback to " + endpoint, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new InstancePrepareException("Interrupted while sending callback to " + endpoint, ex);
    }
  }
}
