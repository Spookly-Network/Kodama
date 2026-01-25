package net.spookly.kodama.nodeagent.registration;

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
public class NodeRegistrationClient {

    private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(5);
    private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(10);

    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public NodeRegistrationClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(CONNECT_TIMEOUT)
                        .setResponseTimeout(RESPONSE_TIMEOUT)
                        .build())
                .build();
    }

    public NodeRegistrationResponse register(
            URI endpoint,
            String authHeaderName,
            String authToken,
            NodeRegistrationRequest request
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
            String body = readBody(response);
            if (status < 200 || status >= 300) {
                throw new NodeRegistrationException("Registration failed with status " + status + ": " + body);
            }
            if (body == null || body.isBlank()) {
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
        }
    }

    private String writePayload(NodeRegistrationRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new NodeRegistrationException("Failed to serialize node registration request", ex);
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
            throw new NodeRegistrationException("Failed to close registration HTTP client", ex);
        }
    }
}
