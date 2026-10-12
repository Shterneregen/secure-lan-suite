---
name: bugfix
description: Fix a focused defect and verify the affected behavior
license: MIT
---

# Bugfix

1. Reproduce or establish evidence for the defect.
2. Inspect the owning implementation, tests, and relevant documentation.
3. Correct the smallest coherent cause without expanding the requested scope.
4. Preserve protocols, compatibility, and unrelated behavior.
5. Run focused tests or a runtime check that would fail before the fix.
6. For user-facing Compose changes, apply the relevant checklists under
   `docs/quality/`.
7. Report the cause, change, verification, and any remaining limitation.
