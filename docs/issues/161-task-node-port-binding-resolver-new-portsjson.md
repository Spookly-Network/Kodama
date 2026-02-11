# #161 [Task] Node: support new portsJson array format in port bindings

## Summary
Parse the new portsJson array format (hostPort + containerPort + protocol) while keeping legacy map support.

## Details
Node start currently expects portsJson as a map of name to containerPort. With allocated host ports we need an array form.

## Scope / Requirements
- Update InstancePortBindingsResolver:
  - If portsJson is a JSON array, parse entries with name, protocol, containerPort, hostPort.
  - Use hostPort directly; ignore variable-based host ports when array is present.
  - If portsJson is a JSON object, keep legacy behavior.
- Validate port numbers and protocol values.
- Keep deterministic ordering.

## Acceptance Criteria
- Array format works for multi-port mappings.
- Legacy object format still works without changes.
- Invalid formats return a clear error.
