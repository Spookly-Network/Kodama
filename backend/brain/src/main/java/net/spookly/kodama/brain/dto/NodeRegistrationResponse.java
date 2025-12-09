package net.spookly.kodama.brain.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NodeRegistrationResponse {

    private UUID nodeId;
    private int heartbeatIntervalSeconds;
}
