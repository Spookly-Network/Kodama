# #154 [Feature] Brain: blueprint group links API

## Summary
Add endpoints to manage blueprint-to-group links for instance creation.

## Details
Blueprint group links define which instance groups should be applied when creating instances from a blueprint.

## Scope / Requirements
- Endpoints:
  - GET /api/blueprints/{id}/groups
  - PUT /api/blueprints/{id}/groups/{groupId}
  - DELETE /api/blueprints/{id}/groups/{groupId}
- Validate groupId exists.
- PUT is idempotent; DELETE is idempotent.
- Store links in blueprint_group_links table.

## Acceptance Criteria
- Links can be added, listed, and removed.
- Invalid groupId returns 404.
- Tests cover idempotent behavior and validation.
