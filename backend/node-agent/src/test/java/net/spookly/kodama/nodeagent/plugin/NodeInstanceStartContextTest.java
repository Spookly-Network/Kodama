package net.spookly.kodama.nodeagent.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NodeInstanceStartContextTest {

    @Test
    void filtersNullEnvAndLabelEntries() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("A", "1");
        env.put("B", null);
        env.put(null, "2");

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("ok", "yes");
        labels.put("bad", null);
        labels.put(null, "no");

        NodeInstanceStartContext context = new NodeInstanceStartContext(
                UUID.randomUUID(),
                "requested",
                null,
                env,
                labels,
                null
        );

        assertThat(context.env()).containsEntry("A", "1");
        assertThat(context.env()).doesNotContainKeys("B", null);
        assertThat(context.labels()).containsEntry("ok", "yes");
        assertThat(context.labels()).doesNotContainKeys("bad", null);
    }

    @Test
    void defaultsToEmptyMapsWhenAllEntriesAreNull() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put(null, null);

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(null, null);

        NodeInstanceStartContext context = new NodeInstanceStartContext(
                UUID.randomUUID(),
                "requested",
                null,
                env,
                labels,
                null
        );

        assertThat(context.env()).isEmpty();
        assertThat(context.labels()).isEmpty();
    }

    @Test
    void startSpecFiltersNullEntries() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("A", "1");
        env.put("B", null);
        env.put(null, "2");

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("ok", "yes");
        labels.put("bad", null);
        labels.put(null, "no");

        NodeInstanceStartSpec spec = new NodeInstanceStartSpec(env, labels, null);

        assertThat(spec.env()).containsEntry("A", "1");
        assertThat(spec.env()).doesNotContainKeys("B", null);
        assertThat(spec.labels()).containsEntry("ok", "yes");
        assertThat(spec.labels()).doesNotContainKeys("bad", null);
    }

    @Test
    void preservesNullCommandWhenNotProvided() {
        NodeInstanceStartContext context = new NodeInstanceStartContext(
                UUID.randomUUID(),
                "requested",
                null,
                Map.of(),
                Map.of(),
                null
        );

        assertThat(context.command()).isNull();
    }

    @Test
    void copiesCommandWhenProvided() {
        NodeInstanceStartContext context = new NodeInstanceStartContext(
                UUID.randomUUID(),
                "requested",
                null,
                Map.of(),
                Map.of(),
                java.util.List.of("run", "server")
        );

        assertThat(context.command()).containsExactly("run", "server");
    }
}
