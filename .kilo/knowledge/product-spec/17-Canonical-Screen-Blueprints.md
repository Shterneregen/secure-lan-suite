
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 17 — Canonical Screen Blueprints

> Purpose
>
> This chapter defines the canonical geometry of every primary workspace.
> These blueprints are layout contracts, not visual mockups.

---

# 1. Blueprint Rules

Every blueprint specifies:

- Mandatory regions
- Optional regions
- Forbidden regions
- Relative proportions
- Visual hero
- Primary CTA
- Adaptive behavior

LLM agents should compose layouts from these blueprints before placing components.

---

# 2. Welcome Blueprint

```
+-------------------------------------------------------------------+
| Header                                                    Settings |
+----------------------+--------------------------------------------+
| Nearby / Recent      |                                            |
| Rooms                |            Welcome Hero                    |
|                      |                                            |
|                      |   [ Host Room ] [ Join Room ]              |
|                      |                                            |
+----------------------+--------------------------------------------+
```

Visual hero:
Welcome Hero

Forbidden:
Diagnostics, Runtime, Technical Networking

---

# 3. Messenger Blueprint

```
+-------------------------------------------------------------------+
| Room | Search | Status                                   Profile  |
+--------------+--------------------------------+-------------------+
| People       |                                |                   |
|              |                                |                   |
|              |        Conversation            | Context Assistant |
|              |                                |                   |
|              |                                |                   |
|              +--------------------------------+                   |
|              | Composer                                           |
+--------------+----------------------------------------------------+
```

Conversation occupies at least 60% of the workspace.

---

# 4. Selected Peer Blueprint

Right panel becomes Peer Profile.

Allowed cards:

- Identity
- Presence
- Quick Actions
- Shared Media
- Recent Files

Forbidden:

- Runtime
- Ports
- Adapters

---

# 5. Active Transfer Blueprint

Center:

Conversation + Inline Transfer Card

Right:

Transfer Details

Transfer remains embedded in chat.

---

# 6. Voice Call Blueprint

Conversation stays visible.

Top:

Compact Call Banner

Right:

Call Controls

Never replace chat with a full call dashboard.

---

# 7. Video Call Blueprint

```
+-------------------------------------------------------------------+
| Header                                                            |
+--------------+--------------------------------+-------------------+
| People       |                                |                   |
|              |        Remote Video            | Call Controls     |
|              |                                |                   |
|              +--------------------------------+-------------------+
|              | Conversation / Composer                            |
+--------------+----------------------------------------------------+
```

Video temporarily becomes the visual hero.

---

# 8. Settings Blueprint

```
+----------------------+--------------------------------------------+
| Categories           | Settings Content                           |
|                      |                                            |
|                      |                                            |
+----------------------+--------------------------------------------+
```

Settings never include conversation.

---

# 9. Diagnostics Blueprint

```
+----------------------+--------------------------------------------+
| Categories           | Health Summary                             |
|                      |                                            |
|                      | Expand → Technical Details                 |
+----------------------+--------------------------------------------+
```

Raw logs remain collapsed.

---

# 10. Responsive Variants

>=1600 px

- Three columns

1400–1599 px

- Collapse secondary cards

1200–1399 px

- Hide context panel
- Show drawer on demand

<1200 px

- Overlay navigation
- Drawer-based context

Conversation is always preserved first.

---

# 11. Region Priority

1. Conversation
2. Current Context
3. Primary Action
4. Context Assistant
5. Supporting Information

Never reverse this order.

---

# 12. Blueprint Validation Checklist

- [ ] Correct blueprint selected.
- [ ] Required regions present.
- [ ] Forbidden regions absent.
- [ ] Visual hero preserved.
- [ ] Conversation remains dominant.
- [ ] Context panel supports the current task.
- [ ] Layout adapts without changing hierarchy.

---

# Chapter 17 Acceptance Criteria

The chapter is complete when every primary application screen can be assembled
from these canonical blueprints without referencing legacy screenshots or
inventing a new layout.
