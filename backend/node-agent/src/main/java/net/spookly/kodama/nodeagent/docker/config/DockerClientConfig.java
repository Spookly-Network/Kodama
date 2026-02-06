package net.spookly.kodama.nodeagent.docker.config;

import java.time.Duration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DockerClientConfig {

    @Bean
    public DockerClient dockerClient(NodeConfig config) {
        NodeConfig.Docker docker = config.getDocker();
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        String dockerHost = config.getEffectiveDockerHost();
        if (hasText(dockerHost)) {
            builder.withDockerHost(dockerHost);
        }
        if (hasText(docker.getApiVersion())) {
            builder.withApiVersion(docker.getApiVersion());
        }
        if (hasText(docker.getCertPath())) {
            builder.withDockerCertPath(docker.getCertPath());
        }
        if (hasText(docker.getConfigDir())) {
            builder.withDockerConfig(docker.getConfigDir());
        }
        if (hasText(docker.getContext())) {
            builder.withDockerContext(docker.getContext());
        }
        if (docker.getTlsVerify() != null) {
            builder.withDockerTlsVerify(docker.getTlsVerify());
        }

        DefaultDockerClientConfig clientConfig = builder.build();
        ApacheDockerHttpClient.Builder httpBuilder = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .connectionTimeout(Duration.ofSeconds(docker.getConnectionTimeoutSeconds()))
                .responseTimeout(Duration.ofSeconds(docker.getResponseTimeoutSeconds()));

        Integer maxConnections = docker.getMaxConnections();
        if (maxConnections != null) {
            httpBuilder.maxConnections(maxConnections);
        }

        DockerHttpClient httpClient = httpBuilder.build();
        return DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
