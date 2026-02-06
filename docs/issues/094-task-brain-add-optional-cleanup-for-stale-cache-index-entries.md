# #94 [Task] Brain: add optional cleanup for stale cache index entries

## Summary
Add a small cleanup job or admin action to remove cache index entries that have not been seen for a long time.

## Details
Nodes might be rebuilt, disks wiped, or caches manually cleared without a Brain purge. Stale rows can slowly accumulate and reduce accuracy. A simple cleanup keeps the table small and trustworthy.

## Scope / Requirements
- Decide on a conservative retention window (example: 14–30 days).
- Implement cleanup as one of:
  - scheduled job (daily), or
  - admin endpoint / CLI command
- Delete rows where `last_seen_at < now - retentionWindow`.
- Log removedRowCount and retention window.

## Acceptance Criteria
- Cleanup removes stale rows based on `last_seen_at` and retention config.
- Cleanup can be disabled (config flag) if desired.

## Notes / References
- Blocked by #90.
- Parent epic: #89
