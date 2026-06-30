
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 15 — Approved Design Patterns Catalog

> Purpose
>
> This catalog defines the only approved UI patterns for SecureLanSuite.
> New screens should be composed from these patterns rather than invented from scratch.

---

# Pattern Structure

Each pattern defines:

- Intent
- When to use
- When NOT to use
- Composition
- Behaviour
- Accessibility
- LLM Rules
- Acceptance Criteria

---

# DP-01 Welcome Workspace

## Intent

Help users enter communication with minimum effort.

## Use

- First launch
- Disconnected state

## Do Not Use

- Connected messenger

## Composition

- Product hero
- Nearby rooms
- Recent rooms
- Host CTA
- Join CTA

LLM Rule:

Never add diagnostics, networking or advanced controls.

---

# DP-02 Messenger Workspace

## Intent

Support uninterrupted communication.

## Composition

Left:
People

Center:
Conversation

Right:
Context Assistant

Rules

- Conversation dominates.
- Composer is always visible.
- One visual hero.

---

# DP-03 Peer Profile

## Intent

Present a person.

Contains:

- Avatar
- Presence
- Trust
- Quick actions
- Shared media

Never display implementation details.

---

# DP-04 Context Assistant

## Intent

Explain the current activity.

Contexts:

- Peer
- Transfer
- Call
- Diagnostics

Rules

- Maximum six cards.
- One primary card.
- Collapse secondary information.

---

# DP-05 Recovery Card

## Intent

Recover from problems.

Contains:

- Human explanation
- Recommended action
- Advanced details (optional)

Never expose raw logs first.

---

# DP-06 Empty Workspace

## Intent

Guide rather than merely inform.

Every empty state contains:

- Situation
- Explanation
- Primary next action

---

# DP-07 Attachment Flow

Intent:

Send content without leaving the conversation.

Flow:

Attach

↓

Preview

↓

Transfer

↓

Completion

Rules

Transfer remains embedded in chat.

---

# DP-08 Call Banner

Intent

Expose call status without replacing conversation.

Contains:

- Duration
- Mute
- End call

Conversation remains visible.

---

# DP-09 Diagnostics Pattern

Intent

Support troubleshooting.

Default screen:

Health Summary

↓

Suggested Fix

↓

Technical Details

Raw logs are never first.

---

# DP-10 Progressive Disclosure

Intent

Reveal complexity only when requested.

Sequence

Summary

↓

Details

↓

Advanced

↓

Engineering

Never skip levels.

---

# Pattern Selection Matrix

| Situation | Pattern |
|-----------|---------|
| First launch | Welcome Workspace |
| Connected | Messenger Workspace |
| Selected peer | Peer Profile |
| Active transfer | Attachment Flow |
| Voice call | Call Banner |
| Empty room | Empty Workspace |
| Error | Recovery Card |
| Diagnostics | Diagnostics Pattern |

---

# Composition Rules

Approved screens should reuse patterns.

Avoid creating custom layouts.

Compose screens by combining:

- One workspace
- Zero or one assistant
- Zero or one recovery pattern
- Zero or one call pattern

---

# LLM Pattern Selection Algorithm

1. Detect application mode.
2. Detect current context.
3. Select approved pattern.
4. Apply Design System.
5. Apply Decision Matrix.
6. Validate against Forbidden Patterns.
7. Run Product QA.

Never skip steps.

---

# Review Checklist

- [ ] Existing pattern reused.
- [ ] No custom layout invented.
- [ ] Correct pattern selected.
- [ ] Pattern matches context.
- [ ] Accessibility preserved.
- [ ] Communication remains primary.

---

# Chapter 15 Acceptance Criteria

The chapter is complete when new product screens can be assembled by combining approved patterns without inventing new interaction models.
