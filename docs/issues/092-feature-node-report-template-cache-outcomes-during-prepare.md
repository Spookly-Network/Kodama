# #92 [Feature] Node: report template cache outcomes during prepare

## Summary
During `prepare_instance`, report which template layers were cache hits vs downloaded so the Brain can maintain a cache index.

## Details
The node already checks if a template is present in the local cache and downloads it from S3 if not (or if checksum differs). This feature emits a small report back to the Brain that includes only the templates involved in the instance being prepared.

## Scope / Requirements
- Extend the prepare flow to track per-template outcome:
  - HIT (found in cache and checksum matches)
  - DOWNLOADED (fetched from S3 and cached)
  - CHECKSUM_MISMATCH (found but checksum differs, triggers re-download)
- Send a `TemplateCacheReport` to Brain once outcomes are known (end of prepare is fine).
- Make reporting best-effort:
  - if report fails, do not fail the whole prepare; log and continue.
- Respect dev-mode behavior:
  - if dev-mode forces refetch, report outcomes accordingly (likely DOWNLOADED).

## Acceptance Criteria
- Node emits a report for every prepare that includes all template layers used.
- A failed report does not break instance preparation.
- Logs contain a single concise line per report (counts by status).

## Notes / References
- Parent epic: #89
- Depends on Brain endpoint (#91) only for end-to-end usefulness (node can still implement the report payload first).
