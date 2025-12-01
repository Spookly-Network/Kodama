# Node Agent Flow

This document describes how the Node Agent handles instructions from the Brain.

---

## 1. Registration

On startup:
1. Node sends a registration request
2. Brain returns Node ID + config (intervals, etc.)

---

## 2. Heartbeats

Node sends heartbeat:
- usedSlots
- status
- metrics (optional)
- cache size (optional)

Brain updates node status → ONLINE.

---

## 3. Handling Prepare Command

Steps:
1. Receive template layers + variables
2. Validate checksums
3. Download missing templates
4. Extract into workspace
5. Merge layers
6. Replace variables
7. Send PREPARED callback

---

## 4. Handling Start Command

1. Start Docker container (or native server)
2. Monitor logs
3. On success → RUNNING callback

---

## 5. Handling Stop / Destroy

1. Stop container
2. Clean workspace
3. Update cache if needed
4. Send STOPPED or DESTROYED callback
