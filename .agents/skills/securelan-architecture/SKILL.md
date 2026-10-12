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

1. Read `AGENTS.md` for repository boundaries.
2. Inspect `settings.gradle`, affected build files, and public APIs.
3. Trace callers and tests across module boundaries before moving ownership.
4. Read only the feature documentation relevant to the change.

## Complete the change

1. Keep public APIs small and implement the smallest coherent boundary change.
2. Test the affected contract and direct consumers.
3. Update public documentation when module ownership, compatibility, or
   supported behavior changes.
4. Report affected modules, dependency direction, API and protocol impact,
   validation, risks, and follow-up work.
