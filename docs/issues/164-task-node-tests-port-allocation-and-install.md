# #164 [Task] Node tests: port allocation and install/start flow

## Summary
Add tests for port allocation, reservation, and install/start behavior.

## Details
We need confidence that the node allocates ports deterministically and runs install scripts once.

## Scope / Requirements
- Port allocator tests:
  - deterministic allocation with step
  - conflict avoidance using existing registry entries
  - invalid ranges return clear errors
- Install/start tests:
  - install script runs once and sets sentinel
  - start command uses registry values
- Keep tests fast; use unit tests and lightweight fakes where possible.

## Acceptance Criteria
- Tests cover allocation, reservation, and install/start behavior.
- Tests are deterministic and pass in CI.
