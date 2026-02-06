# #97 [Task] Brain: unit tests for cache-aware scheduling selection

## Summary
Add unit tests proving scheduling prefers higher cache affinity while staying deterministic with tie-breakers.

## Details
This locks in the intended behavior and prevents regressions when scheduling evolves.

## Scope / Requirements
- Add unit tests for:
  - higher cacheScore wins when nodes are otherwise equally eligible
  - tie-breakers are deterministic (resource rule from #86, then name/id)
  - dev-mode node cacheScore treated as 0
  - when no cache data exists, scheduler behavior falls back to normal selection

## Acceptance Criteria
- Tests pass reliably and do not depend on timing or random ordering.
- At least one test covers tie-break determinism.

## Notes / References
- Blocked by #95.
- Parent epic: #89
