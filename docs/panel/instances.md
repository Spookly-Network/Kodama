# Instances View

## Purpose
Provide an operator-facing list of orchestrated instances with status, node assignment, and template layer coverage.

## What changed
- Added the Instances list view in the web panel.
- The view loads instance data from the Brain API and surfaces lifecycle status, node assignment, and layer counts.

## How to use / impact
- Navigate to `/instances` to see the current instance fleet.
- Summary cards show total, running, starting, and failed counts.
- The table lists instance metadata, template layer breakdown, and lifecycle timestamps.
- Use the Refresh button to re-fetch data from the Brain API.

## Edge cases / risks
- If `/api/instances` is unavailable, the page shows an error with a retry option.
- Node names are shown as IDs because the instance payload only includes `nodeId`.

## Links
- `webpanel/app/pages/instances/index.vue`
- `webpanel/app/components/app/instances/columns.ts`
- `contracts/openapi.yml`
