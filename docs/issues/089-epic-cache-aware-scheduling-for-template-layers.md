# #89 [Epic] Cache-aware scheduling for template layers

## Summary
Prefer nodes that already have the instance’s required template layers cached, to reduce cold starts and S3 traffic.

## Goal
Scheduling decisions consider template cache affinity (as a preference), while keeping the existing eligibility rules and deterministic tie-breakers. Node agents report cache hits/downloads so the Brain can build a lightweight cache index.

## Context
Nodes maintain a local template cache keyed by Template ID + Version + Checksum. When scheduling ignores cache state, instances often land on cold nodes and re-download templates. This epic adds a small Brain-side cache index and uses it during scheduling. Scheduling is also being refactored to resource-based selection (#83 / #86), so cache affinity must integrate cleanly after that refactor.

## Scope
- Brain stores a lightweight node→template cache index (not a full filesystem listing).
- Node reports cache outcomes during prepare (hit/download/mismatch).
- Brain uses cache affinity to rank eligible nodes (preference, not hard constraint).
- Purge-cache invalidates the Brain index for that node.
- Tests + docs for the above.

## Linked Tasks
- [ ] #90
- [ ] #91
- [ ] #92
- [ ] #93
- [ ] #94
- [ ] #95
- [ ] #96
- [ ] #97
- [ ] #98

## Notes
- Related: #83, #86
- Keep scheduling deterministic (stable tie-breakers).
