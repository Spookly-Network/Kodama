# #138 [Task] Brain: migrate legacy `instance_template_layers` to assignment tables

## Summary
Add Flyway migration and backfill logic to move existing instance layer data into the new assignment-table schema safely.

## Details
Existing rows in `instance_template_layers` must be preserved as equivalent direct instance assignments with deterministic ordering semantics intact.

## Scope / Requirements
- Create migrations for:
  - `instance_template_assignments`
  - `group_template_assignments`
  - `instance_group_memberships`
  - `instance_groups`
- Backfill `instance_template_layers` into `instance_template_assignments`.
- Validate migrated rows for count and ordering equivalence.
- Document migration behavior and compatibility expectations.

## Acceptance Criteria
- Migration runs cleanly on MySQL.
- Existing instances retain equivalent effective template output after migration.
- No data loss for order/priority semantics.
- Roll-forward migration path is documented.

## Notes / References
- Parent epic: #131
- Blocked by #134.
