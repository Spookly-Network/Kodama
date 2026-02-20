package net.spookly.kodama.nodeagent.instance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void rejectsLegacyObjectPortsJson() {
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                UUID.randomUUID(),
                "instance-name",
                null,
                "{\"game\":25565}",
                Map.of("PORT_GAME", "30000"),
                List.of(),
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> resolver.resolveBindings(entry))
                .isInstanceOf(InstanceStartException.class)
                .hasMessageContaining("portsJson must be a JSON array");
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
                null,
                null,
                null,
                null,
                null,
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
                null,
                null,
                null,
                null,
                null,
                null
        );

        List<DockerPortBinding> bindings = resolver.resolveBindings(entry);

        assertThat(bindings)
                .extracting(DockerPortBinding::hostPort)
                .containsExactlyInAnyOrder(30001, 30002);
    }

    @Test
    void resolvesPortsFromArrayFormatAndIgnoresVariableFallback() {
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                UUID.randomUUID(),
                "instance-name",
                null,
                """
                        [
                          {"name":"game","protocol":"udp","containerPort":25565,"hostPort":30001},
                          {"name":"query","protocol":"tcp","containerPort":25566,"hostPort":30002}
                        ]
                        """,
                Map.of("PORT_GAME", "39999"),
                List.of(),
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        List<DockerPortBinding> bindings = resolver.resolveBindings(entry);

        assertThat(bindings).hasSize(2);
        assertThat(bindings.get(0).containerPort()).isEqualTo(25565);
        assertThat(bindings.get(0).hostPort()).isEqualTo(30001);
        assertThat(bindings.get(0).protocol()).isEqualTo("udp");
        assertThat(bindings.get(1).containerPort()).isEqualTo(25566);
        assertThat(bindings.get(1).hostPort()).isEqualTo(30002);
        assertThat(bindings.get(1).protocol()).isEqualTo("tcp");
    }

    @Test
    void arrayFormatDoesNotFallBackToVariablesWhenArrayIsEmpty() {
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                UUID.randomUUID(),
                "instance-name",
                null,
                "[]",
                Map.of("PORT", "25565"),
                List.of(),
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        List<DockerPortBinding> bindings = resolver.resolveBindings(entry);

        assertThat(bindings).isEmpty();
    }

    @Test
    void rejectsArrayProtocolWhenInvalid() {
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                UUID.randomUUID(),
                "instance-name",
                null,
                """
                        [
                          {"name":"game","protocol":"icmp","containerPort":25565,"hostPort":30001}
                        ]
                        """,
                Map.of(),
                List.of(),
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> resolver.resolveBindings(entry))
                .isInstanceOf(InstanceStartException.class)
                .hasMessageContaining("protocol must be tcp or udp");
    }

    @Test
    void rejectsArrayEntryWhenNameMissing() {
        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                UUID.randomUUID(),
                "instance-name",
                null,
                """
                        [
                          {"protocol":"udp","containerPort":25565,"hostPort":30001}
                        ]
                        """,
                Map.of(),
                List.of(),
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> resolver.resolveBindings(entry))
                .isInstanceOf(InstanceStartException.class)
                .hasMessageContaining("name is required");
    }
}
