package net.spookly.kodama.nodeagent.docker.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.dockerjava.api.DockerClient;
import net.spookly.kodama.nodeagent.docker.dto.DockerContainerCreateRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DockerServiceTest {

    @Test
    void createContainerRejectsMissingImage() {
        DockerClient dockerClient = Mockito.mock(DockerClient.class);
        DockerService service = new DockerService(dockerClient);

        DockerContainerCreateRequest request = new DockerContainerCreateRequest(
                " ",
                null,
                null,
                null,
                null,
                null
        );

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
}
