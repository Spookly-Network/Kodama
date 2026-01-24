# Brain Metrics and Health

## Purpose
- Expose minimal operational health and metrics for Brain.

## What changed
- Actuator endpoints expose health details (including DB) and metrics.
- Added custom gauges for instances per state and nodes per status.

## How to use / impact
- `GET /actuator/health` returns overall status and component details, including database connectivity.
- `GET /actuator/metrics` lists metric names.
- `GET /actuator/metrics/kodama.instances.state?tag=state:RUNNING` returns instance counts by state.
- `GET /actuator/metrics/kodama.nodes.status?tag=status:ONLINE` returns node counts by status.
- Configuration can be overridden via Spring environment properties:
  - `management.endpoints.web.exposure.include`
  - `management.endpoint.health.show-details`
  - `management.endpoint.health.show-components`

## Edge cases / risks
- If the database is unavailable, the health component reports DOWN and metric gauges return `NaN` with warnings in logs.

## Links
- `backend/brain/src/main/java/net/spookly/kodama/brain/metrics/BrainMetricsBinder.java`
