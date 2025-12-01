## 1. Scope of the Brain

The Brain is the **single source of truth** for everything:

* Knows all **templates** (but not the file contents, only metadata + S3 keys).
* Knows all **nodes** and their status (online, capacity, dev-mode, etc.).
* Knows all **instances** (game server instances) and their full lifecycle.
* Talks to:

    * **Nodes**: to prepare, start, stop, destroy instances.
    * **Webpanel / API clients**: CRUD for templates, nodes, instances, plus actions (start/stop/etc.).
* Handles **scheduling**, auth, auditing, and state transitions.

No template merging, no Docker calls, no file system work on the Brain. That’s Node territory.

---

## 2. High-level architecture inside the Brain

Think classic layered Spring Boot app:

```text
brain/
  src/main/java/...
    config/
    controller/        <- REST APIs (webpanel + node)
    dto/               <- API models
    service/           <- business logic
    domain/            <- entities, value objects, enums
    repository/        <- Spring Data interfaces
    security/          <- auth, roles, tokens, mTLS config
    scheduling/        <- node selection, capacity logic
    events/            <- domain events, state transitions
    util/
```

### 2.1 Main internal components

1. **Template Management**

    * CRUD for templates and versions.
    * Stores ID, version, checksum, S3 key, type, tags.

2. **Node Management**

    * Node registration, heartbeats, dev-mode flag.
    * Tracks capacity, tags (region, type), status (online/offline).

3. **Instance Management**

    * Lifecycle state machine (REQUESTED → PREPARING → STARTING → RUNNING → …).
    * Keeps template layers for each instance.
    * Controls transitions via events (prepared, running, failed, stopped).

4. **Scheduler**

    * Chooses a Node for an instance based on rules (online, capacity, tags, etc.).

5. **Command Dispatcher**

    * Actually sends commands to nodes: `prepare_instance`, `start_instance`, `stop_instance`, `destroy_instance`, `set_dev_mode`, `purge_cache`.

6. **Auth / Security**

    * Webpanel / external clients (likely JWT / OAuth).
    * Node auth (mTLS or node tokens).
    * Role system: Admin / Operator / Viewer.

7. **Audit & Observability**

    * Logs state changes, admin actions.
    * Basic metrics and health endpoints.

---

## 3. Domain model (Brain only)

### 3.1 Templates

Core idea: template = **metadata + S3 key**, never the tarball itself.

Entities:

* `Template`

    * `id` (UUID / long)
    * `name`
    * `description`
    * `type` (ENUM: MASTER, GAMEMODE, MAP, CUSTOM, …)
    * `createdAt`, `createdBy`
* `TemplateVersion`

    * `id`
    * `template` (FK)
    * `version` (simple integer or semantic string)
    * `checksum`
    * `s3Key`
    * `metadataJson` (optional for arbitrary info)
    * `createdAt`

A **template layer** in an instance links to one specific `TemplateVersion` and has an order index:

* `InstanceTemplateLayer`

    * `id`
    * `instance` (FK)
    * `templateVersion` (FK)
    * `orderIndex` (int 0..N, “lower first”).

The “last layer wins” behavior is enforced on Node side, but Brain owns this **ordered list**.

---

### 3.2 Nodes

Entity:

* `Node`

    * `id` (UUID)
    * `name`
    * `status` (ENUM: ONLINE, OFFLINE, UNKNOWN)
    * `devMode` (boolean)
    * `region` (string)
    * `tags` (comma-separated or separate table)
    * `capacitySlots` (how many instances it can run)
    * `usedSlots` (current usage, updated on heartbeat)
    * `lastHeartbeatAt`
    * `nodeVersion` (agent version string, set on registration / upgrade)
    * `authId` or `authTokenId` (for node authentication linkage)

Plus a `NodeHeartbeat` log table if you want history; or just store last values on Node.

---

### 3.3 Instances

Entity:

* `Instance`

    * `id` (UUID)
    * `name`
    * `displayName`
    * `state` (ENUM: REQUESTED, PREPARING, STARTING, RUNNING, STOPPING, DESTROYED, FAILED)
    * `requestedByUserId`
    * `node` (FK, nullable when not yet scheduled)
    * `portsJson` (or separate table if many ports)
    * `variablesJson` (key/value map: INSTANCE_ID, SERVER_NAME, SEED, etc.)
    * `createdAt`
    * `updatedAt`
    * `startedAt`
    * `stoppedAt`
    * `failureReason` (text)

`InstanceTemplateLayer` links to this.

For state history: `InstanceEvent`

* `InstanceEvent`

    * `id`
    * `instance` (FK)
    * `timestamp`
    * `type` (ENUM: REQUESTED, SCHEDULED, PREPARE_SENT, PREPARED, START_SENT, RUNNING, STOP_SENT, STOPPED, DESTROY_SENT, DESTROYED, FAILED)
    * `payloadJson` (for extra info, exit code, error message, etc.)

---

### 3.4 Auth / Users / Roles

Even if the full auth system comes later, define core entities now:

* `User`

    * `id`
    * `username`
    * `displayName`
    * `email`
    * `authProvider` / `externalId` (if OIDC later)
* `Role` (ADMIN, OPERATOR, VIEWER…)
* `UserRole` (join table)

And optionally `ApiToken` or `ServiceAccount` for automations.

---

## 4. Persistence layout (MySQL)

Rough table overview (no full DDL, just structure):

* `templates`
* `template_versions`
* `nodes`
* `instances`
* `instance_template_layers`
* `instance_events`
* `users`
* `roles`
* `user_roles`
* `api_tokens` (optional)
* `node_auth_tokens` (optional)

Use Flyway or Liquibase for migrations, as per agent guide.

---

## 5. External interfaces

Two big groups:

1. **Node API** (Brain <-> Node)
2. **Admin / Webpanel API** (Brain <-> Panel or other clients)

### 5.1 Node API

Goal: small, predictable, no unnecessary complexity. All actions initiated by Brain.

**Auth:**

* Either mTLS (client certificate per node)
* Or a Bearer token per node.
  Either way, node identity must be clear.

Endpoints (example paths, not final):

* `POST /api/nodes/register`

    * Body: node metadata (name, tags, capacity, version).
    * Response: node ID and config (e.g. heartbeat interval).

* `POST /api/nodes/{nodeId}/heartbeat`

    * Body: current slots usage, status, optional metrics.
    * Response: maybe commands in response or just 200 OK.
    * Node version/devMode changes are sent via registration/update, not in heartbeat.

* `POST /api/nodes/{nodeId}/instances/{instanceId}/prepared`

* `POST /api/nodes/{nodeId}/instances/{instanceId}/running`

* `POST /api/nodes/{nodeId}/instances/{instanceId}/stopped`

* `POST /api/nodes/{nodeId}/instances/{instanceId}/failed`

    * Node reports state transitions upwards.

Brain → Node commands:

Here you can either:

* Use **push-style** (Brain calls Node’s HTTP endpoint), or
* Use **pull-style** (Node polls `/commands` on heartbeat).

For a simple version: push-style HTTP calls from Brain to Node (Brain stores Node’s base URL when registering).

Commands from Brain (conceptual):

* `POST //node/api/internal/instances/{instanceId}/prepare`

    * Body: template layers + variables.
* `POST .../instances/{instanceId}/start`
* `POST .../instances/{instanceId}/stop`
* `POST .../instances/{instanceId}/destroy`
* `POST .../node/dev-mode`
* `POST .../node/cache/purge`

Exact path details later, but the Brain has a **CommandDispatcherService** that knows:

* Node base URL
* Node auth credentials
* Standard timeouts and retry policy

---

### 5.2 Webpanel / Admin API

All HTTP / JSON, REST-ish, fairly straightforward.

Groups:

1. **Templates**

    * `GET /api/templates`
    * `POST /api/templates`
    * `GET /api/templates/{id}`
    * `POST /api/templates/{id}/versions`
    * `GET /api/templates/{id}/versions`

2. **Nodes**

    * `GET /api/nodes`
    * `GET /api/nodes/{id}`
    * `POST /api/nodes/{id}/set-dev-mode`
    * `POST /api/nodes/{id}/purge-cache`

3. **Instances**

    * `POST /api/instances`

        * Payload: name, templateLayer definitions, variables, optional node constraints (region, tags).
    * `GET /api/instances`
    * `GET /api/instances/{id}`
    * `POST /api/instances/{id}/start`
    * `POST /api/instances/{id}/stop`
    * `DELETE /api/instances/{id}` (destroy)
    * `GET /api/instances/{id}/events`

4. **Auth / Users**

    * `POST /api/auth/login` (if not using external provider)
    * `GET /api/me`
    * `GET /api/users`, etc.

All of this respects roles:

* `ADMIN`: full access
* `OPERATOR`: can operate nodes/instances, not manage users
* `VIEWER`: read-only

As in the spec.

---

## 6. Scheduling & capacity model

Keep the first version simple and deterministic.

### 6.1 Node selection algorithm (v1)

Given a `CreateInstanceRequest`:

1. Filter nodes:

    * `status == ONLINE`
    * `devMode` allowed or not (depending on a flag in the request)
    * `region` matches requested region if provided
    * node’s tags include requested tags if any

2. Filter by capacity:

    * `usedSlots < capacitySlots`

3. Sort candidates:

    * by `usedSlots` ascending (spread load)
    * tie-breaker: `id` or `name` to stay deterministic

4. Pick first.

If no node is available, instance stays in `REQUESTED` and Brain emits an event. Maybe optional retry later (background scheduler or cron job).

### 6.2 Capacity model

For now, treat `capacitySlots` as “max concurrent instances” per node.
Later you can switch to a more detailed model (RAM/CPU-based) without changing the external API.

---

## 7. State handling & event flow

### 7.1 Instance state machine (Brain’s view)

From spec:

```text
REQUESTED -> PREPARING -> STARTING -> RUNNING -> STOPPING -> DESTROYED
      \         \           \           \
       \         \           \           -> FAILED
        \         \
         ----------> FAILED
```

Transitions driven by Brain actions + Node callbacks:

* `create_instance` → `REQUESTED`
* Brain schedules → assign node, send `prepare_instance`, set `PREPARING`
* Node -> `prepared` callback → Brain sets `STARTING`, sends `start_instance`
* Node -> `running` callback → Brain sets `RUNNING`
* User sends stop → Brain sets `STOPPING`, calls node
* Node -> `stopped` callback → Brain sets `DESTROYED` (or `STOPPED` if you want an extra state)
* Any error → `FAILED`, with `failureReason`

### 7.2 Events inside Brain

Better to use a simple internal event system:

* `InstanceCreatedEvent`
* `InstanceNodeAssignedEvent`
* `InstancePreparedEvent`
* `InstanceRunningEvent`
* `InstanceFailedEvent`, etc.

These:

* Update `Instance` table
* Append to `InstanceEvent` log
* Trigger optional side effects (webhooks, metrics)

Implementation can be Spring `ApplicationEventPublisher` or just service calls; keep it clear and testable.

---

## 8. Error handling & retries

Key points:

* **Node unreachable** when sending a command:

    * Mark instance as `FAILED` or keep it in current state and mark node as `OFFLINE`, depending on severity.
    * Log detailed error and probably emit an `InstanceFailedEvent` with “node unreachable”.

* **Timeout** waiting for Node callbacks:

    * Optionally, mark node as “stale” if no heartbeat for X seconds.
    * You can run a periodic job that checks for instances stuck in `PREPARING` / `STARTING` too long and marks them `FAILED`.

* **Database / S3 metadata errors**:

    * Fail fast and return proper HTTP errors to clients.
    * No S3 access from Brain, so only metadata-level mistakes here (e.g. wrong S3 key string).

* **Idempotency**:

    * Use unique `instanceId` and treat Node callbacks as idempotent:

        * If `running` arrives twice, ignore second.
    * For create: optional idempotency key in API can be added later.

---

## 9. Configuration & environment

All config via `application.yml` / env vars, no hard-coded values.

Important config groups:

* `database` (MySQL URL, user, pass)
* `security`

    * JWT secret / key or OIDC issuer
    * Node auth config (accepted CA, TLS settings)
* `node-communication`

    * timeouts (connect, read)
    * default heartbeat timeout threshold
* `metrics/logging`

    * log level
    * tracing settings

Profiles:

* `dev`: H2/Testcontainers, no TLS, relaxed security
* `prod`: MySQL, full TLS, strict auth

---

## 10. Control-plane project structure (files / packages)

Something like:

```text
brain/
  src/main/java/net/spookly/kodoma/brain/
    BrainApplication.java

    config/
      SecurityConfig.java
      WebConfig.java
      NodeClientConfig.java

    security/
      JwtAuthFilter.java
      NodeAuthFilter.java
      Role.java
      UserPrincipal.java

    domain/
      template/
        Template.java
        TemplateVersion.java
      node/
        Node.java
        NodeStatus.java
      instance/
        Instance.java
        InstanceState.java
        InstanceEvent.java
        InstanceTemplateLayer.java
      user/
        User.java
        RoleEntity.java
        ApiToken.java

    repository/
      TemplateRepository.java
      TemplateVersionRepository.java
      NodeRepository.java
      InstanceRepository.java
      InstanceEventRepository.java
      InstanceTemplateLayerRepository.java
      UserRepository.java
      RoleRepository.java

    service/
      TemplateService.java
      NodeService.java
      InstanceService.java
      SchedulingService.java
      CommandDispatcherService.java
      UserService.java

    controller/
      TemplateController.java
      NodeController.java
      InstanceController.java
      AuthController.java
      NodeCallbackController.java   // for prepared/running/failed callbacks

    events/
      InstanceEvents.java
      NodeEvents.java

    dto/
      TemplateDto.java
      InstanceDto.java
      NodeDto.java
      CreateInstanceRequest.java
      NodeRegistrationRequest.java
      HeartbeatRequest.java
```

---

## 11. Brain-only roadmap

Focus just on the control-plane part of the big roadmap.

### Phase 1 – Skeleton & core domain

* Set up Spring Boot project (`brain` module).
* Create base packages and empty entities:

    * `Template`, `TemplateVersion`, `Node`, `Instance`, `InstanceTemplateLayer`, `InstanceEvent`.
* Add Flyway/Liquibase with first migrations.
* Implement `TemplateService` + `TemplateController` (simple CRUD).
* Implement `NodeController` for registration + heartbeat.
* Implement basic `InstanceService`:

    * `createInstance` (without real scheduling yet, maybe just manual node assignment).

### Phase 2 – Scheduling & commands

* Implement `SchedulingService` with simple slot-based scheduler.
* Implement `CommandDispatcherService` with HTTP client and basic error handling.
* Integrate:

    * `createInstance` → schedule node → call `prepare_instance` → set state `PREPARING`.
* Add `NodeCallbackController` for `prepared/running/stopped/failed`.
* Wire full instance state machine.

### Phase 3 – Security & roles

* Add basic auth (JWT or simple login) and roles: ADMIN / OPERATOR / VIEWER.
* Protect endpoints according to roles.
* Implement simple user/role management for now (later you can plug in OIDC).

### Phase 4 – Stabilization & observability

* Add proper logging and correlation IDs for commands.
* Implement metrics: number of instances per state, node statuses, failed commands count.
* Background jobs:

    * detect stale instances stuck in PREPARING/STARTING
    * check node heartbeats and mark nodes OFFLINE if needed.

### Phase 5 – Quality of life

* Filters in APIs (by node, by state, by template).
* Webhooks or notifications for important events (instance failed, node offline).
* Optional idempotency for instance creation.
