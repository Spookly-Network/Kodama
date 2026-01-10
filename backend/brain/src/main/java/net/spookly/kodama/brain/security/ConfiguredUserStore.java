package net.spookly.kodama.brain.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import net.spookly.kodama.brain.config.BrainSecurityProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredUserStore {

    private final BrainSecurityProperties securityProperties;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, StoredUser> usersByUsername = new HashMap<>();

    public ConfiguredUserStore(BrainSecurityProperties securityProperties, PasswordEncoder passwordEncoder) {
        this.securityProperties = securityProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void initialize() {
        if (!securityProperties.isEnabled()) {
            return;
        }
        securityProperties.validate();
        for (BrainSecurityProperties.UserDefinition userDefinition : securityProperties.getUsers()) {
            UserPrincipal principal = new UserPrincipal(
                    userDefinition.getUsername(),
                    userDefinition.getDisplayName(),
                    userDefinition.getEmail(),
                    userDefinition.getRoles()
            );
            String passwordHash = normalizePassword(userDefinition.getPassword());
            usersByUsername.put(userDefinition.getUsername(), new StoredUser(passwordHash, principal));
        }
    }

    public Optional<UserPrincipal> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        StoredUser storedUser = usersByUsername.get(username);
        if (storedUser == null) {
            return Optional.empty();
        }
        return Optional.of(storedUser.principal());
    }

    public Optional<StoredUser> findForAuthentication(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByUsername.get(username));
    }

    public boolean matchesPassword(StoredUser storedUser, String rawPassword) {
        Objects.requireNonNull(storedUser, "storedUser");
        return passwordEncoder.matches(rawPassword, storedUser.passwordHash());
    }

    private String normalizePassword(String password) {
        if (password.startsWith("{")) {
            return password;
        }
        return "{noop}" + password;
    }

    public record StoredUser(String passwordHash, UserPrincipal principal) {
    }
}
