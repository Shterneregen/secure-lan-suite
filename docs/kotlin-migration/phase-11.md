# Phase 11 — UI/UX Refinement (Context Panel Extension)

## 12A. Context Panel UX Architecture

**Goal**

The right panel must become a contextual assistant instead of a permanent toolbox.
It should display only information relevant to the current task and hide unrelated functionality.

---

## 12A. Context-first behavior

- [ ] The right panel always represents the current context.
- [ ] Only one primary context exists at any moment.
- [ ] Never display every feature simultaneously.
- [ ] Every visible section answers:
    - What is happening?
    - What can I do next?
- [ ] Hide unrelated functionality.
- [ ] Empty states guide the next action.
- [ ] Hide technical controls until explicitly requested.
- [ ] Normal workflow should require little or no scrolling.

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

- [ ] Maximum 6 visible cards.
- [ ] Maximum 3 primary buttons.
- [ ] Maximum 2 nesting levels.
- [ ] No nested cards.
- [ ] No endless vertical forms.
- [ ] Avoid full-height technical sections.
- [ ] Minimize scrolling.

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

- [ ] Never expose engineering UI by default.
- [ ] Show advanced controls only from More, Settings or Diagnostics.
- [ ] Hide Runtime, Quick Share, Steganography, Audio Devices and Camera Devices until requested.

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

- [ ] >=1600 px — Full panel.
- [ ] 1400–1600 px — Collapse secondary cards.
- [ ] 1200–1400 px — Collapse history.
- [ ] <1200 px — Drawer mode.

**Definition of Done**

The right panel never dominates the messenger.
