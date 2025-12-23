package net.spookly.kodama.brain.dto.node;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NodePrepareInstanceRequest {

    private UUID instanceId;
    private String name;
    private String displayName;
    private String portsJson;
    private Map<String, String> variables;
    private String variablesJson;
    private List<NodePrepareInstanceLayer> layers;
}
