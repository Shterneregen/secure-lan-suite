---
name: securelan-ui-design
license: MIT
description: >
  Define SecureLanSuite product direction, information hierarchy, user flows,
  layout, and UX behavior. Use when a desktop surface or interaction needs a
  product decision. Do not use for low-level Compose mechanics or review-only
  tasks.
---

# SecureLanSuite UI Design

## Sources

1. The user request and accepted scope.
2. `docs/product-ui.md`.
3. Current implementation, state model, and adjacent UI.
4. Existing components and semantic tokens.

## Workflow

1. Identify the user goal and current product state.
2. Decide whether an existing surface can meet the goal before adding a new
   screen, dialog, tool, or reusable component.
3. Identify the room, peer, conversation, draft, transfer, and call context that
   must remain available.
4. Define primary, secondary, contextual, and hidden information.
5. Define actions and applicable loading, empty, disabled, success, error, and
   recovery states.
6. Define keyboard, focus, accessibility, resizing, and narrow-window behavior.
7. Produce a concrete design direction and acceptance criteria.

Use `compose-engineering` for implementation. Do not invent screens or
capabilities that the product does not implement.
