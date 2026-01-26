package net.spookly.kodama.nodeagent.heartbeat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

@Component
public class NodeHeartbeatClient {

    private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(5);
    private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(10);

    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public NodeHeartbeatClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(CONNECT_TIMEOUT)
                        .setResponseTimeout(RESPONSE_TIMEOUT)
                        .build())
                .build();
    }

    public void sendHeartbeat(
            URI endpoint,
            String authHeaderName,
            String authToken,
            NodeHeartbeatRequest request
    ) {
        String payload = writePayload(request);
        HttpPost post = new HttpPost(endpoint);
        post.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
        post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        post.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        if (authToken != null && !authToken.isBlank()) {
            post.setHeader(authHeaderName, authToken);
        }
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getCode();
            if (status < 200 || status >= 300) {
                String body = readBody(response);
                throw new NodeHeartbeatException("Heartbeat failed with status " + status + ": " + body);
            }
        } catch (IOException ex) {
            throw new NodeHeartbeatException("Failed to send heartbeat to " + endpoint, ex);
        }
    }

    private String writePayload(NodeHeartbeatRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new NodeHeartbeatException("Failed to serialize node heartbeat request", ex);
        }
    }

    private String readBody(CloseableHttpResponse response) throws IOException {
        if (response.getEntity() == null) {
            return "";
        }
        try {
            String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            return body == null ? "" : body.trim();
        } catch (ParseException ex) {
            throw new IOException("Failed to parse response body", ex);
        }
    }

    @PreDestroy
    public void close() {
        try {
            httpClient.close();
        } catch (IOException ex) {
            throw new NodeHeartbeatException("Failed to close heartbeat HTTP client", ex);
        }
    }
}
