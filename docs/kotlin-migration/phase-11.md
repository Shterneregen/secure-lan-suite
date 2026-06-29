# Phase 11 — SecureLanSuite Chat UI/UX Redesign Checklist

> Phase goal: rebuild SecureLanSuite Chat from a technical control panel into a modern desktop messenger for secure local communication.
>
> Core principle: the user sees people, rooms, chat, files, and calls. IP addresses, ports, adapters, listener state, and raw logs appear only in Advanced / Diagnostics.

---

## 0. Phase rules

- [ ] 0.1. Do not do a cosmetic redesign of the existing screen.
- [ ] 0.2. Do not move the old `Room connection + Peers + Chat + Actions` structure into a new visual shell.
- [ ] 0.3. Do not show the top block with Host / Join / Password / Hidden / Advanced after connection.
- [ ] 0.4. Do not show the connection flow and the communication flow at the same time.
- [ ] 0.5. Do not show IP addresses, ports, adapters, server state, listener state, or raw diagnostics in the main interface.
- [ ] 0.6. Do not show Voice / Video / Transfer controls until a peer is selected or an active call/transfer exists.
- [ ] 0.7. Do not use a huge centered card as the only desktop layout.
- [ ] 0.8. Do not make a dashboard. The interface must feel like a messenger.
- [ ] 0.9. All changes in this phase are limited to the presentation layer. Do not break protocols, networking, encryption, discovery, transfer bridges, or the JavaFX fallback.
- [ ] 0.10. Before every sub-step, check whether the next user action became easier to understand.

---

## 1. Preparation and checkpoint

- [ ] 1.1. Save the current `phase-11.md` as `phase-11-ux-architecture.md` or `phase-11-old.md`.
- [ ] 1.2. Create a new `phase-11.md` based on this checklist.
- [ ] 1.3. Create a separate branch: `phase-11-product-ui-redesign`.
- [ ] 1.4. Take screenshots of the current states: first launch, host setup, join setup, waiting, connected peer, no peer, active transfer, diagnostics.
- [ ] 1.5. Create the folder `docs/design/phase-11/`.
- [ ] 1.6. Put the old screenshots there as the baseline.
- [ ] 1.7. Define target window widths for review: 1280, 1440, 1600, 1920 px.
- [ ] 1.8. Define the minimum supported width: 900 or 1024 px.
- [ ] 1.9. Lock the direction: the main working layout is desktop-first, not mobile-first.
- [ ] 1.10. Add this checklist to the PR description so every item can be checked off separately.

**Definition of Done:** branch, baseline screenshots, new checklist, and clear rollback path exist.

---

## 2. Product screen model

- [x] 2.1. Introduce `AppMode`.
- [x] 2.2. Add `Welcome` mode.
- [x] 2.3. Add `HostSetup` mode.
- [x] 2.4. Add `JoinSetup` mode.
- [x] 2.5. Add `Messenger` mode.
- [x] 2.6. Add `Settings` mode.
- [x] 2.7. Add `Diagnostics` mode.
- [x] 2.8. Remove permanent Host/Join controls from the main layout.
- [x] 2.9. Make `HostSetup` and `JoinSetup` mutually exclusive screens.
- [x] 2.10. After successful host/join, the app must switch to `Messenger`, not expand a connection panel.
- [x] 2.11. Introduce `RoomState`: `Offline`, `Hosting`, `WaitingForPeers`, `Connected`, `Issue`.
- [x] 2.12. Introduce `SelectionState`: `None`, `RoomConversation`, `Peer`, `Transfer`, `Call`.
- [x] 2.13. Introduce `RightPanelMode`: `Hidden`, `RoomInfo`, `PeerInfo`, `Transfers`, `Call`, `Diagnostics`, `AdvancedConnection`.
- [x] 2.14. Verify that states are not tied to the JavaFX parity layout.
- [x] 2.15. Verify that old services remain data sources but do not dictate the UI structure.

**Definition of Done:** the application state describes user modes, not technical panels.

---

## 3. Design tokens

- [x] 3.1. Create `SecureLanTheme` or update the existing theme layer.
- [x] 3.2. Add tokens for colors, spacing, radius, typography, elevation, and borders.
- [x] 3.3. Main background: deep graphite/navy, almost black, but not pure `#000000`.
- [x] 3.4. Surface level 1: main application layer.
- [x] 3.5. Surface level 2: side panels and header.
- [x] 3.6. Surface level 3: cards, popovers, modals.
- [x] 3.7. Accent: restrained blue/cyan, not neon.
- [x] 3.8. Success: calm green.
- [x] 3.9. Warning: amber.
- [x] 3.10. Error: muted red, not aggressive bright red.
- [x] 3.11. Text primary: high contrast.
- [x] 3.12. Text secondary: muted but readable.
- [x] 3.13. Text tertiary: for timestamps, hints, metadata.
- [x] 3.14. Border subtle: barely visible.
- [x] 3.15. Border focus: accent blue.
- [x] 3.16. Radius small: 8 px.
- [x] 3.17. Radius medium: 12 px.
- [x] 3.18. Radius large: 18–24 px.
- [x] 3.19. Spacing scale: 4, 8, 12, 16, 20, 24, 32, 40.
- [x] 3.20. Typography: Segoe UI Variable / system default; fallback without downloading fonts.
- [x] 3.21. Title sizes: 24–32 px only for welcome/empty states.
- [x] 3.22. Body sizes: 13–15 px for desktop density.
- [x] 3.23. Button height: 36–40 px for compact desktop.
- [x] 3.24. Input height: 38–42 px.
- [x] 3.25. Sidebar row height: 48–60 px.
- [x] 3.26. Composer height: minimum 52 px.
- [ ] 3.27. Remove heavy borders around every block.
- [ ] 3.28. Use separation through background, spacing, and subtle dividers.
- [ ] 3.29. Add visible keyboard focus for all interactive elements.
- [ ] 3.30. Check contrast in dark theme.

**Definition of Done:** the new UI can be built from tokens, not random colors and sizes.

---

## 4. App shell

- [x] 4.1. Create a new `SecureLanAppShell`.
- [x] 4.2. The app shell must have one global top bar, 48–56 px high.
- [x] 4.3. The top bar must be calm and compact.
- [x] 4.4. On the left side of the top bar, show app name / current room / current conversation.
- [x] 4.5. Show one global status indicator in the top bar.
- [x] 4.6. On the right side of the top bar, show Search, Settings, Diagnostics only if warning, and Theme.
- [x] 4.7. Remove the five status chips from the top area.
- [x] 4.8. Remove long text like `Server stopped · Connection idle · ...` from the primary UI.
- [ ] 4.9. Open status details when the user clicks the status indicator.
- [ ] 4.10. Show technical details in the status popover only as a second-level view.
- [x] 4.11. Use a lightweight shell in `Welcome`, `HostSetup`, and `JoinSetup`.
- [x] 4.12. Use a full three-column shell in `Messenger`.
- [x] 4.13. Do not render connection setup inside the messenger layout.
- [ ] 4.14. Verify that the window does not look empty at 1600–1920 px.
- [ ] 4.15. Verify that the shell does not turn into a dashboard.

**Definition of Done:** the application has one modern shell where status is readable and compact.

---

## 5. Welcome screen: product start, not a card

- [x] 5.1. Replace the small centered card with a full desktop welcome layout.
- [x] 5.2. The welcome screen must use the whole working canvas, not a small island in empty space.
- [x] 5.3. Use a two-zone composition: left/center for product value, right/bottom for actions and nearby/recent rooms.
- [x] 5.4. Headline: `Secure chat for people nearby`.
- [x] 5.5. Subtitle: `Private LAN messages, files, and calls without cloud accounts.`
- [x] 5.6. Primary action: `Host secure room`.
- [x] 5.7. Secondary action: `Join nearby room`.
- [x] 5.8. Tertiary links: `Advanced connection`, `Settings`.
- [ ] 5.9. If recent rooms exist, show recent rooms directly on the welcome screen.
- [x] 5.10. If discovery is enabled, show `Looking for nearby rooms…` without technical noise.
- [ ] 5.11. If nearby rooms are found, show them as room cards/list items.
- [x] 5.12. Do not show `Your name`, `Room password`, `Hidden`, or `Ports` on the first screen.
- [x] 5.13. Add a simple brand visual: shield/wave/local-network glyph, without overload.
- [x] 5.14. Add short benefit chips: `LAN only`, `Encrypted`, `Files`, `Calls`.
- [ ] 5.15. At 1280 px width, the welcome screen must feel filled but not overloaded.
- [ ] 5.16. At 1920 px width, the welcome screen must not look like a lonely form.
- [x] 5.17. Buttons must not span the full width of a huge card.
- [x] 5.18. Primary button width: 180–240 px.
- [x] 5.19. Secondary button width: 180–240 px.
- [x] 5.20. Add a proper empty state when there are no recent/nearby rooms.

**Definition of Done:** the first screen feels like the start of a modern messenger, not a login form.

---

## 6. Host setup

- [ ] 6.1. Host setup opens as a separate screen or modal sheet, not together with Join setup.
- [ ] 6.2. Title: `Host a secure room`.
- [ ] 6.3. Subtitle explains the human scenario, not the server: `People nearby can join this trusted room.`
- [ ] 6.4. Field: `Room name`.
- [ ] 6.5. Field: `Your display name`.
- [ ] 6.6. Field: `Room password`.
- [ ] 6.7. Toggle: `Visible to nearby devices`.
- [ ] 6.8. Remove the word `Hidden` from primary copy. Use human-readable text.
- [ ] 6.9. Primary button: `Start room`.
- [ ] 6.10. Secondary: `Back`.
- [ ] 6.11. Advanced disclosure: `Advanced hosting settings`.
- [ ] 6.12. Hide chat port in Advanced.
- [ ] 6.13. Hide file port in Advanced.
- [ ] 6.14. Hide network adapter in Advanced.
- [ ] 6.15. Hide local IPs in Advanced.
- [ ] 6.16. Add `Copy connection details` in Advanced.
- [ ] 6.17. Add validation for password/ports.
- [ ] 6.18. Show errors next to the relevant field, not in a diagnostic log.
- [ ] 6.19. After successful host, automatically switch to Messenger.
- [ ] 6.20. After successful host, do not keep Host setup at the top.

**Definition of Done:** hosting feels like creating a room, not starting a server.

---

## 7. Join setup

- [ ] 7.1. Join setup opens as a separate screen or modal sheet.
- [ ] 7.2. Title: `Join a secure room`.
- [ ] 7.3. Show nearby rooms as the first block.
- [ ] 7.4. If there are no nearby rooms, show a calm empty state.
- [ ] 7.5. Show recent rooms as the second block.
- [ ] 7.6. Add invite/manual address only inside `Advanced manual connection`.
- [ ] 7.7. Hide Host address from the default view.
- [ ] 7.8. Hide chat port/file port from the default view.
- [ ] 7.9. Show the password prompt only after room selection or manual target selection.
- [ ] 7.10. Primary action: `Join room`.
- [ ] 7.11. Secondary: `Back`.
- [ ] 7.12. Show `Connection refused` as a recovery card.
- [ ] 7.13. Recovery card must contain `Try again`, `Change room`, `Advanced`, `Diagnostics`.
- [ ] 7.14. After successful join, switch to Messenger.
- [ ] 7.15. After successful join, do not keep Join setup at the top.

**Definition of Done:** joining feels like entering a room, not manually connecting to a socket.

---

## 8. Messenger layout

- [ ] 8.1. Create `MessengerScreen`.
- [ ] 8.2. `MessengerScreen` does not contain HostSetup/JoinSetup forms.
- [ ] 8.3. Layout: left sidebar + center chat + optional right panel.
- [ ] 8.4. Left sidebar width: 280–320 px.
- [ ] 8.5. Center chat: flexible, minimum 55–60% of usable width.
- [ ] 8.6. Right panel: 320–360 px, collapsible.
- [ ] 8.7. Use subtle dividers between columns, not heavy cards.
- [ ] 8.8. Top bar must not take too much vertical space.
- [ ] 8.9. The center chat must have the main visual weight.
- [ ] 8.10. When no peer is selected, the center shows a useful state, not a huge empty block.
- [ ] 8.11. When no peers are online, the center shows a room-ready empty state.
- [ ] 8.12. When a peer is selected, the center shows the conversation.
- [ ] 8.13. When a call is active, the center shows a call banner or call stage.
- [ ] 8.14. When a transfer is active, it is visible inline in the chat timeline.
- [ ] 8.15. Diagnostics must not take the main screen without explicit user action.
- [ ] 8.16. Remove the persistent `Actions` card when no peer is selected.
- [ ] 8.17. Remove persistent `LAN browser quick share`, `Steganography`, and `Audio devices` sections from the right column.
- [ ] 8.18. These features must live in Attach / More / Settings / Diagnostics.
- [ ] 8.19. Check the layout at 1280, 1440, 1600, 1920 px.
- [ ] 8.20. Verify that chat does not look secondary.

**Definition of Done:** after connection, the user sees a messenger, not a connection panel.

---

## 9. Left sidebar: rooms and peers

- [ ] 9.1. Create `LeftNavigationPanel`.
- [ ] 9.2. Top of the left panel: current room summary.
- [ ] 9.3. Add room conversation item: `Room chat`.
- [ ] 9.4. Add `Online` section.
- [ ] 9.5. Add `Nearby` section for discovery candidates.
- [ ] 9.6. Add `Recent` section.
- [ ] 9.7. Add `Offline` section if history exists.
- [ ] 9.8. Peer row shows avatar/device glyph.
- [ ] 9.9. Peer row shows display name.
- [ ] 9.10. Peer row shows presence in one line.
- [ ] 9.11. Peer row shows unread badge.
- [ ] 9.12. Peer row shows capability hints only as compact icons, not text like `chat · voice · video`.
- [ ] 9.13. Selected peer has a clear active background.
- [ ] 9.14. All rows have hover state.
- [ ] 9.15. Left panel empty state: `No one is here yet`.
- [ ] 9.16. `Invite` or `Copy invite` button is available if room is active.
- [ ] 9.17. Do not show technical IPs in the peer list.
- [ ] 9.18. Add keyboard navigation through the list.
- [ ] 9.19. Add search/filter peers via Ctrl+K or search.
- [ ] 9.20. Add the ability to collapse the sidebar on medium widths.

**Definition of Done:** on the left, the user sees people and rooms, not network entities.

---

## 10. Center chat

- [ ] 10.1. Create `ConversationPane`.
- [ ] 10.2. Header height: 56–64 px.
- [ ] 10.3. Header shows room/peer name.
- [ ] 10.4. Header shows human presence: `Online`, `Waiting`, `In call`, `Last seen`.
- [ ] 10.5. Header shows security/trust hint compactly.
- [ ] 10.6. Header actions: call, video, search, more.
- [ ] 10.7. Call/video actions are disabled/hidden if the peer does not support them.
- [ ] 10.8. Timeline occupies all remaining space.
- [ ] 10.9. Composer is always pinned to the bottom.
- [ ] 10.10. System events in timeline are quiet and compact.
- [ ] 10.11. Do not mix diagnostic messages with normal messages unless there is a clear reason.
- [ ] 10.12. Group messages by sender/time.
- [ ] 10.13. Local/remote messages are visually distinct, but avoid cartoon bubble overload.
- [ ] 10.14. Timestamps are muted.
- [ ] 10.15. Failed messages have retry.
- [ ] 10.16. Sending state is visible.
- [ ] 10.17. Show read/delivered state only if it is actually supported.
- [ ] 10.18. Empty chat state: `Start the conversation`.
- [ ] 10.19. No peer selected state: `Choose someone to start`.
- [ ] 10.20. Waiting for peers state: `Your room is ready`.

**Definition of Done:** chat became the main content of the application.

---

## 11. Composer and Attach menu

- [ ] 11.1. Composer contains Attach button, text input, Send button.
- [ ] 11.2. Attach button opens a menu.
- [ ] 11.3. Attach menu item: `Send secure file`.
- [ ] 11.4. Attach menu item: `Share on LAN temporarily`.
- [ ] 11.5. Attach menu item: `Encrypt text or file`.
- [ ] 11.6. Attach menu item: `Hide message in image`.
- [ ] 11.7. Attach menu item: `Extract hidden message`.
- [ ] 11.8. Do not show Steganography as a separate permanent panel.
- [ ] 11.9. Do not show LAN Quick Share as a separate permanent panel.
- [ ] 11.10. Disabled attach menu items must explain why they are disabled.
- [ ] 11.11. Drag and drop file onto conversation triggers file send preview.
- [ ] 11.12. Drag and drop file onto peer row prepares sending to that peer.
- [ ] 11.13. Enter sends message.
- [ ] 11.14. Shift+Enter adds newline.
- [ ] 11.15. Empty input disables Send.
- [ ] 11.16. Composer must not take too much vertical space visually.
- [ ] 11.17. Composer focus state must be visible.
- [ ] 11.18. Add placeholder: `Message Victor…` or `Message room…`.
- [ ] 11.19. When no peer is selected, composer is disabled and explains why.
- [ ] 11.20. When room chat is selected, composer works for shared room chat.

**Definition of Done:** files and privacy tools became part of the messenger flow, not separate modules.

---

## 12. Right context panel

- [ ] 12.1. Create `ContextPanel`.
- [ ] 12.2. The panel is hidden by default or shows lightweight guidance.
- [ ] 12.3. If a peer is selected, use `PeerInfo` mode.
- [ ] 12.4. `PeerInfo` shows avatar, name, presence, trust/security.
- [ ] 12.5. `PeerInfo` shows actions: Message, Send file, Call, Video, More.
- [ ] 12.6. If there is an active transfer, `Transfers` mode is available.
- [ ] 12.7. If there is an active call, use `Call` mode.
- [ ] 12.8. Diagnostics opens only by explicit action or warning.
- [ ] 12.9. AdvancedConnection opens only by explicit action.
- [ ] 12.10. The right panel can be collapsed.
- [ ] 12.11. At width < 1400 px, the right panel is collapsed by default.
- [ ] 12.12. At width < 1050 px, the right panel opens as a drawer/modal.
- [ ] 12.13. Do not keep permanent Transfers/Steganography/Audio Devices sections in the right panel.
- [ ] 12.14. Empty right panel must not look like an error.
- [ ] 12.15. Panel title must match the selected context.

**Definition of Done:** the right side is not a junk panel; it is context for the selected action.

---

## 13. File transfer UX

- [ ] 13.1. Sent file is displayed inline in the chat timeline.
- [ ] 13.2. Incoming file request is displayed inline in the chat timeline.
- [ ] 13.3. File card shows icon, filename, size.
- [ ] 13.4. File card shows recipient/sender.
- [ ] 13.5. File card shows progress.
- [ ] 13.6. File card shows speed/ETA only during transfer.
- [ ] 13.7. File card shows Accept / Save as / Decline for incoming files.
- [ ] 13.8. File card shows Open / Show in folder after completion.
- [ ] 13.9. File card shows Retry on error.
- [ ] 13.10. File card shows Cancel during active transfer if supported.
- [ ] 13.11. Transfer details are available in the right panel.
- [ ] 13.12. Transfer diagnostics are available from the overflow menu.
- [ ] 13.13. Transfer completion can be shown as a toast.
- [ ] 13.14. Move auto-save/ask-before-saving to Settings.
- [ ] 13.15. Do not show `0 active · 0 completed` as primary UI.
- [ ] 13.16. Do not show a transfer module until there is transfer context.
- [ ] 13.17. Show Quick Share generated link as a share card in chat.
- [ ] 13.18. Incoming unsafe/unknown file state must be understandable.
- [ ] 13.19. File transfer errors must be written in human language.
- [ ] 13.20. Support recent transfer card history in the right panel.

**Definition of Done:** file sending looks like a chat action, not a separate engineering module.

---

## 14. Voice and video UX

- [ ] 14.1. Show idle voice/video buttons only in the conversation header or PeerInfo.
- [ ] 14.2. Do not show permanent disabled Voice/Video buttons in shared room chat when no peer is selected.
- [ ] 14.3. Show incoming call as a call banner/sheet.
- [ ] 14.4. Incoming call actions: Accept, Decline, Device menu.
- [ ] 14.5. Outgoing call shows status: `Calling Victor…`.
- [ ] 14.6. Active voice call shows compact banner above the timeline.
- [ ] 14.7. Active voice call banner shows duration.
- [ ] 14.8. Active voice call banner actions: mute, device, end.
- [ ] 14.9. Chat remains usable during voice call.
- [ ] 14.10. Active video call turns the center into a video stage.
- [ ] 14.11. Remote video is the primary tile.
- [ ] 14.12. Local preview is a secondary floating tile.
- [ ] 14.13. Bottom controls: mute, camera, device, end.
- [ ] 14.14. Video errors show human recovery, not raw diagnostics.
- [ ] 14.15. Move audio/video devices to call menu/settings.
- [ ] 14.16. Show device diagnostics only on error or manual opening.
- [ ] 14.17. Active call status affects the global status indicator.
- [ ] 14.18. Ending a call returns the center to chat.
- [ ] 14.19. Show incoming call toast if the window is not focused.
- [ ] 14.20. No peer means no call controls.

**Definition of Done:** calls feel like part of the messenger, not a separate device panel.

---

## 15. Diagnostics and Advanced

- [ ] 15.1. Create Diagnostics surface as a right panel mode or overlay.
- [ ] 15.2. Diagnostics default view: health summary.
- [ ] 15.3. Health summary shows `No issues detected` or the top issue.
- [ ] 15.4. Every issue answers: what happened, what to do, where to see details.
- [ ] 15.5. Raw logs are hidden behind `Show technical log`.
- [ ] 15.6. Diagnostic channels: Connection, Discovery, Chat, Files, Calls, Quick Share.
- [ ] 15.7. Add `Copy diagnostics`.
- [ ] 15.8. Do not write diagnostic messages to the shared chat as normal messages.
- [ ] 15.9. Show connection errors as recovery cards.
- [ ] 15.10. Advanced Connection contains manual IP.
- [ ] 15.11. Advanced Connection contains chat port.
- [ ] 15.12. Advanced Connection contains file port.
- [ ] 15.13. Advanced Connection contains adapter choice.
- [ ] 15.14. Advanced Connection contains local IPs.
- [ ] 15.15. Advanced Connection contains discovery settings.
- [ ] 15.16. Advanced Connection contains copy room address.
- [ ] 15.17. In the main interface, show only `Copy invite` without raw networking.
- [ ] 15.18. Show Diagnostics shortcut in the top bar only on warning or in overflow.
- [ ] 15.19. Advanced must not look like the primary flow.
- [ ] 15.20. Verify that a normal user can use the app without Advanced.

**Definition of Done:** diagnostics are preserved, but do not dominate communication.

---

## 16. Settings

- [ ] 16.1. Create Settings screen/drawer.
- [ ] 16.2. Settings navigation: Profile, Appearance, Notifications, Files, Calls, Privacy, Network, Advanced.
- [ ] 16.3. Profile: display name, avatar/device glyph.
- [ ] 16.4. Appearance: light/dark/system, accent if supported.
- [ ] 16.5. Notifications: incoming calls, files, connection loss.
- [ ] 16.6. Files: default download folder.
- [ ] 16.7. Files: ask before saving.
- [ ] 16.8. Calls: microphone, speakers, camera.
- [ ] 16.9. Privacy: hidden room behavior, password defaults, trust hints.
- [ ] 16.10. Network: discovery defaults, adapter defaults.
- [ ] 16.11. Advanced: ports and technical defaults.
- [ ] 16.12. Settings must not close an active call without confirmation.
- [ ] 16.13. Settings must not break the current room.
- [ ] 16.14. Add settings search if there are many sections.
- [ ] 16.15. Add reset defaults for network settings.

**Definition of Done:** settings became the place for persistent preferences, not the main screen.

---

## 17. Empty states and copywriting

- [ ] 17.1. Replace technical empty states with human ones.
- [ ] 17.2. Welcome empty: `No nearby rooms yet`.
- [ ] 17.3. Waiting empty: `Your room is ready`.
- [ ] 17.4. No peers: `No one is here yet`.
- [ ] 17.5. No selected peer: `Choose someone to start`.
- [ ] 17.6. No messages: `Start the conversation`.
- [ ] 17.7. No transfers: `Shared files will appear here`.
- [ ] 17.8. No diagnostics: `No issues detected`.
- [ ] 17.9. Connection refused: `Couldn’t reach this room`.
- [ ] 17.10. Wrong password: `That password didn’t work`.
- [ ] 17.11. File failed: `File transfer stopped`.
- [ ] 17.12. Peer disconnected: `Victor left the room`.
- [ ] 17.13. Remove `server`, `listener`, `socket`, `port` from normal copy.
- [ ] 17.14. Use `room`, `people`, `nearby`, `trusted`, `secure`.
- [ ] 17.15. Show technical text only in Advanced/Diagnostics.
- [ ] 17.16. Every empty state has a primary action or next step.
- [ ] 17.17. Copy must not be longer than 1–2 lines in the primary UI.
- [ ] 17.18. Do not use `peer not selected` as user-facing text. Use `Choose someone`.
- [ ] 17.19. Do not use `connection idle` as user-facing text. Use `Offline` or `Waiting`.
- [ ] 17.20. Check the entire UI for human wording.

**Definition of Done:** the user reads the interface as a product, not a network status log.

---

## 18. Visual QA

- [ ] 18.1. Check first launch at 1280x720.
- [ ] 18.2. Check first launch at 1600x900.
- [ ] 18.3. Check first launch at 1920x1080.
- [ ] 18.4. Check messenger at 1280x720.
- [ ] 18.5. Check messenger at 1600x900.
- [ ] 18.6. Check messenger at 1920x1080.
- [ ] 18.7. Verify that welcome does not look empty.
- [ ] 18.8. Verify that messenger does not look like a dashboard.
- [ ] 18.9. Verify that chat has the main visual weight.
- [ ] 18.10. Verify that there are no excessive borders.
- [ ] 18.11. Verify that interactive states are visible: hover, pressed, focus, disabled.
- [ ] 18.12. Verify that disabled controls do not look broken.
- [ ] 18.13. Check line height and text readability.
- [ ] 18.14. Check dark theme contrast.
- [ ] 18.15. Check light theme if supported.
- [ ] 18.16. Check long peer names.
- [ ] 18.17. Check long filenames.
- [ ] 18.18. Check many peers in the sidebar.
- [ ] 18.19. Check many messages in the timeline.
- [ ] 18.20. Check scroll behavior.

**Definition of Done:** UI is visually coherent, not overloaded, and readable at desktop sizes.

---

## 19. Interaction QA

- [ ] 19.1. Host room flow works with keyboard.
- [ ] 19.2. Join room flow works with keyboard.
- [ ] 19.3. Tab order is logical.
- [ ] 19.4. Esc closes dialogs/panels.
- [ ] 19.5. Ctrl+K opens search/command palette.
- [ ] 19.6. Ctrl+N opens Host setup.
- [ ] 19.7. Ctrl+J opens Join setup.
- [ ] 19.8. Ctrl+, opens Settings.
- [ ] 19.9. Ctrl+Shift+D opens Diagnostics.
- [ ] 19.10. Enter sends message.
- [ ] 19.11. Shift+Enter inserts newline.
- [ ] 19.12. Drag file to chat works.
- [ ] 19.13. Drag file to peer works or shows a clear prompt.
- [ ] 19.14. Incoming file request is actionable.
- [ ] 19.15. Incoming call is actionable.
- [ ] 19.16. Connection error is recoverable.
- [ ] 19.17. Wrong password is recoverable.
- [ ] 19.18. Peer disconnect does not break selected state.
- [ ] 19.19. Active call survives opening Settings.
- [ ] 19.20. Diagnostics can be copied.

**Definition of Done:** the interface is not only prettier, but genuinely easier to use in real scenarios.

---

## 20. Implementation order for agents

- [ ] 20.1. First PR: introduce theme tokens only. No layout redesign yet.
- [ ] 20.2. Second PR: introduce AppMode/RoomState/SelectionState/RightPanelMode.
- [ ] 20.3. Third PR: build new AppShell.
- [ ] 20.4. Fourth PR: replace Welcome screen.
- [ ] 20.5. Fifth PR: split HostSetup and JoinSetup.
- [ ] 20.6. Sixth PR: build Messenger layout without advanced panels.
- [ ] 20.7. Seventh PR: implement left sidebar rooms/peers.
- [ ] 20.8. Eighth PR: implement conversation header/timeline/composer.
- [ ] 20.9. Ninth PR: move Attach tools into Attach menu.
- [ ] 20.10. Tenth PR: move transfers into chat timeline.
- [ ] 20.11. Eleventh PR: implement contextual right panel.
- [ ] 20.12. Twelfth PR: implement voice/call banners.
- [ ] 20.13. Thirteenth PR: move Advanced/Diagnostics out of main UI.
- [ ] 20.14. Fourteenth PR: implement Settings.
- [ ] 20.15. Fifteenth PR: visual QA and polish.
- [ ] 20.16. Sixteenth PR: interaction QA and keyboard shortcuts.
- [ ] 20.17. Seventeenth PR: screenshots and documentation.
- [ ] 20.18. Do not combine all changes into one massive PR.
- [ ] 20.19. After each PR, attach before/after screenshots.
- [ ] 20.20. If a PR makes the app look like a dashboard again, reject it.

**Definition of Done:** the phase is split into safe PRs that can be reviewed visually and functionally.

---

## 21. Agent prompt for every implementation step

Use this prompt before each coding task:

```text
You are working on SecureLanSuite Chat Phase 11 UI/UX redesign.
Do not make a cosmetic dashboard redesign.
The product must feel like a modern desktop messenger, not a network utility.
Keep protocol, services, discovery, encryption, file transfer, voice/video bridges unchanged.
Only change presentation state, Compose UI structure, components, layout, copy, and interaction.
Hide ports, IPs, adapters, listener state, raw logs, and manual connection details from normal UI.
Show them only in Advanced Connection or Diagnostics.
After host/join, transition into Messenger mode and remove the connection setup from the main screen.
Chat must be the primary visual area.
Peer/person must be the primary object.
Attach menu owns file transfer, Quick Share, encryption and steganography entry points.
Voice/video controls appear only for selected peer or active call.
Before coding, state which checklist items you are implementing.
After coding, state which checklist items are complete and provide screenshots.
```

---

## 22. Final acceptance

- [ ] 22.1. First launch does not show any technical networking controls.
- [ ] 22.2. First launch does not look like a tiny form floating in empty space.
- [ ] 22.3. Host and Join are separate focused flows.
- [ ] 22.4. After connection, Host/Join setup disappears.
- [ ] 22.5. Messenger layout is the primary connected UI.
- [ ] 22.6. Chat is visually dominant.
- [ ] 22.7. Peers are visually treated as people/devices, not socket endpoints.
- [ ] 22.8. Right panel is contextual.
- [ ] 22.9. Files appear as chat attachments.
- [ ] 22.10. Quick Share is reachable from Attach, not a permanent module.
- [ ] 22.11. Steganography is reachable from Attach/Privacy tools, not a permanent module.
- [ ] 22.12. Voice/video are contextual to peer/call.
- [ ] 22.13. Advanced Connection exists but is not primary.
- [ ] 22.14. Diagnostics exists but does not dominate normal use.
- [ ] 22.15. One global status indicator replaces many status chips.
- [ ] 22.16. UI uses consistent tokens.
- [ ] 22.17. UI works at 1280, 1440, 1600, 1920 px.
- [ ] 22.18. Keyboard navigation is usable.
- [ ] 22.19. JavaFX fallback remains available.
- [ ] 22.20. Existing networking/protocol behavior remains intact.

**Final Definition of Done:** SecureLanSuite Chat opens, hosts/joins, chats, transfers files, and starts calls through a UI that feels like a modern secure LAN messenger, not a server administration panel.
