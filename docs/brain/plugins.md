# Brain Plugins

## Purpose
Enable Brain-specific extensions without changing core code. Brain plugins are loaded from a local `plugins` directory and can mutate instance prepare variables before the Node receives them.

## What changed
- Added a Brain plugin API (`KodamaBrainPlugin`) with a single prepare hook.
- Added a plugin loader that scans a local `plugins` directory for JARs.
- Added configuration for plugin directory and enabled plugin IDs.

## How to use / impact
- Place plugin JARs in the Brain working directory under `./plugins` (or override with `PLUGINS_DIR`).
- Provide a ServiceLoader entry in the JAR at `META-INF/services/net.spookly.kodama.brain.plugin.KodamaBrainPlugin`.
- Enable plugins via `PLUGINS_ENABLED` (comma-separated list of plugin IDs).

Plugin hook behavior:
- `onPrepareInstance(...)` can set or remove variables before the prepare command is sent to the Node.
- Variables are delivered as `variables` (map) in the node prepare payload. When a plugin mutates variables, `variablesJson` is cleared to avoid ambiguity.

Configuration keys:
- `plugins.dir` / `PLUGINS_DIR` (default: `./plugins`)
- `plugins.enabled` / `PLUGINS_ENABLED` (default: empty)

## Edge cases / risks
- If `plugins.enabled` is set and the directory is missing or empty, Brain will fail fast on startup.
- Plugin failures during prepare will fail the prepare dispatch (the instance will not start).
- Duplicate plugin IDs are rejected.

## Links
- `backend/brain/src/main/java/net/spookly/kodama/brain/plugin/KodamaBrainPlugin.java`
- `backend/brain/src/main/java/net/spookly/kodama/brain/plugin/BrainPluginRegistry.java`
- `backend/brain/src/main/resources/application.yml`
