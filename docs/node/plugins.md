# Node Agent Plugins

## Purpose
Enable Node Agent extensions without changing core code. Node plugins are loaded from a local `plugins` directory and can mutate container start parameters (env, labels, command).

## What changed
- Added a Node plugin API (`KodamaNodePlugin`) with a pre-start hook.
- Added a plugin loader that discovers enabled plugin IDs from JARs in a local `plugins` directory.
- Added configuration for plugin directory and enabled plugin IDs.

## How to use / impact
- Place plugin JARs in the Node Agent working directory under `./plugins` (or override with `PLUGINS_DIR`).
- Provide a ServiceLoader entry in the JAR at `META-INF/services/net.spookly.kodama.nodeagent.plugin.KodamaNodePlugin`.
- Enable plugins via `PLUGINS_ENABLED` (comma-separated list of plugin IDs).
- Only plugins listed in `PLUGINS_ENABLED` are validated and loaded; other JARs are ignored.

Plugin hook behavior:
- `onBeforeInstanceStart(...)` can set or remove environment variables and labels, and optionally override the container command.
- If multiple plugins attempt to override the command, startup fails with a clear error.

Configuration keys:
- `plugins.dir` / `PLUGINS_DIR` (default: `./plugins`)
- `plugins.enabled` / `PLUGINS_ENABLED` (default: empty)

## Edge cases / risks
- If `plugins.enabled` is set and the directory is missing or empty, the Node Agent will fail fast on startup.
- Plugin failures during start will abort the container start.
- Duplicate enabled plugin IDs are rejected.
- Null env/label entries are ignored when building the start context.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/plugin/KodamaNodePlugin.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/plugin/NodePluginRegistry.java`
- `backend/node-agent/src/main/resources/application.yml`
