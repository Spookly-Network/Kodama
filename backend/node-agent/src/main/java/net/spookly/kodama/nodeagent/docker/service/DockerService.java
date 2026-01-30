package net.spookly.kodama.nodeagent.docker.service;

import java.util.Arrays;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateRequest;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateResult;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerStatus;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerSummary;
import net.spookly.kodama.nodeagent.docker.dto.DockerImageSummary;
import org.springframework.stereotype.Service;

@Service
public class DockerService {

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
            CreateContainerResponse response = createCmd.exec();
            List<String> warnings = response.getWarnings() == null
                    ? List.of()
                    : Arrays.asList(response.getWarnings());
            return new DockerContainerCreateResult(response.getId(), warnings);
        } catch (ConflictException ex) {
            throw new DockerOperationException("Docker container name conflict", ex);
        } catch (DockerException ex) {
            throw new DockerOperationException("Docker create container failed", ex);
        }
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

    public void removeContainer(String containerId, boolean force, boolean removeVolumes) {
        String normalizedId = requireContainerId(containerId);
        try {
            dockerClient.removeContainerCmd(normalizedId)
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
                    state == null ? null : state.getFinishedAt()
            );
        } catch (NotFoundException ex) {
            throw new DockerOperationException("Docker container not found: " + normalizedId, ex);
        } catch (DockerException ex) {
            throw new DockerOperationException("Docker inspect container failed: " + normalizedId, ex);
        }
    }

    public List<DockerContainerSummary> listContainers(boolean includeStopped) {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(includeStopped)
                    .exec();
            return containers.stream()
                    .map(container -> new DockerContainerSummary(
                            container.getId(),
                            container.getImage(),
                            container.getNames(),
                            container.getState(),
                            container.getStatus()
                    ))
                    .toList();
        } catch (DockerException ex) {
            throw new DockerOperationException("Docker list containers failed", ex);
        }
    }

    public List<DockerImageSummary> listImages() {
        try {
            List<Image> images = dockerClient.listImagesCmd().exec();
            return images.stream()
                    .map(image -> new DockerImageSummary(
                            image.getId(),
                            image.getRepoTags(),
                            image.getSize(),
                            image.getCreated()
                    ))
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
}
