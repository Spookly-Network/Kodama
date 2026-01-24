# Instance Stale Monitor

## Purpose
- Mark instances as FAILED when they stay in PREPARING or STARTING beyond the configured timeout.

## What changed
- Added a scheduled Brain job that detects stale PREPARING/STARTING instances and fails them with reason `timeout`.

## How to use / impact
- `instance.stale-detection.enabled` (`INSTANCE_STALE_DETECTION_ENABLED`) toggles the job.
- `instance.stale-detection.monitor-interval-seconds` (`INSTANCE_STALE_DETECTION_INTERVAL_SECONDS`) controls how often the job runs.
- `instance.stale-detection.preparing-timeout-seconds` (`INSTANCE_PREPARING_TIMEOUT_SECONDS`) sets the PREPARING timeout.
- `instance.stale-detection.starting-timeout-seconds` (`INSTANCE_STARTING_TIMEOUT_SECONDS`) sets the STARTING timeout.
- The check uses UTC timestamps and compares `updatedAt` to `now - timeout`.

## Edge cases / risks
- If the job is delayed, instances can remain in transient states longer until the next run.
- Instances updated after the cutoff are skipped even if they were in the query result.

## Links
- `backend/brain/src/main/java/net/spookly/kodama/brain/service/InstanceStaleMonitorService.java`
