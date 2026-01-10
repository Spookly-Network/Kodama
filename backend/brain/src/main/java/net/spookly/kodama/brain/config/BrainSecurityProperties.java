package net.spookly.kodama.brain.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.spookly.kodama.brain.security.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "brain.security")
public class BrainSecurityProperties {

    private boolean enabled = true;

    private Jwt jwt = new Jwt();

    private List<UserDefinition> users = new ArrayList<>();

    public void validate() {
        if (!enabled) {
            return;
        }
        if (jwt == null) {
            throw new IllegalStateException("brain.security.jwt must be configured");
        }
        if (jwt.getIssuer() == null || jwt.getIssuer().isBlank()) {
            throw new IllegalStateException("brain.security.jwt.issuer must be configured");
        }
        if (jwt.getSecret() == null || jwt.getSecret().isBlank()) {
            throw new IllegalStateException("brain.security.jwt.secret must be configured");
        }
        if (jwt.getTokenTtlSeconds() < 60) {
            throw new IllegalStateException("brain.security.jwt.token-ttl-seconds must be at least 60");
        }
        if (users == null || users.isEmpty()) {
            throw new IllegalStateException("brain.security.users must contain at least one user");
        }
        Set<String> usernames = new HashSet<>();
        for (UserDefinition user : users) {
            if (user == null) {
                throw new IllegalStateException("brain.security.users cannot contain null entries");
            }
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                throw new IllegalStateException("brain.security.users.username must be configured");
            }
            if (!usernames.add(user.getUsername())) {
                throw new IllegalStateException("Duplicate brain.security.users.username: " + user.getUsername());
            }
            if (user.getPassword() == null || user.getPassword().isBlank()) {
                throw new IllegalStateException(
                        "brain.security.users.password must be configured for " + user.getUsername());
            }
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                throw new IllegalStateException(
                        "brain.security.users.roles must be configured for " + user.getUsername());
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public List<UserDefinition> getUsers() {
        return users;
    }

    public void setUsers(List<UserDefinition> users) {
        this.users = users;
    }

    public static class Jwt {

        private String issuer = "kodama-brain";

        private String secret;

        private long tokenTtlSeconds = 3600;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getTokenTtlSeconds() {
            return tokenTtlSeconds;
        }

        public void setTokenTtlSeconds(long tokenTtlSeconds) {
            this.tokenTtlSeconds = tokenTtlSeconds;
        }
    }

    public static class UserDefinition {

        private String username;
        private String displayName;
        private String email;
        private String password;
        private Set<Role> roles = new HashSet<>();

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Set<Role> getRoles() {
            return roles;
        }

        public void setRoles(Set<Role> roles) {
            this.roles = roles;
        }
    }
}
