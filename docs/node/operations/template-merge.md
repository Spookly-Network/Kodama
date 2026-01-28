# Template Layer Merge (Node Agent)

## Purpose
Describe how cached template layers are merged into an instance workspace.

## What changed
- Added a merge service that applies cached template layers in order into the instance workspace.
- Enforced deterministic ordering by `orderIndex` with "last layer wins" overwrite semantics.
- Preserved basic file permissions while copying layer contents.

## How to use / impact
- Provide the list of cached template layer contents (one per layer) and call the merge service.
- Layers are applied in ascending `orderIndex`.
- Files from later layers overwrite earlier ones; non-conflicting files are preserved.
- Output is written into the instance `merged` directory under `node-agent.workspace-dir`.

## Edge cases / risks
- Duplicate `orderIndex` values are rejected to avoid non-deterministic merges.
- Missing or non-directory layer contents fail the merge with a clear error.
- Files already present in the merged directory are only overwritten if a layer provides them.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/merge/TemplateLayerMergeService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/merge/TemplateLayerSource.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/workspace/InstanceWorkspaceManager.java`
