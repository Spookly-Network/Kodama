# Agents Guide

This document is written for AI coding assistants and automation agents (Codex, Copilot, internal tools, etc.) that work on this repository.

The goal: generate code and configuration that is stable, predictable, and consistent with the existing architecture and conventions of this project.

## Core Principles

- Do not be clever. Be **predictable**.
- Respect the existing architecture: **Control Plane**, **Node Agent**, **Web Panel**.
- Prefer **clarity over magic**.
- Never introduce breaking changes or big refactors without an explicit developer request in a comment or issue.

---

## Goals for Agents

When editing or creating files in this repo:

- Help implement a **game server orchestrator** for containerized game servers (initially Hytale).
- Keep the code **framework-aligned** with Spring Boot and existing Java conventions.
- Write code that compiles and runs **without extra manual fixes**.
- Preserve **project structure** and naming conventions.

---

## Do / Don't

### Do

- Follow Java and Spring idioms already present in the project.
- Use clear, minimal dependencies.
- Add comments when logic is not obvious at first glance.
- Handle errors explicitly:
  - timeouts
  - connection failures
  - Docker API errors
  - database errors
- Use configuration via **environment variables** or `application.yml` / `application.properties`, not hard-coded values.
- Keep changes **small and focused** (one responsibility per commit/patch).
- Add documentation for new features and changes in the therefore created docs folder or contracts folder.

### Don't

- Do not refactor large areas of code without a clear request in an issue or TODO.
- Do not introduce new frameworks unless absolutely necessary.
- Do not hard-code secrets, tokens, or hostnames.
- Do not add global singletons or static state unless the project already uses that pattern.
- Do not change project style (e.g. switching frameworks, logging libraries, or test libraries) on your own.

---

## Technology Constraints

Agents MUST respect the following stack choices:

- **Language**: Java (Java 21+)
- **Backend Framework**: Spring Boot
- **Database**: MySQL (initial choice, schema should remain portable)
- **Persistence**: Spring Data / JPA and/or JOOQ (if present)
- **Migrations**: Flyway
- **Container Runtime**: Docker (via Docker Engine API or Java client)

---

## Project Structure Expectations

High-level modules:

- `backend/brain/`  
  Spring Boot application that exposes REST APIs and talks to the database.

- `backend/node-agent/`  
  Lightweight Java application that runs on each node and talks to the local Docker Engine and the Control Plane.

- `webpanel/`  
  Frontend (Nuxt) that only communicates with the Control Plane via HTTP.

- `infra/`  
  Docker Compose, scripts, local dev tooling.

Agents should not create new top-level modules without a clear reason.

---

## Naming and Style

- **Classes**: `PascalCase`
- **Methods**: `camelCase`
- **Fields**: `camelCase`
- **Constants / ENV**: `UPPER_SNAKE_CASE`
- **Docker images**: `lowercase-kebab-case`

Use standard Spring patterns:

- Controllers in `...controller`
- Services in `...service`
- Repositories in `...repository`
- DTOs in `...dto`
- Configuration classes in `...config`

---

## Tests

When creating tests:

- Prefer **JUnit 5**.
- Use **Testcontainers** for integration tests that touch Docker or MySQL.
- Do not mock the Docker Engine if you can use Testcontainers to simulate it.
- Keep unit tests fast and focused on core logic (e.g. scheduling decisions).

---

## Quality & Safety

- Code must be **deterministic**. No random behavior without explicit seeding and reason.
- Avoid hidden side effects; prefer pure functions where possible.
- Keep orchestrator logic generic: it should work for “a containerized game server”, not just Hytale.
- Hytale-specific behavior belongs in:
  - template configuration
  - optional integration modules
  not in the core scheduling and orchestration logic.

## Agent rules: always read `.agents/`

Before you make any change in this repository, you **must** read the entire `.agents/` directory.

The files in `.agents/` are the **source of truth** for how you work in this repo:
- follow them **exactly**
- if there is a conflict between documents, **`.agents/` wins**
- do not guess or invent missing rules, look them up in `.agents/`

If you didn’t read `.agents/`, you are not ready to start coding.