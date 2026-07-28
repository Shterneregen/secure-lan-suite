# Phase 10: Compose runtime stabilization, UX modernization, and release readiness (closed)

[Back to Kotlin Migration Plan](kotlin-migration.md#phase-index)

## Goal

Turn the Phase 9 Compose shell from an experimental parity implementation into a release-ready desktop UI candidate through runtime hardening, user-friendly UX improvements, packaging validation, and an explicit launcher/fallback decision.

This phase now owns the desktop-specific checklist that was previously kept in the deleted desktop checklist file.

## Closure decision

- Phase 8 desktop Kotlin interop is closed.
- Phase 9 initial Compose desktop parity implementation is closed.
- Phase 10 is closed as of 2026-06-29.
- JavaFX is now documented as **deprecated for desktop UI evolution**. Keep it only as the packaged launcher, rollback fallback, and critical-fix path until a separate removal/promotion task is accepted.
- New desktop UI/UX improvements must target the Compose UI in [`apps/desktop-client`](../../apps/desktop-client/build.gradle). Do not add new JavaFX screens or JavaFX UX polish unless the change is a critical fix that protects users before final JavaFX removal.
- The explicit launcher/fallback decision for this closure is: keep [`application.mainClass`](../../apps/desktop-client/build.gradle:14), manifest behavior, and `jpackage` main class on the JavaFX launcher while Phase 11 continues Compose-first UX hardening and prepares a later Compose promotion/removal decision.
- Remaining runtime, cross-device, packaging, and UX-hardening tasks are carried forward to Phase 11 instead of being deleted from the historical Phase 10 checklist.

## Phase 10 closure evidence

- Deterministic coverage exists for capability-aware peer targeting, Android file-capable peer behavior, discovery listener startup, local LAN diagnostics, chat transcript mapping, Enter-to-send readiness, selected-peer targeting, file-transfer prompts, unknown/offline sender rejection, Android host matching, asynchronous outgoing sends, progress diagnostic de-duplication, speaker output state, camera-preview failure state, diagnostic channels, regression readiness, and packaging gates in [`ComposeShellMetadataTest.kt`](../../apps/desktop-client/src/test/kotlin/com/shterneregen/securelan/desktop/compose/ComposeShellMetadataTest.kt) and [`ComposeDesktopHostAdapterTest.kt`](../../apps/desktop-client/src/test/kotlin/com/shterneregen/securelan/desktop/compose/ComposeDesktopHostAdapterTest.kt).
- [`SecureLanComposeApp.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/SecureLanComposeApp.kt) contains the accepted Phase 10 Compose UX modernization baseline for transfer, quick-share, steganography, audio/video setup, diagnostics, tooltips, native file dialogs, and clearer primary actions.
- [`ComposeDesktopHostAdapter.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/ComposeDesktopHostAdapter.kt) is the live Compose host boundary for discovery, peer-list state, chat transcript, file transfer, quick share, stego, realtime, diagnostics, runtime evidence, and packaging evidence.
- [`ComposeShellMetadata.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/state/shell/ComposeShellMetadata.kt) centralizes shell constants and default Compose state fixtures.

## Latest commit impact summary

The latest Phase 10 commit materially changes the migration plan without changing the JavaFX launcher decision:

- [`chat-core`](../../modules/chat-core/build.gradle) now advertises peer capability metadata through [`PeerCapabilities.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/protocol/handshake/PeerCapabilities.kt). Desktop peers advertise file, voice, video, quick-share, steganography, and RTC data support; Android peers advertise file send/receive but intentionally disable voice/video/data-channel actions.
- [`HandshakeRequest.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/protocol/handshake/HandshakeRequest.kt), [`HandshakeResponse.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/protocol/handshake/HandshakeResponse.kt), [`ChatClientConnectRequest.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/service/ChatClientConnectRequest.kt), [`ChatConnectedEvent.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/event/ChatConnectedEvent.kt), and [`ChatUserJoinedEvent.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/event/ChatUserJoinedEvent.kt) gained capability fields with compatibility constructors/defaults, so Java/Kotlin source compatibility remains a required review point for future changes.
- [`apps/android-client`](../../apps/android-client/build.gradle) now sends Android capability metadata during secure chat connection, enabling desktop send-to-Android file targeting without relying only on UDP discovery.
- [`MainViewDelegate.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ui/MainViewDelegate.java), [`PeerPresence.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui/PeerPresence.kt), [`DesktopPeerFormatters.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui/DesktopPeerFormatters.kt), and [`DesktopMainViewHelpers.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui/DesktopMainViewHelpers.kt) are capability-aware. Android peers can remain file-capable while voice/video/data-channel controls stay blocked.
- [`ComposeDesktopHostAdapter.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/ComposeDesktopHostAdapter.kt) now starts listen-only discovery on startup, publishes local LAN info, resolves file-capable peers from chat capabilities, waits for explicit incoming-transfer decisions off the UI thread, sends outgoing files asynchronously, deduplicates progress diagnostics, handles camera-preview failures, and exposes speaker output selection/testing.
- [`SecureLanComposeApp.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/SecureLanComposeApp.kt) moved from strict visual parity into Phase 10 UX modernization for transfers, quick share, steganography, audio/video setup, diagnostics, tooltips, native file dialogs, and clearer primary actions.
- [`apps/desktop-client/build.gradle`](../../apps/desktop-client/build.gradle) adds the coroutine runtime dependency and a separate experimental Compose portable packaging path. The JavaFX portable and EXE paths remain the production packaging baseline.

## Updated priorities

1. Validate capability-aware desktop/Android interoperability, especially Android file receive ports, Android-to-desktop file sender IDs, and disabled Android voice/video actions.
2. Keep Compose UX modernization focused on accepted behavior: clearer workflows are allowed, protocol drift is not.
3. Validate non-blocking file-transfer behavior under real desktop runtime conditions, not only deterministic adapter tests.
4. Treat separate Compose portable packaging as a new release gate before any launcher decision.
5. Keep JavaFX fallback intact until runtime parity, UX stabilization, JavaFX portable validation, Compose portable validation, Windows EXE validation, rollback plan, and explicit approval are complete.

## Updated risks and dependencies

- Capability metadata extends the encrypted chat handshake payload. Older peers should fall back to unknown capabilities, but mixed-version desktop/Android smoke tests are required before promotion.
- [`HandshakeRequest.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/protocol/handshake/HandshakeRequest.kt), [`HandshakeResponse.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/protocol/handshake/HandshakeResponse.kt), and [`ChatClientConnectRequest.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/service/ChatClientConnectRequest.kt) are now Java-callable Kotlin classes rather than simple JVM records; future refactors must preserve constructors, null handling, accessors, and readable `toString()` output.
- Compose file-transfer sending now uses coroutines in [`apps/desktop-client`](../../apps/desktop-client/build.gradle). Packaging and shutdown behavior must be validated so background IO does not leak across app shutdown.
- Native file dialogs in [`SecureLanComposeApp.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose/SecureLanComposeApp.kt) require manual runtime checks on Windows and any other supported desktop OS before promotion.
- Speaker output selection depends on `webrtc-java` render device enumeration through Java [`DefaultRtcMediaDeviceService.java`](../../modules/webrtc-core/src/main/java/com/shterneregen/securelan/webrtc/service/impl/DefaultRtcMediaDeviceService.java); keep it behind diagnostics and do not assume all devices expose stable render IDs.
- The Compose portable artifact is separate from JavaFX packaging. Both artifacts must be tested because the JavaFX launcher remains the stable fallback while Compose promotion is undecided.

## Desktop boundary and guardrails

- Keep JavaFX and Compose desktop UI code inside [`apps/desktop-client`](../../apps/desktop-client/build.gradle).
- Keep reusable modules under [`modules`](../../modules) UI-agnostic.
- Do not change discovery, chat, encrypted file transfer, quick share, steganography, RTC signaling, voice, or experimental video wire formats as part of UI work.
- Keep [`Main.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/Main.java), [`ChatApplication.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ChatApplication.java), [`MainView.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui/MainView.kt), and [`MainViewDelegate.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ui/MainViewDelegate.java) available until Compose promotion is explicitly approved.
- Do not change [`application.mainClass`](../../apps/desktop-client/build.gradle:14), JAR manifest behavior, or jpackage main class until runtime, packaging, rollback, and approval gates are complete.

## Historical Phase 10 checklist and Phase 11 carry-forward

The checklist below is intentionally preserved as historical context. Checked items are closed Phase 10 work. Unchecked items are **not removed**; they become Phase 11 acceptance criteria or release-gate tasks in [`phase-11.md`](phase-11.md).

### Runtime parity and interoperability

- [ ] Re-run side-by-side JavaFX and Compose visual review at the 1360x860 baseline and common resized desktop sizes.
- [ ] Capture evidence for My profile, Manual connection, Peers, Chat, Selected peer, Transfers, Quick share, Steganography, Audio / Video devices, Runtime / Diagnostics, light/dark theme, empty/loading/error/disabled states, and long-content scrolling.
- [ ] Validate JavaFX server to Compose client desktop chat interoperability.
- [ ] Validate Compose server to JavaFX client desktop chat interoperability.
- [ ] Confirm connected peer rows show real clients rather than self/server entries.
- [x] Keep deterministic coverage for custom/random nickname propagation, self/server peer filtering, and Enter-to-send behavior in Compose state/adapter tests.
- [ ] Runtime-confirm custom and random nicknames propagate correctly through Compose hosting and manual connection flows.
- [ ] Runtime-confirm Enter-to-send, disconnect/left state, and transcript ordering match JavaFX behavior.
- [x] Add capability-aware desktop/Android peer modeling so Android file actions can stay enabled while voice/video/data-channel actions are blocked.
- [ ] Re-check Android interoperability for LAN discovery, secure chat, encrypted file send/receive, transfer progress, Android file-receiver capabilities, Android sender IDs, and diagnostics logs.
- [x] Start Compose discovery in listen-only mode on adapter startup and surface local LAN IP diagnostics.
- [ ] Harden LAN discovery for firewalls, VPNs, multi-adapter hosts, duplicate announcements, loopback/self filtering, stale peer expiry, hidden rooms, and manual peer fallback.

### Feature completion and hardening

- [x] Add deterministic coverage for incoming receive prompt decisions, auto-accept behavior, unknown/offline sender rejection, Android host matching, selected-peer send targeting, asynchronous outgoing sends, and progress diagnostic de-duplication.
- [ ] Runtime-complete file-transfer parity for incoming receive prompts, accept/decline side effects, auto-accept behavior, unknown/offline sender rejection, selected-peer send targeting, listener port selection, failed transfer recovery, Android file receiver ports, and advanced transfer controls.
- [ ] Runtime-test quick-share start/stop, copy index, text links, file links, expiration/access limits, stopped/expired rows, remote LAN access diagnostics, and trusted-LAN warning clarity.
- [ ] Runtime-test steganography BMP inspect, hide, encrypted hide, extract, encrypted extract, file chooser behavior, output path behavior, success copy, and failure copy.
- [x] Add speaker output enumeration/testing to the WebRTC media device service boundary and Compose state.
- [ ] Stabilize voice as the primary realtime flow: microphone selection, speaker output selection, voice start/end, SDP/ICE diagnostics, audio levels, runtime failure recovery, and cross-device behavior.
- [x] Add deterministic camera-preview failure handling so failed preview startup clears preview state and reports diagnostics.
- [ ] Keep experimental video guarded while validating camera selection, camera preview start/stop, frame/status labels, 1-to-1 video smoke behavior, and fallback diagnostics on real devices.
- [ ] Preserve visible diagnostics for discovery, chat, file transfer, quick share, stego, RTC provider initialization, SDP/ICE, media devices, audio levels, video frames, preview conversion, and runtime failures.

### User-friendly Compose UX modernization

- [x] Begin UX modernization within existing product behavior for transfer, quick-share, steganography, audio/video setup, diagnostics, tooltips, and clearer primary actions.
- [ ] Continue UX modernization only within accepted behavior: clearer navigation, less visual noise, better grouped workflows, and preserved diagnostics.
- [x] Improve deterministic empty/disabled/warning/error/active-transfer/stopped-share/readiness copy in Compose state models.
- [ ] Runtime-review empty, loading, disabled, warning, error, active-transfer, active-call, and stopped-share states in the live Compose shell.
- [ ] Improve keyboard behavior, including Enter-to-send and safe focus traversal across dense desktop forms.
- [ ] Improve accessibility labels, contrast, selected/hover states, and visible validation feedback.
- [ ] Validate resizable desktop layout quality for narrow, normal, and wide windows.
- [ ] Keep desktop density; do not turn the desktop shell into a mobile-style layout.

### Tests and validation

- [ ] Keep [`gradlew.bat :apps:desktop-client:test --no-daemon`](../../gradlew.bat) green after each Compose UI/runtime change.
- [ ] Keep [`gradlew.bat :apps:desktop-client:build --no-daemon`](../../gradlew.bat) green after each Compose UI/runtime change.
- [ ] Run [`gradlew.bat :apps:desktop-client:runComposeShell --no-daemon`](../../gradlew.bat) for manual runtime checks when UI behavior changes.
- [x] Add deterministic tests for new capability mapping, peer readiness, file-transfer decisions, transfer diagnostics, media device output state, camera preview failure state, diagnostic channels, and packaging readiness gates.
- [ ] Keep adding deterministic tests for new state mapping, formatting, readiness, diagnostics, and adapter behavior instead of putting business logic in composables.
- [ ] Run broader desktop/Android interoperability validation before any launcher promotion.

### Packaging, release, and promotion

- [ ] Validate portable ZIP with [`gradlew.bat :apps:desktop-client:buildPortable --no-daemon`](../../gradlew.bat).
- [ ] Smoke-launch the generated portable artifact from [`apps/desktop-client/build/distributions`](../../apps/desktop-client/build.gradle:100).
- [ ] Validate the separate Compose portable ZIP with [`gradlew.bat :apps:desktop-client:buildComposePortable --no-daemon`](../../gradlew.bat).
- [ ] Smoke-launch the generated Compose portable artifact from [`apps/desktop-client/build/distributions`](../../apps/desktop-client/build.gradle) and verify it uses the Compose entry point without changing JavaFX packaging.
- [ ] Validate Windows EXE packaging with [`gradlew.bat :apps:desktop-client:buildExe --no-daemon`](../../gradlew.bat) or [`gradlew.bat :apps:desktop-client:createExe --no-daemon`](../../gradlew.bat) on a WiX-enabled Windows environment.
- [ ] Smoke-test install, launch, and uninstall behavior for the generated Windows installer from [`apps/desktop-client/build/packaging`](../../apps/desktop-client/build.gradle:158).
- [ ] Decide explicitly whether to keep JavaFX fallback, promote Compose as the packaged launcher, or remove unused JavaFX paths.
- [ ] Change [`application.mainClass`](../../apps/desktop-client/build.gradle:14), manifest behavior, jpackage main class, or JavaFX files only after runtime parity, UX stabilization, packaging validation, rollback plan, and explicit approval are complete.

## Validation commands

- Desktop Compose tests: [`gradlew.bat :apps:desktop-client:test --no-daemon`](../../gradlew.bat)
- Desktop build: [`gradlew.bat :apps:desktop-client:build --no-daemon`](../../gradlew.bat)
- Experimental Compose shell: [`gradlew.bat :apps:desktop-client:runComposeShell --no-daemon`](../../gradlew.bat)
- JavaFX fallback launcher: [`gradlew.bat :apps:desktop-client:run --no-daemon`](../../gradlew.bat)
- Portable ZIP: [`gradlew.bat :apps:desktop-client:buildPortable --no-daemon`](../../gradlew.bat)
- Compose portable ZIP: [`gradlew.bat :apps:desktop-client:buildComposePortable --no-daemon`](../../gradlew.bat)
- Windows EXE: [`gradlew.bat :apps:desktop-client:buildExe --no-daemon`](../../gradlew.bat)
