package net.spookly.kodama.brain;

import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.config.InstanceStaleDetectionProperties;
import net.spookly.kodama.brain.config.NodeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        NodeProperties.class,
        BrainSecurityProperties.class,
        InstanceStaleDetectionProperties.class
})
@EnableScheduling
public class BrainApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrainApplication.class, args);
    }

}
