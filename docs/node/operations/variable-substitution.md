# Instance Variable Substitution (Node Agent)

## Purpose
Replace placeholder variables inside merged instance workspace files with values provided by the Brain.

## What changed
- Added a variable substitution service that scans the merged workspace and replaces `${VARIABLE}` placeholders.
- Substitution is applied only to text files (UTF-8); binary files are skipped to avoid corruption.
- Added a size guard to skip files larger than `node-agent.variable-substitution.max-file-bytes`.
- Unknown variables are left untouched.

## How to use / impact
- After template layers are merged into the instance `merged` directory, run variable substitution.
- Provide the variable map from the Brain prepare payload (e.g., `INSTANCE_ID`, `PORT`, `SERVER_NAME`).
- If the variable map is empty, no files are modified.
- Large files are skipped to avoid loading large binaries into memory.

## Edge cases / risks
- Binary files (or invalid UTF-8) are skipped and remain unchanged.
- Files larger than `node-agent.variable-substitution.max-file-bytes` are skipped (`0` disables the limit).
- Placeholders without matching variables remain in the file.
- Only `${NAME}` patterns are substituted; other formats are not recognized.
- Read/write failures abort the substitution step and surface as workspace errors.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/instance/workspace/InstanceVariableSubstitutionService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/merge/TemplateLayerMergeService.java`
