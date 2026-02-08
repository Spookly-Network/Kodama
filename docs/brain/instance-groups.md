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

## Links
- OpenAPI: `contracts/openapi.yml`
- Assignments: `docs/brain/template-assignments.md`
