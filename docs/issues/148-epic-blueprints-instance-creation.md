# #148 [Epic] Blueprint-driven instance creation

## Summary
Introduce reusable blueprints that define how instances are created and prepared.

## Goal
Instances can be created from a blueprint reference with override support, while nodes allocate ports and run install/start commands based on blueprint data.

## Context
Instance creation currently requires explicit data per request. Blueprints provide editable defaults (templates, ports, runtime config) without versioning.

## Scope
- Brain blueprint CRUD with soft delete and core fields.
- Blueprint attachments: template assignments, port definitions, group links.
- Instance creation via blueprint reference with override replacement semantics.
- Node prepare/start updates: port allocation, install script, start command.
- Scheduling and heartbeat updates for slotsRequired usage.
- Contracts, tests, and documentation updates.

## Linked Tasks
- [ ] #149
- [ ] #150
- [ ] #151
- [ ] #152
- [ ] #153
- [ ] #154
- [ ] #155
- [ ] #156
- [ ] #157
- [ ] #158
- [ ] #159
- [ ] #160
- [ ] #161
- [ ] #162
- [ ] #163
- [ ] #164
- [ ] #165

## Notes
- Plan: `docs/plan/blueprints/plan.md`.
