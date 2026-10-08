---
description: Implement defined desktop behavior in Compose and verify it
agent: code
---

# Compose Implementation

Task context: $ARGUMENTS

1. Read `.kilo/knowledge/PRODUCT-UI.md` and inspect the affected UI and state.
2. Confirm the behavior to implement and the context it must preserve.
3. Reuse the current shell, semantic tokens, icons, and components.
4. Keep business logic behind callbacks, adapters, services, or focused state
   holders; do not change protocols unless requested.
5. Implement applicable loading, empty, disabled, success, and error states.
6. Apply `.kilo/checklists/ui.md` plus the accessibility or responsive checklist
   when its applicability conditions are met.
7. Run focused tests or a runtime check and report evidence and limitations.
