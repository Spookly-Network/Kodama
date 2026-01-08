# Instance State Machine

## Purpose

Centralize and validate instance lifecycle transitions in the Brain.

## What changed

- All lifecycle changes now go through `InstanceStateMachine`.
- Each transition updates `Instance.state` and writes an `InstanceEvent`.

## How to use / impact

Allowed transitions:

- `REQUESTED` → `PREPARING`
- `PREPARING` → `STARTING`
- `STARTING` → `RUNNING`
- `RUNNING` → `STOPPING`
- `STOPPING` → `DESTROYED`
- `STOPPING` → `STOPPED`
- `STOPPED` → `DESTROYED`
- `STOPPED` → `STARTING`
- Any non-terminal state → `FAILED`

Node callbacks map to the state machine:

- `/prepared` transitions to `STARTING` and logs `PREPARE_COMPLETED`.
- `/running` transitions to `RUNNING` and logs `START_COMPLETED`.
- `/stopped` transitions to `STOPPED` and logs `STOP_COMPLETED`.
- `/destroyed` transitions to `DESTROYED` and logs `DESTROY_COMPLETED`.
- `/failed` transitions to `FAILED` and logs `FAILURE_REPORTED`.

Invalid transitions throw `InvalidInstanceStateTransitionException`.

## Edge cases / risks

- Terminal states (`FAILED`, `DESTROYED`) do not transition further.
- Instances can transition from `STOPPED` to `STARTING` to restart without re-preparing.
- Ensure a `STOPPING` transition is recorded before a node reports a stop/destroy callback.

## Links

- `backend/brain/src/main/java/net/spookly/kodama/brain/service/InstanceStateMachine.java`
- `docs/brain/node-callback-endpoints.md`
