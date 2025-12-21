# Scheduling Service

The scheduling service selects a node for a requested instance using a simple slot-based algorithm.

Selection rules:
- Only consider nodes with `status=ONLINE`.
- If a region is provided, only consider nodes in that region.
- If tags are provided, the node must contain all requested tags.
- If `devModeAllowed` is provided, the node's `devMode` must match it.
- Nodes must have `usedSlots < capacitySlots`.
- Choose the node with the lowest `usedSlots`, then by name, then by id.

Tag format:
- Tags are comma-separated strings.
- Whitespace is trimmed.
- Matching is case-insensitive.

Implementation:
- `brain/src/main/java/net/spookly/kodama/brain/service/SchedulingService.java`
- Returns a `Node` or `null` if no candidate is available.
