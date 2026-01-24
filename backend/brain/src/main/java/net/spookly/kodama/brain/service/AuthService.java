package net.spookly.kodama.brain.service;

import net.spookly.kodama.brain.config.BrainSecurityProperties;
import net.spookly.kodama.brain.dto.LoginRequest;
import net.spookly.kodama.brain.dto.LoginResponse;
import net.spookly.kodama.brain.security.ConfiguredUserStore;
import net.spookly.kodama.brain.security.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final BrainSecurityProperties securityProperties;
    private final ConfiguredUserStore userStore;
    private final JwtTokenService tokenService;

    public AuthService(
            BrainSecurityProperties securityProperties,
            ConfiguredUserStore userStore,
            JwtTokenService tokenService
    ) {
        this.securityProperties = securityProperties;
        this.userStore = userStore;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest request) {
        if (!securityProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication is disabled");
        }
        ConfiguredUserStore.StoredUser storedUser = userStore.findForAuthentication(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!userStore.matchesPassword(storedUser, request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        JwtTokenService.IssuedToken token = tokenService.issueToken(storedUser.principal());
        return new LoginResponse(
                token.token(),
                "Bearer",
                token.expiresAt(),
                storedUser.principal().getRoles()
        );
    }
}
