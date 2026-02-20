# #152 [Feature] Brain: blueprint template assignments API

## Summary
Add endpoints to manage template assignments attached to blueprints.

## Details
Blueprints define template layers for instance creation using the same assignment model as instances/groups.

## Scope / Requirements
- Endpoints:
  - GET /api/blueprints/{id}/template-assignments
  - POST /api/blueprints/{id}/template-assignments
  - DELETE /api/blueprints/{id}/template-assignments/{assignmentId}
- Validate templateId exists.
- Validate templateVersionId belongs to templateId when provided.
- Priority is non-negative; duplicates allowed.
- Responses mirror existing assignment DTOs.

## Acceptance Criteria
- Assignments can be created, listed, and removed.
- Validation errors return 400/404 consistent with existing assignment endpoints.
- Tests cover assignment lifecycle and template/version validation.
