# #91 [Feature] Brain: accept template cache reports from nodes

## Summary
Add an API endpoint for nodes to report cache hits/downloads for the templates used during prepare, and update the cache index accordingly.

## Details
Nodes already know which template layers were cache hits vs downloaded during `prepare_instance`. The Brain should learn this incrementally by receiving a small report for only the templates involved in that instance.

## Scope / Requirements
- Add node-authenticated endpoint (or reuse existing node callback channel) to accept a `TemplateCacheReport` payload:
  - nodeId, instanceId
  - templates: [{templateId, version, checksum, status}]
- Validate payload size and fields (reject unknown statuses / missing ids).
- On `HIT` or `DOWNLOADED`: upsert into `node_template_cache` and update `last_seen_at`.
- On `CHECKSUM_MISMATCH`: do not upsert unless accompanied by `DOWNLOADED` for the final checksum.
- Add minimal logging (nodeId, instanceId, counts by status).

## Acceptance Criteria
- Endpoint accepts a valid report and persists cache entries for HIT/DOWNLOADED.
- Invalid payloads are rejected with a clear error response.
- Repeated reports update `last_seen_at` instead of creating duplicates.

## Notes / References
- Blocked by #90 (needs cache index table/service).
- Parent epic: #89
