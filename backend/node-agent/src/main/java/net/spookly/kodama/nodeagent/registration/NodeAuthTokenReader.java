package net.spookly.kodama.nodeagent.registration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.springframework.stereotype.Component;

@Component
public class NodeAuthTokenReader {

    private final NodeConfig config;

    public NodeAuthTokenReader(NodeConfig config) {
        this.config = config;
    }

    public String readToken() {
        String tokenPath = config.getAuth().getTokenPath();
        if (tokenPath == null || tokenPath.isBlank()) {
            return null;
        }
        Path path = Path.of(tokenPath);
        if (!Files.exists(path)) {
            throw new NodeRegistrationException("Node auth token file does not exist: " + tokenPath);
        }
        try {
            String token = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (token.isBlank()) {
                throw new NodeRegistrationException("Node auth token file is empty: " + tokenPath);
            }
            return token;
        } catch (IOException ex) {
            throw new NodeRegistrationException("Failed to read node auth token file: " + tokenPath, ex);
        }
    }
}
