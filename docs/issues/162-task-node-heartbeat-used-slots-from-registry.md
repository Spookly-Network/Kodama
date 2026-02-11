# #162 [Task] Node: heartbeat usedSlots from registry

## Summary
Report usedSlots based on local registry entries and slotsRequired.

## Details
Slots should count instances in STARTING, RUNNING, and STOPPING. The node must compute usedSlots from registry data.

## Scope / Requirements
- Add slotsRequired to registry entries (from prepare payload).
- Track containerStatus transitions:
  - set status to starting before starting container
  - set status to stopping before stopping container
- Compute usedSlots as sum of slotsRequired for entries with status starting/running/stopping.
- Default slotsRequired to 1 when missing.
- Update HeartbeatScheduler or NodeHeartbeatState to use computed value.

## Acceptance Criteria
- usedSlots reflects active instances with correct slot counts.
- No negative or over-capacity values are reported.
- Tests cover status transitions and default slotsRequired.
