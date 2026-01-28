package net.spookly.kodama.nodeagent.instance.callback;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.PreDestroy;
import net.spookly.kodama.nodeagent.instance.service.InstancePrepareException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

@Component
public class NodeCallbackClient {

    private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(5);
    private static final Timeout RESPONSE_TIMEOUT = Timeout.ofSeconds(10);

    private final CloseableHttpClient httpClient;

    public NodeCallbackClient() {
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(CONNECT_TIMEOUT)
                        .setResponseTimeout(RESPONSE_TIMEOUT)
                        .build())
                .build();
    }

    public void sendCallback(URI endpoint, String authHeaderName, String authToken) {
        HttpPost post = new HttpPost(endpoint);
        post.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        post.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        if (authToken != null && !authToken.isBlank()) {
            post.setHeader(authHeaderName, authToken);
        }
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int status = response.getCode();
            if (status < 200 || status >= 300) {
                String body = readBody(response);
                throw new InstancePrepareException("Callback failed with status " + status + ": " + body);
            }
        } catch (IOException ex) {
            throw new InstancePrepareException("Failed to send callback to " + endpoint, ex);
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
            throw new InstancePrepareException("Failed to close callback HTTP client", ex);
        }
    }
}
