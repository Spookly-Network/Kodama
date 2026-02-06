# Orchestrator Roadmap

This roadmap outlines major deliverables and milestones for the full platform.

---

## Milestone: Control Plane MVP

### Goals
- Template metadata storage
- Node registration & heartbeat
- Basic scheduling (slot-based)
- Instance lifecycle (requested → running)
- Command dispatch to nodes

### Issues
- Template CRUD
- Node registration & heartbeat
- Instance entity & events
- Scheduling
- State machine
- Node callback endpoints

---

## Milestone: Node Agent MVP

### Goals
- Receive commands from Brain
- Download templates from S3
- Local template cache
- Merge layers
- Replace variables
- Start/stop servers

### Issues
- Template fetcher
- Cache manager
- Layer merge logic
- Variable processor
- Docker starter
- Callback system

---

## Milestone: Template System v1

### Goals
- Unlimited template layers
- Deterministic merge order
- Versioned templates
- Checksums
- S3 storage tooling

---

## Milestone: End-to-End Orchestration Demo

### Goals
- Deploy Brain + Node + Panel
- Create templates
- Start several server instances
- Observe full lifecycle
- Show logs and metrics

---

## Future Ideas

- Resource-based scheduling (CPU/RAM)
- Multi-region support
- Plugin system for templates
- Custom health checks
- Metrics dashboard
