package net.spookly.kodama.brain;

import net.spookly.kodama.brain.config.BrainCorsProperties;
import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.BrainCorsProperties;
import net.spookly.kodama.brain.config.InstanceStaleDetectionProperties;
import net.spookly.kodama.brain.config.NodeProperties;
import net.spookly.kodama.brain.config.PluginsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        BrainCorsProperties.class,
        NodeProperties.class,
        BrainCorsProperties.class,
        BrainSecurityProperties.class,
        InstanceStaleDetectionProperties.class,
        PluginsProperties.class
})
@EnableScheduling
public class BrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrainApplication.class, args);
    }

}

//codex resume 019c349a-50ca-7250-adbe-c7801f5b5b77
