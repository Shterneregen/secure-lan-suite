
# SecureLanSuite Desktop Product Specification
## Appendix A — Product Design Cookbook

> Purpose
>
> This appendix translates the specification into practical design recipes.
> It explains how designers, developers and AI agents should apply the rules
> from the previous chapters when creating or modifying the product.

---

# Recipe 1 — Designing a New Screen

## Objective

Create a new screen without inventing a new UX model.

### Workflow

1. Identify the user goal.
2. Identify the application state (Chapter 18).
3. Select the user flow (Chapter 06).
4. Choose the canonical blueprint (Chapter 17).
5. Select an approved design pattern (Chapter 15).
6. Build using existing components (Chapter 07).
7. Apply decision matrices (Chapter 11).
8. Validate against forbidden patterns (Chapter 12).
9. Score using the Product Scorecard (Chapter 20).

Never start from a blank canvas.

---

# Recipe 2 — Adding a New Feature

Ask these questions:

- Does this solve a user problem?
- Which feature contract does it belong to?
- Does an existing pattern already solve it?
- Does it introduce a new primary state?
- Does it increase cognitive load?

If the answer to the last question is "yes", redesign the feature.

---

# Recipe 3 — Redesigning a Legacy Screen

Do not copy the old layout.

Instead:

- Preserve user intent.
- Preserve successful workflows.
- Remove engineering leakage.
- Recompose the screen using approved patterns.

The legacy UI is behavioural input, not visual reference.

---

# Recipe 4 — Choosing the Right Container

Use:

Inline
- Temporary information
- Progress
- Transfer

Context Panel
- Related information
- Peer details
- Call controls

Dialog
- Confirmation
- Destructive actions
- Permissions

Overlay
- Settings
- Diagnostics
- Temporary workflows

Never use dialogs for navigation.

---

# Recipe 5 — Designing Empty States

Every empty state must answer:

1. What happened?
2. Why is it empty?
3. What should I do next?

Example:

"No nearby rooms found."

Primary action:

Create a Room

---

# Recipe 6 — Error Recovery

Preferred order:

Problem

↓

Explanation

↓

Recommended Action

↓

Advanced Details

↓

Diagnostics

Never reverse this order.

---

# Recipe 7 — Introducing a New Component

Before creating a component ask:

- Can an existing component be extended?
- Is the behaviour reusable?
- Does it solve one problem only?

If not, reject the component.

---

# Recipe 8 — Reviewing a Pull Request

Review order:

1. User value
2. Product intent
3. UX
4. Design System
5. Accessibility
6. Performance
7. Code quality

Never start with visual polish.

---

# Recipe 9 — Common LLM Mistakes

Avoid:

- Dashboard layouts
- Multiple primary buttons
- Exposed networking
- Mixing contexts
- Recreating JavaFX
- Overusing dialogs
- Permanent utility panels

---

# Recipe 10 — Golden Questions

Before any UI change ask:

- Does this make communication easier?
- Can something be removed?
- Is the next action obvious?
- Is the current context clear?
- Does this follow an approved pattern?
- Would a first-time user understand this in five seconds?

If any answer is "No", continue refining.

---

# Appendix Acceptance Criteria

The appendix is complete when a designer or AI agent can design, review,
extend or migrate any SecureLanSuite screen by following these recipes
without inventing new UX principles.
