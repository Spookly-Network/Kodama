# #131 [Epic] Instance organization with multi-group template assignments

## Summary
Add instance grouping as an optional organizational addon where each instance can belong to multiple groups, while template assignments resolve from both direct instance assignments and assigned groups.

## Goal
Deliver a deterministic, testable, and migration-safe model based on assignment relationships (no shared `template_layers` table), with explicit override semantics and non-unique priorities.

## Context
The current instance template model is directly chained to instances and does not support group contributions cleanly. This epic introduces many-to-many memberships and assignment tables that carry metadata (`priority`, optional `templateVersionId`) directly on the relationship.

## Scope
- Add groups and instance-group memberships.
- Add assignment tables for instance->template and group->template with metadata.
- Implement effective resolution from both sources.
- Enforce validation: `templateId` required, `templateVersionId` optional.
- Add migrations, tests, and documentation/contract updates.

## Linked Tasks
- [ ] #132
- [ ] #133
- [ ] #134
- [ ] #135
- [ ] #136
- [ ] #137
- [ ] #138
- [ ] #139
- [ ] #140

## Notes
- `priority` is intentionally non-unique.
- Allowed payload forms are `templateId` and `templateId + templateVersionId`.
- Group contributions are additive; direct instance assignments win conflicts.
