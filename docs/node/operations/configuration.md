# Node Agent Configuration

## Purpose
Describe the configuration inputs for the node agent and how they map to environment variables or CLI arguments.

## What changed
- Introduced a typed `NodeConfig` model that binds to `node-agent.*` settings.
- Added startup validation and a sanitized configuration log line.
- Added registration-related configuration for Brain startup registration.
- Added a heartbeat interval override for node-agent heartbeats.
- Added S3-backed template storage configuration for fetching template tarballs.
- Added optional template cache check inputs for manual cache validation at startup.
- Documented runtime dev-mode toggling for cache bypass.
- Documented Brain authentication requirements for command endpoints.
- Added Docker client connection settings for local or TCP Docker Engine access.
- Added instance runtime settings for container image and workspace mount paths.
- Added an optional instance stop timeout for graceful shutdowns.

## How to use / impact
- Configure with environment variables or CLI args (`--node-agent.<key>=...`).
- Required settings:
  - `node-agent.node-name` (`NODE_AGENT_NAME`)
  - `node-agent.node-version` (`NODE_AGENT_NODE_VERSION`)
  - `node-agent.region` (`NODE_AGENT_REGION`)
  - `node-agent.capacity-slots` (`NODE_AGENT_CAPACITY_SLOTS`)
  - `node-agent.brain-base-url` (`NODE_AGENT_BRAIN_BASE_URL`)
  - `node-agent.cache-dir` (`NODE_AGENT_CACHE_DIR`)
  - `node-agent.s3.endpoint` (`NODE_AGENT_S3_ENDPOINT`)
  - `node-agent.s3.bucket` (`NODE_AGENT_S3_BUCKET`)
  - `node-agent.s3.access-key` (`NODE_AGENT_S3_ACCESS_KEY`)
  - `node-agent.s3.secret-key` (`NODE_AGENT_S3_SECRET_KEY`)
- Optional settings:
  - `node-agent.node-id` (`NODE_AGENT_ID`, assigned on registration)
  - `node-agent.dev-mode` (`NODE_AGENT_DEV_MODE`, default `false`)
  - `node-agent.tags` (`NODE_AGENT_TAGS`)
  - `node-agent.base-url` (`NODE_AGENT_BASE_URL`)
  - `node-agent.registration-enabled` (`NODE_AGENT_REGISTRATION_ENABLED`, default `true`)
  - `node-agent.heartbeat-interval-seconds` (`NODE_AGENT_HEARTBEAT_INTERVAL_SECONDS`, default `0`)
  - `server.port` (`NODE_AGENT_HTTP_PORT`, default `8080`)
  - `server.address` (`NODE_AGENT_HTTP_BIND_ADDRESS`, default `0.0.0.0`)
  - `node-agent.workspace-dir` (`NODE_AGENT_WORKSPACE_DIR`, default `./data`)
  - `node-agent.docker-host` (`NODE_AGENT_DOCKER_HOST`)
  - `node-agent.docker.host` (`NODE_AGENT_DOCKER_HOST`)
  - `node-agent.docker.tls-verify` (`NODE_AGENT_DOCKER_TLS_VERIFY`)
  - `node-agent.docker.cert-path` (`NODE_AGENT_DOCKER_CERT_PATH`)
  - `node-agent.docker.api-version` (`NODE_AGENT_DOCKER_API_VERSION`)
  - `node-agent.docker.config-dir` (`NODE_AGENT_DOCKER_CONFIG_DIR`)
  - `node-agent.docker.context` (`NODE_AGENT_DOCKER_CONTEXT`)
  - `node-agent.docker.max-connections` (`NODE_AGENT_DOCKER_MAX_CONNECTIONS`)
  - `node-agent.docker.connection-timeout-seconds` (`NODE_AGENT_DOCKER_CONNECTION_TIMEOUT_SECONDS`, default `5`)
  - `node-agent.docker.response-timeout-seconds` (`NODE_AGENT_DOCKER_RESPONSE_TIMEOUT_SECONDS`, default `30`)
  - `node-agent.template-cache-check.enabled` (`NODE_AGENT_TEMPLATE_CACHE_CHECK_ENABLED`, default `false`)
  - `node-agent.template-cache-check.template-id` (`NODE_AGENT_TEMPLATE_CACHE_CHECK_TEMPLATE_ID`)
  - `node-agent.template-cache-check.version` (`NODE_AGENT_TEMPLATE_CACHE_CHECK_VERSION`)
  - `node-agent.template-cache-check.checksum` (`NODE_AGENT_TEMPLATE_CACHE_CHECK_CHECKSUM`)
  - `node-agent.template-cache-limits.max-extracted-bytes` (`NODE_AGENT_TEMPLATE_CACHE_LIMITS_MAX_EXTRACTED_BYTES`, default `10737418240`)
  - `node-agent.template-cache-limits.max-entries` (`NODE_AGENT_TEMPLATE_CACHE_LIMITS_MAX_ENTRIES`, default `100000`)
  - `node-agent.variable-substitution.max-file-bytes` (`NODE_AGENT_VARIABLE_SUBSTITUTION_MAX_FILE_BYTES`, default `1048576`)
  - `node-agent.instance-runtime.image` (`NODE_AGENT_INSTANCE_RUNTIME_IMAGE`)
  - `node-agent.instance-runtime.workspace-mount-path` (`NODE_AGENT_INSTANCE_RUNTIME_WORKSPACE_MOUNT_PATH`, default `/workspace`)
  - `node-agent.instance-runtime.working-dir` (`NODE_AGENT_INSTANCE_RUNTIME_WORKING_DIR`, defaults to workspace mount path)
  - `node-agent.instance-runtime.stop-timeout-seconds` (`NODE_AGENT_INSTANCE_RUNTIME_STOP_TIMEOUT_SECONDS`, unset uses Docker defaults)
  - `node-agent.auth.header-name` (`NODE_AGENT_AUTH_HEADER_NAME`, default `X-Node-Token`)
  - `node-agent.auth.token-path` (`NODE_AGENT_AUTH_TOKEN_PATH`)
  - `node-agent.auth.cert-path` (`NODE_AGENT_AUTH_CERT_PATH`)
  - `node-agent.s3.region` (`NODE_AGENT_S3_REGION`)
- When registration is enabled, the node agent reads the token from `node-agent.auth.token-path`
  and sends it to the Brain using `node-agent.auth.header-name`.
- Command endpoints require Brain authentication:
  - If `node-agent.auth.cert-path` is set, the Brain must connect with a matching client certificate.
  - Otherwise, the Brain must send the shared token using `node-agent.auth.header-name`.
- `node-agent.base-url` is used by the Brain to issue commands to the node (including cache purge).
- `server.port` and `server.address` control the embedded HTTP listener used by the Brain to issue commands.
- When `node-agent.heartbeat-interval-seconds` is `0`, the node agent uses the heartbeat interval
  provided by the Brain during registration.
- `node-agent.docker.host` configures the Docker Engine socket or TCP endpoint. If unset, the Docker
  client uses the docker-java defaults (including `DOCKER_HOST` or the local Unix socket).
- `node-agent.docker-host` remains supported for backwards compatibility, but prefer `node-agent.docker.host`.
- When `node-agent.docker.tls-verify=true`, you can set `node-agent.docker.cert-path` or
  `node-agent.docker.config-dir` to load TLS certificates. If neither is set, the Docker client
  falls back to the default Docker config location (including Docker contexts).
- `node-agent.docker.connection-timeout-seconds` and `node-agent.docker.response-timeout-seconds`
  control the Docker client HTTP timeouts.
- `node-agent.cache-dir` is the root for template cache storage. The node agent creates a
  `templates/` subdirectory on startup. See `docs/node/operations/template-cache.md` for the layout.
- `node-agent.workspace-dir` is the root for instance workspaces. The node agent creates
  `instances/<instanceId>/{merged,logs,temp}` on demand when preparing a workspace.
- See `docs/node/operations/instance-workspaces.md` for the full layout details.
- Dev-mode defaults to `node-agent.dev-mode` and can be toggled at runtime via `POST /api/node/dev-mode`
  with body `{ "devMode": true|false }`. The runtime value is in-memory only; restarting the node agent
  resets it to the configured default.
- When `node-agent.template-cache-check.enabled=true`, the node agent validates a single cached
  template at startup and logs the cache hit/miss outcome.
- `node-agent.template-cache-limits.*` caps extracted tarball size and entry count to protect disk usage.
- `node-agent.variable-substitution.max-file-bytes` skips large files during placeholder substitution to avoid memory spikes (set to `0` to disable).
- `node-agent.instance-runtime.image` provides the default Docker image when the prepare payload does not supply `DOCKER_IMAGE`.
- `node-agent.instance-runtime.workspace-mount-path` controls where the merged workspace is mounted inside the container.
- `node-agent.instance-runtime.working-dir` overrides the container working directory (defaults to the workspace mount path).
- `node-agent.instance-runtime.stop-timeout-seconds` controls the graceful stop timeout before the node agent force-kills a container (unset uses Docker defaults).
- S3 configuration is required for template storage. When `node-agent.s3.endpoint` is set, the client
  uses path-style requests for local or custom S3 endpoints.

## Edge cases / risks
- Missing required values stops the node agent at startup with a detailed error.
- Secrets are redacted in startup logs, but the paths to secrets are not.
- When `node-agent.registration-enabled=true`, failed Brain registration stops the node agent.
- If `node-agent.auth.token-path` is set but unreadable, registration fails and the node agent stops.
- If neither `node-agent.auth.cert-path` nor `node-agent.auth.token-path` is configured, command endpoints respond with HTTP 500.
- Missing or invalid Brain credentials on command endpoints return HTTP 401.
- When `node-agent.template-cache-check.enabled=true`, missing template-id/version/checksum values
  stop the node agent at startup.
- Invalid template cache limit values stop the node agent at startup.
- Missing or invalid S3 settings stop the node agent when template storage is initialized.
- Invalid Docker timeout values stop the node agent at startup.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/config/NodeConfig.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/devmode/controller/DevModeController.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/devmode/service/DevModeService.java`
- `backend/node-agent/src/main/resources/application.yml`
- `docs/node/operations/instance-workspaces.md`
