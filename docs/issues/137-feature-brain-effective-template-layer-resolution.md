# #137 [Feature] Brain: effective template assignment resolution from instance + groups

## Summary
Resolve effective template versions by merging direct instance assignments and group assignments with deterministic conflict rules.

## Details
Rules:
- Source set = all group assignments + direct instance assignments.
- `priority` sorted ascending, duplicates allowed.
- Resolve `templateId` to latest version when `templateVersionId` is absent.
- On conflict for the same effective template target, direct instance assignment wins over group assignment.
- Same template contributed by multiple groups is allowed and deduplicated deterministically.

## Scope / Requirements
- Implement merge/resolution service.
- Use resolver in runtime path that builds prepare/scheduling layer stacks.
- Define deterministic tie-breakers beyond priority and source precedence.
- Ensure clear behavior for missing referenced versions during resolve.

## Acceptance Criteria
- Effective output is stable across repeated runs.
- Direct instance assignment overrides group assignment on conflict.
- Multi-group duplicates are accepted and resolved predictably.
- Service behavior is covered by unit/integration tests.

## Notes / References
- Parent epic: #131
- Blocked by #135 and #136.
