package net.spookly.kodama.nodeagent.instance.registry;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceLayer;
import net.spookly.kodama.nodeagent.instance.dto.NodePrepareInstanceRequest;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstanceRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceRegistryService.class);
    private static final String REGISTRY_FILENAME = "instance.json";

    private final ObjectMapper objectMapper;
    private final InstanceWorkspaceLayout workspaceLayout;
    private final ReentrantLock portReservationLock = new ReentrantLock();

    public InstanceRegistryService(ObjectMapper objectMapper, InstanceWorkspaceLayout workspaceLayout) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.workspaceLayout = Objects.requireNonNull(workspaceLayout, "workspaceLayout");
    }

    public void recordPrepared(
            InstanceWorkspacePaths workspace,
            NodePrepareInstanceRequest request,
            List<NodePrepareInstanceLayer> layers,
            Map<String, String> variables,
            String portsJson
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
        List<String> safeStartCommand = request.startCommand() == null
                ? List.of()
                : new ArrayList<>(request.startCommand());
        Path instanceRoot = Objects.requireNonNull(workspace.instanceRoot(), "instanceRoot");
        if (!Files.isDirectory(instanceRoot)) {
            throw new InstanceRegistryException("Instance workspace root is missing at " + instanceRoot);
        }

        InstanceRegistryEntry entry = new InstanceRegistryEntry(
                instanceId,
                request.name(),
                request.displayName(),
                request.containerImage(),
                request.installScript(),
                safeStartCommand,
                request.slotsRequired(),
                portsJson,
                false,
                safeVariables,
                new ArrayList<>(safeLayers),
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                resolveWorkspacePath(null, instanceRoot)
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
            InstanceRegistryEntry normalized = ensureWorkspacePath(entry, instanceRoot, registryFile, true);
            if (normalized.instanceId() == null) {
                throw new InstanceRegistryException("Instance registry has no instanceId at " + registryFile);
            }
            return normalized;
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
                entry.containerImage(),
                entry.installScript(),
                entry.startCommand(),
                entry.slotsRequired(),
                entry.portsJson(),
                entry.installCompleted(),
                entry.variables(),
                entry.layers(),
                entry.preparedAt(),
                containerId.trim(),
                "running",
                OffsetDateTime.now(),
                null,
                null,
                resolveWorkspacePath(entry.workspacePath(), workspace.instanceRoot())
        );
        Path registryFile = workspace.instanceRoot().resolve(REGISTRY_FILENAME);
        writeRegistry(registryFile, updated);
        logger.info("Instance registry updated with containerId. instanceId={} containerId={}", instanceId, containerId);
    }

    public void recordContainerStatus(
            InstanceWorkspacePaths workspace,
            UUID instanceId,
            String containerStatus
    ) {
        recordContainerStatus(workspace, instanceId, containerStatus, null, null);
    }

    public void recordContainerStatus(
            InstanceWorkspacePaths workspace,
            UUID instanceId,
            String containerStatus,
            Integer exitCode,
            String exitReason
    ) {
        if (workspace == null) {
            throw new InstanceRegistryException("instance workspace is required");
        }
        requireInstanceId(instanceId);
        if (containerStatus == null || containerStatus.isBlank()) {
            throw new InstanceRegistryException("containerStatus is required");
        }
        requireMatchingInstanceId(instanceId, workspace.instanceId());
        InstanceRegistryEntry entry = loadRegistry(workspace);
        if (!instanceId.equals(entry.instanceId())) {
            throw new InstanceRegistryException("instanceId does not match registry: " + instanceId + " vs " + entry.instanceId());
        }
        if (entry.containerId() == null || entry.containerId().isBlank()) {
            throw new InstanceRegistryException("containerId is required");
        }
        String normalizedReason = normalizeExitReason(exitReason);
        InstanceRegistryEntry updated = new InstanceRegistryEntry(
                entry.instanceId(),
                entry.name(),
                entry.displayName(),
                entry.containerImage(),
                entry.installScript(),
                entry.startCommand(),
                entry.slotsRequired(),
                entry.portsJson(),
                entry.installCompleted(),
                entry.variables(),
                entry.layers(),
                entry.preparedAt(),
                entry.containerId().trim(),
                containerStatus.trim(),
                OffsetDateTime.now(),
                exitCode,
                normalizedReason,
                resolveWorkspacePath(entry.workspacePath(), workspace.instanceRoot())
        );
        Path registryFile = workspace.instanceRoot().resolve(REGISTRY_FILENAME);
        writeRegistry(registryFile, updated);
        logger.info(
                "Instance registry updated with containerStatus. instanceId={} containerId={} status={}",
                instanceId,
                entry.containerId(),
                containerStatus
        );
    }

    public void deleteRegistryIfExists(InstanceWorkspacePaths workspace) {
        if (workspace == null) {
            throw new InstanceRegistryException("instance workspace is required");
        }
        Path instanceRoot = Objects.requireNonNull(workspace.instanceRoot(), "instanceRoot");
        if (!Files.isDirectory(instanceRoot)) {
            return;
        }
        Path registryFile = instanceRoot.resolve(REGISTRY_FILENAME);
        try {
            if (Files.deleteIfExists(registryFile)) {
                logger.info("Instance registry deleted. instanceId={} path={}", workspace.instanceId(), registryFile);
            }
        } catch (IOException ex) {
            throw new InstanceRegistryException("Failed to delete instance registry at " + registryFile, ex);
        }
    }

    public List<InstanceRegistryEntry> listRegistries() {
        return listRegistries(true);
    }

    public List<InstanceRegistryEntry> listRegistriesForAllocation() {
        return listRegistries(false);
    }

    public <T> T withPortReservationLock(Supplier<T> operation) {
        if (operation == null) {
            throw new InstanceRegistryException("operation is required");
        }
        portReservationLock.lock();
        try {
            return operation.get();
        } finally {
            portReservationLock.unlock();
        }
    }

    private List<InstanceRegistryEntry> listRegistries(boolean sanitizeVariables) {
        Path instancesRoot = workspaceLayout.getInstancesRoot();
        if (!Files.isDirectory(instancesRoot)) {
            return List.of();
        }
        List<InstanceRegistryEntry> entries = new ArrayList<>();
        try (Stream<Path> stream = Files.list(instancesRoot)) {
            stream.filter(Files::isDirectory)
                    .forEach(instanceRoot -> {
                        InstanceRegistryEntry entry = loadRegistryForListing(instanceRoot, sanitizeVariables);
                        if (entry != null) {
                            entries.add(entry);
                        }
                    });
        } catch (IOException ex) {
            throw new InstanceRegistryException("Failed to list instance registries at " + instancesRoot, ex);
        }
        entries.sort(Comparator.comparing(entry -> entry.instanceId().toString()));
        return entries;
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

    private InstanceRegistryEntry loadRegistryForListing(Path instanceRoot, boolean sanitizeVariables) {
        if (instanceRoot == null || !Files.isDirectory(instanceRoot)) {
            return null;
        }
        Path registryFile = instanceRoot.resolve(REGISTRY_FILENAME);
        if (!Files.isRegularFile(registryFile)) {
            return null;
        }
        try {
            InstanceRegistryEntry entry = objectMapper.readValue(registryFile.toFile(), InstanceRegistryEntry.class);
            if (entry == null) {
                logger.warn("Instance registry is empty at {}", registryFile);
                return null;
            }
            if (entry.instanceId() == null) {
                logger.warn("Instance registry has no instanceId at {}", registryFile);
                return null;
            }
            InstanceRegistryEntry normalized = ensureWorkspacePath(entry, instanceRoot, registryFile, true);
            if (sanitizeVariables) {
                return sanitizeForListing(normalized, instanceRoot);
            }
            return normalized;
        } catch (IOException ex) {
            logger.warn("Failed to read instance registry at {}", registryFile, ex);
            return null;
        }
    }

    private InstanceRegistryEntry sanitizeForListing(InstanceRegistryEntry entry, Path instanceRoot) {
        String relativeWorkspacePath = toRelativeWorkspacePath(instanceRoot);
        return new InstanceRegistryEntry(
                entry.instanceId(),
                entry.name(),
                entry.displayName(),
                entry.containerImage(),
                entry.installScript(),
                entry.startCommand(),
                entry.slotsRequired(),
                entry.portsJson(),
                entry.installCompleted(),
                null,
                entry.layers(),
                entry.preparedAt(),
                entry.containerId(),
                entry.containerStatus(),
                entry.containerStatusUpdatedAt(),
                entry.containerExitCode(),
                entry.containerExitReason(),
                relativeWorkspacePath
        );
    }

    private String toRelativeWorkspacePath(Path instanceRoot) {
        if (instanceRoot == null) {
            return null;
        }
        Path normalizedInstanceRoot = instanceRoot.toAbsolutePath().normalize();
        Path workspaceRoot = workspaceLayout.getWorkspaceRoot();
        if (workspaceRoot != null) {
            Path normalizedWorkspaceRoot = workspaceRoot.toAbsolutePath().normalize();
            if (normalizedInstanceRoot.startsWith(normalizedWorkspaceRoot)) {
                return normalizedWorkspaceRoot.relativize(normalizedInstanceRoot).toString();
            }
        }
        Path instancesRoot = workspaceLayout.getInstancesRoot();
        if (instancesRoot != null) {
            Path normalizedInstancesRoot = instancesRoot.toAbsolutePath().normalize();
            if (normalizedInstanceRoot.startsWith(normalizedInstancesRoot)) {
                Path relative = normalizedInstancesRoot.relativize(normalizedInstanceRoot);
                return normalizedInstancesRoot.getFileName().resolve(relative).toString();
            }
        }
        return null;
    }

    private InstanceRegistryEntry ensureWorkspacePath(
            InstanceRegistryEntry entry,
            Path instanceRoot,
            Path registryFile,
            boolean persistIfUpdated
    ) {
        String resolvedPath = resolveWorkspacePath(entry.workspacePath(), instanceRoot);
        if (Objects.equals(resolvedPath, entry.workspacePath())) {
            return entry;
        }
        InstanceRegistryEntry updated = new InstanceRegistryEntry(
                entry.instanceId(),
                entry.name(),
                entry.displayName(),
                entry.containerImage(),
                entry.installScript(),
                entry.startCommand(),
                entry.slotsRequired(),
                entry.portsJson(),
                entry.installCompleted(),
                entry.variables(),
                entry.layers(),
                entry.preparedAt(),
                entry.containerId(),
                entry.containerStatus(),
                entry.containerStatusUpdatedAt(),
                entry.containerExitCode(),
                entry.containerExitReason(),
                resolvedPath
        );
        if (persistIfUpdated && registryFile != null) {
            writeRegistry(registryFile, updated);
        }
        return updated;
    }

    private String resolveWorkspacePath(String existing, Path instanceRoot) {
        if (instanceRoot != null) {
            return instanceRoot.toAbsolutePath().normalize().toString();
        }
        if (existing != null && !existing.isBlank()) {
            return existing.trim();
        }
        return null;
    }

    private String normalizeExitReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.trim();
    }
}
