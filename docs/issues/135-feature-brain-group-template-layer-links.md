# #135 [Feature] Brain: group template assignments

## Summary
Add APIs and persistence logic to attach/detach template assignments to groups.

## Details
Groups contribute template assignments to instances via membership. This issue adds group assignment linkage only, not final merge execution behavior.

## Scope / Requirements
- Add `group_template_assignments` with fields:
  - `group_id`
  - `template_id` (required)
  - `template_version_id` (optional)
  - `priority`
- Endpoints:
  - add assignment to group
  - remove assignment from group
  - list assignments for group
- Validate duplicate assignment rules and bad references.

## Acceptance Criteria
- Group assignments can be added, listed, and removed.
- `templateId` is mandatory in requests.
- Invalid group/template/version IDs return clear errors.
- Tests cover assignment lifecycle.

## Notes / References
- Parent epic: #131
- Blocked by #133 and #134.
