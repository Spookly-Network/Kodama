# Instances View

## Purpose
Provide an operator-facing list of orchestrated instances with status, node assignment, and template layer coverage.

## What changed
- Added the Instances list view in the web panel.
- The view loads instance data from the Brain API and surfaces lifecycle status, node assignment, and layer counts.
- Added a Create instance dialog that submits `POST /api/instances`.

## How to use / impact
- Navigate to `/instances` to see the current instance fleet.
- Summary cards show total, running, starting, and failed counts.
- The table lists instance metadata, template layer breakdown, and lifecycle timestamps.
- Use the Refresh button to re-fetch data from the Brain API.
- Use the Create instance button to submit `name`, `displayName`, and at least one template layer. Optional fields include `nodeId`, `region`, `tags`, and `devModeAllowed`.

## Edge cases / risks
- If `/api/instances` is unavailable, the page shows an error with a retry option.
- Node names are shown as IDs because the instance payload only includes `nodeId`.
- If `/api/templates` fails to load, template suggestions are unavailable but you can still paste template IDs manually.

## Links
- `webpanel/app/pages/instances/index.vue`
- `webpanel/app/components/app/instances/columns.ts`
- `contracts/openapi.yml`
