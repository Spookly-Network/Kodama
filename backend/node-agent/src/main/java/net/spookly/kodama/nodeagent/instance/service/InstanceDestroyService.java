package net.spookly.kodama.nodeagent.instance.service;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import net.spookly.kodama.nodeagent.config.InstanceProperties;
import net.spookly.kodama.nodeagent.config.NodeConfig;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerStatus;
import net.spookly.kodama.nodeagent.docker.service.DockerOperationException;
import net.spookly.kodama.nodeagent.docker.service.DockerService;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryException;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceManager;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstanceDestroyService {

  private static final Logger logger = LoggerFactory.getLogger(InstanceDestroyService.class);
  private static final String CONTAINER_NAME_PREFIX = "kodama-instance-";
  private static final String REGISTRY_FILENAME = "instance.json";

  private final DockerService dockerService;
  private final InstanceRegistryService registryService;
  private final InstanceWorkspaceLayout workspaceLayout;
  private final InstanceWorkspaceManager workspaceManager;
  private final NodeConfig config;
  private final InstanceProperties instanceProperties;

  public InstanceDestroyService(
      DockerService dockerService,
      InstanceRegistryService registryService,
      InstanceWorkspaceLayout workspaceLayout,
      InstanceWorkspaceManager workspaceManager,
      NodeConfig config,
      InstanceProperties instanceProperties) {
    this.dockerService = Objects.requireNonNull(dockerService, "dockerService");
    this.registryService = Objects.requireNonNull(registryService, "registryService");
    this.workspaceLayout = Objects.requireNonNull(workspaceLayout, "workspaceLayout");
    this.workspaceManager = Objects.requireNonNull(workspaceManager, "workspaceManager");
    this.config = Objects.requireNonNull(config, "config");
    this.instanceProperties = Objects.requireNonNull(instanceProperties, "instanceProperties");
  }

  public void destroyInstance(UUID instanceId) {
    if (instanceId == null) {
      throw new InstanceDestroyException("instanceId is required");
    }
    InstanceWorkspacePaths workspace = workspaceLayout.resolveWorkspace(instanceId.toString());
    String containerId = resolveContainerId(instanceId, workspace);
    stopAndRemoveContainer(instanceId, containerId);
    deleteRegistryIfExists(instanceId, workspace);
    deleteWorkspaceIfExists(instanceId, workspace);
    logger.info(
        "Instance destroy completed. instanceId={} containerId={} workspaceRoot={}",
        instanceId,
        valueOrDash(containerId),
        workspace.instanceRoot());
  }

  private String resolveContainerId(UUID instanceId, InstanceWorkspacePaths workspace) {
    if (workspace != null && Files.isDirectory(workspace.instanceRoot())) {
      Path registryFile = workspace.instanceRoot().resolve(REGISTRY_FILENAME);
      if (Files.isRegularFile(registryFile)) {
        try {
          InstanceRegistryEntry registry = registryService.loadRegistry(workspace);
          if (registry != null
              && registry.instanceId() != null
              && !instanceId.equals(registry.instanceId())) {
            logger.warn(
                "Instance registry does not match instanceId during destroy. instanceId={} registryInstanceId={}",
                instanceId,
                registry.instanceId());
          } else if (registry != null && hasText(registry.containerId())) {
            return registry.containerId().trim();
          }
        } catch (InstanceRegistryException ex) {
          logger.warn(
              "Failed to read instance registry during destroy. instanceId={} path={}",
              instanceId,
              registryFile,
              ex);
        }
      } else {
        logger.info(
            "Instance registry missing during destroy. instanceId={} path={}",
            instanceId,
            registryFile);
      }
    } else {
      logger.info(
          "Instance workspace missing during destroy. instanceId={} workspaceRoot={}",
          instanceId,
          workspaceRootOrDash(workspace));
    }
    return CONTAINER_NAME_PREFIX + instanceId;
  }

  private void stopAndRemoveContainer(UUID instanceId, String containerId) {
    if (!hasText(containerId)) {
      logger.info("No container id resolved for destroy. instanceId={}", instanceId);
      return;
    }
    String normalizedContainerId = containerId.trim();
    DockerContainerStatus status = dockerService.inspectContainerIfExists(normalizedContainerId);
    if (status == null) {
      logger.info(
          "Instance container not found during destroy. instanceId={} containerId={}",
          instanceId,
          normalizedContainerId);
      return;
    }
    if (Boolean.TRUE.equals(status.running())) {
      Integer stopTimeoutSeconds = resolveStopTimeoutSeconds();
      try {
        dockerService.stopContainer(normalizedContainerId, stopTimeoutSeconds);
      } catch (DockerOperationException ex) {
        if (!isStopRace(ex)) {
          throw ex;
        }
        logger.warn(
            "Instance container already stopped during destroy stop. instanceId={} containerId={}",
            instanceId,
            normalizedContainerId);
      }
      DockerContainerStatus stoppedStatus =
          dockerService.inspectContainerIfExists(normalizedContainerId);
      if (stoppedStatus != null && Boolean.TRUE.equals(stoppedStatus.running())) {
        try {
          dockerService.killContainer(normalizedContainerId);
        } catch (DockerOperationException ex) {
          if (!isStopRace(ex)) {
            throw ex;
          }
          logger.warn(
              "Instance container already stopped during destroy kill. instanceId={} containerId={}",
              instanceId,
              normalizedContainerId);
        }
        stoppedStatus = dockerService.inspectContainerIfExists(normalizedContainerId);
        if (stoppedStatus != null && Boolean.TRUE.equals(stoppedStatus.running())) {
          throw new InstanceDestroyException(
              "Container still running after force kill: " + normalizedContainerId);
        }
      }
    }
    try {
      dockerService.removeContainer(normalizedContainerId, true, false);
      logger.info(
          "Instance container removed during destroy. instanceId={} containerId={}",
          instanceId,
          normalizedContainerId);
    } catch (DockerOperationException ex) {
      if (isContainerMissing(ex)) {
        logger.info(
            "Instance container already removed during destroy. instanceId={} containerId={}",
            instanceId,
            normalizedContainerId);
        return;
      }
      throw ex;
    }
  }

  private void deleteRegistryIfExists(UUID instanceId, InstanceWorkspacePaths workspace) {
    if (workspace == null || workspace.instanceRoot() == null) {
      return;
    }
    Path instanceRoot = workspace.instanceRoot();
    if (!Files.isDirectory(instanceRoot)) {
      return;
    }
    try {
      registryService.deleteRegistryIfExists(workspace);
    } catch (InstanceRegistryException ex) {
      logger.warn(
          "Failed to delete instance registry during destroy. instanceId={} path={}",
          instanceId,
          instanceRoot,
          ex);
      throw ex;
    }
  }

  private void deleteWorkspaceIfExists(UUID instanceId, InstanceWorkspacePaths workspace) {
    if (workspace == null || workspace.instanceRoot() == null) {
      return;
    }
    if (!Files.exists(workspace.instanceRoot())) {
      logger.info(
          "Instance workspace already removed during destroy. instanceId={} path={}",
          instanceId,
          workspace.instanceRoot());
      return;
    }
    workspaceManager.deleteWorkspace(workspace);
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

  private boolean isContainerMissing(DockerOperationException ex) {
    if (ex == null) {
      return false;
    }
    return ex.getCause() instanceof NotFoundException;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String valueOrDash(String value) {
    if (!hasText(value)) {
      return "-";
    }
    return value.trim();
  }

  private String workspaceRootOrDash(InstanceWorkspacePaths workspace) {
    if (workspace == null || workspace.instanceRoot() == null) {
      return "-";
    }
    return workspace.instanceRoot().toString();
  }
}
