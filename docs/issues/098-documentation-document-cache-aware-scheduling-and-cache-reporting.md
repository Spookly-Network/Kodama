# #98 [Documentation] Document cache-aware scheduling and cache reporting

## Summary
Update docs to describe the new cache index, node reporting, and scheduling ranking behavior.

## Details
This feature spans Brain and Node. Without docs, it’s hard to debug why a node got selected or why cache affinity isn’t applied.

## Scope / Requirements
- Update `scheduling-service.md` with cache affinity ranking and tie-breakers.
- Update `TEMPLATE-SYSTEM.md` with Brain-side cache index + report flow.
- Update `NODE-FLOW.md` to mention reporting of cache outcomes during prepare.
- Add a short troubleshooting section:
  - how to inspect cache index for a node (db query / admin endpoint if available)
  - why dev-mode disables cache preference

## Acceptance Criteria
- Docs reflect the implemented behavior and reference relevant endpoints/tables.
- A developer can understand why a node was preferred from reading the docs.

## Notes / References
- Parent epic: #89
