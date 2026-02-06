# Kodama – Brain, Node Agent & Webpanel
[![License](https://img.shields.io/badge/license-Apache%20Licence%202.0-green)](LICENSE)
![GitHub Release](https://img.shields.io/github/v/release/Spookly-Network/Kodama)
[![Discord](https://img.shields.io/discord/900708000900194314?label=Discord)](https://discord.gg/9bpxXyszCb)

This repository contains the complete orchestration platform for managing game servers across multiple nodes.  
The system is built around a clear three-layer architecture:

1. **Brain / Control Plane**  
   Central service responsible for templates, nodes, instances, scheduling and lifecycle management.

2. **Node Agent**  
   Lightweight runtime component installed on each host.  
   Handles template merging, variable replacement, caching and starting game servers (via Docker or native binaries).

3. **Webpanel**  
   Web interface for managing templates, nodes and server instances.

---

## Project Goals

- **Scalable by design**  
  Adding new nodes should be simple, regardless of where they run.

- **Clean separation of responsibilities**  
  The Brain makes decisions.  
  Nodes execute the work.  
  The panel provides visibility.  
  Nothing overlaps.

- **Flexible template system**  
  Unlimited layers per instance, merged in a deterministic order.  
  Useful for:
  - Base server setups  
  - Game modes  
  - Maps  
  - Add-ons or overrides  

- **Predictable deployments**  
  Every server instance is built from known template versions and checksums.

- **Modular and extendable**  
  Each component stays independent but works together as part of the full platform.

---

## Architecture Overview

### Brain / Control Plane
Responsibilities:
- Template metadata (S3 keys, checksums, versions)
- Node registration and heartbeats
- Scheduling & capacity decisions
- Instance lifecycle:
  - Requested  
  - Preparing  
  - Starting  
  - Running  
  - Stopping / Destroyed  
- Dispatching commands to nodes
- Authentication & role management
- All persistent state (MySQL + Flyway)

The Brain exposes a REST API used by both the Webpanel and the Node Agents.

---

### Node Agent
Responsibilities:
- Fetch template layers from S3 (with a local cache)
- Merge templates in the correct order (`last layer wins`)
- Replace variables
- Start the final game server (Docker or native)
- Report state changes back to the Brain:
  - prepared  
  - running  
  - stopped  
  - failed  

The Node Agent never stores long-term state.  
Everything important stays inside the Brain.

---

### Webpanel
- Uses the Brain’s REST API
- Shows:
  - Templates & versions  
  - Nodes & status  
  - Instances & lifecycle state  
- No direct communication with Nodes

---

## Repository Structure

```

/brain              → Control Plane (Spring Boot)
/node               → Node Agent (Java)
/panel              → Webpanel (Nuxt)
/docs               → Architecture documents, diagrams, flow specs
.github             → Issue Types, PR templates, workflows

```

---

## Workflow & Development

The workflow is built around GitHub’s modern features:

### Issue Types
- Epic  
- Feature  
- Architecture  
- Research  
- Task  
- Docs  

Every piece of work starts as one of these types.

### Branches
```

feature/<name>
arch/<name>
fix/<name>
docs/<name>
research/<name>

```

### Pull Requests
- Every change goes through a PR  
- Reference issues with `Closes #123`  
- Small, focused, reviewed  
- Merging a PR closes the issue automatically  

### Milestones
Used for real deliverables:
- Control Plane MVP  
- Node Agent MVP  
- Template System v1  
- Full Orchestration Demo  

### Project Board (Kanban)
Backlog → Ready → In Progress → Review → Done  
Epics stay in Backlog or in a separate column.

More details in **CONTRIBUTING.md**.

---

## Template System Overview

- All template layers live in S3 as `.tar` archives  
- The Brain stores **only metadata**:
  - ID  
  - Version  
  - Checksum  
  - S3 key  
- Instances combine multiple layers in a defined order
- The Node merges them and starts the server

This design allows configuring servers for:
- Any game  
- Any map  
- Any game mode  
- Custom variations  
- Overrides without touching base templates  

---

## Security

- Admin authentication via JWT or provider of your choice  
- Node authentication separate (token or mTLS)  
- Role system:
  - Admin  
  - Operator  
  - Viewer  

Security logic is centralized inside the Brain.

---

## Project Status

Active development.  
The roadmap is available via milestones and epic issues.

---

## License

Kodoma is licensed under the Apache License 2.0. See the LICENSE file for more information.
