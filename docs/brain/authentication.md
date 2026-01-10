# Brain Authentication

## Purpose
Describe the JWT-based authentication used by the Brain admin API.

## What changed
- Added `/api/auth/login` for credential exchange.
- Added JWT bearer authentication for admin endpoints.
- Enforced role-based access using `ADMIN`, `OPERATOR`, and `VIEWER`.

## How to use / impact
- Configure the JWT settings and users via `brain.security` properties.
- Obtain a token by calling `POST /api/auth/login` with `username` and `password`.
- Send `Authorization: Bearer <token>` on protected requests.
- All `/api/**` endpoints are protected except `/api/auth/login`.

Required configuration (example):

```yaml
brain:
  security:
    enabled: true
    jwt:
      issuer: kodama-brain
      secret: ${BRAIN_JWT_SECRET}
      token-ttl-seconds: 3600
    users:
      - username: admin
        display-name: Admin
        email: admin@example.com
        password: ${BRAIN_ADMIN_PASSWORD}
        roles: ADMIN
```

Password values without a `{id}` prefix are treated as `{noop}` for development.

## Edge cases / risks
- `brain.security.jwt.secret` must be at least 32 bytes for HS256.
- Missing or invalid tokens return `401 Unauthorized`.
- Tokens expire after `token-ttl-seconds` and must be refreshed via login.
- `VIEWER` tokens are read-only; write endpoints return `403 Forbidden`.
- Set `brain.security.enabled=false` to disable HTTP and method security in local development.

## Links
- Config: `backend/brain/src/main/java/net/spookly/kodama/brain/config/BrainSecurityProperties.java`
- Security filter: `backend/brain/src/main/java/net/spookly/kodama/brain/security/JwtAuthFilter.java`
- Login endpoint: `backend/brain/src/main/java/net/spookly/kodama/brain/controller/AuthController.java`
- OpenAPI: `contracts/openapi.yml`
