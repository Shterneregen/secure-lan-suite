
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 08 — Context-Aware Workspace Specification

> Purpose
>
> The workspace is not static.
> It continuously adapts to the user's current intention.
>
> SecureLanSuite is a context-driven application.
>
> Users never manually assemble their workspace.
> The workspace reorganizes itself around the current activity.

---

# 1. Context-Driven Product Philosophy

Traditional enterprise software exposes every tool simultaneously.

SecureLanSuite does the opposite.

The interface asks:

"What is the user trying to do right now?"

Everything unrelated disappears.

Context determines:

- Visible panels
- Available actions
- Primary CTA
- Visual hierarchy
- Keyboard shortcuts
- Empty states

---

# 2. Context Model

Only one primary context may exist.

Allowed contexts:

- Welcome
- Room
- Conversation
- Peer
- Transfer
- Voice Call
- Video Call
- Settings
- Diagnostics

Subcontexts may exist but never compete with the primary context.

---

# 3. Workspace Composition

Workspace consists of:

LEFT

Navigation

CENTER

Primary Activity

RIGHT

Context Assistant

Only the center is guaranteed to remain visible.

The left and right columns adapt.

---

# 4. Context Matrix

## Welcome

Left:
Recent Rooms

Center:
Welcome Experience

Right:
Hidden

---

## Room

Left:
Room Members

Center:
Room Conversation

Right:
Room Summary

---

## Peer

Left:
People

Center:
Conversation

Right:
Peer Profile

---

## Transfer

Left:
People

Center:
Conversation + Transfer Card

Right:
Transfer Details

---

## Voice Call

Left:
People

Center:
Conversation + Call Banner

Right:
Call Controls

---

## Video Call

Left:
People

Center:
Video Stage

Right:
Call Controls

---

## Diagnostics

Left:
Collapsed

Center:
Diagnostics

Right:
Issue Details

---

# 5. Context Switching Rules

Changing context must never surprise the user.

Allowed automatic transitions:

Room

↓

Peer

↓

Transfer

↓

Conversation

Returning always restores previous context.

Never reset unrelated UI.

---

# 6. Visibility Matrix

Always Visible

- Conversation
- Current Room
- Current Peer

Contextual

- Transfer Details
- Call Controls
- Security Information

Hidden

- Runtime
- Ports
- Adapters
- Raw Logs
- Engineering Status

Explicit Only

- Diagnostics
- Advanced Connection
- Technical Details

---

# 7. Right Panel Behavior

The right panel is a Context Assistant.

It never becomes:

- Toolbox
- Dashboard
- Settings page
- Monitoring console

Rules

- Maximum one primary card.
- Maximum six cards.
- Secondary cards collapse automatically.
- Diagnostics never occupy the panel unless requested.

---

# 8. Center Workspace Rules

The center always represents the current activity.

Examples:

Conversation

Transfer

Video

Diagnostics

Never display two competing primary activities.

---

# 9. Context Transition Animation

Every context transition should explain change.

Preferred transitions:

Fade

Slide

Expand

Never use decorative animation.

Animation duration:

150–250 ms.

---

# 10. Cognitive Load Constraints

Users must never process more than:

- One primary goal
- One primary CTA
- One visual hero

Everything else is secondary.

---

# 11. LLM Context Rules

Before rendering UI determine:

1. Current application mode
2. Current user intention
3. Current communication state
4. Current selection
5. Current activity

Only then choose visible components.

Never render the full workspace first.

Never render hidden engineering tools.

---

# 12. Workspace Anti-Patterns

Reject any workspace that:

- Shows every feature simultaneously
- Looks like a dashboard
- Requires scrolling before communication
- Shows configuration after connection
- Uses the right panel as permanent storage
- Forces users to mentally filter irrelevant controls

---

# 13. Workspace Review Checklist

- [ ] Current context is obvious.
- [ ] One visual hero exists.
- [ ] One primary CTA exists.
- [ ] Chat dominates the workspace.
- [ ] Right panel supports rather than competes.
- [ ] Hidden engineering features stay hidden.
- [ ] Context transitions preserve orientation.
- [ ] Empty states guide the next action.
- [ ] Communication is never interrupted by configuration.
- [ ] The workspace feels calm.

---

# Chapter 08 Acceptance Criteria

The chapter is complete when:

- Every screen can be derived from a single context.
- Every component has a predictable visibility rule.
- Context transitions never confuse users.
- LLM agents can infer the correct workspace without relying on screenshots.
