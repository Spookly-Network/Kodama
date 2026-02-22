# Node Launcher Operations

## Purpose
Describe how the `NodeLauncher` module updates and starts the Node Agent on Linux nodes.

## What changed
- Added `backend/NodeLauncher` implementation for a self-updating node launcher.
- Added startup config loading from `KODAMA_LAUNCHER_CONFIG` with fallback `./config.yml`.
- Added automatic default config creation when the resolved config file is missing.
- Added GitHub Releases lookup with support for `stable` and `beta` channels.
- Added asset selection via regex and SHA256 verification support.
- Added atomic symlink switching for `agent/current` and `agent/previous`.
- Added process startup logic for the node-agent jar with configurable Java binary and args.
- Agent process now starts with `/opt/kodama-node/agent` as working directory so local Spring
  config files (for example `application.yaml`) are loaded from the agent folder.
- Added crash monitoring with rollback after repeated startup failures.

## How to use / impact
- Build launcher: `./gradlew :NodeLauncher:jar` (from `backend/`).
- Install artifact as `/opt/kodama-node/launcher/launcher.jar`.
- Provide config file (`/opt/kodama-node/launcher/config.yml` by default), or let launcher create one on first start.
- After auto-creation, set `github.owner` and `github.repo` for your repository.
- Manage launcher with `systemd`:
  - `ExecStart=/usr/bin/java -jar /opt/kodama-node/launcher/launcher.jar`
  - `WorkingDirectory=/opt/kodama-node/launcher`
  - `Environment=KODAMA_LAUNCHER_CONFIG=/opt/kodama-node/launcher/config.yml`
- The launcher updates node-agent only during startup (`updateMode: NEXT_START`).

Required filesystem layout:

```text
/opt/kodama-node/
  launcher/
    launcher.jar
    config.yml
  agent/
    agent-<version>.jar
    current
    previous
  data/
    logs/
```

Crash rollback behavior:
- If node-agent exits within 30 seconds and this happens 3 times in a row:
  - launcher points `current` back to `previous`
  - launcher starts node-agent one more time on the rolled-back version

## Edge cases / risks
- If `agent/current` is missing and no update can be installed, launcher exits with an error.
- If checksum verification is required and checksum asset is missing, update is skipped and current version is kept.
- If rollback is required but `agent/previous` does not exist, launcher exits with the failing agent exit code.
- Launcher does not self-update and does not modify Brain or Node Agent behavior.

## Links
- `backend/NodeLauncher/src/main/java/net/spookly/kodama/nodelauncher/LauncherApplication.java`
- `backend/NodeLauncher/src/main/java/net/spookly/kodama/nodelauncher/ConfigLoader.java`
- `backend/NodeLauncher/src/main/java/net/spookly/kodama/nodelauncher/GitHubReleaseClient.java`
- `backend/NodeLauncher/src/main/java/net/spookly/kodama/nodelauncher/SymlinkManager.java`
- `backend/NodeLauncher/README.md`
