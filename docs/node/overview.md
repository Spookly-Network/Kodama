# Node Agent Overview

## Purpose
The Node Agent is a lightweight Java service that runs on each node and executes Brain commands.

## What changed
- Added a typed `NodeConfig` model for node-agent settings.
- Added startup validation for required configuration values.
- Expanded the startup log to include the effective configuration (sans secrets).
- Added Brain registration on startup and in-memory caching of the assigned node id.
- Added a heartbeat scheduler that reports node status and usage to the Brain.
- Added a cache purge endpoint so the Brain can instruct nodes to clear cached templates.
- Added a dev-mode toggle endpoint so the Brain can force cache bypass on template fetches.
- Added per-instance workspace layout helpers that create merged/config, logs, and temp folders on demand.
- Added a template layer merge service that applies cached templates into the merged workspace directory.
- Added variable substitution over merged workspace text files using Brain-provided values.
- Added a prepare command handler that assembles templates, applies variable substitution, and calls back to the Brain.

## How to use / impact
- Build and run with `./gradlew :node-agent:bootRun` from `backend/`.
- Configure via environment variables or CLI args (`--node-agent.<key>=...`).
  - Required:
    - `NODE_AGENT_NAME`
    - `NODE_AGENT_NODE_VERSION`
    - `NODE_AGENT_REGION`
    - `NODE_AGENT_CAPACITY_SLOTS`
    - `NODE_AGENT_BRAIN_BASE_URL`
    - `NODE_AGENT_CACHE_DIR`
  - Optional:
    - `NODE_AGENT_ID` (assigned on registration)
    - `NODE_AGENT_DEV_MODE`
    - `NODE_AGENT_TAGS`
    - `NODE_AGENT_BASE_URL`
    - `NODE_AGENT_REGISTRATION_ENABLED`
    - `NODE_AGENT_HEARTBEAT_INTERVAL_SECONDS` (override Brain-provided interval)
    - `NODE_AGENT_WORKSPACE_DIR`
    - `NODE_AGENT_DOCKER_HOST`
    - `NODE_AGENT_VARIABLE_SUBSTITUTION_MAX_FILE_BYTES`
    - `NODE_AGENT_AUTH_HEADER_NAME`
    - `NODE_AGENT_AUTH_TOKEN_PATH`
    - `NODE_AGENT_AUTH_CERT_PATH`
    - `NODE_AGENT_S3_ENDPOINT`
    - `NODE_AGENT_S3_REGION`
    - `NODE_AGENT_S3_BUCKET`
    - `NODE_AGENT_S3_ACCESS_KEY`
    - `NODE_AGENT_S3_SECRET_KEY`
- See `docs/node/operations/configuration.md` for the full mapping.

## Edge cases / risks
- If required configuration is missing, the node agent will exit on startup with a clear error.
- If Brain registration fails, the node agent will exit on startup and log the error.
- Heartbeat failures are logged with retries, but do not crash the node agent process.
- Instance workspaces are created when requested; invalid workspace configuration will fail at request time.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/NodeAgentApplication.java`
- `backend/node-agent/src/main/resources/application.yml`
- `docs/node/operations/configuration.md`
- `docs/node/operations/instance-workspaces.md`
- `docs/node/operations/template-merge.md`
- `docs/node/operations/instance-commands.md`
