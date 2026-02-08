# Docker Compose (Dev)

## Purpose
Document the dev compose settings that must align with Brain and Node Agent configuration.

## What changed
- Updated Node Agent environment variables to match `node-agent.*` settings.
- Added required Node Agent S3 settings and cache/workspace directories.
- Disabled Brain security in dev to allow local node registration without tokens.

## How to use / impact
- Start local services with `infra/docker-compose.dev.yml`.
- The Node Agent now requires these env vars to pass startup validation:
  - `NODE_AGENT_NAME`
  - `NODE_AGENT_NODE_VERSION`
  - `NODE_AGENT_REGION`
  - `NODE_AGENT_CAPACITY_SLOTS`
  - `NODE_AGENT_BRAIN_BASE_URL`
  - `NODE_AGENT_CACHE_DIR`
  - `NODE_AGENT_S3_ENDPOINT`
  - `NODE_AGENT_S3_BUCKET`
  - `NODE_AGENT_S3_ACCESS_KEY`
  - `NODE_AGENT_S3_SECRET_KEY`
- `NODE_AGENT_BASE_URL` is set so the Brain can issue commands back to the Node Agent.
- Cache and workspace directories are mounted as named volumes.
- `BRAIN_SECURITY_ENABLED=false` disables auth filters for local development.

## Edge cases / risks
- If Brain security is enabled, node registration requires a node auth token and the dev compose file must mount it.
- Missing required Node Agent values will stop the container on startup.
- The S3 bucket must exist before templates can be downloaded.

## Links
- `infra/docker-compose.dev.yml`
- `docs/node/operations/configuration.md`
- `backend/node-agent/src/main/resources/application.yml`
- `backend/brain/src/main/resources/application.yml`
