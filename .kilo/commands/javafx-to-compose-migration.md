---
description: Migrate JavaFX behavior into the current Compose product direction
agent: code
---

# JavaFX to Compose Migration

Task context: $ARGUMENTS

1. Inspect the JavaFX flow as a behavioral reference and identify the user goal.
2. Read `.kilo/knowledge/PRODUCT-UI.md` and inspect the current Compose state.
3. Preserve protocols and the isolated fallback application.
4. Implement the behavior with the current Compose shell, components, and
   semantic tokens; do not copy the deprecated visual layout.
5. Verify behavior, accessibility, and relevant responsive states.
6. Run focused tests or a runtime check and report evidence and remaining gaps.
