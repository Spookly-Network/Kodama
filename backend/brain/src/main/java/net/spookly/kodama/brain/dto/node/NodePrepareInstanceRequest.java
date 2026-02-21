package net.spookly.kodama.brain.dto.node;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.dto.PortDefinitionRequest;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NodePrepareInstanceRequest {

  private UUID instanceId;
  private String name;
  private String displayName;
  private String containerImage;
  private String installScript;
  private List<String> startCommand;
  private Integer slotsRequired;
  private List<PortDefinitionRequest> portDefinitions;
  private String portsJson;
  private Map<String, String> variables;
  private String variablesJson;
  private List<NodePrepareInstanceLayer> layers;
}
