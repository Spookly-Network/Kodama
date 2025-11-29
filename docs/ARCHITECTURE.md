# Architecture Overview

This document outlines the overall architecture of the Orchestrator platform.  
The system is built on a three-layer structure:

1. **Control Plane (Brain)**
2. **Node Agent**
3. **Webpanel**

Each component is independent but communicates through clear, defined APIs.

---

## 1. Control Plane (Brain)

The Brain coordinates everything:

- Template metadata
- Node registration and status
- Instance lifecycle
- Scheduling logic
- Authentication and roles
- Command dispatch to nodes

### Responsibilities
- Maintain persistent state (MySQL + Flyway)
- Expose REST API for:
    - Webpanel
    - Node callbacks
    - Internal operations
- Decide where instances should run
- Track instance state through events
- Mark nodes offline if heartbeats are missing

### Not responsible for
- Downloading templates
- Merging templates
- Running game servers
- Docker logic
- File system operations

Those belong to the Node.

---

## 2. Node Agent

The Node executes actions passed down from the Brain.

### Responsibilities
- Fetch templates from S3 with local caching
- Merge template layers
- Replace variables
- Start and stop game servers
- Report lifecycle changes back to the Brain
- Maintain local template cache

### Not responsible for
- Scheduling
- Storing metadata
- Complex state handling

Nodes should stay lightweight and stateless.

---

## 3. Webpanel

The Webpanel is a simple UI that interacts only with the Brain.

### Responsibilities
- List templates, nodes, instances
- Trigger actions (create, start, stop, destroy)
- Display status and lifecycle updates
- Manage user roles (if UI integrates)

---

## 4. High-Level Flow

1. User creates instance through Webpanel
2. Brain stores instance in REQUESTED
3. Brain selects a node
4. Brain sends prepare command
5. Node downloads templates, merges, prepares
6. Node reports PREPARED → STARTING → RUNNING
7. Brain updates state and logs events
8. User can stop/destroy instance
9. Node removes resources and reports STOPPED/DESTROYED

---

## 5. Technology Stack

- **Brain:** Java 21, Spring Boot, Flyway, MySQL
- **Node:** Java 21, Docker runtime
- **Webpanel:** Nuxt
- **Storage:** S3-compatible storage
- **Communication:** REST (JSON)

---

## 6. Diagrams

See `/docs/diagrams/` for flowcharts and diagrams.
