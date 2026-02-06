package net.spookly.kodama.brain.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.util.List;
import java.util.Set;

import net.spookly.kodama.brain.config.BrainSecurityProperties;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    @Test
    void issuedTokenIsParsable() {
        BrainSecurityProperties securityProperties = new BrainSecurityProperties();
        securityProperties.setEnabled(true);
        BrainSecurityProperties.Jwt jwt = new BrainSecurityProperties.Jwt();
        jwt.setIssuer("kodama-test");
        jwt.setSecret("01234567890123456789012345678901");
        jwt.setTokenTtlSeconds(3600);
        securityProperties.setJwt(jwt);
        BrainSecurityProperties.NodeAuth nodeAuth = new BrainSecurityProperties.NodeAuth();
        nodeAuth.setToken("test-node-token");
        nodeAuth.setHeaderName("X-Node-Token");
        securityProperties.setNode(nodeAuth);

        BrainSecurityProperties.UserDefinition user = new BrainSecurityProperties.UserDefinition();
        user.setUsername("admin");
        user.setPassword("test-pass");
        user.setRoles(Set.of(Role.ADMIN));
        securityProperties.setUsers(List.of(user));

        JwtTokenService tokenService = new JwtTokenService(securityProperties, Clock.systemUTC());
        tokenService.initialize();
        UserPrincipal principal = new UserPrincipal("admin", "Admin", "admin@example.com", Set.of(Role.ADMIN));

        String token = tokenService.issueToken(principal).token();

        assertTrue(tokenService.parseToken(token).isPresent(), "issued token should parse");
    }
}
