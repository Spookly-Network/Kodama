package net.spookly.kodama.nodeagent.docker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.PullResponseItem;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateRequest;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DockerServiceTest {
  private static final String IMAGE = "ghcr.io/spookly/hytale:latest";

  @Test
  void createContainerRejectsMissingImage() {
    DockerClient dockerClient = Mockito.mock(DockerClient.class);
    DockerService service = new DockerService(dockerClient);

    DockerContainerCreateRequest request =
        new DockerContainerCreateRequest(" ", null, null, null, null, null, null, null);

    assertThatThrownBy(() -> service.createContainer(request))
        .isInstanceOf(DockerOperationException.class)
        .hasMessageContaining("Container image is required");
  }

  @Test
  void startContainerRejectsBlankContainerId() {
    DockerClient dockerClient = Mockito.mock(DockerClient.class);
    DockerService service = new DockerService(dockerClient);

    assertThatThrownBy(() -> service.startContainer(" "))
        .isInstanceOf(DockerOperationException.class)
        .hasMessageContaining("Container id is required");
  }

  @Test
  void createContainerPullsImageAndRetriesWhenMissingLocally() throws InterruptedException {
    DockerClient dockerClient = Mockito.mock(DockerClient.class);
    CreateContainerCmd firstCreateCmd = Mockito.mock(CreateContainerCmd.class);
    CreateContainerCmd secondCreateCmd = Mockito.mock(CreateContainerCmd.class);
    PullImageCmd pullImageCmd = Mockito.mock(PullImageCmd.class);
    ResultCallback.Adapter<PullResponseItem> pullResult = mockPullResult();
    CreateContainerResponse createContainerResponse = Mockito.mock(CreateContainerResponse.class);
    DockerService service = new DockerService(dockerClient);

    when(dockerClient.createContainerCmd(IMAGE)).thenReturn(firstCreateCmd, secondCreateCmd);
    when(firstCreateCmd.exec()).thenThrow(new NotFoundException("No such image: " + IMAGE));
    when(dockerClient.pullImageCmd(IMAGE)).thenReturn(pullImageCmd);
    when(pullImageCmd.start()).thenReturn(pullResult);
    when(pullResult.awaitCompletion()).thenReturn(pullResult);
    when(secondCreateCmd.exec()).thenReturn(createContainerResponse);
    when(createContainerResponse.getId()).thenReturn("container-1");
    when(createContainerResponse.getWarnings()).thenReturn(null);

    DockerContainerCreateRequest request = createRequest(IMAGE);
    DockerContainerCreateResult result = service.createContainer(request);

    assertThat(result.containerId()).isEqualTo("container-1");
    assertThat(result.warnings()).isEmpty();
    verify(dockerClient, times(2)).createContainerCmd(IMAGE);
    verify(dockerClient).pullImageCmd(IMAGE);
    verify(pullImageCmd).start();
    verify(pullResult).awaitCompletion();
  }

  @Test
  void createContainerFailsWhenImagePullFails() throws InterruptedException {
    DockerClient dockerClient = Mockito.mock(DockerClient.class);
    CreateContainerCmd createContainerCmd = Mockito.mock(CreateContainerCmd.class);
    PullImageCmd pullImageCmd = Mockito.mock(PullImageCmd.class);
    ResultCallback.Adapter<PullResponseItem> pullResult = mockPullResult();
    DockerService service = new DockerService(dockerClient);

    when(dockerClient.createContainerCmd(IMAGE)).thenReturn(createContainerCmd);
    when(createContainerCmd.exec()).thenThrow(new NotFoundException("No such image: " + IMAGE));
    when(dockerClient.pullImageCmd(IMAGE)).thenReturn(pullImageCmd);
    when(pullImageCmd.start()).thenReturn(pullResult);
    when(pullResult.awaitCompletion()).thenThrow(new RuntimeException("pull failed"));

    DockerContainerCreateRequest request = createRequest(IMAGE);

    assertThatThrownBy(() -> service.createContainer(request))
        .isInstanceOf(DockerOperationException.class)
        .hasMessageContaining("Docker image pull failed: " + IMAGE);
    verify(dockerClient, times(1)).createContainerCmd(IMAGE);
  }

  @Test
  void createContainerDoesNotPullImageForNonImageErrors() {
    DockerClient dockerClient = Mockito.mock(DockerClient.class);
    CreateContainerCmd createContainerCmd = Mockito.mock(CreateContainerCmd.class);
    DockerService service = new DockerService(dockerClient);

    when(dockerClient.createContainerCmd(IMAGE)).thenReturn(createContainerCmd);
    when(createContainerCmd.exec()).thenThrow(new DockerException("docker daemon down", 500));

    DockerContainerCreateRequest request = createRequest(IMAGE);

    assertThatThrownBy(() -> service.createContainer(request))
        .isInstanceOf(DockerOperationException.class)
        .hasMessageContaining("Docker create container failed");
    verify(dockerClient, never()).pullImageCmd(anyString());
  }

  private DockerContainerCreateRequest createRequest(String image) {
    return new DockerContainerCreateRequest(image, null, null, null, null, null, null, null);
  }

  @SuppressWarnings("unchecked")
  private ResultCallback.Adapter<PullResponseItem> mockPullResult() {
    return (ResultCallback.Adapter<PullResponseItem>) Mockito.mock(ResultCallback.Adapter.class);
  }
}
