# Instance Lifecycle

This document describes the lifecycle of a game server instance and how the Brain and Nodes communicate.

---

## 1. States

- **REQUESTED**
- **PREPARING**
- **STARTING**
- **RUNNING**
- **STOPPING**
- **DESTROYED**
- **FAILED**

---

## 2. Lifecycle Flow

1. **Instance created → REQUESTED**
2. Brain selects a node
3. Brain sends prepare command → PREPARING
4. Node downloads templates & merges
5. Node reports prepared → STARTING
6. Brain sends start command
7. Node reports running → RUNNING
8. User stops instance → STOPPING
9. Node shuts down server
10. Node reports destroyed → DESTROYED

---

## 3. Failure Scenarios

- Template merge fails → FAILED
- Node unreachable → FAILED
- Docker error → FAILED
- Missing callbacks → Brain marks as FAILED

---

## 4. Events

Each state change generates an InstanceEvent:

- REQUEST_RECEIVED
- NODE_SCHEDULED
- PREPARE_DISPATCHED
- PREPARE_COMPLETED
- START_DISPATCHED
- START_COMPLETED
- STOP_DISPATCHED
- STOP_COMPLETED / DESTROY_COMPLETED
- FAILURE_REPORTED
