# Project Coding Guidelines

You generate or modify code in this repo. Follow these rules strictly.
Language-agnostic: adapt syntax for TypeScript, Java, etc.

## Operating principles
- Prefer existing project conventions first.
- Keep diffs small and focused. Avoid unrelated refactors.
- Make behavior predictable: clear names, clear control flow, explicit errors.
- Add/update tests for behavior changes.

---

## Naming
MUST
- Use intention-revealing names (state what and why).
- Use repo vocabulary consistently.
- Use searchable names (no magic numbers, no vague identifiers).
- Avoid encoding (no Hungarian notation, no scope prefixes).
- Avoid mental mapping (no `a`, `b`, `tmp` outside tiny scopes).

SHOULD
- Plural for collections (`users`, `userIds`).
- `xByY` for maps (`userByEmail`).
- Booleans read like questions (`isReady`, `hasAccess`).

MUST NOT
- Introduce ambiguous “noise” names (`data`, `info`, `manager`) without real meaning.

---

## Functions
MUST
- Keep functions small and focused (one reason to change).
- Keep one abstraction level per function.
- Separate pure logic from side effects.
- Apply command-query separation:
  - Queries return data and do not mutate state.
  - Commands mutate state and return minimal results.
- Make side effects obvious in naming (`save*`, `delete*`, `publish*`, `enqueue*`).

SHOULD
- Prefer 0–2 parameters; use an options object for more.
- Avoid boolean flag parameters that change behavior.
- Use guard clauses to reduce nesting.

Switch statements
- Prefer mapping/strategy.
- If switch exists: short cases, no fallthrough, exhaustive, unknown values fail loudly.

---

## Error handling
MUST
- Handle expected business outcomes explicitly (validation, not found, conflict).
- Propagate unexpected failures (throw/reject). Never swallow errors.
- If catching: add context and rethrow or convert to a domain error.
- Clean up resources (finally / try-with-resources / defer).

MUST NOT
- Log secrets or sensitive data.

---

## Formatting
MUST
- Use project formatter/linter outputs. Do not hand-style.
- Avoid clever one-liners in control flow.
- Reduce nesting via early returns.

---

## Architecture
MUST
- Keep domain logic independent from frameworks/I/O where feasible.
- Keep I/O at boundaries (HTTP, DB, external providers).
- Do not introduce new dependencies unless necessary and consistent with the stack.

---

## Testing
MUST
- Add/update tests when behavior changes.
- Keep tests deterministic (no real network/time flakiness).
- Cover critical edge cases.

SHOULD
- Unit tests for pure logic; few integration tests for critical flows.

---

## Logging
MUST
- Log at boundaries with useful context fields.
- Keep logs minimal in hot paths.

MUST NOT
- Spam logs in loops or deep helpers.
- Log secrets/tokens/passwords/personal data.

---

## Agent work method
Follow this sequence:
1. Identify boundary vs core logic; push I/O to boundary.
2. Write the happy path first.
3. Extract helpers named by intent.
4. Add explicit error handling.
5. Add/update tests.
6. Format and lint.
7. Re-read diff for clarity, hidden side effects, missing tests.
