# Node Heartbeat Monitor

## Purpose
- Mark nodes OFFLINE when heartbeats stop arriving within the configured timeout.

## What changed
- Added a scheduled Brain job that checks `lastHeartbeatAt` and flips stale nodes to OFFLINE.

## How to use / impact
- `node.heartbeat-timeout-seconds` (`NODE_HEARTBEAT_TIMEOUT_SECONDS`) controls how long a node can miss heartbeats before being marked OFFLINE.
- `node.heartbeat-monitor-interval-seconds` (`NODE_HEARTBEAT_MONITOR_INTERVAL_SECONDS`) controls how often the job runs.
- The check uses UTC timestamps and compares `lastHeartbeatAt` to `now - timeout`.

## Edge cases / risks
- If the job is delayed (GC pauses, DB outages), nodes may remain ONLINE longer until the next run.
- Heartbeats that resume will update the node status through the heartbeat endpoint.

## Links
- `backend/brain/src/main/java/net/spookly/kodama/brain/service/NodeHeartbeatMonitorService.java`
