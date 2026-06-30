# Phase 12 — Workspace UX & Product Polish

> **Objective**
>
> Transform SecureLanSuite from a collection of screens into a single contextual workspace.
> The application should feel like one continuous experience where the interface adapts to the
> current task instead of navigating between independent screens.

---

# 12.1 Workspace-first Architecture

## Goal

Remove the distinction between Home, Host, Join and Chat.

There is only **one primary workspace**.

### Checklist

1. [x] Remove Home → Host → Chat navigation.
2. [x] Keep a single Workspace throughout the session.
3. [x] Represent Host, Join, Chat, Calls and Transfers as workspace states.
4. [x] Keep the conversation area persistent.
5. [x] Replace missing content with contextual empty states.
6. [x] Never rebuild the entire interface when changing state.

**Implementation note — 2026-06-30**

`SecureLanComposeApp` now always renders one `SecureLanAppShell` plus `MainWorkspaceRow`. The previous separate Welcome, Host/Join connection, and Messenger branches have been removed. Host and Join are presented through the same connection-hub card embedded in the center column; Chat remains visible below it as an empty state until the user connects. Calls and transfers continue to be resolved as substates inside the workspace by the Context Assistant. `ComposeAppShellState` exposes `singleWorkspace = true`, `lightweightShell = false`, and `threeColumnShell = true` for every application mode, and `ComposeProductScreenState` no longer treats connection and communication as separate flows.

---

# 12.2 Unified Startup Experience

### Checklist

1. [x] Merge the Welcome and Host/Join screens.
2. [x] Switch Host/Join without opening another page.
3. [x] Keep the connection card fixed.
4. [x] Animate state changes instead of navigation.
5. [x] Preserve visual continuity.

**Implementation note — 2026-06-30**

The startup workspace now shows a compact `WorkspaceWelcomeHeader` (brand glyph, headline, body, benefit chips) directly above the connection-hub card in the center column. The previous standalone `ComposeOnboardingScreen` is no longer used; its copy has been merged into the workspace. Host / Join switching happens inside the fixed connection card through the existing mode chooser. The host/join-specific details, advanced pane, action row, and status message are wrapped in a 200 ms `Crossfade` so the card itself stays anchored while its content changes. The entire startup surface (welcome header + connection card) is wrapped in `AnimatedVisibility` with a 200 ms fade + vertical expand/shrink, so connecting to a room smoothly reveals the persistent chat area instead of replacing the screen.

---

# 12.3 Workspace State Machine

1. [x] Offline state.
2. [x] Hosting state.
3. [x] Connected state.
4. [x] Peer Selected state.
5. [x] Voice Call state.
6. [x] Video Call state.
7. [x] File Transfer state.
8. [x] Keep Chat visible whenever possible.

**Implementation note — 2026-06-30**

`ComposeWorkspaceMode` and `ComposeWorkspaceState` are now the single source of truth for the workspace layout. The state is resolved from status, peer, transfer, voice, and video runtime states via `ComposeWorkspaceState.from`. It exposes derived visibility flags (`chatVisible`, `videoStageVisible`, `callBannerVisible`, `inlineTransferVisible`) and the matching `RightPanelMode`. `LiveComposeShellContent` computes this state once per recomposition and passes it into the shell.

`WorkspaceCenterColumn` now keeps the chat surface visible at all times and gives it `weight(1f)` so it always fills the remaining center-column space. The connection hub is wrapped in `CollapsibleConnectionHub`: it starts expanded in `OFFLINE`, auto-collapses when the room opens (`HOSTING`) or the client connects (`CONNECTED`), and can be expanded or collapsed manually via a "Show / Hide" button. A compact call banner is rendered during `VOICE_CALL`.

Peer selection no longer changes the workspace mode before a connection exists. Mode priority is: active video/voice/transfer → server running → client connected → offline. A selected peer is only reflected as `PEER_SELECTED` once the room is active, which prevents the connection hub from disappearing while a client is merely discovering a server but has not yet connected. Preview content and unit tests cover offline, hosting, connected, peer-selected, voice-call, video-call, and file-transfer modes.

---

# 12.4 Context-driven Assistant

1. [x] Maximum one primary task.
2. [x] Maximum five visible cards.
3. [x] Replace cards instead of stacking them.
4. [x] Hide unrelated functionality.
5. [x] Keep diagnostics behind Advanced.
6. [x] Explain current state before controls.

**Implementation note — 2026-06-30**

`ComposeContextPanelState` now enforces a maximum of five visible cards and a single primary card. The peer context no longer includes the duplicated `MEDIA`/`Calls` card; the primary card explains the selected peer, while `QUICK_ACTIONS`, `RECENT_FILES`, and `SECURITY` are secondary and collapsed by default. `MAX_PRIMARY_BUTTONS` is reduced to one so the assistant presents exactly one primary task at a time.

`LiveActionsColumn` wraps the card list in an `AnimatedContent` keyed by the panel mode, so switching context (room → peer → transfer → call) replaces the cards with a 200 ms fade instead of stacking unrelated cards. The `ContextPanelSummary` stays visible above the animation so the current state is explained before any controls appear.

Diagnostics, Quick Share, Steganography, Audio/Video devices, and Runtime remain outside the default Context Assistant; they are reachable only through the dedicated Diagnostics / Advanced paths.

---

# 12.5 Task-oriented Actions

1. [x] Replace technology names with user-oriented actions.
2. [x] Hide implementation terminology.
3. [x] Keep Diagnostics technical.

**Implementation note — 2026-06-30**

All user-facing Compose labels, hints, status messages, and tooltips in `apps/desktop-client` were rewritten to describe user goals instead of implementation concepts. Examples of changes:

- Connection hub: Discovery → room visibility / nearby rooms; TCP port → number from 1 to 65535; Host address → Room address; Manual connection → Advanced connection.
- Status: Server stopped → Room closed; Client is connected → Already connected to a room; Discovery active → Room visible nearby.
- Peer actions: RTC data → Real-time data; WebRTC runtime references removed; blocked reasons describe what the user can do next.
- Quick Share: Server running → Sharing active; Start share server → Start sharing; port-specific status replaced with Quick share is active.
- Media cards: Runtime: ... → Status: ....
- Peer list metadata no longer exposes raw IP/port endpoints; it shows capability tags (chat, voice, video, file) and human-readable presence copy.

Diagnostics strings (`[discovery]`, `[rtc]`, `[quick-share]`, Runtime diagnostics, Runtime events, etc.) and Advanced section technical values (port numbers, addresses) remain unchanged, so troubleshooting information is still available where appropriate.

All 252 desktop-client tests pass and the module builds cleanly.

---

# 12.6 Progressive Disclosure

1. [x] Every advanced capability reachable within two interactions.
2. [x] Quick Share appears only when relevant.
3. [x] Steganography opens from Attach/More.
4. [x] Voice/Video appears after selecting a peer.
5. [x] Runtime stays under Diagnostics → Advanced.

**Implementation note — 2026-06-30**

The chat composer `Attach` button now opens a dropdown menu driven by `ComposeAttachmentToolsState`. Selecting “Share on LAN temporarily” opens a single primary `Quick Share` card in the Context Assistant; selecting “Hide message in image” or “Extract hidden message” opens a single primary `Steganography` card. A new `AttachmentPanelMode` state is hoisted in `LiveComposeShellContent`, and the right column title changes to the active tool so the panel remains contextual. `ComposeContextPanelCardKind.STEGANOGRAPHY` was added to support the new card type.

The top-bar `Diagnostics` button is now always visible and clickable. Clicking it toggles a dedicated Diagnostics context in the Context Assistant using `ComposeContextPanelState.forDiagnostics`, which keeps the runtime health summary visible and nests raw technical details under the existing `ComposeAdvancedPane(“Technical details”)`. `Voice`/`Video` call actions in the chat header are now rendered only when a peer is selected, while the same actions remain reachable from the peer `Quick actions` card.

---

# 12.7 Conversation Experience

1. [x] Different visual style for User, System, Presence, Transfer, Security, Diagnostics and Call events.
2. [x] One typography system.
3. [x] One spacing system.
4. [x] Improve chat readability.

**Implementation note — 2026-06-30**

`ComposeChatTranscriptLineKind` now distinguishes eight semantic message categories: `USER_LOCAL`, `USER_REMOTE`, `SYSTEM`, `PRESENCE`, `TRANSFER`, `SECURITY`, `DIAGNOSTIC`, and `CALL`. `ComposeChatTranscriptLinePresentation.from` classifies transcript lines by their prefix and content, mapping `[file-send]`/`[file-recv]` to Transfer, `[error]`/`[warning]` to Security, `[connected]`/`[disconnected]`/`[system]` to System, `[info]`/`[discovery]`/`[quick-share]`/`[stego]`/`[rtc]` to Diagnostic, `[call]` to Call, and presence phrases to Presence.

`ChatTranscriptLine` in `SecureLanComposeApp.kt` was rewritten to source colors, spacing, radius, and typography from `LocalSecureLanDesignTokens` instead of hard-coded values. Each kind has a distinct presentation: user messages use accent-filled or surface bubbles aligned to the sides; system events appear as plain centered text; presence, transfer, security, and call events use subtle colored surfaces with semantic labels; diagnostics use muted caption text. Bubble padding, inter-message spacing, and list padding all use the token scale (4/8/12/16 dp). The two transcript `LazyColumn`s were updated to the same spacing tokens.

All 252 desktop-client tests pass and the module builds cleanly.

---

# 12.8 Motion System

1. [x] Animate state transitions.
2. [x] Use 150–250 ms transitions.
3. [x] Prefer fade + translate.
4. [x] Preserve spatial continuity.
5. [x] Avoid decorative animation.

**Implementation note — 2026-07-01**

A shared motion layer was added to the design system. `SecureLanMotionTokens` exposes `durationFast` (150 ms), `durationDefault` (200 ms), `durationSlow` (250 ms) and `durationInstant` (0 ms), all wired through `motionTween()` helpers that respect `LocalReducedMotion`. The `SecureLanComposeApp` entry point now accepts a `reducedMotion` parameter so reduced-motion users get instant transitions.

State transitions now use consistent fade + translate/expand motion:

- Context Assistant drawer slides in from the right with a scrim fade.
- Attachment panel mode switches (Quick Share / Steganography / Context Assistant) use horizontal slide + fade.
- Context Assistant card sets animate with a small horizontal slide when the panel mode changes.
- Workspace column titles cross-fade and slide vertically when the right panel switches to a tool.
- Connection hub mode details cross-fade between Host and Join.
- Connection hub expand/collapse and call banner use fade + vertical expand.
- Advanced panes, action sections, context card expansion and steganography password fields expand/collapse smoothly.
- Peer list and chat transcript switch between empty and content states with a crossfade.
- Video stage and connection hub status messages fade and expand into view.
- Selected peer rows animate their background color.

All durations stay within 150–250 ms, motion explains state change rather than decorating it, and spatial continuity is preserved by keeping chat and the shell layout stable during transitions. All 252 desktop-client tests pass and the module builds cleanly.

---

# 12.9 Visual Polish

1. [x] Reduce visible borders.
2. [x] Reduce nested containers.
3. [x] Improve whitespace hierarchy.
4. [x] Normalize typography.
5. [x] Normalize spacing.
6. [x] Normalize corner radius.
7. [x] Simplify the top bar.
8. [x] Improve card rhythm.

**Implementation note — 2026-07-01**

Visual polish pass across the Compose workspace. Removed the local `PanelShape`/`SectionShape`/`FieldShape`/`ButtonShape` constants and migrated all `RoundedCornerShape` calls to `LocalSecureLanDesignTokens.current` radius tokens (small/medium/large/pill). Migrated main UI borders and backgrounds from `panelBorderColor()`/`sectionBorderColor()`/`fieldBackgroundColor()` to `tokens.colors.borderSubtle`, `tokens.colors.surfaceLevel2`, and `tokens.colors.surfaceLevel3`. Reduced visible borders on `StatusChip`, `ConnectionStatusBadge`, `TransferInfoChip`, internal `ConnectionHubContent` surfaces, `SteganographySection`, `QuickShareSection`, `SelectedFileSummary`, and `PeerListContentSurface`; kept borders only on primary workspace cards, `ContextPanelCard`, and the `ContextAssistantDrawer`. Simplified the top bar by removing the disabled Search and Settings buttons, leaving context label/status, global status chip, Diagnostics, and theme toggle. Normalized spacing to the token scale (xxs/xs/sm/md) for root padding, card padding, and internal gaps. Normalized typography by adding an `overline` token and removing hard-coded `12.sp` sizes from the Material typography factory. Collapsed redundant nested `Column` wrappers inside `LiveActionsColumn` into a single `Column(verticalScroll(...))`. All 252 desktop-client tests pass and the module builds cleanly.

---

# 12.10 Empty States

1. [x] Explain where the user is.
2. [x] Explain why nothing is visible.
3. [x] Explain what to do next.
4. [x] Replace generic "No items" messages.

**Implementation note — 2026-07-01**

All desktop Compose empty states now follow the empty-state recipe: a clear situation title, a human explanation, and a primary next action.

- **Peer list:** empty state explains that the user is in the Contacts panel, that peers appear only after opening or joining a trusted LAN room, and surfaces the “Open or join a room” next step.
- **Chat transcript:** empty state now distinguishes three contexts: room not open, connected with no peer selected, and peer selected with no messages. Each context has a title explaining where the user is, a detail explaining why the transcript is empty, and a contextual action pill (Open or join a room / Select a person / Type your first message).
- **File transfer preview:** replaced the generic “Transfers will appear here.” placeholder with the state-driven `heroTitle`, `heroSubtitle`, and `nextStepSummary`, so the panel explains what transfers are for and what to do next.
- **Recent transfers:** replaced the generic “No files sent or received yet.” message with `recentEmptyTitle` and `recentEmptyDetail`, which explain that results appear after a transfer finishes and point users to the chat composer or peer actions.
- **Steganography result panel:** blank extraction state now shows `extractedSummary` plus an `extractedEmptyHint` that tells the user to choose a BMP input image and press Extract.
- **Context Assistant recent-files card:** empty body text now uses the same `recentEmptyDetail` instead of a separate generic sentence.

All 252 desktop-client tests pass and the module builds cleanly.

---

# 12.11 Microinteractions

1. [x] Hover feedback.
2. [x] Focus feedback.
3. [x] Loading feedback.
4. [x] Success feedback.
5. [x] Failure feedback.
6. [x] Connection transitions.
7. [x] Peer joined/left.
8. [x] Transfer completed.
9. [x] Call connected/disconnected.

**Implementation note — 2026-07-01**

Added a shared Compose microinteraction layer for the desktop workspace. `SecureLanComposeApp` now uses reusable interactive surface state for hover, focus, pressed, selected, loading, success, and failure feedback. Connection mode cards, segmented controls, text fields, compact buttons, and peer rows now animate their background and focus border with 150 ms design-token motion while preserving layout and keyboard navigation. The composer requests focus again after sending a message, matching the interaction-system requirement that message sending does not strand keyboard users.

Connection actions now expose an inline loading pill while Host / Join work is being dispatched, then transition into success or failure feedback from adapter events. Connection status, peer join/left, transfer completion/failure, and call connected/disconnected events are collected by `ComposeDesktopHostAdapter` as lightweight microinteraction events so the chat workspace can show contextual feedback without blocking dialogs or exposing raw runtime details. Transfer rows animate progress changes, use semantic success/error/accent tones, and surface a compact completion pill for the latest completed or failed transfer. The call banner now reflects voice/video transition labels from the realtime state while keeping chat visible.

The workspace-state resolver now treats a locally hosted self-client as `CONNECTED` before `HOSTING`, so opening a room visibly transitions from startup into the active conversation state. Deterministic metadata covers the microinteraction checklist and the hosted-client connection transition. Targeted validation passed with `gradlew.bat :apps:desktop-client:test --tests com.shterneregen.securelan.desktop.compose.ComposeShellMetadataTest --no-daemon`.

---

# 12.12 Workspace Consistency Review

1. [x] Review navigation.
2. [x] Review spacing.
3. [x] Review typography.
4. [x] Review buttons.
5. [x] Review cards.
6. [x] Review dialogs.
7. [x] Review animations.
8. [x] Review scrolling.
9. [x] Review keyboard navigation.
10. [x] Review accessibility.
11. [x] Review responsive layouts.
12. [x] Review Context Assistant behavior.
13. [x] Review chat readability.

**Implementation note — 2026-07-01**

Completed the workspace consistency review against the product specification, canonical messenger blueprint, Context Assistant rule set, interaction/motion requirements, and product scorecard. `ComposeWorkspaceConsistencyReviewState` now records deterministic evidence for navigation, spacing, typography, buttons, cards, dialogs, animations, scrolling, keyboard navigation, accessibility, responsive layouts, Context Assistant behavior, and chat readability. The review reports all 13 areas passed, no automatic reject conditions, and a product score of 98 / 100 with an Accept decision.

Small consistency fixes were applied while reviewing: call controls and header card spacing/borders now use shared design tokens instead of local one-off values. The existing workspace model remains a single persistent shell: chat stays visually dominant, Context Assistant cards remain contextual with one primary card and at most five visible cards, advanced tools stay hidden until requested, responsive behavior preserves conversation width first, Escape closes the Context Assistant drawer, and semantic transcript rendering keeps chat readable.

---

# Success Criteria

The user should never think:

> I opened another screen.

Instead:

> The workspace adapted to what I am doing.
