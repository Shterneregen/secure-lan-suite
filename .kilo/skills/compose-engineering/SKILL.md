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

1. Read `.kilo/rules/rules.md` and the relevant part of
   `.kilo/knowledge/PRODUCT-UI.md`.
2. Inspect the affected composables, state types, callbacks, and tests.
3. Establish current behavior and the context that must survive the change.
4. Resolve product direction with `securelan-ui-design` when it is undefined.

## Implement

- Keep business and protocol logic outside composables.
- Prefer explicit immutable UI state and event callbacks.
- Reuse the existing shell, semantic tokens, `SecureLanIcons`, and components.
- Keep local UI state local; do not create a second owner for existing state.
- Model applicable loading, empty, disabled, success, and error behavior.
- Preserve focus, selection, drafts, scrolling, transfers, and calls when the
  workflow requires it.
- Keep effects keyed to their real lifecycle inputs and clean up resources.
- Avoid unnecessary recomposition work in frequently updated media or transfer
  surfaces.

## Verify

- Apply `.kilo/checklists/ui.md` for user-facing changes.
- Apply the accessibility checklist when interaction, focus, semantics, color,
  or motion changes.
- Apply the responsive checklist when layout, visibility, dialogs, or content
  sizing changes.
- Run focused tests or a runtime walkthrough that exercises the changed state.
- Report verified behavior, untested states, and material tradeoffs.
