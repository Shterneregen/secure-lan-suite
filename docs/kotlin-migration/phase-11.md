# Phase 11: Compose-first desktop UX hardening and JavaFX deprecation

[Back to Kotlin Migration Plan](kotlin-migration.md#phase-index)

## Goal

Make the desktop client a Compose-first product surface after Phase 10 closure. Phase 11 improves user experience, navigation, state clarity, diagnostics, and release confidence in the Compose UI while JavaFX is deprecated and retained only as the packaged launcher, rollback fallback, and critical-fix path until a later explicit removal or Compose-promotion task.

## Current decision

- Phase 10 is closed and its remaining open runtime/packaging/user-experience tasks are carried forward here.
- JavaFX is **deprecated for desktop UI evolution**. Do not add new JavaFX UI features, screens, or non-critical visual polish.
- New desktop UI/UX improvements must be implemented in [`SecureLanComposeApp.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/SecureLanComposeApp.kt), Compose state models in [`ComposeShellMetadata.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/ComposeShellMetadata.kt), and the live Compose host boundary in [`ComposeDesktopHostAdapter.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/ComposeDesktopHostAdapter.kt).
- JavaFX files such as [`Main.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/Main.java), [`ChatApplication.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ChatApplication.java), [`MainView.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui/MainView.kt), and [`MainViewDelegate.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ui/MainViewDelegate.java) remain only for fallback, packaging continuity, and critical fixes.
- Do not change [`application.mainClass`](../../apps/desktop-client/build.gradle:14), manifest behavior, or `jpackage` main class during Phase 11 unless a separate Compose promotion task explicitly accepts all runtime, packaging, rollback, and approval gates.

## Compose UI baseline coverage already verified deterministically

- Discovery and peer-list state: listen-only discovery startup, local LAN diagnostics, local/self filtering, manual peers, selected-peer preservation by nickname, online/offline/discovered/chat-only/file-capable modeling.
- Peer targeting: chat, file, voice, video, and RTC data readiness; Android peers can remain file-capable while voice/video/data-channel actions stay blocked.
- Chat: transcript mapping for connected, disconnected, received-message, join, left, and error lines; blank-message and disconnected send guards; Enter-to-send state coverage.
- File transfer: send only to online file-capable peers, selected-peer file targeting, incoming receive prompts, accept/decline/auto-accept states, unknown/offline sender rejection, Android host matching, asynchronous sends, and progress diagnostic de-duplication.
- Settings and network state: nickname/password fields, chat/file/discovery/quick-share ports, discoverable/hidden state, local LAN IP diagnostics, speaker/microphone/camera device state.
- Diagnostics and error handling: channel cards for chat, file-transfer, quick-share, and realtime; fallback warnings; transfer failures; camera-preview failure recovery; packaging/readiness gates.

## Phase 11 UX implementation plan

Use the existing deterministic Compose state contracts, adapter mappings, readiness summaries, diagnostics metadata, and packaging gates in [`ComposeShellMetadata.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/ComposeShellMetadata.kt) as the testable non-visual baseline. Do not depend on a dedicated Phase 11 UX plan state; the plan below is documentation-owned and should be validated through the relevant feature states and tests.

| Area                 | Work                                                                               | Acceptance criteria                                                                                                                                                |
|----------------------|------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Navigation and shell | Make primary Compose workflows easier to find without changing protocol behavior.  | Primary actions are obvious; advanced diagnostics are available through progressive disclosure; window resizing keeps desktop density.                             |
| Peer list states     | Clarify discovered, online, offline, selected, chat-only, and file-capable states. | Empty state explains discovery and manual fallback; rows distinguish peer states; file actions remain blocked for offline or non-file-capable peers.               |
| Chat and transcript  | Improve shared-room transcript readability and disconnected/error guidance.        | Enter-to-send remains covered; empty and disconnected states show next steps; UI copy does not imply private text chat while text remains shared-room behavior.    |
| File transfer        | Make send/receive decisions and capability gating more understandable.             | Attach and Send encrypted file require an online file-capable peer; prompts show accept/decline/auto-accept/rejected states; progress and failures remain visible. |
| Session settings     | Group profile, password, ports, discoverability, and media devices more clearly.   | Password fields keep visual transformation; network status and local LAN IP details remain visible; media device controls preserve service boundaries.             |
| Diagnostics          | Keep troubleshooting powerful but less visually dominant.                          | Discovery, chat, file-transfer, quick-share, stego, RTC, media-device, and packaging diagnostics remain reachable; raw details are expandable.                     |
| Errors and recovery  | Convert failures into user-facing recovery guidance.                               | Blocked actions explain what to fix; transfer, discovery, camera-preview, and RTC failures preserve diagnostics and recovery copy.                                 |

## Carry-forward release gates

- Runtime smoke-check Compose against JavaFX behavior at 1360x860 and common resized window sizes.
- Re-check desktop-to-desktop and desktop-to-Android chat/file-transfer interoperability, including Android file receiver ports and Android sender IDs.
- Runtime-test quick share, steganography, voice, camera preview, experimental video, empty/loading/disabled/warning/error states, active transfers, active calls, stopped shares, keyboard navigation, focus traversal, hover/selected states, validation feedback, and resizing.
- Validate [`gradlew.bat :apps:desktop-client:test --no-daemon`](../../gradlew.bat), [`gradlew.bat :apps:desktop-client:build --no-daemon`](../../gradlew.bat), [`gradlew.bat :apps:desktop-client:runComposeShell --no-daemon`](../../gradlew.bat), [`gradlew.bat :apps:desktop-client:buildPortable --no-daemon`](../../gradlew.bat), [`gradlew.bat :apps:desktop-client:buildComposePortable --no-daemon`](../../gradlew.bat), and Windows EXE packaging on a WiX 5.0.2 host before any launcher promotion.
- Keep adding deterministic tests for state mapping, formatting, readiness, diagnostics, and adapter behavior instead of moving business logic into composables.

## Exit criteria

- Compose UI is accepted as the normal desktop UI for day-to-day chat, peer discovery, file transfer, quick share, steganography, voice, diagnostics, and supported experimental video flows.
- JavaFX has no remaining planned feature work and only documented critical-fix/fallback use remains.
- Portable JavaFX fallback, Compose portable ZIP, and Windows EXE validation are recorded.
- A separate accepted task explicitly decides whether to promote Compose as the packaged launcher and remove JavaFX fallback files.
