package net.spookly.kodama.nodeagent.heartbeat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NodeHeartbeatStateTest {

    @Test
    void defaultsAreOnlineAndZeroUsedSlots() {
        NodeHeartbeatState state = new NodeHeartbeatState();

        assertThat(state.getStatus()).isEqualTo(NodeStatus.ONLINE);
        assertThat(state.getUsedSlots()).isZero();
    }

    @Test
    void setUsedSlotsRejectsNegativeValues() {
        NodeHeartbeatState state = new NodeHeartbeatState();

        assertThatThrownBy(() -> state.setUsedSlots(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("usedSlots cannot be negative");
    }

    @Test
    void setStatusRejectsNull() {
        NodeHeartbeatState state = new NodeHeartbeatState();

        assertThatThrownBy(() -> state.setStatus(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("status");
    }
}
