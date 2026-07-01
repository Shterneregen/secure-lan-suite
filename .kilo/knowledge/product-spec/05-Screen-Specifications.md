
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 05 — Screen Specifications

> This chapter defines every primary product screen.
> Each screen is described as a product experience rather than a UI mock-up.
> Designers and LLM agents must implement behavior from these specifications instead of interpreting screenshots.

---

# Screen Specification Template

Every screen must define:

- Goal
- User Intent
- Entry Points
- Exit Points
- Layout
- Visible Information
- Hidden Information
- Primary CTA
- Secondary CTA
- Empty States
- Error States
- Context Changes
- Motion
- Acceptance Criteria

---

# 5.1 Welcome Screen

## Goal

Help a first-time user understand the product within five seconds.

## User Intent

"I want to start communicating."

## Entry

- Application launch
- Manual disconnect

## Layout

Hero area

↓

Nearby / Recent rooms

↓

Primary actions

## Visible

- Product identity
- Nearby rooms
- Recent rooms
- Host Room
- Join Room

## Hidden

- Ports
- IP addresses
- Runtime
- Diagnostics
- Network adapters

## Primary CTA

Host secure room

## Secondary CTA

Join nearby room

## Empty State

"No nearby rooms yet."

## Success

User starts hosting or joining in a single interaction.

---

# 5.2 Host Setup

## Goal

Create a room with minimum cognitive effort.

## Required Fields

- Room name
- Display name
- Password (optional)

## Progressive Disclosure

Advanced Hosting contains:

- Adapter
- Discovery
- Ports
- Manual IP

Never expose these by default.

## Success

Transition directly into Messenger.

Host setup disappears completely.

---

# 5.3 Join Setup

## Goal

Join an existing room.

Priority:

1. Nearby discovery
2. Recent rooms
3. Manual connection

Manual connection belongs inside Advanced.

## Failure Recovery

Display recovery cards instead of logs.

Examples:

Couldn't reach this room.

Try Again

Change Room

Diagnostics

---

# 5.4 Messenger

## Goal

Become the permanent working environment.

## Visual Hero

Conversation.

## Layout

Left Sidebar

Conversation

Context Panel

## Never Show

Host Setup

Join Setup

Technical networking

## Primary Actions

Message

Attach

Call

Video

---

# 5.5 Conversation

## Goal

Communication without distractions.

## Components

Conversation header

Timeline

Composer

## Timeline

Messages

Transfers

System events

## Composer

Attach

Input

Send

Disabled only when communication is impossible.

---

# 5.6 Peer Screen

## Goal

Present a person rather than a device.

Visible

- Avatar
- Name
- Presence
- Trust
- Recent files
- Shared media

Hidden

- Socket
- Port
- Runtime

Quick Actions

- Message
- File
- Voice
- Video

---

# 5.7 File Transfer

Transfer lives inside the conversation.

Never create a dedicated transfer workspace.

Transfer Card

- Filename
- Progress
- ETA
- Retry
- Cancel
- Open

History appears inside Context Panel.

---

# 5.8 Voice Call

Conversation remains visible.

Voice controls appear in:

Header

Context Panel

Compact Call Banner

Never replace chat with configuration.

---

# 5.9 Video Call

Video becomes the visual hero.

Remote participant

↓

Local preview

↓

Controls

Conversation remains accessible.

---

# 5.10 Settings

Settings are persistent preferences.

Categories

- Profile
- Appearance
- Notifications
- Files
- Calls
- Privacy
- Network
- Advanced

Opening Settings never destroys current communication context.

---

# 5.11 Diagnostics

Diagnostics explain.

Diagnostics never intimidate.

First screen:

Health Summary

Everything looks good.

or

Connection issue detected.

Technical logs remain collapsed.

---

# Global Screen Rules

Every screen has:

Exactly one hero.

Exactly one primary action.

One obvious next step.

Never display unrelated engineering features.

Every empty state must explain what to do next.

---

# LLM Validation Checklist

Before implementation verify:

- [ ] Is the goal of the screen obvious?
- [ ] Is there only one visual hero?
- [ ] Are engineering concepts hidden?
- [ ] Does the layout prioritize communication?
- [ ] Is there exactly one primary CTA?
- [ ] Does the empty state guide the user?
- [ ] Can the user recover from errors?
- [ ] Does the screen fit naturally into the navigation model?

Reject the implementation if any answer is "No".

---

# Chapter 05 Acceptance Criteria

The chapter is complete when:

- Every product screen has a single responsibility.
- Every screen can be implemented without referring to the legacy JavaFX UI.
- Every LLM can determine what should and should not appear on a screen without guessing.
