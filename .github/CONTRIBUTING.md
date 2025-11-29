## CONTRIBUTING

Thanks for contributing to the project.
This document explains how we work, how to create issues, and how pull requests should look.

---

## 1. Issues

Every piece of work starts as an issue.

### Types of issues

Use the templates provided:

* **Epic** — large feature group or initiative
* **Feature / Task** — normal development work
* **Architecture Task** — structural / design changes
* **Research** — investigation or evaluation
* **Docs** — documentation work

### Rules

* One issue = one clear outcome
* Keep it small
* Use labels: type + area + priority
* Issues go into the GitHub Project (Kanban board) automatically

---

## 2. Kanban Workflow (GitHub Project)

Columns:

1. **Backlog** – new issues
2. **Ready** – fully defined and ready for pickup
3. **In Progress**
4. **Review** – open pull requests
5. **Done**

### Epics on the board

Epics stay in **Backlog** or a dedicated “Epics” column.
Only their child tasks move across the board.

---

## 3. Branch Naming

Use lowercase and hyphens:

```
feature/<name>
fix/<name>
arch/<name>
research/<name>
docs/<name>
refactor/<name>
```

Examples:

```
feature/node-registration-api
arch/db-schema-refactor
fix/scheduler-ordering
```

---

## 4. Pull Requests

Every change goes through a PR. No direct commits to `main`.

### PR rules

* Reference issues with: `Closes #123`
* Small and focused
* Use one PR per issue
* Title: meaningful and short
* Add labels matching the issue
* Move to “Review” column automatically
* Wait for at least one review (self-review is fine if alone)

### PR checklist

* Code compiles
* Tests updated (if needed)
* Only relevant changes included
* No unrelated formatting changes

---

## 5. Milestones

Milestones represent real deliverables, like:

* **Brain MVP**
* **Node Agent MVP**
* **Template System v1**

Assign issues or epics to milestones only if they directly contribute.

---

## 6. Coding Style

* Keep classes small
* Clear package structure:
  `controller`, `service`, `repository`, `domain`, `security`, `config`, `dto`
* Controllers → Services → Repositories
* No controller calling repositories directly
* Avoid static util hell
* Prefer constructor injection

---

## 7. Commit Messages

Format:

```
<type>: <short summary>
```

Types:

* feat
* fix
* refactor
* docs
* chore
* test

Example:

```
feat: implement node heartbeat endpoint
fix: correct scheduler ordering logic
```

---

## 8. Testing (when relevant)

* Unit tests for core logic
* Integration tests for DB-backed components
* Avoid testing frameworks for everything
* Mock external services

---

## 9. Release Flow

Main branches:

* `main`: stable
* `dev` (optional): integration branch

Workflow:

1. Branch from `main`
2. Implement
3. PR → `main`
4. GitHub action builds + tests
5. Milestone progress updates