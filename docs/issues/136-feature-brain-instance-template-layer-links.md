# #136 [Feature] Brain: direct instance template assignments

## Summary
Add APIs and persistence logic for direct template assignments on instances.

## Details
Direct instance assignments remain supported and must override group-contributed assignments during effective resolution.

## Scope / Requirements
- Add `instance_template_assignments` with fields:
  - `instance_id`
  - `template_id` (required)
  - `template_version_id` (optional)
  - `priority`
- Extend instance create/update flows to accept direct assignments.
- Validate required `templateId`, optional `templateVersionId`, and mismatch conditions.
- Preserve backward compatibility where feasible.

## Acceptance Criteria
- Instance assignments can be created, listed, and removed.
- Existing instance creation path remains stable for non-group usage.
- Assignment operations are covered by tests.

## Notes / References
- Parent epic: #131
- Blocked by #134.
