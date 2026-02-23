package net.spookly.kodama.nodeagent.docker.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateRequest;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateResult;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerStatus;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerSummary;
import net.spookly.kodama.nodeagent.docker.dto.DockerImageSummary;
import net.spookly.kodama.nodeagent.docker.dto.DockerPortBinding;
import net.spookly.kodama.nodeagent.docker.dto.DockerVolumeMount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DockerService {

  private static final Logger logger = LoggerFactory.getLogger(DockerService.class);
  private static final String MISSING_IMAGE_MESSAGE = "no such image";

  private final DockerClient dockerClient;

  public DockerService(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
  }

  public DockerContainerCreateResult createContainer(DockerContainerCreateRequest request) {
    if (request == null) {
      throw new DockerOperationException("Container create request is required");
    }
    if (isBlank(request.image())) {
      throw new DockerOperationException("Container image is required");
    }
    try {
      return createContainerOnce(request);
    } catch (ConflictException ex) {
      throw new DockerOperationException("Docker container name conflict", ex);
    } catch (DockerException ex) {
      return handleCreateContainerFailure(request, ex);
    }
  }

  private DockerContainerCreateResult createContainerAfterPull(
      DockerContainerCreateRequest request) {
    String image = request.image().trim();
    pullImage(image);
    return createContainerWithMappedErrors(
        request, "Docker create container failed after pulling image: " + image);
  }

  private DockerContainerCreateResult handleCreateContainerFailure(
      DockerContainerCreateRequest request, DockerException exception) {
    if (isMissingImageFailure(exception)) {
      return createContainerAfterPull(request);
    }
    throw new DockerOperationException("Docker create container failed", exception);
  }

  private DockerContainerCreateResult createContainerWithMappedErrors(
      DockerContainerCreateRequest request, String dockerFailureMessage) {
    try {
      return createContainerOnce(request);
    } catch (ConflictException ex) {
      throw new DockerOperationException("Docker container name conflict", ex);
    } catch (DockerException ex) {
      throw new DockerOperationException(dockerFailureMessage, ex);
    }
  }

  private DockerContainerCreateResult createContainerOnce(DockerContainerCreateRequest request) {
    CreateContainerCmd createCmd = dockerClient.createContainerCmd(request.image());
    if (hasText(request.name())) {
      createCmd.withName(request.name());
    }
    if (request.command() != null && !request.command().isEmpty()) {
      createCmd.withCmd(request.command().toArray(String[]::new));
    }
    if (request.env() != null && !request.env().isEmpty()) {
      createCmd.withEnv(request.env().toArray(String[]::new));
    }
    if (request.labels() != null && !request.labels().isEmpty()) {
      createCmd.withLabels(request.labels());
    }
    if (hasText(request.workingDir())) {
      createCmd.withWorkingDir(request.workingDir());
    }
    HostConfig hostConfig = buildHostConfig(request.volumeMounts(), request.portBindings());
    if (hostConfig != null) {
      createCmd.withHostConfig(hostConfig);
    }
    List<ExposedPort> exposedPorts = buildExposedPorts(request.portBindings());
    if (!exposedPorts.isEmpty()) {
      createCmd.withExposedPorts(exposedPorts);
    }
    CreateContainerResponse response = createCmd.exec();
    List<String> warnings =
        response.getWarnings() == null ? List.of() : Arrays.asList(response.getWarnings());
    return new DockerContainerCreateResult(response.getId(), warnings);
  }

  private void pullImage(String image) {
    logger.info("Container image missing locally, pulling image. image={}", image);
    try {
      PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
      pullImageCmd.start().awaitCompletion();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new DockerOperationException("Docker image pull interrupted: " + image, ex);
    } catch (RuntimeException ex) {
      throw new DockerOperationException("Docker image pull failed: " + image, ex);
    }
  }

  private boolean isMissingImageFailure(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      String message = current.getMessage();
      if (message != null && message.toLowerCase(Locale.ROOT).contains(MISSING_IMAGE_MESSAGE)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  public void startContainer(String containerId) {
    String normalizedId = requireContainerId(containerId);
    try {
      dockerClient.startContainerCmd(normalizedId).exec();
    } catch (NotFoundException ex) {
      throw new DockerOperationException("Docker container not found: " + normalizedId, ex);
    } catch (DockerException ex) {
      throw new DockerOperationException("Docker start container failed: " + normalizedId, ex);
    }
  }

  public void stopContainer(String containerId, Integer timeoutSeconds) {
    String normalizedId = requireContainerId(containerId);
    try {
      StopContainerCmd stopCmd = dockerClient.stopContainerCmd(normalizedId);
      if (timeoutSeconds != null) {
        stopCmd.withTimeout(timeoutSeconds);
      }
      stopCmd.exec();
    } catch (NotFoundException ex) {
      throw new DockerOperationException("Docker container not found: " + normalizedId, ex);
    } catch (NotModifiedException ex) {
      throw new DockerOperationException("Docker container not running: " + normalizedId, ex);
    } catch (DockerException ex) {
      throw new DockerOperationException("Docker stop container failed: " + normalizedId, ex);
    }
  }

  public void killContainer(String containerId) {
    String normalizedId = requireContainerId(containerId);
    try {
      dockerClient.killContainerCmd(normalizedId).exec();
    } catch (NotFoundException ex) {
      throw new DockerOperationException("Docker container not found: " + normalizedId, ex);
    } catch (DockerException ex) {
      throw new DockerOperationException("Docker kill container failed: " + normalizedId, ex);
    }
  }

  public void removeContainer(String containerId, boolean force, boolean removeVolumes) {
    String normalizedId = requireContainerId(containerId);
    try {
      dockerClient
          .removeContainerCmd(normalizedId)
          .withForce(force)
          .withRemoveVolumes(removeVolumes)
          .exec();
    } catch (NotFoundException ex) {
      throw new DockerOperationException("Docker container not found: " + normalizedId, ex);
    } catch (DockerException ex) {
      throw new DockerOperationException("Docker remove container failed: " + normalizedId, ex);
    }
  }

  public DockerContainerStatus inspectContainer(String containerId) {
    String normalizedId = requireContainerId(containerId);
    try {
      InspectContainerResponse response = dockerClient.inspectContainerCmd(normalizedId).exec();
      InspectContainerResponse.ContainerState state = response.getState();
      return new DockerContainerStatus(
          response.getId(),
          response.getName(),
          response.getConfig() == null ? null : response.getConfig().getImage(),
          state == null ? null : state.getStatus(),
          state == null ? null : state.getRunning(),
          state == null ? null : state.getPaused(),
          state == null ? null : state.getRestarting(),
          state == null ? null : state.getOOMKilled(),
          state == null ? null : state.getDead(),
          state == null ? null : state.getExitCode(),
          state == null ? null : state.getError(),
          state == null ? null : state.getStartedAt(),
          state == null ? null : state.getFinishedAt());
    } catch (NotFoundException ex) {
      throw new DockerOperationException("Docker container not found: " + normalizedId, ex);
    } catch (DockerException ex) {
      throw new DockerOperationException("Docker inspect container failed: " + normalizedId, ex);
    }
  }

  public DockerContainerStatus inspectContainerIfExists(String containerId) {
    String normalizedId = requireContainerId(containerId);
    try {
      InspectContainerResponse response = dockerClient.inspectContainerCmd(normalizedId).exec();
      InspectContainerResponse.ContainerState state = response.getState();
      return new DockerContainerStatus(
          response.getId(),
          response.getName(),
          response.getConfig() == null ? null : response.getConfig().getImage(),
          state == null ? null : state.getStatus(),
          state == null ? null : state.getRunning(),
          state == null ? null : state.getPaused(),
          state == null ? null : state.getRestarting(),
          state == null ? null : state.getOOMKilled(),
          state == null ? null : state.getDead(),
          state == null ? null : state.getExitCode(),
          state == null ? null : state.getError(),
          state == null ? null : state.getStartedAt(),
          state == null ? null : state.getFinishedAt());
    } catch (NotFoundException ex) {
      return null;
    } catch (DockerException ex) {
      throw new DockerOperationException("Docker inspect container failed: " + normalizedId, ex);
    }
  }

  public List<DockerContainerSummary> listContainers(boolean includeStopped) {
    try {
      List<Container> containers =
          dockerClient.listContainersCmd().withShowAll(includeStopped).exec();
      return containers.stream()
          .map(
              container ->
                  new DockerContainerSummary(
                      container.getId(),
                      container.getImage(),
                      container.getNames(),
                      container.getState(),
                      container.getStatus()))
          .toList();
    } catch (DockerException ex) {
      throw new DockerOperationException("Docker list containers failed", ex);
    }
  }

  public List<DockerImageSummary> listImages() {
    try {
      List<Image> images = dockerClient.listImagesCmd().exec();
      return images.stream()
          .map(
              image ->
                  new DockerImageSummary(
                      image.getId(), image.getRepoTags(), image.getSize(), image.getCreated()))
          .toList();
    } catch (DockerException ex) {
      throw new DockerOperationException("Docker list images failed", ex);
    }
  }

  private String requireContainerId(String containerId) {
    if (isBlank(containerId)) {
      throw new DockerOperationException("Container id is required");
    }
    return containerId;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private HostConfig buildHostConfig(
      List<DockerVolumeMount> volumeMounts, List<DockerPortBinding> portBindings) {
    List<Bind> binds = buildBinds(volumeMounts);
    Ports ports = buildPorts(portBindings);
    if (binds.isEmpty() && ports == null) {
      return null;
    }
    HostConfig hostConfig = HostConfig.newHostConfig();
    if (!binds.isEmpty()) {
      hostConfig.withBinds(binds);
    }
    if (ports != null) {
      hostConfig.withPortBindings(ports);
    }
    return hostConfig;
  }

  private List<Bind> buildBinds(List<DockerVolumeMount> volumeMounts) {
    if (volumeMounts == null || volumeMounts.isEmpty()) {
      return List.of();
    }
    List<Bind> binds = new ArrayList<>();
    for (DockerVolumeMount mount : volumeMounts) {
      if (mount == null) {
        continue;
      }
      String hostPath = requirePath("hostPath", mount.hostPath());
      String containerPath = requirePath("containerPath", mount.containerPath());
      AccessMode mode = mount.readOnly() ? AccessMode.ro : AccessMode.rw;
      binds.add(new Bind(hostPath, new Volume(containerPath), mode));
    }
    return binds;
  }

  private Ports buildPorts(List<DockerPortBinding> portBindings) {
    if (portBindings == null || portBindings.isEmpty()) {
      return null;
    }
    Ports ports = new Ports();
    Map<String, Ports.Binding> seen = new LinkedHashMap<>();
    for (DockerPortBinding binding : portBindings) {
      if (binding == null) {
        continue;
      }
      ExposedPort exposedPort = toExposedPort(binding);
      Ports.Binding hostBinding =
          Ports.Binding.bindPort(requirePort(binding.hostPort(), "hostPort"));
      String key = exposedPort.toString();
      Ports.Binding existing = seen.putIfAbsent(key, hostBinding);
      if (existing != null) {
        throw new DockerOperationException("Duplicate port binding for " + key);
      }
      ports.bind(exposedPort, hostBinding);
    }
    return ports;
  }

  private List<ExposedPort> buildExposedPorts(List<DockerPortBinding> portBindings) {
    if (portBindings == null || portBindings.isEmpty()) {
      return List.of();
    }
    Map<String, ExposedPort> exposed = new LinkedHashMap<>();
    for (DockerPortBinding binding : portBindings) {
      if (binding == null) {
        continue;
      }
      ExposedPort exposedPort = toExposedPort(binding);
      exposed.putIfAbsent(exposedPort.toString(), exposedPort);
    }
    return new ArrayList<>(exposed.values());
  }

  private ExposedPort toExposedPort(DockerPortBinding binding) {
    int containerPort = requirePort(binding.containerPort(), "containerPort");
    String protocol = normalizeProtocol(binding.protocol());
    return "udp".equals(protocol) ? ExposedPort.udp(containerPort) : ExposedPort.tcp(containerPort);
  }

  private String normalizeProtocol(String protocol) {
    if (protocol == null || protocol.isBlank()) {
      return "tcp";
    }
    String normalized = protocol.trim().toLowerCase();
    if (!"tcp".equals(normalized) && !"udp".equals(normalized)) {
      throw new DockerOperationException("Unsupported port protocol: " + protocol);
    }
    return normalized;
  }

  private int requirePort(int port, String label) {
    if (port <= 0 || port > 65535) {
      throw new DockerOperationException(label + " must be between 1 and 65535");
    }
    return port;
  }

  private String requirePath(String label, String value) {
    if (value == null || value.isBlank()) {
      throw new DockerOperationException(label + " is required");
    }
    return value.trim();
  }
}
