# #163 [Task] Brain tests: blueprint resolution and overrides

## Summary
Add unit tests for blueprint-based instance creation and override replacement rules.

## Details
We need deterministic tests to ensure blueprint defaults and overrides resolve correctly.

## Scope / Requirements
- Tests for:
  - blueprint defaults applied when no overrides are provided
  - overrides replace blueprint values (templates, ports, groups, variables, runtime fields)
  - deleted blueprint rejects create
  - slotsRequired defaults to 1 when missing
- Use existing service tests or create new focused tests.

## Acceptance Criteria
- Tests pass and cover core resolution paths.
- No test relies on external services or timing.
