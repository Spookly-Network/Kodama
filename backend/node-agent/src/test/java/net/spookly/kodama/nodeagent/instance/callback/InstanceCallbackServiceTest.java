package net.spookly.kodama.nodeagent.instance.callback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.UUID;

import net.spookly.kodama.nodeagent.config.InstanceProperties;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.registration.NodeAuthTokenReader;
import net.spookly.kodama.nodeagent.registration.NodeRegistrationResponse;
import net.spookly.kodama.nodeagent.registration.NodeRegistrationState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InstanceCallbackServiceTest {

    @Test
    void sendRunningRetriesUntilSuccess() {
        NodeConfig config = buildConfig();
        InstanceProperties instanceProperties = buildInstanceProperties(3, 0);
        NodeRegistrationState registrationState = buildRegistrationState();
        NodeAuthTokenReader tokenReader = mock(NodeAuthTokenReader.class);
        when(tokenReader.readToken()).thenReturn(null);
        BrainCallbackClient callbackClient = mock(BrainCallbackClient.class);
        doThrow(new RuntimeException("boom"))
                .doThrow(new RuntimeException("boom"))
                .doNothing()
                .when(callbackClient)
                .sendCallback(any(), anyString(), any());

        InstanceCallbackService service = new InstanceCallbackService(
                config,
                instanceProperties,
                registrationState,
                tokenReader,
                callbackClient
        );
        UUID instanceId = UUID.randomUUID();

        assertThatNoException().isThrownBy(() -> service.sendRunning(instanceId));

        ArgumentCaptor<URI> endpointCaptor = ArgumentCaptor.forClass(URI.class);
        verify(callbackClient, times(3)).sendCallback(endpointCaptor.capture(), anyString(), any());
        URI endpoint = endpointCaptor.getValue();
        assertThat(endpoint.toString())
                .contains("/api/nodes/")
                .contains("/instances/" + instanceId + "/running");
    }

    @Test
    void sendRunningStopsAfterMaxAttempts() {
        NodeConfig config = buildConfig();
        InstanceProperties instanceProperties = buildInstanceProperties(2, 0);
        NodeRegistrationState registrationState = buildRegistrationState();
        NodeAuthTokenReader tokenReader = mock(NodeAuthTokenReader.class);
        when(tokenReader.readToken()).thenReturn(null);
        BrainCallbackClient callbackClient = mock(BrainCallbackClient.class);
        doThrow(new RuntimeException("boom"))
                .when(callbackClient)
                .sendCallback(any(), anyString(), any());

        InstanceCallbackService service = new InstanceCallbackService(
                config,
                instanceProperties,
                registrationState,
                tokenReader,
                callbackClient
        );
        UUID instanceId = UUID.randomUUID();

        assertThatThrownBy(() -> service.sendRunning(instanceId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");

        verify(callbackClient, times(2)).sendCallback(any(), anyString(), any());
    }

    private NodeConfig buildConfig() {
        NodeConfig config = new NodeConfig();
        config.setBrainBaseUrl("http://brain");
        NodeConfig.Auth auth = new NodeConfig.Auth();
        auth.setHeaderName("X-Node-Token");
        config.setAuth(auth);
        return config;
    }

    private InstanceProperties buildInstanceProperties(int maxAttempts, long backoffMillis) {
        InstanceProperties properties = new InstanceProperties();
        InstanceProperties.InstanceCallbacks callbacks = new InstanceProperties.InstanceCallbacks();
        callbacks.setMaxAttempts(maxAttempts);
        callbacks.setRetryBackoffMillis(backoffMillis);
        properties.setInstanceCallbacks(callbacks);
        return properties;
    }

    private NodeRegistrationState buildRegistrationState() {
        NodeRegistrationState registrationState = new NodeRegistrationState();
        NodeRegistrationResponse response = new NodeRegistrationResponse();
        response.setNodeId(UUID.randomUUID());
        response.setHeartbeatIntervalSeconds(10);
        registrationState.update(response);
        return registrationState;
    }
}
