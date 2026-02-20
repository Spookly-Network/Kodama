# #157 [Feature] Brain: scheduling uses slotsRequired

## Summary
Use slotsRequired when selecting nodes and validating capacity.

## Details
Instances reserve multiple slots based on blueprint or override. Scheduling must account for this when choosing nodes.

## Scope / Requirements
- Store effective slotsRequired on instance (default 1).
- Update scheduling eligibility to require:
  - usedSlots + slotsRequired <= capacitySlots
- Keep existing filters (status, region, tags, devModeAllowed).
- Update conflict message when no nodes can satisfy slotsRequired.
- Ensure slotsRequired is included in InstanceDto for visibility.

## Acceptance Criteria
- Instances with slotsRequired > 1 are rejected when capacity is insufficient.
- Scheduling behavior remains deterministic with existing tie-breakers.
- Tests cover slotsRequired behavior and default value.
