---
name: compose-engineering
license: MIT
description: Implement and review Jetpack Compose and Compose Multiplatform UI. Use for Compose UI code, state modeling, architecture, accessibility, performance, testing, desktop behavior, and Kotlin Multiplatform target sharing after product direction is known.
---

# Compose Engineering

## Role and boundary

Implement approved product behavior with explicit state, accessible UI,
predictable interactions, and minimal complexity. This skill answers **how to
implement in Compose**. It does not choose product strategy or feature scope.

## Sources of truth

1. User request and accepted scope.
2. Feature specification or task acceptance criteria.
3. `.kilo/knowledge/PRODUCT-UI.md` for SecureLanSuite UI work.
4. Project rules and architecture.
5. Existing implementation and conventions.
6. Legacy UI only as a behavior reference unless strict parity is requested.

Surface material conflicts before implementing a direction that changes scope,
protocols, architecture, or product behavior.

## Bundled resources

Load only what the task requires:

- [`DECISION-TREES.md`](DECISION-TREES.md) for state ownership, architecture,
  containers, dialogs, source sets, and dependency decisions.
- [`CHECKLISTS.md`](CHECKLISTS.md) for implementation and review validation.
- [`REFERENCES.md`](REFERENCES.md) before version-sensitive APIs, dependency
  changes, or Kotlin Multiplatform support claims.

## Implementation workflow

1. Confirm the requested behavior and acceptance criteria.
2. Inspect the affected screen, state, services, and existing conventions.
3. Identify all reachable states and relevant platform behavior.
4. Choose the smallest approach that fits the existing architecture.
5. Implement within scope and preserve service and protocol boundaries.
6. Run focused tests or validation.
7. Complete the relevant bundled checklist.
8. Report changes, evidence, risks, and remaining limitations.

## Architecture

Prefer unidirectional data flow, immutable UI state, state hoisting, narrow state
reads, small stateless composables, and stable lazy-list keys. Respect the
project's existing MVI or MVVM convention.

Use these boundaries where they fit the project:

- Route: obtains the ViewModel, collects state/effects, and handles navigation or platform APIs.
- Screen: renders immutable state and emits callbacks.
- Leaf: renders narrow sub-state and owns only visual-local state.

Keep business logic, networking, persistence, crypto, and platform objects out of
composables and screen state. Do not introduce a competing architecture without
an explicit requirement.

## State and effects

- Use `StateFlow` for durable screen state when consistent with the project.
- Use `Channel` or `SharedFlow` for true one-shot effects.
- Separate editable input, derived values, persisted domain data, and visual-local state.
- Do not store safely derivable values twice.
- Keep focus, scroll, hover, expansion, and animation progress local when they
  have no business or persistence meaning.
- Preserve raw editable input separately from parsed or validated values.

## Compose Desktop

- Use desktop density, resizing, hover, focus, selection, menus, split panes,
  toolbars, and keyboard flows where appropriate.
- Keep focus visible and tab order logical.
- Define narrow-window, overflow, scrolling, and dialog behavior.
- Avoid phone-shaped navigation and oversized controls unless explicitly required.

## KMP and dependencies

Before adding a dependency to `commonMain`, verify coordinates, target support,
API shape, project compatibility, and maintenance cost using current primary
documentation. Do not assume every AndroidX artifact supports every target.

Keep business logic and presentation state shared where practical. Isolate file
pickers, notifications, media devices, storage, and OS integration behind
interfaces or `expect`/`actual` only when the project needs platform separation.

## UI quality and accessibility

Provide applicable loading, empty, error, disabled, success, validation, and
recovery states. Preserve useful content during refresh where possible.

Use semantic components and verify labels, roles, state descriptions, keyboard
operation, visible focus, contrast, and reduced motion. Do not duplicate semantics
already supplied by Material or Foundation components.

## Performance

Keep parsing, blocking I/O, and heavy work outside composition. Use `remember`
and `derivedStateOf` only when they solve a measured or evident state problem.
Use stable keys for changing lazy collections and keep state reads as narrow as
practical.

## Testing

Prioritize tests for ViewModels, reducers, validators, formatters, presentation
mappers, business logic, and platform bindings. Add UI tests for meaningful
interaction or regression risk; do not mirror every visual composable in a test.

## Completion report

Report files changed, behavior or criteria implemented, architecture used,
validation run, known risks, remaining work, and any scope deviations. Do not
claim completion or runtime behavior without supporting evidence.
