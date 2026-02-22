package net.spookly.kodama.nodeagent.registration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

class NodeRegistrationClientTest {

  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void registerUsesBuildVersionInPayload() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/api/nodes/register",
        exchange -> {
          if ("POST".equals(exchange.getRequestMethod())) {
            requestBody.set(readRequestBody(exchange));
            writeResponse(
                exchange,
                200,
                "{\"nodeId\":\"" + UUID.randomUUID() + "\",\"heartbeatIntervalSeconds\":30}");
            return;
          }
          writeResponse(exchange, 405, "");
        });
    server.start();

    ObjectMapper objectMapper = new ObjectMapper();
    BuildProperties buildProperties = createBuildProperties("9.9.9");
    NodeConfig config = new NodeConfig();
    NodeRegistrationClient client =
        new NodeRegistrationClient(objectMapper, buildProperties, config);
    NodeRegistrationRequest request = new NodeRegistrationRequest();
    request.setName("node-a");
    request.setRegion("local");
    request.setCapacitySlots(4);
    request.setNodeVersion("from-env");

    URI endpoint =
        URI.create("http://localhost:" + server.getAddress().getPort() + "/api/nodes/register");
    client.register(endpoint, "X-Node-Token", "", request);

    assertThat(requestBody.get()).isNotBlank();
    assertThat(objectMapper.readTree(requestBody.get()).get("nodeVersion").asText())
        .isEqualTo("9.9.9");
  }

  private String readRequestBody(HttpExchange exchange) throws IOException {
    return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }

  private void writeResponse(HttpExchange exchange, int statusCode, String body)
      throws IOException {
    byte[] payload = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, payload.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(payload);
    }
  }

  private BuildProperties createBuildProperties(String version) {
    Properties properties = new Properties();
    properties.setProperty("name", "kodama-node-agent");
    properties.setProperty("group", "net.spookly.kodama");
    properties.setProperty("artifact", "node-agent");
    properties.setProperty("version", version);
    return new BuildProperties(properties);
  }
}
