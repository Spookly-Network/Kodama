# Panel Nodes

## Purpose
Provide operators with a single view of node fleet health, capacity usage, and editable metadata from the web panel.

## What changed
- The Nodes page now surfaces fleet totals (node counts, slots used/open, utilization) and a richer node table.
- Operators can edit node metadata via an Edit dialog.

## How to use / impact
- Open `Nodes` in the panel to view status, heartbeat timing, capacity usage, version, and tags.
- Use the actions menu on a node row and select `Edit node` to update:
  - region
  - capacity slots
  - node version
  - base URL
  - tags
  - dev mode
- Node name and status are read-only in the edit dialog.
- Editing requires an admin or operator role.

## Edge cases / risks
- Capacity slots cannot be set below the current used slots; the API rejects invalid updates.
- If a base URL is cleared, the control plane will no longer have a URL to reach the node.

## Links
- API contract: `/api/nodes/{nodeId}` (Update node metadata)
- UI: `webpanel/app/pages/nodes/index.vue`
