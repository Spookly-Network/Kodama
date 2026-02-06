package net.spookly.kodama.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import net.spookly.kodama.brain.domain.node.Node;
import net.spookly.kodama.brain.domain.node.NodeStatus;
import org.junit.jupiter.api.Test;

class SchedulingServiceTest {

    private final SchedulingService schedulingService = new SchedulingService(null);

    @Test
    void selectNodeFiltersByStatusAndCapacity() {
        Node offline = buildNode("node-a", "eu-west-1", NodeStatus.OFFLINE, false, 4, 1, null);
        Node full = buildNode("node-b", "eu-west-1", NodeStatus.ONLINE, false, 2, 2, null);
        Node available = buildNode("node-c", "eu-west-1", NodeStatus.ONLINE, false, 3, 1, null);

        Node selected = schedulingService.selectNodeFromCandidates(
                List.of(offline, full, available),
                null,
                null,
                null
        );

        assertThat(selected).isEqualTo(available);
    }

    @Test
    void selectNodeFiltersByRegion() {
        Node euNode = buildNode("node-eu", "eu-west-1", NodeStatus.ONLINE, false, 3, 1, null);
        Node usNode = buildNode("node-us", "us-east-1", NodeStatus.ONLINE, false, 3, 1, null);

        Node selected = schedulingService.selectNodeFromCandidates(
                List.of(euNode, usNode),
                "us-east-1",
                null,
                null
        );

        assertThat(selected).isEqualTo(usNode);
    }

    @Test
    void selectNodeFiltersByTags() {
        Node withTags = buildNode("node-tags", "eu-west-1", NodeStatus.ONLINE, false, 3, 1, "primary,ssd");
        Node missingTag = buildNode("node-missing", "eu-west-1", NodeStatus.ONLINE, false, 3, 1, "primary");

        Node selected = schedulingService.selectNodeFromCandidates(
                List.of(withTags, missingTag),
                null,
                "primary, ssd",
                null
        );

        assertThat(selected).isEqualTo(withTags);
    }

    @Test
    void selectNodeFiltersByDevModeAllowed() {
        Node devNode = buildNode("node-dev", "eu-west-1", NodeStatus.ONLINE, true, 3, 1, null);
        Node prodNode = buildNode("node-prod", "eu-west-1", NodeStatus.ONLINE, false, 3, 1, null);

        Node devSelected = schedulingService.selectNodeFromCandidates(
                List.of(devNode, prodNode),
                null,
                null,
                true
        );

        Node prodSelected = schedulingService.selectNodeFromCandidates(
                List.of(devNode, prodNode),
                null,
                null,
                false
        );

        assertThat(devSelected).isEqualTo(devNode);
        assertThat(prodSelected).isEqualTo(prodNode);
    }

    @Test
    void selectNodeSortsByUsedSlots() {
        Node busy = buildNode("node-busy", "eu-west-1", NodeStatus.ONLINE, false, 5, 3, null);
        Node idle = buildNode("node-idle", "eu-west-1", NodeStatus.ONLINE, false, 5, 1, null);

        Node selected = schedulingService.selectNodeFromCandidates(
                List.of(busy, idle),
                null,
                null,
                null
        );

        assertThat(selected).isEqualTo(idle);
    }

    @Test
    void selectNodeBreaksTiesByName() {
        Node later = buildNode("node-zeta", "eu-west-1", NodeStatus.ONLINE, false, 5, 2, null);
        Node earlier = buildNode("node-alpha", "eu-west-1", NodeStatus.ONLINE, false, 5, 2, null);

        Node selected = schedulingService.selectNodeFromCandidates(
                List.of(later, earlier),
                null,
                null,
                null
        );

        assertThat(selected).isEqualTo(earlier);
    }

    @Test
    void selectNodeReturnsNullWhenNoCandidateMatches() {
        Node offline = buildNode("node-offline", "eu-west-1", NodeStatus.OFFLINE, false, 3, 0, null);
        Node full = buildNode("node-full", "eu-west-1", NodeStatus.ONLINE, false, 2, 2, null);

        Node selected = schedulingService.selectNodeFromCandidates(
                List.of(offline, full),
                null,
                null,
                null
        );

        assertThat(selected).isNull();
    }

    private Node buildNode(
            String name,
            String region,
            NodeStatus status,
            boolean devMode,
            int capacitySlots,
            int usedSlots,
            String tags
    ) {
        return new Node(
                name,
                region,
                status,
                devMode,
                capacitySlots,
                usedSlots,
                OffsetDateTime.now(ZoneOffset.UTC),
                "1.0.0",
                tags,
                null
        );
    }
}
