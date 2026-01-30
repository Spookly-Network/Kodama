package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.nodeagent.docker.dto.DockerPortBinding;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import org.junit.jupiter.api.Test;

class InstancePortBindingsResolverTest {

    private final InstancePortBindingsResolver resolver = new InstancePortBindingsResolver(new ObjectMapper());

    @Test
    void resolvesPortsFromPortsJsonAndVariables() {
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                UUID.randomUUID(),
                "instance-name",
                null,
                "{\"game\":25565}",
                Map.of("PORT_GAME", "30000"),
                List.of(),
                OffsetDateTime.now(),
                null
        );

        List<DockerPortBinding> bindings = resolver.resolveBindings(entry);

        assertThat(bindings).hasSize(1);
        DockerPortBinding binding = bindings.get(0);
        assertThat(binding.containerPort()).isEqualTo(25565);
        assertThat(binding.hostPort()).isEqualTo(30000);
    }

    @Test
    void resolvesPortsFromVariablesWhenPortsJsonMissing() {
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                UUID.randomUUID(),
                "instance-name",
                null,
                null,
                Map.of("PORT", "25565"),
                List.of(),
                OffsetDateTime.now(),
                null
        );

        List<DockerPortBinding> bindings = resolver.resolveBindings(entry);

        assertThat(bindings).hasSize(1);
        DockerPortBinding binding = bindings.get(0);
        assertThat(binding.containerPort()).isEqualTo(25565);
        assertThat(binding.hostPort()).isEqualTo(25565);
    }

    @Test
    void ignoresPortWhenNamedPortsProvided() {
        Map<String, String> variables = new java.util.LinkedHashMap<>();
        variables.put("PORT", "25565");
        variables.put("PORT_GAME", "30001");
        variables.put("PORT_QUERY", "30002");
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                UUID.randomUUID(),
                "instance-name",
                null,
                null,
                variables,
                List.of(),
                OffsetDateTime.now(),
                null
        );

        List<DockerPortBinding> bindings = resolver.resolveBindings(entry);

        assertThat(bindings)
                .extracting(DockerPortBinding::hostPort)
                .containsExactlyInAnyOrder(30001, 30002);
    }
}
