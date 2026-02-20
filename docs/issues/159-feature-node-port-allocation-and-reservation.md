# #159 [Feature] Node: port allocation and reservation on prepare

## Summary
Allocate host ports from blueprint ranges during prepare and reserve them until destroy.

## Details
Ports are allocated per node from configured ranges and persisted to avoid conflicts across restarts.

## Scope / Requirements
- Implement port allocation service:
  - Input: list of port definitions (name, protocol, containerPort, hostRange).
  - Output: allocated hostPort for each definition.
  - Strategy: pick lowest available port per range using step; deterministic.
- Reserve ports by persisting allocation in registry; include in portsJson.
- Avoid conflicts by scanning existing registry entries on allocation.
- Update prepare flow to:
  - allocate ports
  - write portsJson to registry
  - inject PORT/PORT_<NAME> variables into the variable map
  - include portsJson in prepared callback payload
- Allocation errors fail prepare and trigger /failed callback.

## Acceptance Criteria
- Ports are allocated deterministically and reserved until destroy.
- Prepared callback sends portsJson with host and container ports.
- Conflicting allocations are prevented even after node restart.
