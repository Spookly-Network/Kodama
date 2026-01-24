# Node Agent Overview

## Purpose
The Node Agent is a lightweight Java service that runs on each node and executes Brain commands.

## What changed
- Added a Node Agent module skeleton with a Spring Boot entrypoint.
- Added minimal configuration wiring and a startup log line.

## How to use / impact
- Build and run with `./gradlew :node-agent:bootRun` from `backend/`.
- Configure via environment variables:
  - `NODE_AGENT_ID`
  - `NODE_AGENT_NAME`
  - `NODE_AGENT_WORKSPACE_DIR`
  - `NODE_AGENT_BRAIN_BASE_URL`
  - `NODE_AGENT_DOCKER_HOST`

## Edge cases / risks
- `NODE_AGENT_BRAIN_BASE_URL` and `NODE_AGENT_DOCKER_HOST` can be left empty for now; no connections are attempted.
- The workspace directory is not created automatically yet.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/NodeAgentApplication.java`
- `backend/node-agent/src/main/resources/application.yml`
