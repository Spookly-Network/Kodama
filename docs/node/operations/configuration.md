# Node Agent Configuration

## Purpose
Describe the configuration inputs for the node agent and how they map to environment variables or CLI arguments.

## What changed
- Introduced a typed `NodeConfig` model that binds to `node-agent.*` settings.
- Added startup validation and a sanitized configuration log line.
- Added registration-related configuration for Brain startup registration.
- Added a heartbeat interval override for node-agent heartbeats.

## How to use / impact
- Configure with environment variables or CLI args (`--node-agent.<key>=...`).
- Required settings:
  - `node-agent.node-name` (`NODE_AGENT_NAME`)
  - `node-agent.node-version` (`NODE_AGENT_NODE_VERSION`)
  - `node-agent.region` (`NODE_AGENT_REGION`)
  - `node-agent.capacity-slots` (`NODE_AGENT_CAPACITY_SLOTS`)
  - `node-agent.brain-base-url` (`NODE_AGENT_BRAIN_BASE_URL`)
  - `node-agent.cache-dir` (`NODE_AGENT_CACHE_DIR`)
- Optional settings:
  - `node-agent.node-id` (`NODE_AGENT_ID`, assigned on registration)
  - `node-agent.dev-mode` (`NODE_AGENT_DEV_MODE`, default `false`)
  - `node-agent.tags` (`NODE_AGENT_TAGS`)
  - `node-agent.base-url` (`NODE_AGENT_BASE_URL`)
  - `node-agent.registration-enabled` (`NODE_AGENT_REGISTRATION_ENABLED`, default `true`)
  - `node-agent.heartbeat-interval-seconds` (`NODE_AGENT_HEARTBEAT_INTERVAL_SECONDS`, default `0`)
  - `node-agent.workspace-dir` (`NODE_AGENT_WORKSPACE_DIR`, default `./data`)
  - `node-agent.docker-host` (`NODE_AGENT_DOCKER_HOST`)
  - `node-agent.auth.header-name` (`NODE_AGENT_AUTH_HEADER_NAME`, default `X-Node-Token`)
  - `node-agent.auth.token-path` (`NODE_AGENT_AUTH_TOKEN_PATH`)
  - `node-agent.auth.cert-path` (`NODE_AGENT_AUTH_CERT_PATH`)
  - `node-agent.s3.endpoint` (`NODE_AGENT_S3_ENDPOINT`)
  - `node-agent.s3.region` (`NODE_AGENT_S3_REGION`)
  - `node-agent.s3.bucket` (`NODE_AGENT_S3_BUCKET`)
  - `node-agent.s3.access-key` (`NODE_AGENT_S3_ACCESS_KEY`)
  - `node-agent.s3.secret-key` (`NODE_AGENT_S3_SECRET_KEY`)
- When registration is enabled, the node agent reads the token from `node-agent.auth.token-path`
  and sends it to the Brain using `node-agent.auth.header-name`.
- When `node-agent.heartbeat-interval-seconds` is `0`, the node agent uses the heartbeat interval
  provided by the Brain during registration.

## Edge cases / risks
- Missing required values stops the node agent at startup with a detailed error.
- Secrets are redacted in startup logs, but the paths to secrets are not.
- When `node-agent.registration-enabled=true`, failed Brain registration stops the node agent.
- If `node-agent.auth.token-path` is set but unreadable, registration fails and the node agent stops.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/config/NodeConfig.java`
- `backend/node-agent/src/main/resources/application.yml`
