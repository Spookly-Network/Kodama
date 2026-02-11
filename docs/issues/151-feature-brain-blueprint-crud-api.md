# #151 [Feature] Brain: blueprint CRUD API

## Summary
Expose REST endpoints to create, list, update, and soft-delete blueprints.

## Details
Provide CRUD with validation and consistent DTOs for blueprint metadata and runtime defaults.

## Scope / Requirements
- Endpoints:
  - GET /api/blueprints
  - POST /api/blueprints
  - GET /api/blueprints/{id}
  - PUT /api/blueprints/{id}
  - DELETE /api/blueprints/{id} (soft delete)
- Validate:
  - name is required and unique
  - slotsRequired >= 1
  - containerImage is required
  - startCommand is required and stored as exec-form array
- installScript and variablesJson are optional.
- List endpoint returns non-deleted blueprints only.
- Delete sets deletedAt; no hard delete.

## Acceptance Criteria
- CRUD endpoints return expected payloads and status codes.
- Duplicate name returns 409.
- Invalid fields return 400 with clear errors.
- Tests cover create, update, delete, and validation errors.
