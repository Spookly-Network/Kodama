# #153 [Feature] Brain: blueprint port definitions API

## Summary
Add endpoints to manage blueprint port definitions and validate host range configuration.

## Details
Port definitions describe how nodes allocate host ports for containers created from a blueprint.

## Scope / Requirements
- Endpoints:
  - GET /api/blueprints/{id}/ports
  - POST /api/blueprints/{id}/ports
  - DELETE /api/blueprints/{id}/ports/{portId}
- Validate fields:
  - name required and unique per blueprint
  - protocol is tcp or udp
  - containerPort between 1 and 65535
  - hostRange min <= max, step >= 1
- Persist port definitions for use during prepare.

## Acceptance Criteria
- Port definitions can be added, listed, and removed.
- Invalid ranges or protocols return 400 with clear errors.
- Tests cover validation and persistence.
