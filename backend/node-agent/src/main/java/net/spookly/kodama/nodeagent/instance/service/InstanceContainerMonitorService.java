package net.spookly.kodama.nodeagent.instance.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerStatus;
import net.spookly.kodama.nodeagent.docker.service.DockerService;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryEntry;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryException;
import net.spookly.kodama.nodeagent.instance.registry.InstanceRegistryService;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspaceLayout;
import net.spookly.kodama.nodeagent.instance.workspace.InstanceWorkspacePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InstanceContainerMonitorService {

  private static final Logger logger =
      LoggerFactory.getLogger(InstanceContainerMonitorService.class);

  private final DockerService dockerService;
  private final InstanceRegistryService registryService;
  private final InstanceWorkspaceLayout workspaceLayout;

  public InstanceContainerMonitorService(
      DockerService dockerService,
      InstanceRegistryService registryService,
      InstanceWorkspaceLayout workspaceLayout) {
    this.dockerService = Objects.requireNonNull(dockerService, "dockerService");
    this.registryService = Objects.requireNonNull(registryService, "registryService");
    this.workspaceLayout = Objects.requireNonNull(workspaceLayout, "workspaceLayout");
  }

  public void monitorOnce() {
    List<InstanceRegistryEntry> entries = registryService.listRegistries();
    if (entries.isEmpty()) {
      return;
    }
    for (InstanceRegistryEntry entry : entries) {
      if (entry == null) {
        continue;
      }
      UUID instanceId = entry.instanceId();
      if (instanceId == null) {
        logger.warn("Skipping registry entry with missing instanceId.");
        continue;
      }
      String containerId = normalizeContainerId(entry.containerId());
      if (containerId == null) {
        continue;
      }
      if (!shouldInspect(entry)) {
        continue;
      }
      InstanceWorkspacePaths workspace = workspaceLayout.resolveWorkspace(instanceId.toString());
      if (workspace == null) {
        logger.warn("Workspace layout missing for instanceId={}", instanceId);
        continue;
      }
      inspectAndRecord(entry, workspace, instanceId, containerId);
    }
  }

  private void inspectAndRecord(
      InstanceRegistryEntry entry,
      InstanceWorkspacePaths workspace,
      UUID instanceId,
      String containerId) {
    DockerContainerStatus status;
    try {
      status = dockerService.inspectContainerIfExists(containerId);
    } catch (RuntimeException ex) {
      logger.warn(
          "Failed to inspect container for instance monitor. instanceId={} containerId={}",
          instanceId,
          containerId,
          ex);
      return;
    }
    if (status == null) {
      Integer exitCode = entry.containerExitCode();
      String exitReason = normalizeExitReason(entry.containerExitReason());
      if (exitCode == null && exitReason == null) {
        exitReason = "missing";
      }
      if (shouldRecordStopped(entry, exitCode, exitReason)) {
        recordStopped(workspace, instanceId, exitCode, exitReason);
      }
      return;
    }
    if (Boolean.TRUE.equals(status.running())) {
      if (shouldRecordRunning(entry)) {
        recordRunning(workspace, instanceId);
      }
      return;
    }
    String exitReason = InstanceContainerExitReasonResolver.resolveExitReason(status);
    if (shouldRecordStopped(entry, status.exitCode(), exitReason)) {
      recordStopped(workspace, instanceId, status.exitCode(), exitReason);
    }
  }

  private void recordStopped(
      InstanceWorkspacePaths workspace, UUID instanceId, Integer exitCode, String exitReason) {
    try {
      registryService.recordContainerStatus(workspace, instanceId, "stopped", exitCode, exitReason);
    } catch (InstanceRegistryException ex) {
      logger.warn(
          "Failed to record stopped container status. instanceId={} reason={}",
          instanceId,
          exitReason,
          ex);
    }
  }

  private void recordRunning(InstanceWorkspacePaths workspace, UUID instanceId) {
    try {
      registryService.recordContainerStatus(workspace, instanceId, "running", null, null);
    } catch (InstanceRegistryException ex) {
      logger.warn("Failed to record running container status. instanceId={}", instanceId, ex);
    }
  }

  private boolean shouldInspect(InstanceRegistryEntry entry) {
    String status = normalizeStatus(entry.containerStatus());
    if (status == null || "running".equals(status) || "stopped".equals(status)) {
      return true;
    }
    return isExitMetadataMissing(entry);
  }

  private boolean shouldRecordRunning(InstanceRegistryEntry entry) {
    String status = normalizeStatus(entry.containerStatus());
    if (!"running".equals(status)) {
      return true;
    }
    return entry.containerExitCode() != null || hasText(entry.containerExitReason());
  }

  private boolean shouldRecordStopped(
      InstanceRegistryEntry entry, Integer exitCode, String exitReason) {
    String status = normalizeStatus(entry.containerStatus());
    if (!"stopped".equals(status)) {
      return true;
    }
    Integer recordedExitCode = entry.containerExitCode();
    String recordedExitReason = normalizeExitReason(entry.containerExitReason());
    String normalizedExitReason = normalizeExitReason(exitReason);
    if (!Objects.equals(recordedExitCode, exitCode)) {
      return true;
    }
    return !Objects.equals(recordedExitReason, normalizedExitReason);
  }

  private boolean isExitMetadataMissing(InstanceRegistryEntry entry) {
    return entry.containerExitCode() == null && !hasText(entry.containerExitReason());
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String normalizeExitReason(String exitReason) {
    if (!hasText(exitReason)) {
      return null;
    }
    return exitReason.trim();
  }

  private String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    return status.trim().toLowerCase();
  }

  private String normalizeContainerId(String containerId) {
    if (containerId == null || containerId.isBlank()) {
      return null;
    }
    return containerId.trim();
  }
}
