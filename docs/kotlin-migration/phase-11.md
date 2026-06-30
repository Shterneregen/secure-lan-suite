# Phase 11 — UI/UX Refinement (Context Panel Extension)

## 12A. Context Panel UX Architecture

**Goal**

The right panel must become a contextual assistant instead of a permanent toolbox.
It should display only information relevant to the current task and hide unrelated functionality.

---

## 12A. Context-first behavior

- [x] The right panel always represents the current context.
- [x] Only one primary context exists at any moment.
- [x] Never display every feature simultaneously.
- [x] Every visible section answers:
    - What is happening?
    - What can I do next?
- [x] Hide unrelated functionality.
- [ ] Empty states guide the next action.
- [x] Hide technical controls until explicitly requested.
- [ ] Normal workflow should require little or no scrolling.

**Implementation note — 2026-06-30**

The Compose context assistant now derives room, peer, transfer, call, and diagnostics cards from deterministic context-panel state. Room and peer contexts no longer expose Quick Share as a default card; Quick Share, Runtime, raw logs, device setup, and detailed diagnostics are hidden until the user enters a relevant context or explicit advanced/diagnostics flow. Deterministic tests cover the context-assistant contract and the transfer/call priority order.

The connection hub baseline was also re-aligned with Phase 11 wording: Host/Join setup uses task-oriented labels such as "Host a secure room", "Host secure room", "Join nearby room", and "Visible to nearby devices" instead of the older generic "Room connection" / "Host Room" / "Join Room" copy. The full deterministic `ComposeShellMetadataTest` class is green after this alignment.

**Definition of Done**

- The panel behaves like contextual assistance rather than an engineering toolbox.

---

## 12B. Panel hierarchy

Priority:

1. Current context
2. Primary actions
3. Related information
4. History
5. Advanced
6. Diagnostics

Never violate this order.

---

## 12C. Maximum visual complexity

- [x] Maximum 6 visible cards.
- [x] Maximum 3 primary buttons.
- [ ] Maximum 2 nesting levels.
- [x] No nested cards.
- [ ] No endless vertical forms.
- [x] Avoid full-height technical sections.
- [ ] Minimize scrolling.

**Implementation note — 2026-06-30**

`ComposeContextPanelState` enforces the visible-card and primary-button caps for room, peer, transfer, call, and diagnostics contexts. Room context currently renders two cards; peer context renders five cards after Quick Share was moved behind progressive disclosure. Remaining work is runtime review of scrolling and nesting behavior in the live Compose shell at common desktop sizes.

---

## 12D. Unified card system

Each card contains:

- Title
- Optional badge
- Content
- Optional primary action
- Overflow menu

Rules:

- [ ] Consistent padding.
- [ ] Consistent spacing.
- [ ] Consistent typography.
- [ ] Consistent border radius.
- [ ] No custom layouts per feature.

---

## 12E. Context modes

### No peer selected

Visible:

- Guidance
- Room status

Hidden:

- Transfers
- Calls
- Quick Share
- Audio
- Camera
- Steganography
- Runtime
- Detailed diagnostics

### Peer selected

Visible:

- Peer card
- Quick actions
- Recent files
- Media
- Security

Hidden:

- Quick Share
- Device setup
- Runtime
- Validation
- Port information
- Raw logs

### Active transfer

Visible:

- Transfer card
- Peer
- Quick actions

Transfer card becomes the first card.

### Voice call

Visible:

- Call controls
- Peer
- Quick actions

Hide unrelated modules.

### Video call

Visible:

- Video controls
- Peer
- Security

Hide unrelated modules.

### Diagnostics

Visible:

- Health summary
- Connection
- Discovery
- Files
- Calls

Raw logs remain collapsed behind 'Show technical log'.

---

## 12F. Progressive disclosure

- [x] Never expose engineering UI by default.
- [ ] Show advanced controls only from More, Settings or Diagnostics.
- [x] Hide Runtime, Quick Share, Steganography, Audio Devices and Camera Devices until requested.

**Implementation note — 2026-06-30**

Room and peer Context Assistant states now hide Quick Share by default and replace the previous explicit hidden-feature list in the visible UI with human-readable disclosure copy. Runtime and raw technical details remain limited to Diagnostics or Advanced flows.

---

## 12G. Diagnostics redesign

Diagnostics must explain problems instead of exposing implementation.

Each issue includes:

- What happened
- Why it happened
- How to fix it
- Show details

Raw logs stay hidden.

---

## 12H. Responsive behavior

- [x] >=1600 px — Full panel.
- [x] 1400–1600 px — Collapse secondary cards.
- [x] 1200–1400 px — Collapse history.
- [x] <1200 px — Drawer mode.

**Implementation note — 2026-06-30**

Responsive Context Assistant metadata now maps desktop width to full panel, collapsed-secondary, collapsed-history, and drawer states. The Compose workspace consumes that state so the inline right panel is shown only when appropriate, secondary/history cards collapse before the conversation loses priority, and the conversation remains the primary surface.

The `<1200 px` drawer interaction is now implemented with a narrow-width Context entry point, overlay drawer surface, human-readable drawer labels, screen-reader descriptions, outside-click dismissal, and Escape-to-close behavior. Follow-up accessibility hardening should still runtime-check focus restoration and tab order in the live Compose shell.

**Definition of Done**

The right panel never dominates the messenger.
