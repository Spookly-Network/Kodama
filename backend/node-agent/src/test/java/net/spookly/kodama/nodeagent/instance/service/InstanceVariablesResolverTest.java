package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class InstanceVariablesResolverTest {

    private final InstanceVariablesResolver resolver = new InstanceVariablesResolver(new ObjectMapper());

    @Test
    void returnsEmptyWhenNoVariablesProvided() {
        Map<String, String> resolved = resolver.resolve(null, null);

        assertThat(resolved).isEmpty();
    }

    @Test
    void returnsVariablesMapWhenProvided() {
        Map<String, String> resolved = resolver.resolve(Map.of("A", "1"), null);

        assertThat(resolved).containsEntry("A", "1");
    }

    @Test
    void parsesVariablesJsonWhenProvided() {
        Map<String, String> resolved = resolver.resolve(null, "{\"A\":\"1\",\"B\":\"2\"}");

        assertThat(resolved).containsEntry("A", "1").containsEntry("B", "2");
    }

    @Test
    void rejectsVariablesAndJsonTogether() {
        assertThatThrownBy(() -> resolver.resolve(Map.of("A", "1"), "{\"A\":\"2\"}"))
                .isInstanceOf(InstancePrepareValidationException.class)
                .hasMessageContaining("either variables or variablesJson");
    }
}
