# #93 [Task] Brain: invalidate cache index on purge-cache command

## Summary
When Brain triggers a template cache purge on a node, also clear the Brain-side cache index entries for that node.

## Details
If the Brain keeps believing templates are cached after a purge, scheduling decisions will be wrong. This task ensures the Brain’s cache index stays consistent with the purge command semantics.

## Scope / Requirements
- Locate the code path that sends the purge-cache instruction to a node.
- After dispatching purge-cache (or after receiving its acknowledgement, if that exists), delete cache index rows for the node.
- Add logging: nodeId, removedRowCount.
- If purge fails to send, do not delete the index.

## Acceptance Criteria
- Purge-cache dispatch triggers cache index invalidation for the targeted node.
- If purge cannot be sent, cache index is not deleted.

## Notes / References
- Blocked by #90 (needs cache index persistence).
- Parent epic: #89
