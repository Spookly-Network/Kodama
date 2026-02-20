# Scheduling Service

The scheduling service selects a node for a requested instance using a simple slot-based algorithm.
When `POST /api/instances` omits `nodeId`, the Brain uses this service to assign a node before persisting the instance.

Slot input:
- `slotsRequired` comes from resolved runtime configuration in `InstanceCreationPreparationService`.
- For blueprint-backed creation, that means `blueprint.slotsRequired` overridden by request `slotsRequired` when provided.
- When neither blueprint nor request sets it, the effective value defaults to `1`.

Selection rules:
- Only consider nodes with `status=ONLINE`.
- If a region is provided, only consider nodes in that region.
- If tags are provided, the node must contain all requested tags.
- If `devModeAllowed` is provided, the node's `devMode` must match it.
- Nodes must satisfy `usedSlots + slotsRequired <= capacitySlots`.
- Choose the node with the lowest `usedSlots`, then by name, then by id.

Tag format:
- Tags are comma-separated strings.
- Whitespace is trimmed.
- Matching is case-insensitive.

Implementation:
- `brain/src/main/java/net/spookly/kodama/brain/service/SchedulingService.java`
- Returns a `Node` or `null` if no candidate is available.
- If no node matches filters, instance creation fails with `409 Conflict` and `No eligible nodes found`.
- If nodes match filters but lack capacity for `slotsRequired`, instance creation fails with
  `409 Conflict` and an insufficient capacity message.
- Node `usedSlots` values are driven by node heartbeats, which include currently reserved slot totals from local node registry state.
