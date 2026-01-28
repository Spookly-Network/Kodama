# Template Cache Layout

## Purpose
Define where the node agent stores cached templates and how cache paths are resolved.

## What changed
- Added a deterministic cache directory layout rooted at `node-agent.cache-dir`.
- Created the template cache root on startup before any downloads occur.
- Added cache lookup that validates cached template checksums before reuse.
- Added an optional startup cache check for manually seeded cache entries.
- Added cache population that downloads template tarballs from S3, extracts them into a temp directory, and moves them into place atomically.
- Added metadata written alongside cached templates (including checksum, S3 key, and cache timestamp).

## How to use / impact
- The node agent creates `<cacheDir>/templates` on startup.
- For each template version, the layout is:
  - `<cacheDir>/templates/<templateId>/<version>/contents/` (extracted files)
  - `<cacheDir>/templates/<templateId>/<version>/checksum.sha256` (checksum marker)
  - `<cacheDir>/templates/<templateId>/<version>/metadata.json` (metadata marker)
- Use `TemplateCacheLayout.resolveTemplateVersion(templateId, version)` to resolve paths.
- Write the expected checksum (hex string) into `checksum.sha256`. Whitespace is trimmed on read.
- Use `TemplateCacheLookupService.findCachedTemplate(templateId, version, expectedChecksum)` to validate a cache entry.
  - Returns `NOT_FOUND` when the contents directory or checksum file is missing.
  - Returns `CHECKSUM_MISMATCH` when the stored checksum differs.
- Use `TemplateCachePopulateService.ensureCachedTemplate(templateId, version, checksum, s3Key)` to download and extract a tarball when cache is missing or invalid.
  - Downloads the tarball to a temp file, extracts into a temp directory, then atomically moves into `<templateId>/<version>`.
  - Writes `checksum.sha256` and `metadata.json` before the atomic move.
  - Validates the downloaded tarball checksum (SHA-256) against the expected checksum before writing cache markers.
  - If the storage response reports a content length, the download length is verified before extraction.
  - Enforces extraction limits (`node-agent.template-cache-limits.*`) for total extracted bytes and entry count.
  - Restores POSIX permissions from tar entry modes when supported; non-POSIX filesystems fall back to best-effort executable bits.
  - `metadata.json` fields: `templateId`, `version`, `checksum`, `s3Key`, `cachedAt`.
- For manual validation at startup, set `node-agent.template-cache-check.*` to trigger a single
  cache lookup and log the hit/miss decision.

## Edge cases / risks
- `templateId` and `version` must be single path segments (no slashes or `..`).
- Invalid cache paths or permission failures stop the node agent at startup.
- Unreadable checksum files throw `TemplateCacheException` and should be treated as cache errors.
- Partial downloads or extraction failures are cleaned up before the error is raised.
- Tarballs that exceed extraction limits are rejected and cleaned up.

## Links
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/cache/TemplateCacheLayout.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/cache/TemplateCacheInitializer.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/cache/TemplateCacheLookupService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/cache/TemplateCacheLookupResult.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/template/cache/TemplateCachePopulateService.java`
- `backend/node-agent/src/main/java/net/spookly/kodama/nodeagent/config/NodeConfig.java`
