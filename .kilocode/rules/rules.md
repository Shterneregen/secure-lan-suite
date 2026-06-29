# SecureLanSuite Assistant Rules

## Purpose

This file is a concise rule set for assistant decisions. It must not duplicate full project documentation.

For detailed human-readable project docs, use:
- [`README.md`](../../README.md)
- [`docs/development.md`](../../docs/development.md)
- [`docs/kotlin-migration/kotlin-migration.md`](../../docs/kotlin-migration/kotlin-migration.md)
- [`docs/migration-roadmap.md`](../../docs/migration-roadmap.md)
- [`docs/webrtc-architecture.md`](../../docs/webrtc-architecture.md)
- [`docs/wix-installation.md`](../../docs/wix-installation.md)

## Project Baseline

- Project: **SecureLanSuite**.
- Current version: `0.5.0`.
- JVM baseline: **Java 25** toolchain and Java compile release.
- Kotlin baseline: **Kotlin 2.2.21** for Android, migrated JVM modules, desktop helpers, and the experimental desktop Compose shell.
- Kotlin JVM modules currently compile with JVM target **24** while the Java toolchain remains **25**; keep target validation ignored until Kotlin supports JVM target 25.
- Build: **Gradle multi-project**; Gradle **9.1+** is recommended.
- Desktop UI: **Compose Multiplatform 1.9.0** is the Compose-first target for new desktop UI/UX work in `apps/desktop-client`.
- Deprecated desktop fallback: **JavaFX 25.0.2** remains the packaged launcher/runtime fallback only for rollback and critical fixes until explicit Compose promotion/removal is accepted.
- Android UI: experimental native Android client in `apps/android-client` uses Android Gradle Plugin **8.13.2**, Android SDK **35**, Kotlin **2.2.21**, and Jetpack Compose Material 3.
- Realtime: **webrtc-java 0.14.0** in `modules/webrtc-core`.
- Packaging: `jpackage`; WiX **5.0.2** for Windows EXE installers.
- Architecture: modular monorepo with UI-agnostic core modules.

## Non-Negotiable Architecture Rules

- Do not introduce Spring or Spring Boot unless explicitly requested.
- Keep reusable core modules UI-agnostic.
- Do not put JavaFX, Compose, Android, or other UI framework code in reusable core modules.
- Keep JavaFX and Compose desktop UI code inside `apps/desktop-client`.
- Keep Android app/UI/platform code inside `apps/android-client` unless an explicit shared Android-safe abstraction is designed.
- Do not make any module depend on `apps/desktop-client` or `apps/android-client`.
- Avoid cyclic dependencies.
- Keep crypto logic out of UI code.
- Keep network and transport orchestration behind service boundaries where practical.
- Prefer plain Java/Kotlin, constructor injection, explicit interfaces, Kotlin/JVM records or Java records for record-compatible immutable DTOs, defensive-copy classes for byte-array payload models, and small focused classes.

## Module Boundary Rules

Allowed internal dependency directions:

- `apps/*` may depend on `modules/*`.
- `common-model` must not depend on internal modules.
- `common-net` may depend only on `common-model`.
- `crypto-core` may depend only on `common-model`.
- `chat-core` may depend on `common-model`, `common-net`, `crypto-core`.
- `file-transfer-core` may depend on `common-model`, `common-net`, `crypto-core`.
- `webrtc-core` may depend on `common-model` and external `webrtc-java`.
- `audio-core` may depend on `common-model`, `common-net`.
- `webcam-core` may depend on `common-model`.
- `stego-core` may depend on `common-model`, `crypto-core`.

## Current UI Priority

Current priority: user-friendly Compose Desktop UI hardening and JavaFX deprecation.

When the task is about improving the interface, UX, usability, layout clarity, or user-friendly Compose UI, use JavaFX as a behavioral reference rather than a strict visual target. Preserve protocols, features, service boundaries, diagnostics availability, and JavaFX fallback, but improve information architecture, layout hierarchy, navigation, labels, validation, empty/error/loading states, accessibility, and desktop responsiveness.

For tasks that explicitly reference Phase 11 UI/UX redesign, `docs/kotlin-migration/phase-11.md`, or the messenger-first redesign checklist, Phase 11 is the accepted modernization baseline. In that mode, do not preserve the old `Room connection + Peers + Chat + Actions` dashboard layout. JavaFX and old screenshots are behavioral/rollback references only, not visual acceptance criteria.

Do not let JavaFX parity block useful UX improvements when the user explicitly wants a better Compose interface. For pure migration/parity tasks, keep JavaFX as the visual and behavioral source of truth.

## Kotlin and UI Migration Rules

- The reusable core-module Java-to-Kotlin migration is completed; preserve public API compatibility and Java-callable contracts when changing migrated modules.
- Do not start large whole-repository rewrites. Keep each Java/Kotlin migration or UI replacement as a small, independently validated slice.
- Keep `Main.java`, `ChatApplication.java`, `MainView.kt`, and `MainViewDelegate.java` as deprecated JavaFX fallback boundaries until Compose runtime, feature parity, portable ZIP, and WiX EXE validation justify replacement.
- Do not add new JavaFX UI features or non-critical JavaFX UX polish. New desktop UI/UX improvements belong in Compose; JavaFX changes should be limited to critical fixes and fallback preservation.
- Compose Multiplatform work must stay under `apps/desktop-client` and must not move Compose runtime dependencies into reusable modules.
- Use `:apps:desktop-client:runComposeShell` for the experimental Compose shell; do not change `application.mainClass`, JAR manifest, or `jpackage` main class unless explicitly promoting Compose after validation.
- Preserve discovery, chat, file transfer, quick share, steganography, RTC signaling, voice, and experimental video behavior during UI migration.
- Maintain an obvious Diagnostics entry point before retiring JavaFX equivalents in Compose slices; raw logs and technical details may move behind Advanced/Diagnostics in UX modernization work.

## Current Product Constraints

Treat these as current constraints when planning or implementing:

- UDP LAN discovery is implemented, but still needs hardening for firewalls, VPNs, multi-adapter networks, and complex LANs.
- `common-net` provides shared network constants, TCP endpoint/socket helpers, reusable text/frame channels, server accept-loop utilities, close helpers, and UDP broadcast address resolution.
- File transfer is encrypted and progress-aware, but advanced transfer controls are not fully exposed in the UI.
- Quick share exposes temporary no-auth LAN browser links and must be treated as trusted-LAN-only local HTTP sharing.
- Desktop steganography tools exist for BMP text payload hide/extract workflows, including password-encrypted payloads through `stego-core`.
- Android interoperability MVP exists for desktop discovery, secure chat, encrypted file send/receive, transfer progress, dark theme toggle, and diagnostics logs.
- `webrtc-core` supports data channels and voice as primary realtime flows.
- Camera preview and 1-to-1 video exist, but video remains experimental.
- Microphone and camera capture selection is exposed; audio output device selection is not exposed yet.
- Chunked large-file transfer over `RTCDataChannel` is not implemented.
- Screen sharing is not implemented.

## Realtime Rules

- Keep RTC signaling transport routed through `chat-core`.
- Keep provider/runtime logic inside `webrtc-core`.
- Treat voice as the primary stable realtime media flow.
- Treat video/camera preview as experimental until cross-device stability improves.
- Preserve diagnostics for provider initialization, SDP/ICE, media devices, audio levels, video frames, preview conversion, and runtime failures.
- Keep `audio-core` and `webcam-core` as profile/configuration modules unless standalone media workflows are explicitly prioritized.

## Android Interoperability Rules

- Keep reusable modules free of Android UI and Android platform dependencies unless an explicit architecture change is requested.
- Preserve desktop-compatible UDP discovery, secure chat handshake, AES-GCM/RSA file-transfer handshake, metadata formats, and transfer behavior.
- Keep Android protocol compatibility changes coordinated with desktop/core tests and docs.
- Release signing must remain configurable through Gradle properties or `ANDROID_RELEASE_*` environment variables, with debug signing fallback for local installable builds.

## Legacy Migration Rules

Legacy repositories are source material, not final architecture references:

- `java-crypto` and `java-encryption-tool` map primarily to `modules/crypto-core`.
- `java-lan-chat` maps to `modules/chat-core`.
- `java-file-transceiver` maps to `modules/file-transfer-core`.
- `java-audio-transceiver` is lower priority because current voice uses WebRTC.
- `webcam-catcher` is lower priority because current camera/video uses WebRTC.
- `java-steganography-tool` maps to `modules/stego-core`.

When migrating, extract reusable services/models and remove legacy CLI/UI orchestration.

## Packaging Rules

- Portable ZIP: `:apps:desktop-client:buildPortable`.
- Windows EXE: `:apps:desktop-client:buildExe` or `:apps:desktop-client:createExe`.
- Portable ZIP output: `apps/desktop-client/build/distributions/`.
- `jpackage` output: `apps/desktop-client/build/packaging/`.
- WiX is required only for Windows EXE installer builds.
- Use WiX 5.0.2 with `WixToolset.UI.wixext` and `WixToolset.Util.wixext`.
- Do not recommend WiX 7 unless packaging is revalidated.
- There is currently no `printPackagingEnvironment` Gradle task.
- Compose runtime dependency changes are packaging-sensitive; validate `:apps:desktop-client:build`, portable ZIP, and Windows EXE before promoting Compose-only desktop UI.

## Implementation Style Rules

Prefer:

- incremental, focused changes;
- small classes and explicit service boundaries;
- immutable shared models and Java/Kotlin JVM records where constructor contracts are record-compatible;
- defensive-copy classes for byte-array payload models;
- validation of important constructor/input values;
- deterministic tests for pure logic;
- clear diagnostics for network, file-transfer, Android interoperability, and realtime failures.

Avoid:

- large unstructured rewrites;
- copying legacy repositories as-is;
- giant UI/controller classes when reusable services are appropriate;
- hidden framework magic;
- protocol or wire-format drift between desktop and Android;
- expanding experimental video features without preserving fallback and diagnostics.

## Documentation Maintenance Rules

When changing architecture, supported Java/Kotlin versions, module responsibilities, UI migration status, packaging flow, Android interoperability, or product status:

1. Update the relevant public docs in [`README.md`](../../README.md) or [`docs`](../../docs).
2. Keep this file short and rule-focused.
3. Do not duplicate full planning or how-to content here.
4. Add only constraints that should affect future assistant decisions.

## Compose UI Quality Rules

- JavaFX UI, CSS, screenshots, and existing layout structure are the visual source of truth only during strict parity migration. During explicit UX modernization or Phase 11 redesign work, the accepted modernization checklist and approved redesign screenshots are the visual source of truth.
- Do not redesign screens unless explicitly requested.
- Compose slices must aim for production-level visual quality, even while the Compose shell is experimental.
- Preserve visual hierarchy, spacing, colors, grouping, toolbar/sidebar/status-area structure, disabled states, loading states, and diagnostics in parity mode. In Phase 11 redesign mode, preserve behavior and feature availability, but replace the old dashboard hierarchy with the messenger-first screen model.
- Compose Desktop UI must look like a desktop application, not an Android/mobile screen.
- Prefer compact desktop spacing, resizable layouts, desktop navigation patterns, hover states, split panes, sidebars, and clear information density.
- Before implementing a Compose screen, inspect the corresponding JavaFX/FXML/CSS/controller code and summarize the current layout.
- After implementation, compare the Compose result against the JavaFX baseline for parity tasks, or against the Phase 11 checklist/design baseline for redesign tasks.
- Even though Compose shell is experimental, Compose UI slices must be implemented with production-level visual quality and feature parity goals.
- Do not remove JavaFX fallback boundaries, but Compose code may introduce clean independent screen structure, reusable composables, state holders, and UI abstractions.
- For strict parity UI migration, each slice must still preserve the full screen composition and visual hierarchy. For Phase 11 redesign, each slice must preserve functional behavior while following the new messenger-first composition.
## Code Artifact Planning-Status Rule

- Do not add phase numbers, active-phase statements, roadmap/status-plan wording, migration-stage labels, launcher-stage labels, or temporary work-stage notes to source code, tests, resources, comments, metadata, or user-facing strings.
- Code artifacts must describe only the current technical behavior, state, constraints, and service boundaries.
- Keep historical planning, roadmap status, migration status, and future work sequencing only in documentation or task trackers.
