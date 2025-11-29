# Brain

The Brain is the authoritative source of truth for everything.

---

## Responsibilities

- Storing metadata
- Scheduling nodes
- Tracking instance lifecycle
- Dispatching commands to nodes
- Secure API for Webpanel and Node Agents

---

## Scheduling Algorithm (v1)

1. Filter ONLINE nodes
2. Filter by region/tags (if provided)
3. Filter by devModeAllowed
4. Filter by slot availability
5. Sort by usedSlots ascending
6. Pick the first

---

## State Machine

See `/docs/INSTANCE-LIFECYCLE.md`.

---

## Data Model

Entities:
- Template
- TemplateVersion
- Node
- Instance
- InstanceEvent
- InstanceTemplateLayer
- User
- Role

All persisted via MySQL + Flyway.
