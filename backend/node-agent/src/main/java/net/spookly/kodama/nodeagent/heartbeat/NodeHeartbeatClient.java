package net.spookly.kodama.nodeagent.heartbeat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.http.BrainHttpClientFactory;
import net.spookly.kodama.nodeagent.http.JsonHttpRequestSupport;
import org.springframework.stereotype.Component;

@Component
public class NodeHeartbeatClient {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public NodeHeartbeatClient(ObjectMapper objectMapper, NodeConfig config) {
    this.objectMapper = objectMapper;
    this.httpClient = BrainHttpClientFactory.create(config, CONNECT_TIMEOUT);
  }

  public void sendHeartbeat(
      URI endpoint, String authHeaderName, String authToken, NodeHeartbeatRequest request) {
    String payload = writePayload(request);
    HttpRequest.Builder requestBuilder =
        JsonHttpRequestSupport.newJsonPostRequestBuilder(
                endpoint, RESPONSE_TIMEOUT, authHeaderName, authToken)
            .POST(JsonHttpRequestSupport.jsonBody(payload));
    try {
      HttpResponse<String> response =
          JsonHttpRequestSupport.sendUtf8(httpClient, requestBuilder.build());
      int status = response.statusCode();
      if (status < 200 || status >= 300) {
        String body = JsonHttpRequestSupport.normalizeBody(response);
        throw new NodeHeartbeatException("Heartbeat failed with status " + status + ": " + body);
      }
    } catch (IOException ex) {
      throw new NodeHeartbeatException("Failed to send heartbeat to " + endpoint, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new NodeHeartbeatException("Interrupted while sending heartbeat to " + endpoint, ex);
    }
  }

  private String writePayload(NodeHeartbeatRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException ex) {
      throw new NodeHeartbeatException("Failed to serialize node heartbeat request", ex);
    }
  }
}
