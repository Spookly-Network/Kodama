package net.spookly.kodama.nodeagent.registration;

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
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component
public class NodeRegistrationClient {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(10);

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final String nodeVersion;

  public NodeRegistrationClient(
      ObjectMapper objectMapper, BuildProperties buildProperties, NodeConfig config) {
    this.objectMapper = objectMapper;
    this.httpClient = BrainHttpClientFactory.create(config, CONNECT_TIMEOUT);
    this.nodeVersion = buildProperties.getVersion();
  }

  public NodeRegistrationResponse register(
      URI endpoint, String authHeaderName, String authToken, NodeRegistrationRequest request) {
    request.setNodeVersion(nodeVersion);
    String payload = writePayload(request);
    HttpRequest.Builder requestBuilder =
        JsonHttpRequestSupport.newJsonPostRequestBuilder(
                endpoint, RESPONSE_TIMEOUT, authHeaderName, authToken)
            .POST(JsonHttpRequestSupport.jsonBody(payload));
    try {
      HttpResponse<String> response =
          JsonHttpRequestSupport.sendUtf8(httpClient, requestBuilder.build());
      int status = response.statusCode();
      String body = JsonHttpRequestSupport.normalizeBody(response);
      if (status < 200 || status >= 300) {
        throw new NodeRegistrationException(
            "Registration failed with status " + status + ": " + body);
      }
      if (body.isBlank()) {
        throw new NodeRegistrationException("Registration response body is empty");
      }
      NodeRegistrationResponse registrationResponse =
          objectMapper.readValue(body, NodeRegistrationResponse.class);
      if (registrationResponse.getNodeId() == null) {
        throw new NodeRegistrationException("Registration response did not include nodeId");
      }
      return registrationResponse;
    } catch (IOException ex) {
      throw new NodeRegistrationException("Failed to register node at " + endpoint, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new NodeRegistrationException("Interrupted while registering node at " + endpoint, ex);
    }
  }

  private String writePayload(NodeRegistrationRequest request) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException ex) {
      throw new NodeRegistrationException("Failed to serialize node registration request", ex);
    }
  }
}
