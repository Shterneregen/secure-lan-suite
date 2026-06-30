
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 18 — Product State Machine

> Purpose
>
> This chapter defines the canonical application state machine.
> Every screen, transition and interaction must be derived from these states.
> A UI may never represent two incompatible primary states simultaneously.

---

# 1. State Machine Principles

Rules:

- The application always has exactly one primary state.
- State transitions are explicit and predictable.
- State changes preserve user context whenever possible.
- UI is a projection of state, not the source of truth.

---

# 2. Primary States

| State | Purpose |
|--------|---------|
| Launch | Initialize application |
| Welcome | Enter communication |
| Host Setup | Create a room |
| Join Setup | Join a room |
| Waiting | Waiting for peers |
| Messenger | Daily communication |
| Voice Call | Audio communication |
| Video Call | Video communication |
| Settings | Preferences |
| Diagnostics | Troubleshooting |

Only one primary state may exist.

---

# 3. Canonical State Diagram

```text
Launch
   |
   v
Welcome
 |      |
 |      |
Host   Join
 |      |
 +------+
    |
Waiting
    |
Messenger
 |   |    |
 |   |    +--> Settings --> Messenger
 |   |
 |   +--> Diagnostics --> Messenger
 |
 +--> Voice Call
 |        |
 |        +------+
 |               |
 +--> Video Call |
          |      |
          +------+
             |
        Messenger
```

Returning from Settings, Diagnostics or Calls restores the previous Messenger context.

---

# 4. Messenger Substates

Messenger contains contextual substates:

- No peer selected
- Peer selected
- File transfer
- Search
- Incoming notification

Substates modify the workspace but never replace Messenger.

---

# 5. Transition Rules

Allowed:

- Welcome → Host Setup
- Welcome → Join Setup
- Host Setup → Waiting
- Join Setup → Messenger
- Waiting → Messenger
- Messenger ↔ Voice Call
- Messenger ↔ Video Call
- Messenger ↔ Settings
- Messenger ↔ Diagnostics

Forbidden:

- Welcome → Diagnostics
- Host Setup → Settings (without explicit action)
- Diagnostics → Welcome (while connected)

---

# 6. State Ownership

Each state owns:

- One visual hero
- One primary CTA
- One dominant workflow

No state may inherit another state's primary CTA.

---

# 7. Persistent Elements

Remain visible across most states:

- Current room
- Window chrome
- Global notifications

Disappear when inappropriate:

- Composer
- Context assistant
- Peer actions

---

# 8. State Restoration

When leaving temporary states:

- Restore selected peer
- Restore scroll position
- Restore composer draft
- Restore filters
- Restore context panel state

Never reset user work unless explicitly requested.

---

# 9. Error States

Errors do not become primary states.

Instead:

Current State
    ↓
Recovery Overlay
    ↓
Return to Current State

Diagnostics remain optional.

---

# 10. AI State Resolution Algorithm

Before rendering:

1. Detect primary state.
2. Detect messenger substate.
3. Determine current selection.
4. Resolve blueprint (Chapter 17).
5. Apply decision matrices (Chapter 11).
6. Validate forbidden patterns (Chapter 12).
7. Render.

Never render before state resolution.

---

# 11. Validation Checklist

- [ ] Exactly one primary state.
- [ ] Valid transition.
- [ ] Correct blueprint selected.
- [ ] Context preserved.
- [ ] Temporary overlays return correctly.
- [ ] No incompatible UI mixed together.

---

# Chapter 18 Acceptance Criteria

This chapter is complete when every screen can be mapped to a single primary state
and every user action results in a valid, predictable transition without losing context.
