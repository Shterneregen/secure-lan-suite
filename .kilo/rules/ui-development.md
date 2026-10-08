# UI Development Rule

Before changing UI:

1. Read `.kilo/knowledge/PRODUCT-UI.md`.
2. Identify the task type.
3. Identify current product state.
4. Identify current user intent.
5. Inspect current behavior and the context that must be preserved.
6. Reuse existing components and tokens where appropriate.
7. Complete `.kilo/checklists/ui.md`.
8. Complete the accessibility and responsive checklists when their applicability conditions are met.

Never start by writing Compose code.

## Hard bans

- No dashboard UI.
- No permanent toolbox panels.
- No Host/Join forms inside Messenger.
- No engineering terminology in normal UI.
- No legacy JavaFX visual recreation.
