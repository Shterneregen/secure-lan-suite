
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 03 — Desktop Layout System

> **Role of this chapter**
>
> This chapter defines how every pixel on the screen participates in the communication experience.
> It intentionally focuses on composition, attention, cognitive load and visual rhythm rather than individual widgets.
>
> **Design invariant**
>
> A user must always understand where to look first without consciously thinking about it.

---

# 1. Layout Philosophy

The desktop workspace is a communication environment.

It is **not**:

- a dashboard
- a settings page
- a server console
- a collection of feature panels

Every layout decision must reinforce one idea:

> Conversation is the product.

Everything else exists to support the conversation.

---

# 2. Eye Flow

Every screen must guide the user's eye using the same path.

```
Header
   ↓
Current Conversation
   ↓
Conversation Timeline
   ↓
Composer
   ↓
Context Panel (if needed)
```

The user should never start scanning from the right panel.

## Rules

- The eye always lands in the center first.
- The right panel never competes with the conversation.
- Empty space must direct attention, not waste space.

**Definition of Done**

A first-time user naturally focuses on the conversation within two seconds.

---

# 3. Visual Weight Budget

Every screen has a limited attention budget.

Recommended distribution:

| Region | Visual Weight |
|---------|---------------:|
| Conversation | 60% |
| Left Sidebar | 20% |
| Header | 8% |
| Right Context Panel | 10% |
| Decorations | 2% |

Rules:

- Never allow the right panel to become visually heavier than the chat.
- Cards must never dominate the conversation.
- Bright accents are reserved for primary actions and active states.

---

# 4. Desktop Grid

Use a stable desktop grid.

## Recommended widths

- Left Sidebar: 280–320 px
- Conversation: flexible (minimum 55% of window width)
- Context Panel: 320–360 px

At widths below 1400 px:

- Collapse secondary context.
- Preserve conversation width.

At widths below 1200 px:

- Context panel becomes an overlay.

Never reduce conversation width before collapsing secondary UI.

---

# 5. Spatial Hierarchy

Spacing communicates relationships.

Preferred spacing scale:

- 4
- 8
- 12
- 16
- 20
- 24
- 32
- 40

Rules:

- Related controls remain visually grouped.
- Different concepts require larger spacing.
- Borders are a last resort.

Whitespace replaces unnecessary containers.

---

# 6. Composition Rules

Every screen has exactly one hero.

Examples:

Welcome → Primary CTA

Messenger → Conversation

Transfer → Transfer progress

Call → Remote participant

The hero receives:

- largest area
- highest contrast
- most breathing room

Nothing may visually compete with it.

---

# 7. Progressive Complexity

The UI exposes complexity in four layers.

Layer 1

Daily communication.

Layer 2

Conversation tools.

Layer 3

Advanced actions.

Layer 4

Engineering tools.

Users should spend almost all of their time in Layers 1 and 2.

---

# 8. Cognitive Load Rules

Avoid simultaneous decisions.

Never ask users to:

- choose a peer
- configure networking
- inspect diagnostics
- start a transfer

at the same time.

Each screen should answer one question:

"What should I do next?"

---

# 9. Empty Space

Empty space is intentional.

It should:

- improve readability
- reduce stress
- improve focus

It must never indicate unfinished design.

Large desktop displays should feel balanced rather than sparse.

---

# 10. Anti-Patterns

Reject layouts that:

- resemble an admin dashboard
- contain multiple competing panels
- expose technical information by default
- require constant scrolling
- contain more than three competing primary actions
- place forms above conversations after connection
- duplicate the same action in multiple places

---

# 11. Review Checklist

Before approving any screen verify:

- [ ] The conversation is the visual hero.
- [ ] The eye naturally moves from header to conversation.
- [ ] The right panel supports rather than competes.
- [ ] Empty space improves focus.
- [ ] No unnecessary containers exist.
- [ ] The layout remains balanced at 1280, 1600 and 1920 px.
- [ ] No engineering terminology appears outside Advanced or Diagnostics.
- [ ] Primary actions are immediately discoverable.

---

# Chapter 03 Acceptance Criteria

This chapter is complete when:

- Designers can build new screens without inventing layouts.
- Developers understand where every component belongs.
- LLM agents can evaluate composition using objective rules rather than subjective preference.
- Every new screen feels like part of the same product.
