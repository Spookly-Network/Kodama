# Template Layer Merge (Node Agent)

## Purpose
Describe how cached template layers are merged into an instance workspace.

## What changed
- Added a merge service that applies cached template layers in order into the instance workspace.
- Enforced deterministic ordering by `orderIndex` with "last layer wins" overwrite semantics.
- Preserved basic file permissions while copying layer contents.
- Integrated optional variable substitution after merge for text-based configuration files.

## How to use / impact
- Provide the list of cached template layer contents (one per layer) and call the merge service.
- Layers are applied in ascending `orderIndex`.
- Files from later layers overwrite earlier ones; non-conflicting files are preserved.
- The merged workspace directory is cleared before applying layers to avoid stale files.
- Output is written into the instance `merged` directory under `node-agent.workspace-dir`.
- If variables are supplied, placeholders in text files are replaced after merge.
- Variable substitution respects `node-agent.variable-substitution.max-file-bytes` and skips larger files.

## Edge cases / risks
- Duplicate `orderIndex` values are rejected to avoid non-deterministic merges.
- Missing or non-directory layer contents fail the merge with a clear error.
- Clearing the merged directory removes any files not present in the current layer set.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/merge/TemplateLayerMergeService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/merge/TemplateLayerSource.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/workspace/InstanceWorkspaceManager.java`
- `docs/node/operations/variable-substitution.md`
