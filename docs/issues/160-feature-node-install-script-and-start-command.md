# #160 [Feature] Node: install script and start command execution

## Summary
Run the blueprint install script once before the start command and use registry values for container image and start command.

## Details
Install script must always run before the first container start; start command uses exec form compatible with Docker.

## Scope / Requirements
- During start:
  - if installScript is present and installCompleted is false, run the install script via /bin/sh -c before the start command
  - mark installCompleted after a successful run (registry field or sentinel file)
- Use containerImage and startCommand from registry; do not rely on variables for image.
- Fail start with clear error if startCommand or containerImage is missing.
- Preserve plugin hook behavior (allow plugins to adjust env/labels/command).

## Acceptance Criteria
- Install script runs exactly once per instance before first start.
- Start uses registry containerImage and startCommand.
- Failures report /failed and do not mark installCompleted.
