package net.spookly.kodama.nodeagent.template.storage.s3;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import lombok.RequiredArgsConstructor;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageClient;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageException;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageObjectNotFoundException;
import net.spookly.kodama.nodeagent.template.storage.TemplateStorageTimeoutException;
import net.spookly.kodama.nodeagent.template.storage.TemplateTarball;
import okhttp3.Response;

@RequiredArgsConstructor
public class S3TemplateStorageClient implements TemplateStorageClient {

    private final MinioClient minioClient;
    private final String bucket;

    @Override
    public TemplateTarball getTemplateTarball(String templateId, String version, String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            throw new IllegalArgumentException("s3Key is required to fetch a template tarball");
        }
        try {
            GetObjectArgs request = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(s3Key)
                    .build();
            return new TemplateTarball(templateId, version, s3Key, -1, minioClient.getObject(request));
        } catch (ErrorResponseException ex) {
            if (isNotFound(ex)) {
                throw new TemplateStorageObjectNotFoundException(
                        "Template tarball not found in S3 bucket " + bucket + " with key " + s3Key,
                        ex
                );
            }
            throw new TemplateStorageException(
                    "Failed to fetch template tarball from S3 bucket " + bucket + " with key " + s3Key,
                    ex
            );
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                throw new TemplateStorageTimeoutException(
                        "Timed out fetching template tarball from S3 bucket " + bucket + " with key " + s3Key,
                        ex
                );
            }
            throw new TemplateStorageException(
                    "Failed to fetch template tarball from S3 bucket " + bucket + " with key " + s3Key,
                    ex
            );
        }
    }

    private boolean isNotFound(ErrorResponseException ex) {
        ErrorResponse errorResponse = ex.errorResponse();
        if (errorResponse != null) {
            String code = errorResponse.code();
            if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code)) {
                return true;
            }
        }
        Response response = ex.response();
        if (response != null && response.code() == 404) {
            return true;
        }
        String message = ex.getMessage();
        return message != null && message.contains("NoSuchKey");
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof TimeoutException
                    || current instanceof java.net.http.HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

}
