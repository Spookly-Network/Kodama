## Agent playbook: working on an issue you receive (using our templates)

### 0) Non negotiables

1. Treat the issue text as the source of truth. Do not expand scope on your own.
2. Keep changes small and predictable. No refactors unless the issue explicitly asks for it.
3. Respect boundaries:
   • Brain schedules and owns state
   • Node executes, merges templates, runs Docker
   • Panel talks only to Brain
4. Handle errors explicitly (timeouts, network, Docker, DB). No silent failures.

---

### 1) Issue intake (first thing you do)

When someone gives you an issue, if its on github, fetch via curl and api then extract these parts depending on template.:

**[Feature] Feature / Task**
• Summary: what outcome is needed
• Details: why and context
• Scope / Requirements: concrete steps
• Acceptance Criteria: what “done” means

**[Arch] Architecture Task**
• Summary: what area changes
• Problem: current limitation
• Proposed Solution: the approach, plus alternatives (if listed)
• Impact Analysis: touched modules, migrations, rollout risk
• Acceptance Criteria

**[Research] Research / Investigation**
• Summary: topic
• Key Questions: what must be answered
• Context: why now
• Research Approach: how to gather info
• Expected Deliverables: what you must produce

**[Epic] Epic**
• Summary + Goal: the target
• Linked Tasks: what exists already
Your job is usually to pick one linked task and execute it, or propose a breakdown if none exists.

---

### 2) Your first response back (before coding)

Reply with a short “working plan” in this order:

1. Restate goal in one sentence
2. List acceptance criteria (or propose them if missing)
3. List assumptions you are making (only if needed)
4. Plan: 3 to 8 steps, ordered
5. Files and modules you expect to touch: /brain, /node, /panel
6. Risks and edge cases you will cover
7. What you will post as progress updates (see section 5)

If something is unclear, do not stop. Make the smallest reasonable assumption and state it.

---

### 3) Implementation rules while working

1. Branch naming
   • feature/* for Feature issues
   • arch/* for Architecture issues
   • fix/* for bug fixes inside any issue

2. Keep the PR focused
   • One PR per issue
   • PR must include “Closes #ID” (or “Refs #ID” if not closing)

3. Code style and safety
   • Configuration via env or application config, never hard coded secrets
   • Deterministic behavior, no randomness unless seeded and justified
   • Add comments only where logic is not obvious
   • Prefer existing patterns already used in the repo

4. Tests
   • Add or update tests when behavior changes
   • Use JUnit 5
   • Use Testcontainers for Docker or MySQL integration cases

5. Docs
   • If you change lifecycle, template merge rules, or APIs, update the matching doc in /docs or the relevant markdown.

---

### 4) Template specific “done” check

**Feature / Task**
You are done when:
• Acceptance criteria are met and demonstrable
• API returns correct data or state changes correctly
• Failure paths are handled
• Tests and docs updated if relevant

**Architecture Task**
You are done when:
• The problem is solved with the proposed approach (or you justify deviations)
• Impact is addressed (migrations, compatibility, rollout plan)
• No regressions, code compiles, tests pass
• Architecture decision is documented (short, concrete)

**Research**
You are done when:
• Every key question is answered
• You give a clear recommendation
• You include pros and cons and a next step plan
• You produce the promised deliverables (short doc, comment, small prototype if requested)

**Epic**
You are done when:
• The specific linked task you picked is complete
• You update the Epic’s linked checklist status (if Niklas wants that in the writeup)

---

### 5) How you update the issue while working

Post progress in a tight loop, usually as comments or chat updates:

1. “Started” update
   • link to branch or commit message summary
   • what is next

2. “Midpoint” update if it takes multiple steps
   • what is done
   • what is left
   • any blocker

3. “Ready for review” update
   • what to test
   • how to verify acceptance criteria
   • known limitations (if any)

If blocked, post:
• what you tried
• what is preventing progress
• 2 options to proceed, with a recommendation

---

### 6) Defaults you should assume unless the issue says otherwise

• Brain owns instance state machine and events
• Node is lightweight and mostly stateless
• Template layers merge in order, last wins, then variable replacement
• Panel never talks directly to Node
• Security: Node auth is token or mTLS, do not invent new auth flows without an Arch issue

---

## Agent rules: what NOT to do with issues and templates

### General (always)

* **Do not “read between the lines” and add requirements.** Only do what the issue explicitly asks for.
* **Do not combine multiple issues into one PR.** One issue = one PR.
* **Do not refactor unrelated code.** No “cleanup while I’m here”.
* **Do not add new dependencies** unless the issue explicitly requires it.
* **Do not mark an issue as done** if acceptance criteria are not clearly met and verifiable.
* **Do not create new issues or restructure epics** unless explicitly told.

---

## Architecture issues: what you must NOT do

Architecture issues are design + contracts + safe scaffolding, not full delivery.

* **Do not implement end to end integration.** No “Brain + Node + Panel + runtime flow” in one go.
* **Do not make meaningful changes in multiple runtime modules in a single PR.** Avoid real logic changes across Brain and Node and Panel at the same time.
* **Do not implement production flow when the issue is about model/contracts.**
  Example: if the Arch issue is “data model + contracts”, then do **not** implement scheduling, instance start, port allocation, Docker flow, or UI wiring.
* **Do not implement extra endpoints/UI “because it makes sense”.** Define first, implement later in separate Feature issues.
* **Do not run DB migrations without a rollout plan.** No “change schema and hope”.
* **Do not break boundaries.**

   * Panel must not talk directly to Node.
   * Node must not become the source of truth.
   * Brain remains the control plane.
* **Do not invent new security/auth flows on the side.** No new token schemes, no new trust chain, no quick secret handling.
* **Do not treat “Arch” as a license for big refactors.** If it’s not required for the architecture goal, don’t touch it.
