package net.spookly.kodama.brain.security;

import java.security.Principal;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class UserPrincipal implements Principal {

    private final String username;
    private final String displayName;
    private final String email;
    private final Set<Role> roles;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String username, String displayName, String email, Set<Role> roles) {
        this.username = Objects.requireNonNull(username, "username");
        this.displayName = displayName;
        this.email = email;
        this.roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
        this.authorities = this.roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.asAuthority()))
                .toList();
    }

    @Override
    public String getName() {
        return username;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
