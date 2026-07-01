
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 07 — Design System & Component Behavior

> Purpose
>
> Components are product building blocks, not reusable drawings.
> Every component must communicate intent, reduce cognitive load, and behave consistently across the application.

---

# 1. Design System Principles

A component may exist only if it has:

- One clear responsibility
- One predictable behavior
- One visual language

A component must never solve multiple unrelated problems.

---

# 2. Component Lifecycle

Every component specification includes:

- Purpose
- Placement
- Allowed content
- Forbidden usage
- States
- Motion
- Keyboard behavior
- Accessibility
- Acceptance criteria

---

# 3. Conversation Pane

## Purpose

The Conversation Pane is the product.

Everything else supports it.

## Rules

- Occupies the largest visual area.
- Never hidden by configuration.
- Never replaced by diagnostics.
- Timeline scroll position is preserved.

Forbidden:

- Embedded settings
- Runtime information
- Large warning banners

---

# 4. Peer Row

## Purpose

Represent a person, not a network endpoint.

Contains:

- Avatar
- Display name
- Presence
- Unread badge
- Optional capability icons

Never contains:

- IP address
- Port
- Runtime state

States:

- Default
- Hover
- Selected
- Offline
- Disabled

---

# 5. Context Card

## Purpose

Present contextual information.

Maximum:

- One primary action
- Two secondary actions

Must be collapsible.

Nested cards are prohibited.

---

# 6. Transfer Card

Purpose:

Represent a single transfer.

Contains:

- Filename
- Direction
- Progress
- Status
- Retry / Cancel / Open

Never display engineering terminology.

Transfer diagnostics belong to overflow actions.

---

# 7. Call Banner

Purpose:

Expose active communication without interrupting chat.

Contains:

- Call duration
- Mute
- Device
- End call

Chat always remains visible.

---

# 8. Composer

Purpose:

Fast message creation.

Structure:

Attach

↓

Input

↓

Send

Rules:

- Always pinned to bottom.
- Focus restored after sending.
- Enter sends.
- Shift+Enter inserts newline.

Disabled only when messaging is impossible.

---

# 9. Action Button

Types:

- Primary
- Secondary
- Tertiary
- Destructive

Rules:

- Only one primary action per region.
- Avoid multiple competing accent buttons.

---

# 10. Empty State

Every empty state must include:

- Situation
- Explanation
- Next action

Never leave empty space unexplained.

---

# 11. Recovery Card

Purpose:

Help users recover from failure.

Contains:

- Human explanation
- Recommended action
- Advanced details (optional)

Never expose raw implementation first.

---

# 12. Toasts

Use for:

- Transfer complete
- Invite copied
- Room created
- Settings saved

Never use toasts for critical failures.

Critical failures require recovery UI.

---

# 13. Dialogs

Dialogs interrupt work.

Use only for:

- Confirmation
- Destructive actions
- File picker
- Permissions

Never use dialogs for navigation.

---

# 14. Motion Rules

Animation exists only to explain state change.

Duration:

100–250 ms for common transitions.

Avoid decorative animation.

Respect reduced-motion preferences.

---

# 15. Accessibility

Every component must support:

- Keyboard navigation
- Visible focus
- Screen readers
- High contrast
- Minimum touch target of 40 px where applicable

---

# 16. Component Anti-Patterns

Reject components that:

- Combine multiple responsibilities
- Duplicate another component
- Depend on hidden state
- Require documentation to understand
- Reveal engineering concepts unnecessarily

---

# 17. Component Review Checklist

- [ ] Purpose is obvious.
- [ ] Only one responsibility.
- [ ] Consistent with design system.
- [ ] Accessible.
- [ ] Keyboard friendly.
- [ ] Responsive.
- [ ] Supports dark and light themes.
- [ ] Avoids engineering terminology.

---

# Chapter 07 Acceptance Criteria

The chapter is complete when:

- Every UI element belongs to a defined component.
- No new component needs to be invented without extending this specification.
- LLM agents can build new screens by composing existing components instead of improvising layouts.
