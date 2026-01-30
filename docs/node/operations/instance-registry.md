# Instance Registry (Node Agent)

## Purpose
Persist a local record of instance metadata after the node finishes preparing a workspace.

## What changed
- The prepare flow now writes an `instance.json` registry entry per instance.
- The start flow updates the registry with the Docker container id.

## How to use / impact
- Location: `${NODE_AGENT_WORKSPACE_DIR:-./data}/instances/<instanceId>/instance.json`.
- Contents include:
  - instance id, name, display name
  - ports JSON (if provided)
  - resolved variables map used for substitution
  - template layer list from the prepare request
  - prepared timestamp
  - container id (set after the instance is started)
- The registry is overwritten on each successful prepare and updated again when the container id is recorded.

## Edge cases / risks
- If the registry write fails, the prepare request fails and a `/failed` callback is attempted.
- Missing or invalid workspace paths are treated as preparation failures.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/registry/InstanceRegistryService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/service/InstancePrepareService.java`
