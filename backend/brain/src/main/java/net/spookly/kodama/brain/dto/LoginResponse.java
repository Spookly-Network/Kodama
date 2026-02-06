package net.spookly.kodama.brain.dto;

import java.time.OffsetDateTime;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.spookly.kodama.brain.security.Role;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private OffsetDateTime expiresAt;
    private Set<Role> roles;
}
