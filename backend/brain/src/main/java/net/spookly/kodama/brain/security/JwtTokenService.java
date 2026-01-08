package net.spookly.kodama.brain.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import net.spookly.kodama.brain.config.BrainSecurityProperties;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private final BrainSecurityProperties securityProperties;
    private final Clock clock;
    private JwtParser jwtParser;
    private SecretKey signingKey;

    public JwtTokenService(BrainSecurityProperties securityProperties) {
        this(securityProperties, Clock.systemUTC());
    }

    JwtTokenService(BrainSecurityProperties securityProperties, Clock clock) {
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    @PostConstruct
    void initialize() {
        if (!securityProperties.isEnabled()) {
            return;
        }
        securityProperties.validate();
        String secret = securityProperties.getJwt().getSecret();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("brain.security.jwt.secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(securityProperties.getJwt().getIssuer())
                .build();
    }

    public Optional<UserPrincipal> parseToken(String token) {
        if (!securityProperties.isEnabled()) {
            return Optional.empty();
        }
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jws<Claims> jws = jwtParser.parseSignedClaims(token);
            Claims claims = jws.getPayload();
            String username = claims.getSubject();
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }
            List<?> roleValues = claims.get("roles", List.class);
            if (roleValues == null || roleValues.isEmpty()) {
                return Optional.empty();
            }
            Set<Role> roles = roleValues.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(Role::valueOf)
                    .collect(Collectors.toUnmodifiableSet());
            String displayName = claims.get("displayName", String.class);
            String email = claims.get("email", String.class);
            return Optional.of(new UserPrincipal(username, displayName, email, roles));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public IssuedToken issueToken(UserPrincipal principal) {
        Objects.requireNonNull(principal, "principal");
        if (!securityProperties.isEnabled()) {
            throw new IllegalStateException("brain.security.enabled is false");
        }
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(securityProperties.getJwt().getTokenTtlSeconds());
        String token = Jwts.builder()
                .issuer(securityProperties.getJwt().getIssuer())
                .subject(principal.getUsername())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(expiresAt))
                .claim("roles", principal.getRoles().stream().map(Role::name).toList())
                .claim("displayName", principal.getDisplayName())
                .claim("email", principal.getEmail())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
    }

    public record IssuedToken(String token, OffsetDateTime expiresAt) {
    }
}
