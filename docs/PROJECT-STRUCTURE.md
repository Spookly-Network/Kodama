# Project Structure

This repository uses a monorepo layout.

---

## Root Structure

```md
/brain → Spring Boot Control Plane
/node → Node Agent (Java)
/panel → Webpanel (Nuxt)
/docs → Documentation & diagrams
/.github → GitHub Issue Types, workflows, templates
```

---

## Module Responsibilities

### /brain
- REST API
- Scheduling
- DB storage
- Instance lifecycle
- Auth & roles

### /node
- Template merge
- Cache
- Server runtime handling
- Status callbacks

### /panel
- Interface for admin/operator
- No direct node communication

---

## Conventions

- Branch naming: `feature/*`, `arch/*`, `fix/*`
- Issue Types: Epic, Feature, Architecture, Research, Task, Docs
- PRs: one PR per issue, `Closes #id`
- Milestones define deliverables
