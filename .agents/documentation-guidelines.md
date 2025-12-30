# Documentation Guidelines (Agents)

These rules apply to all agent contributions in this repository. Goal: changes are discoverable, consistent, and easy to maintain.

## 1) Core rule: documentation is part of the change
Whenever you change code, you must check whether documentation needs to be updated.
A change is not “done” until **code and docs match**.

Common triggers for doc updates:
- new features or changed behavior
- configuration changes (env vars, flags, defaults)
- new/changed endpoints, events, commands
- data model changes (DB, DTOs, contracts)
- breaking changes or migrations
- new dependencies or integration points

---

## 2) Where to document: `/docs/<module>/`
Documentation lives under `/docs/` and is split by module.

**Rule:** if you change something in a module, document it in the matching folder:
- changes in **Brain** → `/docs/brain/`
- changes in **Node** → `/docs/node/`
- changes in **Panel** → `/docs/panel/`
- changes in **Template System** → `/docs/template-system/`
- changes in **Infrastructure/Deployment** → `/docs/infrastructure/`
- changes in **Instance/Lifecycle** → `/docs/instance/`

**If you’re unsure where it belongs:**
- document it where the change is *consumed* (consumer perspective)
- for cross-cutting topics (auth, logging, tracing), document it in the module that “owns” the concept, and link from other modules (don’t duplicate the full explanation)

---

## 3) Structure inside a module
Each module docs folder should grow roughly like this (not everything is required every time):

- `overview.md`  
  What the module is, what it does, key terms.
- `architecture.md`  
  Components, dependencies, important decisions.
- `workflows/`  
  Flows as separate files (e.g. `instance-lifecycle.md`).
- `operations/`  
  Ops docs: configuration, monitoring, troubleshooting.
- `changes/`  
  Optional: larger changes/migrations that are worth keeping historically.

**Rule:** prefer several small focused files over one huge document.

---

## 4) Writing rules (so docs stay consistent)
- Write for humans who are new to the codebase.
- Use clear headings, short paragraphs, and bullet points.
- Use the same vocabulary as the code (domain terms).
- No essays: focus on “what changed”, “how to use it”, and “what to watch out for”.
- If you explain “why”, keep it short and concrete.

Recommended structure per doc:
- **Purpose**
- **What changed**
- **How to use / impact**
- **Edge cases / risks**
- **Links** (code, OpenAPI, related docs)

---

## 5) Data model & API: OpenAPI is mandatory
If you change the **data model** or the **API**, you must update the contract.

**Rule:** all API/contract-relevant changes go into:
- `/contracts/openapi.yml`

What counts as “contract-relevant”?
- new/removed/changed endpoints
- request/response shapes
- status codes and error formats
- auth requirements
- new fields, renamed fields, optional/required changes
- enum value changes/additions
- pagination/sorting/filter parameter changes

**Important:** `/docs/...` explains behavior and usage.  
**`/contracts/openapi.yml`** is the source of truth for the API surface.

### OpenAPI rules
- Keep naming consistent (schema names, field names, examples).
- Use `$ref` instead of duplicating schemas.
- Handle breaking changes intentionally (clear deprecations, versioning strategy if used).
- Describe error responses (at least a standard error schema) and status codes.

---

## 6) Workflow for implementing changes
When you implement a change:

1. **Define scope:** which module is affected? which docs need updates?
2. **Check contracts first:** is API/data model affected? → update `openapi.yml`.
3. **Update module docs:** write/update docs under `/docs/<module>/`.
4. **Add cross-links:** if multiple modules are involved, link instead of duplicating.
5. **Make it review-ready:** reread docs against the code (no contradictions).

---

## 7) PR/Review checklist (agents)
Before opening a PR:
- [ ] Updated/created docs under `/docs/<module>/` for the changed module
- [ ] If API/data model changed: updated `/contracts/openapi.yml`
- [ ] Terms match the code (naming, concepts)
- [ ] Docs explain “what” and “how”, not just “that it exists”
- [ ] No contradictions between docs and the OpenAPI contract

---

## 8) Mini examples

### Example A: Brain feature change
- Code changed in `brain/...`
- Docs:
  - update or add `/docs/brain/workflows/<feature>.md`
  - update `/docs/brain/overview.md` if the feature changes the module’s public behavior
- If API affected:
  - update `/contracts/openapi.yml`

### Example B: Request body gets a new field
- `/contracts/openapi.yml`:
  - update schema
  - update examples
  - set required/optional correctly
- `/docs/<module>/...`:
  - describe behavior: when the field is used, defaults, edge cases
