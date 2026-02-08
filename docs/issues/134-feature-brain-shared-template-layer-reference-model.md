# #134 [Feature] Brain: template assignment reference model with required templateId

## Summary
Introduce template assignment records where `templateId` is required and `templateVersionId` is optional.

## Details
Assignment addressing modes:
- `templateId`: resolve latest version at runtime.
- `templateId + templateVersionId`: pin exact version.
Validation enforces that `templateVersionId` belongs to `templateId` when provided.

## Scope / Requirements
- Add assignment DTO/entity validation with rules:
  - `templateId` required,
  - `templateVersionId` optional,
  - `priority >= 0`, duplicates allowed.
- Add helper for resolving latest version from `templateId`.
- Add validation path for template/version mismatch.
- Keep error behavior explicit for missing templates or versions.

## Acceptance Criteria
- Assignment records persist with required `templateId`.
- Latest-version resolution for `templateId` works and is tested.
- Exact-version pinning for `templateVersionId` works and is tested.
- Priority collisions do not fail persistence.

## Notes / References
- Parent epic: #131
- Blocked by #132.
