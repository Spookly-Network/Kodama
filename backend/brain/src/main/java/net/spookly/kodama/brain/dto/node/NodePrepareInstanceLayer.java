package net.spookly.kodama.brain.dto.node;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NodePrepareInstanceLayer {

    private UUID templateVersionId;
    private String version;
    private String checksum;
    private String s3Key;
    private String metadataJson;
    private int orderIndex;
}
