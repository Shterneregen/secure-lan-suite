---
name: securelan-architecture
license: MIT
description: >
  Plan or implement SecureLanSuite module, dependency, API, and cross-client
  architecture changes. Use when work spans applications and reusable modules,
  changes dependencies, moves ownership, or can affect protocol compatibility.
---

# SecureLanSuite Architecture

## Inspect first

1. Read `.kilo/rules/rules.md`.
2. Inspect `settings.gradle` for current module topology.
3. Inspect the affected Groovy `build.gradle` files and public APIs.
4. Read only the feature documentation relevant to the requested change.
5. Trace callers and tests across module boundaries before moving ownership.

## Boundaries

- Application and platform UI code belongs in `apps/*`.
- Reusable behavior belongs in `modules/*` and must remain UI-framework free.
- Application modules may depend on reusable modules; keep dependencies acyclic.
- Keep cryptography, transport, and runtime/provider work behind explicit
  service boundaries.
- Preserve wire formats and desktop/Android interoperability unless the task
  explicitly changes them.
- Prefer small public APIs, constructor injection, immutable shared models, and
  deterministic tests for pure logic.

## Complete the change

1. Implement the smallest coherent boundary change.
2. Add focused unit or integration tests at the affected contract.
3. Run tests for each changed module and its direct consumers.
4. Update public documentation when module ownership, compatibility, or
   supported behavior changes.
5. Report affected modules, dependency direction, API and protocol impact,
   validation, risks, and follow-up work.
