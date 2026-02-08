# #132 [Architecture] Brain: assignment-table model for instance/group templates

## Summary
Define and implement the target architecture using assignment tables between owners and templates, with relationship metadata (`priority`, optional `templateVersionId`).

## Problem
A shared `template_layers` indirection adds complexity without clear value for current requirements. We need multi-owner assignment behavior with straightforward persistence and queries.

## Proposed Solution
- Use owner-specific assignment tables:
  - `instance_template_assignments`
  - `group_template_assignments`
- Columns include:
  - `template_id` (required)
  - `template_version_id` (optional)
  - `priority` (non-unique)
- Add `instance_group_memberships(instance_id, group_id)`.
- Keep deterministic merge logic in service layer.

## Impact Analysis
- Brain domain model and repositories.
- Flyway migrations and backfill from legacy instance template layers.
- Instance/group services and effective assignment resolver.
- Contract and docs for new payload semantics.

## Acceptance Criteria
- Assignment-table architecture is implemented and documented.
- Existing non-group behavior remains stable.
- Validation rules and merge precedence are explicit and tested.
