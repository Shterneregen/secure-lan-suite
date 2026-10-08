---
name: securelan-ui-design
description: Design or revise SecureLanSuite product UI, individual screens, and feature flows. Use for new screens, redesigns, layout and context-panel decisions, responsive behavior, component reuse, and UX changes to chat, file transfer, realtime communication, Quick Share, steganography, diagnostics, or settings.
license: MIT
---

# SecureLanSuite UI Design

## Role

Own product-level UI direction, screen composition, and feature-level UX without
inventing behavior or crossing service boundaries.

This skill decides **what the UI should communicate and how the interaction
should be structured**. Use `compose-engineering` for detailed implementation
architecture.

## Sources of truth

1. User request and accepted scope.
2. `.kilo/knowledge/PRODUCT-UI.md`.
3. Current implementation and state model.
4. Existing components and design tokens.
5. Legacy JavaFX UI as a behavior reference only.

If product guidance and implemented behavior differ, identify the difference
before choosing a direction. Preserve working protocols and service boundaries.

## Common process

1. Identify the user goal and current product state.
2. Inspect the relevant implementation, state types, and adjacent UI.
3. Identify context that must remain visible through the task.
4. Decide what information is primary, secondary, contextual, or hidden.
5. Reuse existing shell, components, and semantic tokens where appropriate.
6. Define loading, empty, error, disabled, success, and recovery behavior.
7. Define keyboard, focus, accessibility, and relevant responsive behavior.
8. Produce design direction or implementation within the requested scope.
9. Validate concrete behavior and report evidence, limitations, and tradeoffs.

## Route by task

### Product and layout work

Use for navigation, workspace structure, redesigns, and context-panel behavior.

- Preserve communication-first hierarchy.
- Avoid dashboard layouts and permanent tool collections.
- Keep engineering concepts in Diagnostics or Advanced surfaces.
- Keep the context panel tied to the current task and selection.

### Screen work

Use for Welcome, Host Setup, Join Setup, Messenger, Settings, Diagnostics, and
new screens.

- Inspect the screen state and all reachable states before changing layout.
- Define action hierarchy, scrolling, resizing, and narrow-window behavior.
- Prefer existing components; introduce a component only for a reusable need.

### Feature-flow work

Use for chat, file transfer, voice/video, Quick Share, steganography,
diagnostics, and settings.

- Inspect the complete user flow, including failure and recovery.
- Preserve feature ownership and service boundaries.
- Do not duplicate another feature's responsibility.
- Keep protocol and transport details out of normal user-facing copy.

## Completion

For implementation work, complete the applicable UI, accessibility, and
responsive checklists under `.kilo/checklists/`. Report what was verified and
what still requires runtime or visual evidence.
