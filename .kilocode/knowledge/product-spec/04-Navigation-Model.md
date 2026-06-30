
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 04 — Navigation Model

> **Purpose**
>
> Define how users move through SecureLanSuite.
> Navigation must reduce thinking, preserve context and never interrupt communication.

---

# 1. Navigation Philosophy

Navigation is not a list of screens.

Navigation is a model of user intent.

The user is never navigating to "features".

The user is navigating to accomplish a goal.

Every navigation decision must answer:

- Where am I?
- What can I do here?
- How do I return?

---

# 2. Navigation Layers

## Layer 1 — Primary Navigation

Always visible.

Contains:

- Current Room
- People
- Conversation

This layer is responsible for daily work.

---

## Layer 2 — Context Navigation

Appears according to the selected object.

Examples:

Peer selected

Transfer selected

Call active

Room selected

Never visible without context.

---

## Layer 3 — Product Navigation

Accessible through:

- Settings
- Search
- Command Palette

Contains:

- Settings
- Diagnostics
- Advanced Connection
- About

Never competes with communication.

---

# 3. Navigation Rules

## Rule 1

One click changes one context.

Selecting a peer must not also open Settings or Diagnostics.

## Rule 2

Navigation never destroys user work.

Opening Settings never closes:

- active chat
- active transfer
- active call

## Rule 3

Users always know how to return.

Every secondary screen has an obvious exit.

---

# 4. Command Palette

Command Palette is the fastest way to access the application.

Shortcut:

Ctrl+K

Purpose:

- Search peers
- Search rooms
- Start calls
- Send files
- Open Settings
- Open Diagnostics
- Open Advanced Connection

It must never become a replacement for normal navigation.

---

# 5. Search

Search is global.

Results may include:

- Rooms
- People
- Conversations
- Messages
- Recent files

Search never returns engineering objects.

Do not search:

- ports
- listeners
- adapters

---

# 6. Back Navigation

Back always returns to the previous user context.

Examples:

Diagnostics → Conversation

Settings → Conversation

Peer → Room Chat

Never return users to Welcome after they are connected.

---

# 7. Keyboard Navigation

Primary shortcuts:

- Ctrl+K — Command Palette
- Ctrl+N — Host Room
- Ctrl+J — Join Room
- Ctrl+, — Settings
- Ctrl+Shift+D — Diagnostics
- Ctrl+F — Search Conversation
- Esc — Close current overlay

Tab order always follows visual order.

Focus must never disappear.

---

# 8. Deep Linking

Every important destination should have a stable internal route.

Examples:

- Current room
- Peer profile
- Conversation
- Settings section
- Diagnostics page

Deep links must restore context rather than reopen unrelated screens.

---

# 9. Overlay Rules

Use overlays for:

- Settings
- Diagnostics
- Confirmation dialogs
- File picker
- Incoming calls

Do not use overlays for:

- Conversation
- Peer switching
- Room switching

---

# 10. Navigation Anti-Patterns

Reject designs that:

- require users to reopen the same screen repeatedly
- duplicate navigation controls
- expose multiple competing menus
- interrupt communication for configuration
- require more than three steps for common actions

---

# 11. UX Performance Targets

Users should be able to:

- Open any primary destination in one interaction.
- Reach Settings in no more than two interactions.
- Start a conversation in less than five seconds.
- Recover from an error without losing context.

---

# 12. Navigation Acceptance Checklist

- [ ] Primary navigation always remains understandable.
- [ ] Context navigation changes only with user context.
- [ ] Search is global.
- [ ] Command Palette complements rather than replaces the UI.
- [ ] Keyboard navigation works everywhere.
- [ ] Back navigation is predictable.
- [ ] Overlays never destroy communication context.
- [ ] No workflow requires unnecessary navigation.

---

# Chapter 04 Acceptance Criteria

The navigation model is complete when:

- Every destination has one clear entry point.
- Every workflow has one predictable return path.
- Communication always remains the primary destination.
- Users can move through the product without needing documentation.
