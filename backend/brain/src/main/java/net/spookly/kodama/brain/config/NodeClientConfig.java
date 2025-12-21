package net.spookly.kodama.brain.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NodeClientConfig {

    @Bean
    public RestTemplate nodeRestTemplate(NodeProperties nodeProperties) {
        int timeoutMillis = (int) Duration.ofSeconds(nodeProperties.getCommandTimeoutSeconds()).toMillis();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return new RestTemplate(requestFactory);
    }
}
