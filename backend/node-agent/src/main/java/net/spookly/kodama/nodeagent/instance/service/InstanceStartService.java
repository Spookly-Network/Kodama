package net.spookly.kodama.nodeagent.instance.service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.spookly.kodama.nodeagent.config.InstanceProperties;
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
import net.spookly.kodama.nodeagent.plugin.NodeInstanceStartContext;
import net.spookly.kodama.nodeagent.plugin.NodeInstanceStartSpec;
import net.spookly.kodama.nodeagent.plugin.NodePluginRegistry;
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
    private final InstanceProperties instanceProperties;
    private final NodePluginRegistry pluginRegistry;
    private final InstanceInstallScriptRunner installScriptRunner;

    public InstanceStartService(
            DockerService dockerService,
            InstanceRegistryService registryService,
            InstanceWorkspaceLayout workspaceLayout,
            InstancePortBindingsResolver portBindingsResolver,
            NodeConfig config,
            InstanceProperties instanceProperties,
            NodePluginRegistry pluginRegistry,
            InstanceInstallScriptRunner installScriptRunner
    ) {
        this.dockerService = Objects.requireNonNull(dockerService, "dockerService");
        this.registryService = Objects.requireNonNull(registryService, "registryService");
        this.workspaceLayout = Objects.requireNonNull(workspaceLayout, "workspaceLayout");
        this.portBindingsResolver = Objects.requireNonNull(portBindingsResolver, "portBindingsResolver");
        this.config = Objects.requireNonNull(config, "config");
        this.instanceProperties = Objects.requireNonNull(instanceProperties, "instanceProperties");
        this.pluginRegistry = Objects.requireNonNull(pluginRegistry, "pluginRegistry");
        this.installScriptRunner = Objects.requireNonNull(installScriptRunner, "installScriptRunner");
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
        List<String> startCommand = resolveStartCommand(registry);
        Map<String, String> baseEnv = buildEnvMap(instanceId, registry, requestedName);
        boolean installCompleted = runInstallScriptIfRequired(instanceId, workspace, registry, baseEnv);
        if (installCompleted) {
            registry = registryService.loadRegistry(workspace);
            image = resolveImage(registry);
            startCommand = resolveStartCommand(registry);
        }
        String mountPath = resolveWorkspaceMountPath();
        String workingDir = resolveWorkingDir(mountPath);
        List<DockerVolumeMount> volumeMounts = List.of(
                new DockerVolumeMount(workspace.mergedDir().toString(), mountPath, false)
        );
        List<DockerPortBinding> portBindings = portBindingsResolver.resolveBindings(registry);
        Map<String, String> baseLabels = buildLabels(instanceId, registry);
        NodeInstanceStartContext context = new NodeInstanceStartContext(
                instanceId,
                requestedName,
                registry,
                baseEnv,
                baseLabels,
                null
        );
        NodeInstanceStartSpec spec = pluginRegistry.resolveStartSpec(context, baseEnv, baseLabels, startCommand);
        List<String> env = toEnvList(spec.env());
        Map<String, String> labels = spec.labels();
        List<String> command = requireCommand(spec.command());
        String containerName = "kodama-instance-" + instanceId;

        DockerContainerCreateRequest request = new DockerContainerCreateRequest(
                image,
                containerName,
                command,
                env,
                labels,
                workingDir,
                portBindings,
                volumeMounts
        );

        DockerContainerCreateResult result = dockerService.createContainer(request);
        String containerId = result.containerId();
        try {
            registryService.recordContainerId(workspace, instanceId, containerId, "starting");
        } catch (RuntimeException ex) {
            removeContainerSafely(containerId, instanceId, "registry update failure");
            throw ex;
        }
        try {
            dockerService.startContainer(containerId);
        } catch (RuntimeException ex) {
            cleanupAfterStartFailure(workspace, instanceId, containerId, "start failure");
            throw ex;
        }
        try {
            registryService.recordContainerStatus(workspace, instanceId, "running", null, null);
        } catch (RuntimeException ex) {
            cleanupAfterStartFailure(workspace, instanceId, containerId, "registry update failure");
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
        String image = registry.containerImage();
        if (isBlank(image)) {
            throw new InstanceStartException("Container image is required in instance registry");
        }
        return image.trim();
    }

    private List<String> resolveStartCommand(InstanceRegistryEntry registry) {
        return requireCommand(registry.startCommand());
    }

    private List<String> requireCommand(List<String> command) {
        if (command == null || command.isEmpty()) {
            throw new InstanceStartException("Start command is required in instance registry");
        }
        List<String> normalized = command.stream()
                .map(this::normalizeCommandPart)
                .toList();
        if (normalized.isEmpty()) {
            throw new InstanceStartException("Start command is required in instance registry");
        }
        return normalized;
    }

    private String normalizeCommandPart(String commandPart) {
        if (commandPart == null || commandPart.isBlank()) {
            throw new InstanceStartException("Start command contains an empty element");
        }
        return commandPart.trim();
    }

    private boolean runInstallScriptIfRequired(
            UUID instanceId,
            InstanceWorkspacePaths workspace,
            InstanceRegistryEntry registry,
            Map<String, String> env
    ) {
        if (registry.installCompleted()) {
            return false;
        }
        if (isBlank(registry.installScript())) {
            logger.info("Install script not set, skipping install step. instanceId={}", instanceId);
            return false;
        }

        String script = registry.installScript().trim();
        logger.info("Running install script before first start. instanceId={}", instanceId);
        try {
            int exitCode = installScriptRunner.runScript(workspace.mergedDir(), script, env);
            if (exitCode != 0) {
                throw new InstanceStartException(
                        "Install script failed for instance " + instanceId + " with exit code " + exitCode
                );
            }
        } catch (IOException ex) {
            throw new InstanceStartException("Failed to execute install script for instance " + instanceId, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InstanceStartException("Install script execution interrupted for instance " + instanceId, ex);
        }
        registryService.recordInstallCompleted(workspace, instanceId);
        logger.info("Install script completed. instanceId={}", instanceId);
        return true;
    }

    private String resolveWorkspaceMountPath() {
        String mountPath = instanceProperties.getInstanceRuntime().getWorkspaceMountPath();
        if (isBlank(mountPath)) {
            throw new InstanceStartException("node-agent.instance-runtime.workspace-mount-path is required");
        }
        return mountPath.trim();
    }

    private String resolveWorkingDir(String mountPath) {
        String workingDir = instanceProperties.getInstanceRuntime().getWorkingDir();
        if (isBlank(workingDir)) {
            return mountPath;
        }
        return workingDir.trim();
    }

    private Map<String, String> buildEnvMap(UUID instanceId, InstanceRegistryEntry registry, String requestedName) {
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
        return env;
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

    private List<String> toEnvList(Map<String, String> env) {
        if (env == null || env.isEmpty()) {
            return List.of();
        }
        return env.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
    }

    private void cleanupAfterStartFailure(
            InstanceWorkspacePaths workspace,
            UUID instanceId,
            String containerId,
            String reason
    ) {
        boolean removed = removeContainerSafely(containerId, instanceId, reason);
        if (!removed) {
            return;
        }
        clearRegistryContainerStateSafely(workspace, instanceId, reason);
    }

    private boolean removeContainerSafely(String containerId, UUID instanceId, String reason) {
        try {
            dockerService.removeContainer(containerId, true, false);
            logger.warn(
                    "Removed container after {}. instanceId={} containerId={}",
                    reason,
                    instanceId,
                    containerId
            );
            return true;
        } catch (RuntimeException ex) {
            logger.warn(
                    "Failed to remove container after {}. instanceId={} containerId={}",
                    reason,
                    instanceId,
                    containerId,
                    ex
            );
            return false;
        }
    }

    private void clearRegistryContainerStateSafely(
            InstanceWorkspacePaths workspace,
            UUID instanceId,
            String reason
    ) {
        try {
            registryService.clearContainerState(workspace, instanceId, "stopped", null, "start-failed");
            logger.warn(
                    "Cleared registry container state after {}. instanceId={}",
                    reason,
                    instanceId
            );
        } catch (RuntimeException ex) {
            logger.warn(
                    "Failed to clear registry container state after {}. instanceId={}",
                    reason,
                    instanceId,
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
