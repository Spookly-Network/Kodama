# #96 [Task] Brain: unit tests for template cache index service

## Summary
Add focused unit tests for the cache index upsert/query logic.

## Details
Before scheduling uses the cache index, we want confidence that the persistence and matching behavior is correct and stable.

## Scope / Requirements
- Add unit tests for:
  - upsert inserts new entries
  - upsert updates last_seen_at for existing entries
  - querying cached templates returns expected set
  - (optional) matching helper counts exact matches by id/version/checksum
- Keep tests fast (in-memory db if available, otherwise repository mocks + service tests).

## Acceptance Criteria
- Tests cover insert + update behavior and pass in CI.
- Edge cases (duplicate reports, mixed versions/checksums) are handled correctly.

## Notes / References
- Blocked by #90.
- Parent epic: #89
