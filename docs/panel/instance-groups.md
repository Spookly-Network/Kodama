# Instance Groups View

## Purpose
Provide an operator-facing view to manage instance groups and their shared template assignments.

## What changed
- Added the Instance Groups view in the web panel.
- The view loads groups from the Brain API and displays assignment counts.
- Added create-group and template-assignment flows that call the group endpoints.

## How to use / impact
- Navigate to `/instances/groups` to see all groups.
- Use **New group** to create an instance group (`POST /api/instance-groups`).
- Select a group to view metadata and add template assignments (`POST /api/instance-groups/{groupId}/template-assignments`).
- Remove assignments with the **Remove** action (`DELETE /api/instance-groups/{groupId}/template-assignments/{assignmentId}`).
- Summary cards show total groups, groups with templates, assignment count, and latest update.

## Edge cases / risks
- If group assignment fetches fail, the page shows a warning and leaves missing groups empty.
- If `/api/templates` is unavailable, template suggestions are disabled but manual IDs still work.

## Links
- `webpanel/app/pages/instances/groups.vue`
- `contracts/openapi.yml`
