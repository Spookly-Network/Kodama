# Instance Groups

## Purpose
Instance groups provide optional organization for instances. Instances can belong to zero, one, or many groups. Group membership is used to apply shared template assignments.

## Data Model
- `instance_groups`
- `instance_group_memberships(instance_id, group_id)`

## API Surface
- `GET /api/instance-groups` — list groups.
- `POST /api/instance-groups` — create group.
- `GET /api/instance-groups/{groupId}` — fetch group by id.
- `GET /api/instances/{id}/groups` — list groups for an instance.
- `PUT /api/instances/{id}/groups/{groupId}` — add membership (idempotent).
- `DELETE /api/instances/{id}/groups/{groupId}` — remove membership (idempotent).

## Behavior Notes
- Group names must be unique; duplicates return `409 Conflict`.
- Membership operations validate instance and group existence (`404 Not Found`).
- Adding an existing membership is a no-op with `204 No Content`.
- Removing a missing membership is a no-op with `204 No Content` after validating the instance and group exist.
- Membership is many-to-many; instances can join multiple groups and groups can contain many instances.
- Template resolution treats group assignments as additive inputs and applies the rules in `docs/brain/template-assignments.md`.

## Edge Cases / Risks
- Membership changes do not change instance lifecycle state; they only affect resolved template layers.
- If a group is deleted or no longer referenced by any instance, its assignments are not applied anywhere.

## Links
- OpenAPI: `contracts/openapi.yml`
- Assignments: `docs/brain/template-assignments.md`
