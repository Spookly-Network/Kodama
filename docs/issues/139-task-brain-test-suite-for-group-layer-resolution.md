# #139 [Task] Brain: test suite for group-aware assignment resolution

## Summary
Add focused tests for multi-group membership, latest-version resolution, and deterministic merge behavior.

## Details
This change combines multiple assignment sources and precedence rules, which requires strong regression coverage.

## Scope / Requirements
- Unit tests for:
  - equal priorities allowed,
  - direct instance override over group,
  - duplicate group contributions,
  - deterministic tie-breaks.
- Integration/DataJpa tests for assignment persistence and joins.
- Tests for `templateId`-only (latest) and `templateId + templateVersionId` (exact).
- Negative tests for missing/mismatched template-version references.

## Acceptance Criteria
- Tests cover all agreed rules and pass consistently.
- No flaky tests due to ordering or timing.
- Coverage includes happy paths and error/conflict paths.

## Notes / References
- Parent epic: #131
- Blocked by #137.
