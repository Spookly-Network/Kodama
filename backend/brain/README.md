# Kodama Brain

## Template API

The Brain exposes a minimal REST surface for managing templates and their versions:

- `GET /api/templates` — list templates.
- `GET /api/templates/{id}` — fetch a single template (404 if missing).
- `POST /api/templates` — create a template. Body: `name`, `description`, `type` (all required). `
- `POST /api/templates/{id}/versions` — add a version to a template. Body: `version`, `checksum`, `s3Key` (required) and optional `metadataJson` (404 if template missing, 409 on duplicate version).
- `GET /api/templates/{id}/versions` — list versions for a template (newest first).

Validation: `name`, `type`, `s3Key`, `version`, and `checksum` must be provided. Duplicate template names or versions return HTTP 409.

## Instance API

- `GET /api/instances` — list instances.
- `GET /api/instances/{id}` — fetch a single instance (404 if missing).
- `POST /api/instances` — create an instance. Body: `name`, `requestedBy`, and `templateLayers` (each with required `templateId` and optional `templateVersionId`/`priority`) are required. Optional: `displayName`, `nodeId`, `region`, `tags`, `devModeAllowed`, `variables` (map), `variablesJson`, `portsJson`.
- `GET /api/instances/{id}/template-assignments` — list direct instance assignments.
- `POST /api/instances/{id}/template-assignments` — add a direct instance assignment.
- `DELETE /api/instances/{id}/template-assignments/{assignmentId}` — remove a direct instance assignment.
- `GET /api/instances/{id}/groups` — list groups for an instance.
- `PUT /api/instances/{id}/groups/{groupId}` — add instance to a group.
- `DELETE /api/instances/{id}/groups/{groupId}` — remove instance from a group.
- `GET /api/instance-groups` — list instance groups.
- `POST /api/instance-groups` — create an instance group.
- `GET /api/instance-groups/{groupId}` — fetch a group.
- `GET /api/instance-groups/{groupId}/template-assignments` — list group assignments.
- `POST /api/instance-groups/{groupId}/template-assignments` — add a group assignment.
- `DELETE /api/instance-groups/{groupId}/template-assignments/{assignmentId}` — remove a group assignment.

Validation: at least one template assignment with required `templateId`. Duplicate instance names return HTTP 409. Unknown nodes, templates, or template versions return HTTP 404. `variables` and `variablesJson` are mutually exclusive.

## Blueprint API

- `GET /api/blueprints/{id}/template-assignments` — list blueprint assignments.
- `POST /api/blueprints/{id}/template-assignments` — add a blueprint assignment.
- `DELETE /api/blueprints/{id}/template-assignments/{assignmentId}` — remove a blueprint assignment.
