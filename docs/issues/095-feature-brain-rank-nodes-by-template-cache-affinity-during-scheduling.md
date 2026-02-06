# #95 [Feature] Brain: rank nodes by template cache affinity during scheduling

## Summary
After eligibility filtering, prefer the node that already has the most required template layers cached (id/version/checksum match).

## Details
This adds a ranking step to scheduling. It must integrate with the resource-based scheduling refactor (#86) so cache affinity becomes an additional preference, not a replacement for resource fit. The result must stay deterministic.

## Scope / Requirements
- After hard filters pass (ONLINE, region/tags/devModeAllowed, and resource fit from #86):
  - compute `cacheScore(node) = number of required templates cached on node`.
- Pick the node with the highest cacheScore.
- Tie-breakers (keep deterministic):
  - follow the resource-based rule from #86 (e.g. highest remaining headroom / lowest utilization),
  - then node name, then node id.
- Dev-mode handling:
  - if node is in dev-mode, treat cacheScore as 0 (cache affinity should not influence scheduling).
- Add trace logging: instanceId, chosenNodeId, templateCount, cacheScore.

## Acceptance Criteria
- With multiple eligible nodes, scheduling picks the one with highest cacheScore.
- If cacheScore is equal, scheduling result matches the deterministic tie-breakers.
- Dev-mode nodes are never preferred due to cache.

## Notes / References
- Blocked by #86 (resource-based scheduling change) and #90 (cache index).
- Parent epic: #89
- Related: #83, #86
