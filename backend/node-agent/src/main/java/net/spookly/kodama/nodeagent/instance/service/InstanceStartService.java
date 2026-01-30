package net.spookly.kodama.nodeagent.instance.service;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateRequest;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateResult;
import net.spookly.kodama.nodeagent.docker.dto.DockerPortBinding;
import net.spookly.kodama.nodeagent.docker.dto.DockerVolumeMount;
import net.spookly.kodama.nodeagent.docker.service.DockerService;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstanceStartService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceStartService.class);

    private final DockerService dockerService;
    private final InstanceRegistryService registryService;
    private final InstanceWorkspaceLayout workspaceLayout;
    private final InstancePortBindingsResolver portBindingsResolver;
    private final NodeConfig config;

    public InstanceStartService(
            DockerService dockerService,
            InstanceRegistryService registryService,
            InstanceWorkspaceLayout workspaceLayout,
            InstancePortBindingsResolver portBindingsResolver,
            NodeConfig config
    ) {
        this.dockerService = Objects.requireNonNull(dockerService, "dockerService");
        this.registryService = Objects.requireNonNull(registryService, "registryService");
        this.workspaceLayout = Objects.requireNonNull(workspaceLayout, "workspaceLayout");
        this.portBindingsResolver = Objects.requireNonNull(portBindingsResolver, "portBindingsResolver");
        this.config = Objects.requireNonNull(config, "config");
    }

    public String startInstance(UUID instanceId, String requestedName) {
        if (instanceId == null) {
            throw new InstanceStartException("instanceId is required");
        }
        InstanceWorkspacePaths workspace = workspaceLayout.resolveWorkspace(instanceId.toString());
        requirePreparedWorkspace(workspace);
        InstanceRegistryEntry registry = registryService.loadRegistry(workspace);
        if (!instanceId.equals(registry.instanceId())) {
            throw new InstanceStartException("Instance registry does not match instanceId: " + instanceId);
        }
        String image = resolveImage(registry);
        String mountPath = resolveWorkspaceMountPath();
        String workingDir = resolveWorkingDir(mountPath);
        List<DockerVolumeMount> volumeMounts = List.of(
                new DockerVolumeMount(workspace.mergedDir().toString(), mountPath, false)
        );
        List<DockerPortBinding> portBindings = portBindingsResolver.resolveBindings(registry);
        List<String> env = buildEnv(instanceId, registry, requestedName);
        Map<String, String> labels = buildLabels(instanceId, registry);
        String containerName = "kodama-instance-" + instanceId;

        DockerContainerCreateRequest request = new DockerContainerCreateRequest(
                image,
                containerName,
                null,
                env,
                labels,
                workingDir,
                portBindings,
                volumeMounts
        );
        DockerContainerCreateResult result = dockerService.createContainer(request);
        String containerId = result.containerId();
        try {
            dockerService.startContainer(containerId);
        } catch (RuntimeException ex) {
            removeContainerSafely(containerId, instanceId, "start failure");
            throw ex;
        }
        try {
            registryService.recordContainerId(workspace, instanceId, containerId);
        } catch (RuntimeException ex) {
            removeContainerSafely(containerId, instanceId, "registry update failure");
            throw ex;
        }
        logger.info("Instance container started. instanceId={} containerId={}", instanceId, containerId);
        return containerId;
    }

    private void requirePreparedWorkspace(InstanceWorkspacePaths workspace) {
        if (workspace == null) {
            throw new InstanceStartException("instance workspace is required");
        }
        if (!Files.isDirectory(workspace.instanceRoot())) {
            throw new InstanceStartException("Instance workspace root is missing at " + workspace.instanceRoot());
        }
        if (!Files.isDirectory(workspace.mergedDir())) {
            throw new InstanceStartException("Instance merged workspace is missing at " + workspace.mergedDir());
        }
    }

    private String resolveImage(InstanceRegistryEntry registry) {
        Map<String, String> variables = registry.variables() == null ? Map.of() : registry.variables();
        String image = firstNonBlank(
                variables.get("DOCKER_IMAGE"),
                variables.get("CONTAINER_IMAGE"),
                variables.get("IMAGE")
        );
        if (isBlank(image)) {
            image = config.getInstanceRuntime().getImage();
        }
        if (isBlank(image)) {
            throw new InstanceStartException("Container image is required (variables DOCKER_IMAGE or node-agent.instance-runtime.image)");
        }
        return image.trim();
    }

    private String resolveWorkspaceMountPath() {
        String mountPath = config.getInstanceRuntime().getWorkspaceMountPath();
        if (isBlank(mountPath)) {
            throw new InstanceStartException("node-agent.instance-runtime.workspace-mount-path is required");
        }
        return mountPath.trim();
    }

    private String resolveWorkingDir(String mountPath) {
        String workingDir = config.getInstanceRuntime().getWorkingDir();
        if (isBlank(workingDir)) {
            return mountPath;
        }
        return workingDir.trim();
    }

    private List<String> buildEnv(UUID instanceId, InstanceRegistryEntry registry, String requestedName) {
        Map<String, String> env = new LinkedHashMap<>();
        if (registry.variables() != null) {
            env.putAll(registry.variables());
        }
        env.put("INSTANCE_ID", instanceId.toString());
        if (hasText(config.getNodeName())) {
            env.put("NODE_NAME", config.getNodeName().trim());
        }
        String instanceName = firstNonBlank(requestedName, registry.name(), registry.displayName());
        if (hasText(instanceName)) {
            env.put("INSTANCE_NAME", instanceName.trim());
        }
        if (hasText(registry.displayName())) {
            env.put("INSTANCE_DISPLAY_NAME", registry.displayName().trim());
        }
        return env.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
    }

    private Map<String, String> buildLabels(UUID instanceId, InstanceRegistryEntry registry) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("kodama.instance-id", instanceId.toString());
        if (hasText(registry.name())) {
            labels.put("kodama.instance-name", registry.name().trim());
        }
        if (hasText(config.getNodeName())) {
            labels.put("kodama.node-name", config.getNodeName().trim());
        }
        return labels;
    }

    private void removeContainerSafely(String containerId, UUID instanceId, String reason) {
        try {
            dockerService.removeContainer(containerId, true, false);
            logger.warn(
                    "Removed container after {}. instanceId={} containerId={}",
                    reason,
                    instanceId,
                    containerId
            );
        } catch (RuntimeException ex) {
            logger.warn(
                    "Failed to remove container after {}. instanceId={} containerId={}",
                    reason,
                    instanceId,
                    containerId,
                    ex
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String first, String second, String third) {
        if (hasText(first)) {
            return first;
        }
        if (hasText(second)) {
            return second;
        }
        return hasText(third) ? third : null;
    }
}
