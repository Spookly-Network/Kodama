package net.spookly.kodama.brain.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private final BrainSecurityProperties securityProperties;
    private final Clock clock;
    private JwtParser jwtParser;
    private SecretKey signingKey;
    private String initializedIssuer;
    private String initializedSecret;

    @Autowired
    public JwtTokenService(BrainSecurityProperties securityProperties) {
        this(securityProperties, Clock.systemUTC());
    }

    JwtTokenService(BrainSecurityProperties securityProperties, Clock clock) {
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    @PostConstruct
    void initialize() {
        ensureInitialized();
    }

    public Optional<UserPrincipal> parseToken(String token) {
        if (!securityProperties.isEnabled()) {
            return Optional.empty();
        }
        ensureInitialized();
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
            Set<Role> roles = parseRoles(claims.get("roles"));
            if (roles.isEmpty()) {
                return Optional.empty();
            }
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
        ensureInitialized();
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

    private synchronized void ensureInitialized() {
        if (!securityProperties.isEnabled()) {
            return;
        }
        String issuer = securityProperties.getJwt().getIssuer();
        String secret = securityProperties.getJwt().getSecret();
        if (jwtParser != null
                && signingKey != null
                && Objects.equals(initializedIssuer, issuer)
                && Objects.equals(initializedSecret, secret)) {
            return;
        }
        securityProperties.validate();
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("brain.security.jwt.secret must be at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build();
        this.initializedIssuer = issuer;
        this.initializedSecret = secret;
    }

    private Set<Role> parseRoles(Object rolesClaim) {
        if (rolesClaim == null) {
            return Set.of();
        }
        if (rolesClaim instanceof String rolesString) {
            return Arrays.stream(rolesString.split(","))
                    .map(String::trim)
                    .map(this::parseRoleValue)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toUnmodifiableSet());
        }
        if (rolesClaim instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(this::extractRoleValue)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toUnmodifiableSet());
        }
        if (rolesClaim instanceof Object[] array) {
            return Arrays.stream(array)
                    .filter(Objects::nonNull)
                    .map(this::extractRoleValue)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toUnmodifiableSet());
        }
        return extractRoleValue(rolesClaim).stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    private Optional<Role> extractRoleValue(Object value) {
        if (value instanceof String roleString) {
            return parseRoleValue(roleString);
        }
        if (value instanceof Map<?, ?> map) {
            Object authority = map.get("authority");
            if (authority != null) {
                return parseRoleValue(authority.toString());
            }
        }
        return parseRoleValue(value.toString());
    }

    private Optional<Role> parseRoleValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        try {
            return Optional.of(Role.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
