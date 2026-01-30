package net.spookly.kodama.nodeagent.instance.registry;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceLayer;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceRequest;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstanceRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceRegistryService.class);
    private static final String REGISTRY_FILENAME = "instance.json";

    private final ObjectMapper objectMapper;

    public InstanceRegistryService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public void recordPrepared(
            InstanceWorkspacePaths workspace,
            NodePrepareInstanceRequest request,
            List<NodePrepareInstanceLayer> layers,
            Map<String, String> variables
    ) {
        if (workspace == null) {
            throw new InstanceRegistryException("instance workspace is required");
        }
        if (request == null) {
            throw new InstanceRegistryException("prepare request is required");
        }
        UUID instanceId = requireInstanceId(request.instanceId());
        requireMatchingInstanceId(instanceId, workspace.instanceId());
        List<NodePrepareInstanceLayer> safeLayers = requireLayers(layers);
        Map<String, String> safeVariables = variables == null ? Map.of() : new LinkedHashMap<>(variables);
        Path instanceRoot = Objects.requireNonNull(workspace.instanceRoot(), "instanceRoot");
        if (!Files.isDirectory(instanceRoot)) {
            throw new InstanceRegistryException("Instance workspace root is missing at " + instanceRoot);
        }

        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                instanceId,
                request.name(),
                request.displayName(),
                request.portsJson(),
                safeVariables,
                new ArrayList<>(safeLayers),
                OffsetDateTime.now(),
                null
        );

        Path registryFile = instanceRoot.resolve(REGISTRY_FILENAME);
        writeRegistry(registryFile, entry);
        logger.info("Instance registry updated. instanceId={} path={}", instanceId, registryFile);
    }

    public InstanceRegistryEntry loadRegistry(InstanceWorkspacePaths workspace) {
        if (workspace == null) {
            throw new InstanceRegistryException("instance workspace is required");
        }
        Path instanceRoot = Objects.requireNonNull(workspace.instanceRoot(), "instanceRoot");
        if (!Files.isDirectory(instanceRoot)) {
            throw new InstanceRegistryException("Instance workspace root is missing at " + instanceRoot);
        }
        Path registryFile = instanceRoot.resolve(REGISTRY_FILENAME);
        if (!Files.isRegularFile(registryFile)) {
            throw new InstanceRegistryException("Instance registry is missing at " + registryFile);
        }
        try {
            InstanceRegistryEntry entry = objectMapper.readValue(registryFile.toFile(), InstanceRegistryEntry.class);
            if (entry == null) {
                throw new InstanceRegistryException("Instance registry is empty at " + registryFile);
            }
            return entry;
        } catch (IOException ex) {
            throw new InstanceRegistryException("Failed to read instance registry at " + registryFile, ex);
        }
    }

    public void recordContainerId(
            InstanceWorkspacePaths workspace,
            UUID instanceId,
            String containerId
    ) {
        if (workspace == null) {
            throw new InstanceRegistryException("instance workspace is required");
        }
        requireInstanceId(instanceId);
        if (containerId == null || containerId.isBlank()) {
            throw new InstanceRegistryException("containerId is required");
        }
        requireMatchingInstanceId(instanceId, workspace.instanceId());
        InstanceRegistryEntry entry = loadRegistry(workspace);
        if (!instanceId.equals(entry.instanceId())) {
            throw new InstanceRegistryException("instanceId does not match registry: " + instanceId + " vs " + entry.instanceId());
        }
        InstanceRegistryEntry updated = new InstanceRegistryEntry(
                entry.instanceId(),
                entry.name(),
                entry.displayName(),
                entry.portsJson(),
                entry.variables(),
                entry.layers(),
                entry.preparedAt(),
                containerId.trim()
        );
        Path registryFile = workspace.instanceRoot().resolve(REGISTRY_FILENAME);
        writeRegistry(registryFile, updated);
        logger.info("Instance registry updated with containerId. instanceId={} containerId={}", instanceId, containerId);
    }

    private void writeRegistry(Path registryFile, InstanceRegistryEntry entry) {
        Path parent = Objects.requireNonNull(registryFile, "registryFile").getParent();
        if (parent == null) {
            throw new InstanceRegistryException("Instance registry path has no parent: " + registryFile);
        }
        Path tempFile = null;
        boolean moved = false;
        try {
            tempFile = Files.createTempFile(parent, "instance-registry-", ".json");
            objectMapper.writeValue(tempFile.toFile(), entry);
            try {
                Files.move(
                        tempFile,
                        registryFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
                moved = true;
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, registryFile, StandardCopyOption.REPLACE_EXISTING);
                moved = true;
            }
        } catch (IOException ex) {
            throw new InstanceRegistryException("Failed to write instance registry at " + registryFile, ex);
        } finally {
            if (!moved) {
                deleteIfExists(tempFile);
            }
        }
    }

    private void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            logger.warn("Failed to delete temp registry file {}", path, ex);
        }
    }

    private UUID requireInstanceId(UUID instanceId) {
        if (instanceId == null) {
            throw new InstanceRegistryException("instanceId is required");
        }
        return instanceId;
    }

    private void requireMatchingInstanceId(UUID instanceId, String workspaceInstanceId) {
        if (workspaceInstanceId == null || workspaceInstanceId.isBlank()) {
            throw new InstanceRegistryException("workspace instanceId is required");
        }
        if (!instanceId.toString().equals(workspaceInstanceId)) {
            throw new InstanceRegistryException(
                    "instanceId does not match workspace: " + instanceId + " vs " + workspaceInstanceId
            );
        }
    }

    private List<NodePrepareInstanceLayer> requireLayers(List<NodePrepareInstanceLayer> layers) {
        if (layers == null || layers.isEmpty()) {
            throw new InstanceRegistryException("template layers are required");
        }
        for (NodePrepareInstanceLayer layer : layers) {
            if (layer == null) {
                throw new InstanceRegistryException("template layer is required");
            }
        }
        return layers;
    }
}
