package net.spookly.kodama.nodeagent.template.cache;

import net.spookly.kodama.nodeagent.config.NodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class TemplateCacheCheckRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCacheCheckRunner.class);

    private final NodeConfig config;
    private final TemplateCacheLookupService lookupService;

    public TemplateCacheCheckRunner(NodeConfig config, TemplateCacheLookupService lookupService) {
        this.config = config;
        this.lookupService = lookupService;
    }

    @Override
    public void run(ApplicationArguments args) {
        NodeConfig.TemplateCacheCheck check = config.getTemplateCacheCheck();
        if (check == null || !check.isEnabled()) {
            return;
        }

        TemplateCacheLookupResult result = lookupService.findCachedTemplate(
                check.getTemplateId(),
                check.getVersion(),
                check.getChecksum()
        );

        logger.info(
                "Template cache check completed. templateId={}, version={}, hit={}, reason={}",
                result.templateId(),
                result.version(),
                result.isCacheHit(),
                result.missReason()
        );
    }
}
