# Instance Workspaces (Node Agent)

## Purpose
Describe how the node agent maps instance IDs to local workspace directories and prepares them for template/config merges.

## What changed
- Defined a deterministic on-disk layout for instance workspaces under `node-agent.workspace-dir`.
- Added a helper that resolves and creates workspace folders for a given instance id.

## How to use / impact
- Workspace root: `${NODE_AGENT_WORKSPACE_DIR:-./data}/instances`.
- Layout per instance:
  - `<instanceId>/merged` for merged template/config output.
  - `<instanceId>/logs` for instance logs.
  - `<instanceId>/temp` for transient runtime files.
- The node agent creates these directories when `prepareWorkspace(instanceId)` is called.
- `instanceId` must be a single path segment (no slashes or `..`).

## Edge cases / risks
- If `node-agent.workspace-dir` is blank or invalid, workspace resolution fails with a clear error.
- A malformed `instanceId` is rejected to prevent directory traversal.
- Directory creation failures surface as `InstanceWorkspaceException` with the target path.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/workspace/InstanceWorkspaceLayout.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/workspace/InstanceWorkspaceManager.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/config/NodeConfig.java`
