# SecureLanSuite Product UI Guide

This guide defines product intent and UX constraints for the primary Compose
desktop client. The implementation owns current behavior, while `README.md` and
`docs/` own current capabilities and operational facts. Preserve implemented
behavior when these sources disagree unless the task explicitly changes it.

## Product direction

SecureLanSuite should feel like a messenger for people on the same local
network. Rooms, people, conversations, files, and calls are primary. Network and
runtime details support those tasks without dominating ordinary workflows.

The interface should be calm, predictable, keyboard accessible, focused on the
current task, and honest about security boundaries and experimental features.

## Product language and disclosure

Prefer user-facing terms such as room, person or peer, conversation, message,
file transfer, call, nearby room, and connection issue.

Keep ports, IP addresses, adapters, sockets, listeners, runtimes, SDP, ICE,
stack traces, and raw protocol events in settings, advanced controls, or
recovery details where they help the user act.

Security facts must remain visible when they affect a decision. Quick Share
must explain that its browser links are temporary unauthenticated local HTTP
links intended for trusted LANs.

## Desktop structure

Use four types of surface:

1. A disconnected entry experience for hosting or joining a room.
2. A connected messenger workspace for chat, peers, transfers, and calls.
3. Modal tools for focused utilities such as Quick Share and steganography.
4. Settings that preserve the underlying workspace.

The connected workspace has these regions:

| Region | Responsibility |
| --- | --- |
| Top bar | Current context, compact status, tools, settings, theme |
| Left column | Peer discovery, presence, and selection |
| Center | Conversation, composer, inline transfer or call activity |
| Right context area | Actions and details for the current peer or activity |

The center is the primary working area. The context area supports it and should
not become a permanent inventory of unrelated features.

Preserve the current responsive contract unless the task changes it:

| Workspace width (dp) | Context behavior |
| ---: | --- |
| `>= 1600` | Full inline context area |
| `1440-1599` | Inline context with secondary cards collapsed |
| `1280-1439` | Inline context with history collapsed |
| `< 1280` | Context moves to a drawer |

When space is limited, preserve a usable conversation and composer before
secondary context. Inspect the current shell before changing breakpoints or
column widths.

## Core workflows

### Host or join

- Keep hosting and joining separate from the connected workspace.
- Prefer nearby-room discovery; place manual addresses and ports in advanced
  controls.
- Ask only for information required by the selected mode.
- After connection, make communication the obvious next action.
- Room-link paste/scan opens a confirmation draft, prefilling the password only
  from that link. Passwordless links start with an empty password. Whitespace is
  filtered from password entry. An existing room is preserved until explicitly
  left. Hosted-room Invite is a compact temporary QR/copy dialog independent of
  Discoverable; explain that its QR and link include the room password.
- Explain failures in user language and offer recovery before technical detail.

### Conversation

- Keep the selected conversation and composer easy to find.
- Preserve drafts, selection, and scroll position when adjacent UI opens.
- Give immediate send feedback and restore composer focus where appropriate.
- Use empty states to explain how to begin.

### File transfer

- Start transfers from the selected conversation or peer action.
- Show decisions and live progress near the communication context.
- Make filename, direction, progress, outcome, and recovery understandable.
- Keep advanced details in the context area when useful.
- Do not route ordinary transfers through a separate administration workspace.

### Calls

- Keep voice controls compact and chat available.
- Video may become primary content while the conversation remains reachable.
- Device selection and diagnostics must not reset an active call unexpectedly.
- Label experimental behavior honestly.

### Quick Share and steganography

- Open these tools intentionally and restore the prior context when they close.
- Explain the Quick Share trust boundary before publishing a link.
- Make validation, password failures, and output locations understandable.

### Settings

- Open settings without disconnecting, stopping hosting, or discarding drafts.
- Follow the current persistence behavior; do not add a fake Save action when
  changes apply automatically.
- Keep network details on the Network settings page.
- Label experimental and platform-dependent choices clearly.

## State and actions

- Show actions for the current selection and available capabilities.
- Do not enable peer actions without a valid target.
- Preserve room, peer, draft, scroll, transfer, and call state when dialogs,
  drawers, tools, or settings open.
- Treat errors as inline or overlay recovery states instead of replacing the
  entire product state.
- Reuse the existing Compose state owner instead of introducing a parallel
  model.
- Handle applicable loading, empty, disabled, success, error, and recovery
  states.

## Components and visual language

Reuse the current shell, components, semantic tokens, and `SecureLanIcons`.
`SecureLanTheme.kt` is authoritative for exact visual values.

- Use semantic color roles instead of new literal colors.
- Use spacing to group related content and elevation only to clarify structure.
- Use one consistent icon for each concept and text with important actions.
- Keep status indicators compact enough that they do not compete with content.
- Give reusable components one responsibility and meaningful states.
- Keep one-off elements local when abstraction would not provide real reuse.

## Interaction and accessibility

- Provide visible feedback for applicable hover, pressed, focus, disabled,
  loading, success, and error states.
- Keep keyboard focus visible and tab order logical.
- Label icon-only controls and exclude decorative icons from semantics.
- Let Escape close the active dismissible overlay where practical.
- Preserve layout while loading when possible.
- Use motion to explain state changes, follow existing tokens, and respect
  reduced-motion behavior.
- Do not claim screen-reader or high-contrast support without testing it.

## Patterns to avoid

Reject or simplify UI that:

- resembles a network administration dashboard;
- gives unrelated panels equal visual weight or keeps every tool visible;
- exposes network and runtime internals in normal communication;
- mixes disconnected Host or Join forms into an active workspace;
- duplicates the same primary action across several regions;
- loses communication context when a temporary surface opens;
- copies legacy form-heavy layouts;
- adds a destination, shortcut, or capability that is not implemented.

Multiple actions are valid when the workflow needs them. Establish hierarchy
through wording, placement, and treatment rather than an arbitrary action count.
Review UI against concrete behavior and these constraints; do not assign a
numeric product score.
