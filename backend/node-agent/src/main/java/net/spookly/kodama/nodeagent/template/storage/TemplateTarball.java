package net.spookly.kodama.nodeagent.template.storage;

import java.io.IOException;
import java.io.InputStream;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TemplateTarball implements AutoCloseable {

    @NonNull
    private final String templateId;
    @NonNull
    private final String version;
    @NonNull
    private final String s3Key;
    private final long contentLength;
    @NonNull
    private final InputStream inputStream;

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
