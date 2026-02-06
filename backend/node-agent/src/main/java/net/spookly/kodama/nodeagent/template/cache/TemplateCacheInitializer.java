package net.spookly.kodama.nodeagent.template.cache;

import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TemplateCacheInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCacheInitializer.class);

    private final TemplateCacheLayout layout;

    public TemplateCacheInitializer(TemplateCacheLayout layout) {
        this.layout = layout;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureCacheDirectories();
    }

    void ensureCacheDirectories() {
        try {
            Files.createDirectories(layout.getTemplatesRoot());
        } catch (IOException ex) {
            throw new TemplateCacheException(
                    "Failed to create template cache directory at " + layout.getTemplatesRoot(),
                    ex
            );
        }
        logger.info("Template cache directory ready at {}", layout.getTemplatesRoot());
    }
}
