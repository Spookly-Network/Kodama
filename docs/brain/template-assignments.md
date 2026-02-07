# Template Assignments

## Purpose
Template assignments replace the legacy `instance_template_layers` table and allow templates to be attached directly to instances and to groups. Effective template layers are resolved by merging instance and group assignments.

## Data Model
- `instance_template_assignments` (direct instance assignments)
- `group_template_assignments` (group assignments)

Fields:
- `template_id` (required)
- `template_version_id` (optional)
- `priority` (required, non-unique)

When `priority` is omitted in `POST /api/instances`, the list order is used. When omitted in assignment endpoints, it defaults to `0`.

## Resolution Rules
Effective layers are derived with deterministic rules:
- Source set = all group assignments + direct instance assignments.
- `templateId` resolves to the latest template version when `templateVersionId` is absent.
- When `templateVersionId` is omitted, the write path rejects templates with no versions (`404 Not Found`).
- Conflicts by `templateId` are resolved in favor of direct instance assignments.
- Direct instance assignments are not deduplicated; multiple layers for the same `templateId` are preserved and ordered by priority.
- Duplicate group contributions for the same `templateId` are deduplicated by lowest `priority`, then `groupId` (UUID natural order), then assignment id.
- Ordering is by `priority` ascending, then source (`INSTANCE` before `GROUP`), then `templateId` (UUID natural order), then assignment id.
- `orderIndex` is derived from the sorted list to keep node merge order deterministic.

If a template has no versions at resolve time, resolution fails with `404 Not Found`.

## API Surface
- `POST /api/instances` accepts `templateLayers` as assignment definitions.
- `GET /api/instances/{id}/template-assignments` — list direct assignments.
- `POST /api/instances/{id}/template-assignments` — add direct assignment.
- `DELETE /api/instances/{id}/template-assignments/{assignmentId}` — remove direct assignment.
- `GET /api/instance-groups/{groupId}/template-assignments` — list group assignments.
- `POST /api/instance-groups/{groupId}/template-assignments` — add group assignment.
- `DELETE /api/instance-groups/{groupId}/template-assignments/{assignmentId}` — remove group assignment.

## Migration Notes
Flyway migration `V12__create_instance_group_and_assignment_tables.sql` backfills legacy `instance_template_layers` into `instance_template_assignments` with `priority = order_index`.

## Links
- OpenAPI: `contracts/openapi.yml`
- Groups: `docs/brain/instance-groups.md`
