# Template Cache Layout

## Purpose
Define where the node agent stores cached templates and how cache paths are resolved.

## What changed
- Added a deterministic cache directory layout rooted at `node-agent.cache-dir`.
- Created the template cache root on startup before any downloads occur.

## How to use / impact
- The node agent creates `<cacheDir>/templates` on startup.
- For each template version, the layout is:
  - `<cacheDir>/templates/<templateId>/<version>/contents/` (extracted files)
  - `<cacheDir>/templates/<templateId>/<version>/checksum.sha256` (checksum marker)
  - `<cacheDir>/templates/<templateId>/<version>/metadata.json` (metadata marker)
- Use `TemplateCacheLayout.resolveTemplateVersion(templateId, version)` to resolve paths.

## Edge cases / risks
- `templateId` and `version` must be single path segments (no slashes or `..`).
- Invalid cache paths or permission failures stop the node agent at startup.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/cache/TemplateCacheLayout.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/cache/TemplateCacheInitializer.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/config/NodeConfig.java`
