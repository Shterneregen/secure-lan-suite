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

- Keep hosting and joining separate from the connected workspace. The entry
  shows Host / Join choices before revealing the existing connection form;
  returning to the choices preserves the form draft. Nearby rooms update
  automatically; recent endpoints open a connection draft, never an implicit join.
- The active shared-room header offers Invite only to hosts. Room details exposes
  immediate discoverability and Stop hosting for hosts, or Leave room for joined
  participants. Changing credentials or ports requires leaving/recreating the room.
- Prefer nearby-room discovery; place manual addresses and ports in advanced
  controls.
- Ask only for information required by the selected mode.
- Host / Join forms use labelled credential fields and a single primary action.
  Join keeps the selected room and endpoint visible before connecting. Change
  room reveals an editable address without discarding credentials; endpoint edits
  clear invitation provenance and any discovery route. Import provenance does
  not mean the room owner has been authenticated. Manual addresses remain outside
  Advanced; ports remain inside it, with inline validation and automatic reveal
  of invalid values. On narrow windows fields stack and the form scrolls.
- Hosting visibility means advertising the room on the LAN, not selecting trusted
  devices. Hidden rooms can still be joined by address or invitation. Show room
  exit controls only for an active room. Keep repeated submission and form edits
  disabled during a connection operation; failures retain the draft for retry.
- After connection, make communication the obvious next action.
- Room-link paste/scan opens a confirmation draft, prefilling the password only
  from that link. Passwordless links start with an empty password. Whitespace is
  filtered from password entry. An existing room is preserved until explicitly
  left. Hosted-room Invite is a compact temporary QR/copy dialog independent of
  Discoverable; explain that its QR and link include the room password.
- Desktop link import previews actual URI fields automatically and keeps Cancel
  and Use for Join outside its scrollable content. Conceal the raw link initially
  with an explicit show/hide action. Preview hints do not authenticate the sender;
  changing the invitation or chosen address requires fresh external confirmation.
- Explain failures in user language and offer recovery before technical detail.

### Conversation

- Keep the shared room chat and composer easy to find. Selecting a peer targets
  files and calls; messages remain visible to the whole room. Do not imply private
  messaging through a per-person Message action.
- Preserve drafts, selection, and scroll position when adjacent UI opens.
- Give immediate send feedback and restore composer focus where appropriate.
- Hide leading service markers such as `[system]` in transcript events while
  preserving literal prefixes in user messages and file transfer direction.
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

- Keep Quick Share in the top bar. The adjacent Tools menu opens LAN Tools or
  Steganography; settings and theme remain separate controls.
- Open these tools intentionally and restore the prior context when they close.
- Explain the Quick Share trust boundary before publishing a link.
- Make validation, password failures, and output locations understandable.

### LAN Tools

LAN Tools uses a separate wide, resizable dialog, available without a room.
Devices separates saved settings from observed devices, with search, source
filters, details and manual Refresh/Cancel. Discovery is an explicit bounded
DNS-SD browse, not a complete LAN inventory or an online-status check. Mark
completed/cancelled results as historical and limited results as partial. Do not
merge user settings with observations automatically or treat service declarations
as authenticated peers. Unknown hostname/hardware type stays Unknown. Saving an
observation opens an editable form; MAC/WoL settings are never inferred.

Wake-on-LAN is a saved-device action. Device Diagnostics pre-fills an exact
endpoint but requires Check TCP before connecting. Keep last-check results tied
to their submitted endpoint/time rather than a global device status. At short
dialog heights the browse controls scroll with the devices so actions stay
reachable. Diagnostics offers local IPv4 information, explicit TCP checks,
system DNS lookup and a gateway TCP check where OS metadata is available. Keep
each result tied to its submitted endpoint. Distinguish TCP connection time,
name resolution and ICMP RTT; a refused port does not establish device state.
Show unavailable gateway/DNS metadata and ICMP honestly. Do not add fake
discovery or Ping. IP/MAC hints confer no room authentication or trusted identity.

Network refresh reads local information only. Gateway checks require an
OS-reported gateway and an explicit TCP port; never guess addresses or scan
ports. DNS uses system resolution/cache/default routing independently of TCP
source selection. Each check has a timeout, running state and cancellation.
Keep settings and results scrollable on narrow windows, with labelled inputs
and announced status changes.

Require confirmation before deleting settings. Preserve unfinished forms and
selection on close; Cancel editing explicitly discards a form. Select a source
interface explicitly when ambiguous; default-route hints are recommendations
only. Report packet submission without claiming delivery or physical wake-up.
Closing cancels bounded network jobs and returns focus to Tools. Escape first
dismisses a pending delete confirmation, then the containing utility. Keep the
messenger mounted and its services running underneath.

### Settings

- Open settings without disconnecting, stopping hosting, or discarding drafts.
- Use horizontal categories with keyboard navigation and overflow at narrow
  widths. Supplementary explanations may be omitted before constraining controls.
- Follow the current persistence behavior; show saving/error/retry honestly and
  do not add a Save action when changes apply automatically.
- Network distinguishes immediate hosted-room visibility (also persisted for
  future rooms) from port defaults for future connections.
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
