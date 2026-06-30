
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 11 — Decision Matrices

> Purpose
>
> This chapter removes ambiguity from UI generation.
> AI agents and developers must derive layouts from decision matrices instead of personal preference.

---

# 1. Decision Algorithm

Before rendering any screen, determine in order:

1. Application Mode
2. Primary User Goal
3. Current Context
4. Selected Object
5. Active Activity
6. Screen Width
7. Available Capabilities

Only after all seven inputs are known may the UI be composed.

---

# 2. Context → Layout Matrix

| Context | Left | Center | Right |
|---------|------|--------|--------|
| Welcome | Recent/Nearby | Welcome | Hidden |
| Room | Members | Room Chat | Room Summary |
| Peer | People | Conversation | Peer Profile |
| Transfer | People | Conversation + Transfer | Transfer Details |
| Voice Call | People | Conversation | Call Controls |
| Video Call | People | Video Stage | Call Controls |
| Settings | Hidden | Settings | Hidden |
| Diagnostics | Collapsed | Diagnostics | Issue Details |

Rule: Never invent additional columns.

---

# 3. Context → Primary Hero Matrix

| Context | Visual Hero |
|----------|-------------|
| Welcome | Primary CTA |
| Room | Conversation |
| Peer | Conversation |
| Transfer | Transfer Card |
| Voice | Call Banner |
| Video | Remote Video |
| Settings | Settings Content |
| Diagnostics | Health Summary |

Exactly one hero per context.

---

# 4. Context → Primary CTA Matrix

| Context | Primary CTA |
|----------|-------------|
| Welcome | Host Room |
| Join | Join Room |
| Room | Invite |
| Peer | Send Message |
| Transfer | Accept/Save |
| Voice | Answer/End |
| Video | Answer/End |
| Settings | Save |
| Diagnostics | Open Details |

No screen may expose more than one primary CTA.

---

# 5. Context → Visible Components

Always visible:
- Header
- Current Room
- Conversation (except Welcome)

Conditional:
- Composer
- Attach Menu
- Context Panel
- Call Banner
- Transfer Card

Hidden by default:
- Ports
- IP addresses
- Adapters
- Runtime
- Raw logs

---

# 6. Width Decision Matrix

| Width | Behaviour |
|-------:|-----------|
| >=1600 | Three columns |
| 1400-1599 | Collapse secondary cards |
| 1200-1399 | Collapse right panel |
| <1200 | Right panel becomes drawer |

Conversation width is preserved first.

---

# 7. Empty State Matrix

| Context | Empty State |
|----------|-------------|
| Welcome | No nearby rooms |
| Room | Waiting for people |
| Peer | Start the conversation |
| Transfer | No transfers |
| Diagnostics | No issues detected |

Every empty state includes a next action.

---

# 8. Error Matrix

Every error contains:

- Human explanation
- Recovery action
- Advanced details
- Diagnostics link

Never display raw implementation first.

---

# 9. Keyboard Matrix

| Shortcut | Action |
|-----------|--------|
| Ctrl+K | Command Palette |
| Ctrl+N | Host Room |
| Ctrl+J | Join Room |
| Ctrl+, | Settings |
| Ctrl+Shift+D | Diagnostics |
| Esc | Close overlay |

---

# 10. AI Self-Validation Matrix

Before finishing implementation verify:

- Correct context selected.
- Correct hero selected.
- Correct CTA selected.
- Correct components visible.
- Engineering UI hidden.
- Chat remains dominant.
- Layout matches width.
- Motion preserves context.

Reject implementation if any answer is negative.

---

# Chapter 11 Acceptance Criteria

The chapter is complete when two independent AI agents given the same application state produce substantially the same layout and interaction model.
