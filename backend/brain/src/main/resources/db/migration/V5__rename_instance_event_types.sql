-- Align stored instance event types with the refined enum values (no overlap with instance states).
UPDATE instance_events SET type = 'REQUEST_RECEIVED' WHERE type = 'REQUESTED';
UPDATE instance_events SET type = 'NODE_SCHEDULED' WHERE type = 'SCHEDULED';
UPDATE instance_events SET type = 'PREPARE_DISPATCHED' WHERE type = 'PREPARE_SENT';
UPDATE instance_events SET type = 'PREPARE_COMPLETED' WHERE type = 'PREPARED';
UPDATE instance_events SET type = 'START_DISPATCHED' WHERE type = 'START_SENT';
UPDATE instance_events SET type = 'START_COMPLETED' WHERE type = 'RUNNING';
UPDATE instance_events SET type = 'STOP_DISPATCHED' WHERE type = 'STOP_SENT';
UPDATE instance_events SET type = 'STOP_COMPLETED' WHERE type = 'STOPPED';
UPDATE instance_events SET type = 'DESTROY_DISPATCHED' WHERE type = 'DESTROY_SENT';
UPDATE instance_events SET type = 'DESTROY_COMPLETED' WHERE type = 'DESTROYED';
UPDATE instance_events SET type = 'FAILURE_REPORTED' WHERE type = 'FAILED';
