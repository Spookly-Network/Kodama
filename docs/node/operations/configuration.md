# Node Agent Configuration

## Purpose
Describe the configuration inputs for the node agent and how they map to environment variables or CLI arguments.

## What changed
- Introduced a typed `NodeConfig` model that binds to `node-agent.*` settings.
- Added startup validation and a sanitized configuration log line.

## How to use / impact
- Configure with environment variables or CLI args (`--node-agent.<key>=...`).
- Required settings:
  - `node-agent.node-id` (`NODE_AGENT_ID`)
  - `node-agent.node-name` (`NODE_AGENT_NAME`)
  - `node-agent.brain-base-url` (`NODE_AGENT_BRAIN_BASE_URL`)
  - `node-agent.cache-dir` (`NODE_AGENT_CACHE_DIR`)
- Optional settings:
  - `node-agent.workspace-dir` (`NODE_AGENT_WORKSPACE_DIR`, default `./data`)
  - `node-agent.docker-host` (`NODE_AGENT_DOCKER_HOST`)
  - `node-agent.auth.token-path` (`NODE_AGENT_AUTH_TOKEN_PATH`)
  - `node-agent.auth.cert-path` (`NODE_AGENT_AUTH_CERT_PATH`)
  - `node-agent.s3.endpoint` (`NODE_AGENT_S3_ENDPOINT`)
  - `node-agent.s3.region` (`NODE_AGENT_S3_REGION`)
  - `node-agent.s3.bucket` (`NODE_AGENT_S3_BUCKET`)
  - `node-agent.s3.access-key` (`NODE_AGENT_S3_ACCESS_KEY`)
  - `node-agent.s3.secret-key` (`NODE_AGENT_S3_SECRET_KEY`)

## Edge cases / risks
- Missing required values stops the node agent at startup with a detailed error.
- Secrets are redacted in startup logs, but the paths to secrets are not.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/config/NodeConfig.java`
- `backend/node-agent/src/main/resources/application.yml`
