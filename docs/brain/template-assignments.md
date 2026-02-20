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
- If multiple versions share the latest `createdAt`, the resolver picks the version with the highest id to keep ordering deterministic.
- When `templateVersionId` is omitted, the write path rejects templates with no versions (`404 Not Found`).
- Conflicts by `templateId` are resolved in favor of direct instance assignments.
- Direct instance assignments are not deduplicated; multiple layers for the same `templateId` are preserved and ordered by priority.
- Duplicate group contributions for the same `templateId` are deduplicated by lowest `priority`, then `groupId` (UUID natural order), then assignment id.
- Ordering is by `priority` ascending, then source (`INSTANCE` before `GROUP`), then `templateId` (UUID natural order), then assignment id.
- `orderIndex` is derived from the sorted list to keep node merge order deterministic.

If a template has no versions at resolve time, resolution fails with `404 Not Found`.

## Examples
Example input (UUIDs abbreviated for readability):

Instance assignments:
- IA-001: templateId `T-01`, templateVersionId `V-01`, priority `1`
- IA-002: templateId `T-02`, templateVersionId omitted, priority `1`

Group assignments:
- GA-001 (group `G-01`): templateId `T-01`, templateVersionId omitted, priority `0`
- GA-002 (group `G-02`): templateId `T-03`, templateVersionId omitted, priority `1`
- GA-003 (group `G-03`): templateId `T-03`, templateVersionId omitted, priority `1`
- GA-004 (group `G-04`): templateId `T-04`, templateVersionId `V-04`, priority `2`

Assume UUID ordering `T-01 < T-02 < T-03 < T-04` and `G-02 < G-03`.

Resolution:
- `T-01` group assignment is dropped because a direct instance assignment exists.
- `T-03` group duplicates are deduplicated; GA-002 wins over GA-003 because priorities tie and `G-02` sorts before `G-03`.
- `T-02` resolves to the latest version at resolve time because `templateVersionId` is omitted.

Final order (with `orderIndex`) uses priority, source, then `templateId` ordering:

```
0: IA-001 (T-01, source INSTANCE, priority 1)
1: IA-002 (T-02, source INSTANCE, priority 1)
2: GA-002 (T-03, source GROUP, priority 1)
3: GA-004 (T-04, source GROUP, priority 2)
```

## Edge Cases / Risks
- `templateId` is required; missing it returns `400 Bad Request`.
- `templateVersionId` must belong to `templateId`; mismatches return `400 Bad Request`.
- Missing `templateVersionId` uses latest version at resolve time; if none exists, resolution fails with `404 Not Found`.
- Group assignments for a `templateId` are ignored when any direct instance assignment exists for that template.
- Multiple direct instance assignments for the same `templateId` are preserved and ordered; they are not deduplicated.

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
