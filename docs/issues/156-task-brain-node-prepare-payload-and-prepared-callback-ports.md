# #156 [Task] Brain: node prepare payload and prepared callback ports

## Summary
Send resolved blueprint runtime fields to the node during prepare and persist allocated ports reported in the prepared callback.

## Details
The node needs container image, install script, start command, slotsRequired, and port definitions to prepare correctly. The Brain must accept allocated ports from the node and store them in portsJson.

## Scope / Requirements
- Extend NodePrepareInstanceRequest with:
  - containerImage
  - installScript
  - startCommand (exec form array)
  - slotsRequired
  - portDefinitions list
- Update command dispatcher to populate these fields from resolved instance data.
- Update prepared callback endpoint to accept body with portsJson.
- Persist portsJson on the instance when the prepared callback is received.
- Preserve existing behavior when portsJson is absent.

## Acceptance Criteria
- Node prepare payload contains resolved runtime fields.
- Prepared callback can include portsJson and it is persisted.
- Existing callbacks without body still succeed.
