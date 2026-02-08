# #140 [Documentation] Document instance groups and assignment-based template model

## Summary
Update Brain docs and OpenAPI contract for multi-group memberships and assignment-based template resolution.

## Details
This feature introduces new structures and merge rules that must be reflected in both `/contracts/openapi.yml` and `/docs/brain/`.

## Scope / Requirements
- Update `/contracts/openapi.yml` with:
  - group endpoints,
  - membership endpoints,
  - assignment request/response schemas,
  - validation rule (`templateId` required, `templateVersionId` optional),
  - resolution semantics.
- Add/update docs in `/docs/brain/` for:
  - data model,
  - precedence and tie-break behavior,
  - migration notes,
  - common edge cases.

## Acceptance Criteria
- OpenAPI reflects implemented endpoints and payloads.
- Docs explain deterministic assignment resolution with concrete examples.
- A new contributor can understand override behavior and version resolution.

## Notes / References
- Parent epic: #131
- Blocked by #137 and #138.
