# #90 [Feature] Brain: persist node template cache index

## Summary
Add a lightweight DB-backed index that tracks which templates (id/version/checksum) are present in each node’s local cache.

## Details
Scheduling needs a cheap way to estimate cache affinity without asking nodes for their full cache contents. This adds a Brain-side index table and a small service layer to upsert/query entries.

## Scope / Requirements
- Add table `node_template_cache` (or similar) with: node_id, template_id, version, checksum, last_seen_at.
- Add repository + service methods:
  - upsertEntries(nodeId, entries)
  - getCachedTemplatesForNode(nodeId)
  - countMatches(nodeId, requiredTemplates) (optional helper)
- Ensure indexes exist for fast lookups (node_id + template_id + version + checksum).
- Keep the table small and predictable (no giant JSON blobs).

## Acceptance Criteria
- Migration runs successfully on MySQL.
- Service can upsert and query cache entries for a node.
- Lookups for (nodeId + template) are indexed and fast.

## Notes / References
- Parent epic: #89
