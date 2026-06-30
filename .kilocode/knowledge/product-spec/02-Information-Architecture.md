
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 02 — Information Architecture & Product Structure

> Goal:
>
> Define the logical structure of the product before any UI implementation.
> Every screen, component and workflow must originate from this architecture.

---

# 1. Core Product Objects

The application revolves around business objects, not technical objects.

## Primary objects

1. Room
2. Person (Peer)
3. Conversation
4. Message
5. File
6. Call
7. Notification

## Secondary objects

- Settings
- Diagnostics
- Advanced Connection

## Hidden implementation objects

These objects exist in the implementation but must not appear in normal UX:

- Socket
- Listener
- Port
- Adapter
- Discovery Service
- Runtime State
- Encryption Engine
- Transfer Bridge

Definition of Done:

- All primary navigation is built around business objects.

---

# 2. Application Modes

The application has six mutually exclusive modes.

## Welcome

Purpose:

Discover or create communication.

Visible:

- Host Room
- Join Room
- Nearby Rooms
- Recent Rooms

Hidden:

- Chat
- Diagnostics
- Runtime

---

## Host Setup

Purpose:

Create a room.

Contains only room creation workflow.

---

## Join Setup

Purpose:

Join an existing room.

Contains only discovery and connection workflow.

---

## Messenger

Purpose:

Communication.

This is the primary application mode.

---

## Settings

Purpose:

Persistent preferences.

Never mixed with Messenger.

---

## Diagnostics

Purpose:

Problem investigation.

Never part of the default communication flow.

Definition of Done:

Only one application mode is active at any time.

---

# 3. Navigation Hierarchy

Primary Navigation

1. Rooms
2. People
3. Conversation

Secondary Navigation

- Attach
- Search
- Context Panel

Tertiary Navigation

- Settings
- Diagnostics
- Advanced Connection

Engineering navigation must never appear before communication navigation.

---

# 4. Information Hierarchy

Priority 1

Conversation

Priority 2

Selected Person

Priority 3

Primary Actions

Priority 4

Related Information

Priority 5

History

Priority 6

Advanced

Priority 7

Diagnostics

Every screen follows this order.

---

# 5. Layout Regions

## Top Bar

Contains:

- Current room
- Current conversation
- Global status
- Search
- Settings

Never contains:

- Ports
- IP addresses
- Runtime
- Diagnostics logs

---

## Left Sidebar

Contains:

- Room
- People
- Presence
- Recent

Purpose:

Selection.

---

## Center

Contains:

- Conversation
- Messages
- Attachments
- Active call

Purpose:

Communication.

The center always receives the greatest visual weight.

---

## Right Context Panel

Contains only information relevant to the current selection.

Possible modes:

- Empty Guidance
- Peer
- Transfer
- Call
- Diagnostics
- Advanced

Purpose:

Context.

Never permanent tools.

---

# 6. Screen Transition Rules

Welcome

↓

Host

↓

Messenger

or

Welcome

↓

Join

↓

Messenger

Settings and Diagnostics are overlays or dedicated modes.

Users never return to Welcome while connected.

---

# 7. Visibility Rules

Always Visible

- Current room
- Selected conversation
- Composer
- Online people

Contextual

- Call controls
- Transfer details
- Security
- Media

Hidden by Default

- Ports
- Adapters
- Runtime
- Raw logs
- Network topology

Explicit Only

- Advanced Connection
- Diagnostics
- Technical Details

---

# 8. Product Consistency Rules

Every screen must answer:

1. Where am I?
2. Who am I communicating with?
3. What can I do next?

If any answer is unclear, redesign the screen.

---

# 9. Architecture Validation Checklist

- [ ] Navigation starts with people, not networking.
- [ ] Conversation is the visual center.
- [ ] Advanced features never interrupt communication.
- [ ] Every feature belongs to exactly one application mode.
- [ ] Every panel has one responsibility.
- [ ] Technical information is hidden until requested.
- [ ] Empty states always suggest the next action.
- [ ] Messenger mode never contains Host/Join forms.

---

# Chapter 02 Acceptance Criteria

The architecture is complete when:

- Every feature has exactly one logical location.
- No workflow mixes communication with configuration.
- Navigation can be explained in less than one minute.
- A new contributor can understand the application structure without reading implementation code.
