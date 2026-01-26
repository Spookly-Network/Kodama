package net.spookly.kodama.nodeagent.template.storage.s3;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.minio.MinioClient;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageClient;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3TemplateStorageConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(60);

    @Bean
    public TemplateStorageClient templateStorageClient(NodeConfig config) {
        NodeConfig.S3 s3 = config.getS3();
        validateS3Config(s3);
        MinioClient minioClient = buildMinioClient(s3);
        return new S3TemplateStorageClient(minioClient, s3.getBucket());
    }

    private MinioClient buildMinioClient(NodeConfig.S3 s3) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .writeTimeout(API_CALL_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .build();

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(parseEndpoint(s3.getEndpoint()))
                .credentials(s3.getAccessKey(), s3.getSecretKey())
                .httpClient(httpClient);

        String region = s3.getRegion();
        if (region != null && !region.isBlank()) {
            builder.region(region);
        }

        return builder.build();
    }

    private String parseEndpoint(String endpoint) {
        try {
            return URI.create(endpoint).toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("node-agent.s3.endpoint is invalid: " + endpoint, ex);
        }
    }

    private void validateS3Config(NodeConfig.S3 s3) {
        List<String> errors = new ArrayList<>();
        addIfBlank(errors, s3.getEndpoint(), "node-agent.s3.endpoint is required");
        addIfBlank(errors, s3.getBucket(), "node-agent.s3.bucket is required");
        addIfBlank(errors, s3.getAccessKey(), "node-agent.s3.access-key is required");
        addIfBlank(errors, s3.getSecretKey(), "node-agent.s3.secret-key is required");
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid S3 configuration:\n- " + String.join("\n- ", errors));
        }
    }

    private void addIfBlank(List<String> errors, String value, String message) {
        if (value == null || value.isBlank()) {
            errors.add(message);
        }
    }
}
