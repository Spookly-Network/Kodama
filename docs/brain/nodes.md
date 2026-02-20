# Nodes

## Purpose
Describe the Brain endpoints and rules that manage node metadata used by scheduling and monitoring.

## What changed
- Added an operator endpoint to update node metadata without node-side registration.

## How to use / impact
- `PUT /api/nodes/{nodeId}` updates operator-controlled metadata:
  - region
  - capacity slots
  - node version
  - base URL
  - tags
  - dev mode
- The endpoint returns the updated node snapshot.
- Access requires `ROLE_ADMIN` or `ROLE_OPERATOR`.

## Edge cases / risks
- The update request is rejected if `capacitySlots` is lower than the node’s current `usedSlots`.
- Node status is still controlled by heartbeats and the heartbeat monitor.

## Links
- Contract: `/contracts/openapi.yml`
- Service: `backend/brain/src/main/java/net/spookly/kodama/brain/service/NodeService.java`
- Controller: `backend/brain/src/main/java/net/spookly/kodama/brain/controller/NodeController.java`
