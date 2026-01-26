package net.spookly.kodama.nodeagent.template.storage.s3;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageException;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageObjectNotFoundException;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class S3TemplateStorageClientTest {

    private MinioClient minioClient;
    private S3TemplateStorageClient client;

    @BeforeEach
    void setUp() {
        minioClient = Mockito.mock(MinioClient.class);
        client = new S3TemplateStorageClient(minioClient, "templates");
    }

    @Test
    void getTemplateTarballRejectsBlankKey() {
        assertThatThrownBy(() -> client.getTemplateTarball("template", "1.0", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("s3Key is required");
    }

    @Test
    void getTemplateTarballMapsNotFoundStatus() throws Exception {
        ErrorResponseException errorResponseException = Mockito.mock(ErrorResponseException.class);
        ErrorResponse errorResponse = Mockito.mock(ErrorResponse.class);
        when(errorResponseException.errorResponse()).thenReturn(errorResponse);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(errorResponseException);

        assertThatThrownBy(() -> client.getTemplateTarball("template", "1.0", "templates/base.tar"))
                .isInstanceOf(TemplateStorageObjectNotFoundException.class)
                .hasMessageContaining("templates/base.tar");
    }

    @Test
    void getTemplateTarballMapsTimeouts() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new SocketTimeoutException("timeout"));

        assertThatThrownBy(() -> client.getTemplateTarball("template", "1.0", "templates/base.tar"))
                .isInstanceOf(TemplateStorageTimeoutException.class)
                .hasMessageContaining("Timed out");
    }

    @Test
    void getTemplateTarballMapsClientFailures() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> client.getTemplateTarball("template", "1.0", "templates/base.tar"))
                .isInstanceOf(TemplateStorageException.class)
                .hasMessageContaining("Failed to fetch");
    }
}
