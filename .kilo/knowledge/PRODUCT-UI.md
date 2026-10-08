# SecureLanSuite Product UI Guide

This document defines the product direction for the primary Compose desktop UI.
It replaces the former multi-chapter Product Specification.

Use this guide for product intent and UX constraints. Use the current code and
public documentation for implemented capabilities and technical facts. If they
disagree, do not silently implement an aspirational feature: preserve current
behavior unless the task explicitly changes it, and update this guide when the
product decision changes.

## Product direction

SecureLanSuite is a local-network communication application for desktop and
Android. It lets people discover one another, create or join password-protected
rooms, exchange encrypted messages and files, and use voice or experimental
video without a cloud service.

The desktop application should feel like a messenger. Users should primarily
think about rooms, people, conversations, files, and calls. Network and runtime
details support those tasks but should not dominate ordinary workflows.

The interface should be:

- calm and predictable;
- clear without separate instructions;
- focused on the current task;
- usable with keyboard and mouse;
- honest about security boundaries and experimental features.

## Sources of truth

- This guide owns desktop product intent and UX constraints.
- `apps/desktop-client` owns current desktop behavior and UI implementation.
- `README.md` and `docs/` own current feature, build, packaging, and operational
  documentation.
- Reusable modules own protocol and domain behavior.
- Android UI decisions belong to `apps/android-client` and its documentation.

Do not treat planned UI from old screenshots, roadmap phases, or the deprecated
JavaFX client as current behavior.

## Product language and disclosure

Use user-facing terms such as:

- room;
- person or peer;
- conversation and message;
- file transfer;
- voice call and video call;
- nearby rooms;
- connection issue.

Keep these details out of the normal communication flow unless the user asks
for advanced settings or needs them to recover from a problem:

- ports and IP addresses;
- network adapters and discovery internals;
- sockets, listeners, runtimes, SDP, ICE, and stack traces;
- raw protocol or diagnostic events.

Security-relevant facts must remain visible when they affect a decision. In
particular, Quick Share must clearly say that browser links are temporary,
unauthenticated local HTTP links intended for trusted LANs.

## Current desktop experience

The current desktop application has four practical experiences:

1. A disconnected entry experience for hosting or joining a room.
2. A connected messenger workspace for chat, peers, transfers, and calls.
3. Modal tools for Quick Share and steganography.
4. A settings dialog that preserves the underlying workspace.

Desktop diagnostics are written to logs. There is no required standalone
Diagnostics screen. User-facing failures should provide a short explanation and
a recovery action; technical details may point to logs when useful.

Voice is the primary realtime media flow. Camera preview and one-to-one video
exist but remain experimental. Do not present video as equally stable unless
runtime validation justifies that change.

## Workspace layout

The connected workspace has these regions:

| Region | Responsibility |
| --- | --- |
| Top bar | Current context, compact status, tools, settings, theme |
| Left column | Peer discovery, presence, and selection |
| Center | Conversation, composer, inline transfer or call activity |
| Right context area | Actions and details for the current peer or activity |

The center is the primary working area. The right side supports it and should
not become a permanent inventory of every feature. Quick Share and
steganography are opened intentionally from the tools menu or relevant action.

Preserve the current responsive contract unless the task changes it:

| Workspace width (dp) | Context behavior |
| ---: | --- |
| `>= 1600` | Full inline context area |
| `1440-1599` | Inline context with secondary cards collapsed |
| `1280-1439` | Inline context with history collapsed |
| `< 1280` | Context moves to a drawer |

When space is limited, preserve a usable conversation and composer before
secondary context. Check the existing shell before changing column widths or
breakpoints.

## Core workflows

### Host or join

- Keep hosting and joining visually separate from the connected workspace.
- Prefer nearby-room discovery for joining; manual addresses and ports belong
  in advanced controls.
- Ask only for information required by the selected mode.
- After a successful connection, make communication the obvious next step.
- Explain failures in user language and offer retry or another room before
  exposing details.

### Conversation

- Keep the selected conversation and composer easy to find.
- Preserve drafts, selection, and scroll position when adjacent UI opens.
- Sending a message should provide immediate feedback and restore composer
  focus where appropriate.
- Empty states should explain how to begin rather than filling space with
  disabled controls.

### File transfer

- Start a transfer from the selected conversation or peer action.
- Show incoming decisions and live progress near the communication context.
- Keep filename, direction, progress, outcome, and available recovery actions
  understandable.
- Advanced transfer details may live in the context area.
- Do not route ordinary transfers through a separate administration workspace.

### Calls

- Voice controls should remain compact and keep chat available.
- During video, video may become the primary content while conversation remains
  reachable.
- Device selection and diagnostics must not unexpectedly reset an active call.
- Label experimental video honestly.

### Quick Share and steganography

- These are explicit tools, not permanent workspace columns.
- Open them on demand and return to the previous communication context on close.
- Explain the Quick Share trust boundary before publishing links.
- Keep steganography validation, password failures, and output locations clear.

### Settings

- Open settings without disconnecting, stopping hosting, or discarding drafts.
- Save changes according to the current settings behavior; do not add a fake
  Save action when changes are applied automatically.
- Network details are appropriate on the Network settings page.
- Keep experimental and platform-dependent choices clearly labelled.

## Context and state rules

- Show actions for the current selection and available capabilities.
- Do not enable peer actions without a valid target.
- A transfer or call may modify the workspace without erasing the conversation.
- Opening a dialog or drawer must preserve the underlying room, peer, draft,
  transfer, and call state.
- Errors are overlays or inline recovery states, not replacements for the whole
  product state.
- Avoid introducing parallel state models when an existing Compose state type
  already owns the behavior.

## Components and visual language

Reuse the existing components and semantic tokens before creating new ones.
The current token definitions live in `SecureLanTheme.kt`; the implementation is
authoritative for exact colors, spacing, typography, radius, elevation, density,
and motion values.

Use:

- semantic color roles rather than new literal colors;
- spacing to group related content;
- borders and elevation only when they clarify structure or interaction;
- one consistent icon for each product concept through `SecureLanIcons`;
- clear text with icons for important actions;
- compact status indicators that do not compete with content.

A reusable component should have one clear responsibility and meaningful states.
A small local composable is acceptable when reuse would add unnecessary
abstraction; it does not require a specification change.

## Interaction and accessibility

- Provide visible hover, pressed, focus, disabled, loading, success, and error
  feedback where those states apply.
- Keep keyboard focus visible and use a logical tab order.
- Add semantic labels to icon-only controls; decorative icons should not be
  announced separately from adjacent text.
- Escape should close the active dismissible overlay where practical.
- Preserve layout while loading when possible.
- Use animation to explain a state change, generally within the existing motion
  tokens, and honor reduced-motion behavior.
- Do not claim screen-reader or high-contrast support without testing it.

## Patterns to avoid

Reject or simplify UI that:

- resembles a network administration dashboard;
- gives unrelated panels equal visual weight;
- exposes ports, adapters, runtimes, or raw logs in normal communication;
- keeps every tool permanently visible;
- mixes disconnected Host/Join forms into an active messenger workspace;
- duplicates the same primary action in several regions;
- loses the room, selected peer, draft, transfer, or call when opening settings
  or another temporary surface;
- copies the deprecated JavaFX layout;
- adds an unimplemented navigation destination or shortcut only because an old
  specification mentioned it.

Multiple actions are valid when the workflow needs them. Establish hierarchy
through wording, placement, and button treatment instead of enforcing an
arbitrary count of one action per screen.

Review UI against concrete behavior and the constraints above; do not assign a
numeric product score.
