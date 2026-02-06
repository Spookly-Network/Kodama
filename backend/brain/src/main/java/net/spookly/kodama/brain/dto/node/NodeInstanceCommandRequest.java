package net.spookly.kodama.brain.dto.node;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NodeInstanceCommandRequest {

    private UUID instanceId;
    private String name;
}
