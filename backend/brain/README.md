# Kodama Brain

## Template API

The Brain exposes a minimal REST surface for managing templates and their versions:

- `GET /api/templates` — list templates.
- `GET /api/templates/{id}` — fetch a single template (404 if missing).
- `POST /api/templates` — create a template. Body: `name`, `description`, `type`, `createdBy` (all required).
- `POST /api/templates/{id}/versions` — add a version to a template. Body: `version`, `checksum`, `s3Key` (required) and optional `metadataJson` (404 if template missing, 409 on duplicate version).
- `GET /api/templates/{id}/versions` — list versions for a template (newest first).

Validation: `name`, `type`, `s3Key`, `version`, and `checksum` must be provided. Duplicate template names or versions return HTTP 409.

## Instance API

- `GET /api/instances` — list instances.
- `GET /api/instances/{id}` — fetch a single instance (404 if missing).
- `POST /api/instances` — create an instance. Body: `name`, `requestedBy`, and `templateLayers` (each with `templateVersionId` and `orderIndex`) are required. Optional: `displayName`, `nodeId`, `variablesJson`, `portsJson`.

Validation: at least one template layer with unique `orderIndex` values. Duplicate instance names return HTTP 409. Unknown nodes or template versions return HTTP 404.