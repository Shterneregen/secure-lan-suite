---
name: compose-engineering
license: MIT
description: >
  Implement defined SecureLanSuite desktop behavior in Compose. Use for
  composables, UI state, focus and semantics, responsive mechanics, performance,
  and focused UI tests after product direction is known. Do not use for
  product-strategy or review-only tasks.
---

# Compose Engineering

## Prepare

1. Read `AGENTS.md` and the relevant part of `docs/product-ui.md`.
2. Inspect the affected UI, state, callbacks, and tests.
3. Resolve undefined product direction with `securelan-ui-design`.

## Implement

- Prefer explicit immutable UI state and event callbacks.
- Keep local UI state local; do not create a second owner for existing state.
- Model applicable loading, empty, disabled, success, and error behavior.
- Keep effects keyed to their real lifecycle inputs and clean up resources.
- Avoid unnecessary recomposition work in frequently updated media or transfer
  surfaces.
- Before adding a reusable component, confirm that an existing component or a
  local composable cannot meet the need. Give reusable components one clear
  responsibility and verify them in a real caller.

## Verify

- Apply `docs/quality/ui-checklist.md` and its conditional checklists.
- Run focused tests or a runtime walkthrough that exercises the changed state.
- Report verified behavior, untested states, and material tradeoffs.
