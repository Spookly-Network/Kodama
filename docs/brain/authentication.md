# Brain Authentication

## Purpose
Describe the JWT-based authentication used by the Brain admin API.

## What changed
- Added `/api/auth/login` for credential exchange.
- Added JWT bearer authentication for admin endpoints.
- Enforced role-based access using `ADMIN`, `OPERATOR`, and `VIEWER`.
- Added a node authentication token for node callbacks, registration, and heartbeats.

## How to use / impact
- Configure the JWT settings and users via `brain.security` properties.
- Obtain a token by calling `POST /api/auth/login` with `username` and `password`.
- Send `Authorization: Bearer <token>` on protected requests.
- Use `X-Node-Token: <token>` for node endpoints:
  - `POST /api/nodes/register`
  - `POST /api/nodes/{nodeId}/heartbeat`
  - `POST /api/nodes/{nodeId}/instances/{instanceId}/*`
- User tokens are not accepted on node callback endpoints.
- Template creation derives `createdBy` from the authenticated username; clients cannot supply it in the request.
- For cross-origin browser clients, configure `brain.web.cors` to allow the panel origin and include
  `Authorization` and `X-Node-Token` in `allowed-headers`.

Role access summary:
- `ADMIN`: full access to all user-facing endpoints.
- `OPERATOR`: manage instances, read-only access to nodes/templates.
- `VIEWER`: read-only access to lists and details.

Required configuration (example):

```yaml
brain:
  security:
    enabled: true
    jwt:
      issuer: kodama-brain
      secret: ${BRAIN_JWT_SECRET}
      token-ttl-seconds: 3600
    node:
      token: ${BRAIN_NODE_AUTH_TOKEN}
      header-name: X-Node-Token
    users:
      - username: admin
        display-name: Admin
        email: admin@example.com
        password: ${BRAIN_ADMIN_PASSWORD}
        roles: ADMIN
  web:
    cors:
      allowed-origins: ${BRAIN_CORS_ALLOWED_ORIGINS:https://panel.example.com}
      allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
      allowed-headers: Authorization,Content-Type,X-Node-Token
```

Password values without a `{id}` prefix are treated as `{noop}` for development.

## Edge cases / risks
- `brain.security.jwt.secret` must be at least 32 bytes for HS256.
- Missing or invalid tokens return `401 Unauthorized`.
- Tokens expire after `token-ttl-seconds` and must be refreshed via login.
- `VIEWER` tokens are read-only; write endpoints return `403 Forbidden`.
- Node endpoints require `brain.security.node.token` to be configured.
- CORS preflight requests are allowed without node authentication.
- Set `brain.security.enabled=false` to disable HTTP and method security in local development.
- When security is disabled, template creation uses the system username `system` for `createdBy`.

## Links
- Config: `backend/brain/src/main/java/net/spookly/kodama/brain/config/BrainSecurityProperties.java`
- Security filter: `backend/brain/src/main/java/net/spookly/kodama/brain/security/JwtAuthFilter.java`
- Login endpoint: `backend/brain/src/main/java/net/spookly/kodama/brain/controller/AuthController.java`
- OpenAPI: `contracts/openapi.yml`
