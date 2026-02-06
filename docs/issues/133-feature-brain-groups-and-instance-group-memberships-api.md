# #133 [Feature] Brain: groups and instance-group memberships API

## Summary
Add group CRUD and many-to-many instance-group membership management.

## Details
Groups are an optional organization layer. Instances can belong to zero, one, or many groups. This issue focuses on membership structure and APIs, independent from effective template assignment resolution.

## Scope / Requirements
- Add persistence for:
  - `instance_groups`
  - `instance_group_memberships(instance_id, group_id)`
- Add Brain endpoints for:
  - create/list/get groups
  - assign/remove group memberships for an instance
  - list groups for an instance
- Validate duplicate memberships and not-found references.
- Keep membership operations idempotent where possible.

## Acceptance Criteria
- An instance can be assigned to multiple groups.
- Duplicate membership assignment is rejected or treated idempotently with a clear contract.
- Removing membership updates state correctly without affecting lifecycle behavior.
- API behavior is covered by tests.

## Notes / References
- Parent epic: #131
- Keep this issue independent from assignment-merge logic.
