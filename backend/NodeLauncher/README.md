# Node Launcher

Self-updating launcher for the Kodama Node Agent. The launcher is intended to run as a `systemd` service and manage the `current` and `previous` node-agent symlinks in `/opt/kodama-node`.

## Installation

1. Build the launcher jar:
   - From `backend/`: `./gradlew :NodeLauncher:jar`
2. Create target folders:
   - `sudo mkdir -p /opt/kodama-node/launcher /opt/kodama-node/agent /opt/kodama-node/data/logs`
3. Copy launcher artifact:
   - `sudo cp NodeLauncher/build/libs/launcher.jar /opt/kodama-node/launcher/launcher.jar`
4. Place launcher config:
   - `sudo cp NodeLauncher/config.example.yml /opt/kodama-node/launcher/config.yml`
5. Ensure at least one agent jar is installed and linked:
   - `/opt/kodama-node/agent/agent-<version>.jar`
   - `/opt/kodama-node/agent/current -> agent-<version>.jar`

## Required Layout

```text
/opt/kodama-node/
  launcher/
    launcher.jar
    config.yml
  agent/
    agent-<version>.jar
    current   (symlink)
    previous  (symlink)
  data/
    logs/
```

## Example `config.yml`

```yaml
github:
  owner: spookly-net
  repo: kodama
  channel: stable
  assetRegex: "kodama-node-agent-(.*)\\.jar"
verify:
  sha256Required: true
  sha256Suffix: ".sha256"
installDir: "/opt/kodama-node"
javaBin: "java"
agentArgs:
  - "--brainUrl=http://localhost:8080"
updateMode: "NEXT_START"
```

The launcher reads config path from:
- `KODAMA_LAUNCHER_CONFIG`
- Fallback: `./config.yml` (working directory of the launcher process)

## systemd Unit Example

```ini
[Unit]
Description=Kodama Node Launcher
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=/opt/kodama-node/launcher
Environment=KODAMA_LAUNCHER_CONFIG=/opt/kodama-node/launcher/config.yml
ExecStart=/usr/bin/java -jar /opt/kodama-node/launcher/launcher.jar
Restart=always
RestartSec=5
User=kodama
Group=kodama

[Install]
WantedBy=multi-user.target
```

## Runtime Behavior

- On startup, launcher checks GitHub Releases and selects:
  - first non-prerelease for `stable`
  - first release for `beta`
- If a newer node-agent version is found:
  - downloads jar asset and checksum asset
  - verifies SHA256 (when enabled)
  - installs `agent/agent-<version>.jar`
  - atomically updates symlinks:
    - `previous -> old current target`
    - `current -> new version`
- Starts node-agent with:
  - `<javaBin> -jar /opt/kodama-node/agent/current <agentArgs...>`
- Crash rollback policy:
  - if agent exits within 30 seconds, 3 times in a row:
    - rollback `current -> previous`
    - restart once on the rolled-back version
