package net.spookly.kodama.brain.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
@ConditionalOnProperty(prefix = "brain.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MethodSecurityConfig {
}
