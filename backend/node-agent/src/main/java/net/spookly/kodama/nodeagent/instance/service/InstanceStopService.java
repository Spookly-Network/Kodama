package net.spookly.kodama.nodeagent.instance.service;

import java.nio.file.Files;
import java.util.UUID;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.spookly.kodama.nodeagent.config.InstanceProperties;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerStatus;
import net.spookly.kodama.nodeagent.docker.service.DockerOperationException;
import net.spookly.kodama.nodeagent.docker.service.DockerService;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InstanceStopService {

    private static final Logger logger = LoggerFactory.getLogger(InstanceStopService.class);

    @NonNull
    private final DockerService dockerService;
    @NonNull
    private final InstanceRegistryService registryService;
    @NonNull
    private final InstanceWorkspaceLayout workspaceLayout;
    @NonNull
    private final NodeConfig config;
    @NonNull
    private final InstanceProperties instanceProperties;

    public void stopInstance(UUID instanceId) {
        if (instanceId == null) {
            throw new InstanceStopException("instanceId is required");
        }
        InstanceWorkspacePaths workspace = workspaceLayout.resolveWorkspace(instanceId.toString());
        requireWorkspace(workspace);
        InstanceRegistryEntry registry = registryService.loadRegistry(workspace);
        if (!instanceId.equals(registry.instanceId())) {
            throw new InstanceStopException("Instance registry does not match instanceId: " + instanceId);
        }
        String containerId = requireContainerId(registry.containerId());
        DockerContainerStatus initialStatus = dockerService.inspectContainerIfExists(containerId);
        if (initialStatus == null) {
            logger.warn(
                    "Instance container not found, marking stopped. instanceId={} containerId={}",
                    instanceId,
                    containerId
            );
            ExitMetadata exitMetadata = resolveExitMetadata(registry, "missing");
            registryService.recordContainerStatus(
                    workspace,
                    instanceId,
                    "stopped",
                    exitMetadata.exitCode(),
                    exitMetadata.exitReason()
            );
            return;
        }
        if (Boolean.FALSE.equals(initialStatus.running())) {
            logger.info(
                    "Instance container already stopped. instanceId={} containerId={} status={}",
                    instanceId,
                    containerId,
                    safeStatus(initialStatus)
            );
            registryService.recordContainerStatus(
                    workspace,
                    instanceId,
                    "stopped",
                    initialStatus.exitCode(),
                    InstanceContainerExitReasonResolver.resolveExitReason(initialStatus)
            );
            return;
        }
        Integer stopTimeoutSeconds = resolveStopTimeoutSeconds();
        try {
            dockerService.stopContainer(containerId, stopTimeoutSeconds);
        } catch (DockerOperationException ex) {
            if (isStopRace(ex)) {
                logger.warn(
                        "Instance container already stopped during stop. instanceId={} containerId={}",
                        instanceId,
                        containerId
                );
                ExitMetadata exitMetadata = resolveExitMetadata(registry, resolveStopRaceReason(ex));
                registryService.recordContainerStatus(
                        workspace,
                        instanceId,
                        "stopped",
                        exitMetadata.exitCode(),
                        exitMetadata.exitReason()
                );
                return;
            }
            throw ex;
        }
        DockerContainerStatus stoppedStatus = dockerService.inspectContainerIfExists(containerId);
        if (stoppedStatus != null && Boolean.TRUE.equals(stoppedStatus.running())) {
            logger.warn(
                    "Instance container still running after stop, forcing kill. instanceId={} containerId={}",
                    instanceId,
                    containerId
            );
            try {
                dockerService.killContainer(containerId);
            } catch (DockerOperationException ex) {
                if (isStopRace(ex)) {
                    logger.warn(
                        "Instance container already stopped during kill. instanceId={} containerId={}",
                        instanceId,
                        containerId
                );
                    ExitMetadata exitMetadata = resolveExitMetadata(registry, resolveStopRaceReason(ex));
                    registryService.recordContainerStatus(
                            workspace,
                            instanceId,
                            "stopped",
                            exitMetadata.exitCode(),
                            exitMetadata.exitReason()
                    );
                    return;
                }
                throw ex;
            }
            stoppedStatus = dockerService.inspectContainerIfExists(containerId);
            if (stoppedStatus != null && Boolean.TRUE.equals(stoppedStatus.running())) {
                throw new InstanceStopException("Container still running after force kill: " + containerId);
            }
        }
        ExitMetadata exitMetadata;
        if (stoppedStatus == null) {
            exitMetadata = resolveExitMetadata(registry, "stopped");
        } else {
            exitMetadata = new ExitMetadata(
                    stoppedStatus.exitCode(),
                    InstanceContainerExitReasonResolver.resolveExitReason(stoppedStatus)
            );
        }
        registryService.recordContainerStatus(
                workspace,
                instanceId,
                "stopped",
                exitMetadata.exitCode(),
                exitMetadata.exitReason()
        );
        logger.info(
                "Instance container stopped. instanceId={} containerId={} status={}",
                instanceId,
                containerId,
                safeStatus(stoppedStatus)
        );
    }

    private void requireWorkspace(InstanceWorkspacePaths workspace) {
        if (workspace == null) {
            throw new InstanceStopException("instance workspace is required");
        }
        if (!Files.isDirectory(workspace.instanceRoot())) {
            throw new InstanceStopException("Instance workspace root is missing at " + workspace.instanceRoot());
        }
    }

    private String requireContainerId(String containerId) {
        if (containerId == null || containerId.isBlank()) {
            throw new InstanceStopException("containerId is required to stop instance");
        }
        return containerId.trim();
    }

    private Integer resolveStopTimeoutSeconds() {
        if (instanceProperties.getInstanceRuntime() == null) {
            return null;
        }
        return instanceProperties.getInstanceRuntime().getStopTimeoutSeconds();
    }

    private boolean isStopRace(DockerOperationException ex) {
        if (ex == null) {
            return false;
        }
        Throwable cause = ex.getCause();
        return cause instanceof NotFoundException || cause instanceof NotModifiedException;
    }

    private String resolveStopRaceReason(DockerOperationException ex) {
        if (ex == null) {
            return "already-stopped";
        }
        Throwable cause = ex.getCause();
        if (cause instanceof NotFoundException) {
            return "missing";
        }
        if (cause instanceof NotModifiedException) {
            return "already-stopped";
        }
        return "already-stopped";
    }

    private ExitMetadata resolveExitMetadata(InstanceRegistryEntry registry, String fallbackReason) {
        Integer exitCode = registry == null ? null : registry.containerExitCode();
        String exitReason = normalizeExitReason(registry == null ? null : registry.containerExitReason());
        if (exitCode == null && exitReason == null) {
            exitReason = fallbackReason;
        }
        return new ExitMetadata(exitCode, exitReason);
    }

    private String normalizeExitReason(String exitReason) {
        if (exitReason == null || exitReason.isBlank()) {
            return null;
        }
        return exitReason.trim();
    }

    private String safeStatus(DockerContainerStatus status) {
        if (status == null) {
            return "unknown";
        }
        String state = status.status();
        if (state == null || state.isBlank()) {
            return "unknown";
        }
        return state.trim();
    }

    private record ExitMetadata(Integer exitCode, String exitReason) {
    }
}
