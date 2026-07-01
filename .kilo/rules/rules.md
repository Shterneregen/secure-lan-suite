# SecureLanSuite Assistant Rules

## Purpose

This file contains only non-negotiable technical and architectural rules for SecureLanSuite.

It must stay short.

For UI/UX decisions, use the Product Specification and `../knowledge/INDEX.md`.
Do not duplicate the full Product Specification here.

---

## Project Baseline

- Project: **SecureLanSuite**.
- Desktop UI target: **Compose Multiplatform** in `apps/desktop-client`.
- JavaFX remains only as a deprecated launcher/runtime fallback and critical-fix path until explicit Compose promotion/removal is accepted.
- Android client remains in `apps/android-client`.
- Reusable core modules must remain UI-agnostic.
- Build system: Gradle multi-project.
- Packaging: portable ZIP and Windows EXE via `jpackage`; WiX is required only for Windows EXE installer builds.

---

## Product Specification Rule

For any UI/UX, Compose screen, layout, Phase 11, redesign, modernization, or usability task:

1. Read `../knowledge/INDEX.md`.
2. Use the Product Specification as the source of truth.
3. Use JavaFX only as a behavioral and fallback reference.
4. Do not preserve the old dashboard visual layout.
5. Do not expose engineering concepts in normal UI.
6. Validate the result with the Product Scorecard.

The old `Room connection + Peers + Chat + Actions` dashboard is not a valid visual target for new UI work.

---

## Non-Negotiable Architecture Rules

- Do not introduce Spring or Spring Boot unless explicitly requested.
- Keep reusable core modules UI-agnostic.
- Do not put JavaFX, Compose, Android, or any other UI framework code in reusable core modules.
- Keep JavaFX and Compose desktop UI code inside `apps/desktop-client`.
- Keep Android app/UI/platform code inside `apps/android-client` unless an explicit shared Android-safe abstraction is designed.
- Do not make any module depend on `apps/desktop-client` or `apps/android-client`.
- Avoid cyclic dependencies.
- Keep crypto logic out of UI code.
- Keep network and transport orchestration behind service boundaries where practical.
- Prefer plain Java/Kotlin, constructor injection, explicit interfaces, immutable DTOs, small focused classes, and deterministic tests for pure logic.

---

## Module Boundary Rules

Allowed dependency directions:

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

---

## Compose Desktop Rules

- New desktop UI/UX work belongs in Compose, not JavaFX.
- Compose Multiplatform work must stay under `apps/desktop-client`.
- Do not move Compose runtime dependencies into reusable modules.
- Do not change `application.mainClass`, JAR manifest, or `jpackage` main class unless explicitly promoting Compose after validation.
- Preserve JavaFX fallback boundaries until explicit removal is accepted.
- Do not add new JavaFX screens or non-critical JavaFX polish.
- Do not put business logic in composables.
- Do not put all UI into a single composable.
- Use project design tokens and approved components.
- Do not use random colors, arbitrary spacing, or hard-coded visual values.

---

## Protocol Preservation Rules

Do not change the following protocols or wire formats as part of UI work unless explicitly requested:

- UDP LAN discovery
- secure chat handshake
- encrypted file transfer handshake
- Quick Share behavior
- steganography payload behavior
- RTC signaling
- voice flow
- experimental video flow
- Android interoperability formats

UI changes must preserve existing service boundaries and protocol compatibility.

---

## Feature Constraints

Treat these as current product constraints:

- UDP LAN discovery exists, but still needs hardening for firewalls, VPNs, multi-adapter networks, and complex LANs.
- File transfer is encrypted and progress-aware, but advanced transfer controls are not fully exposed.
- Quick Share exposes temporary no-auth LAN browser links and must be treated as trusted-LAN-only local HTTP sharing.
- Desktop steganography supports BMP text payload hide/extract workflows, including password-encrypted payloads through `stego-core`.
- Android interoperability MVP exists for discovery, secure chat, encrypted file send/receive, transfer progress, dark theme toggle, and diagnostics logs.
- Voice is the primary stable realtime media flow.
- Camera preview and 1-to-1 video exist, but video remains experimental.
- Microphone and camera capture selection are exposed.
- Audio output device selection is not exposed yet.
- Chunked large-file transfer over `RTCDataChannel` is not implemented.
- Screen sharing is not implemented.

---

## Realtime Rules

- Keep RTC signaling transport routed through `chat-core`.
- Keep provider/runtime logic inside `webrtc-core`.
- Treat voice as the primary stable realtime media flow.
- Treat video/camera preview as experimental until cross-device stability improves.
- Preserve diagnostics for provider initialization, SDP/ICE, media devices, audio levels, video frames, preview conversion, and runtime failures.
- Keep `audio-core` and `webcam-core` as profile/configuration modules unless standalone media workflows are explicitly prioritized.

---

## Android Interoperability Rules

- Keep reusable modules free of Android UI and Android platform dependencies unless an explicit architecture change is requested.
- Preserve desktop-compatible UDP discovery, secure chat handshake, AES-GCM/RSA file-transfer handshake, metadata formats, and transfer behavior.
- Coordinate protocol compatibility changes with desktop/core tests and docs.
- Release signing must remain configurable through Gradle properties or `ANDROID_RELEASE_*` environment variables, with debug signing fallback for local installable builds.

---

## Legacy Migration Rules

Legacy repositories are source material, not final architecture references:

- `java-crypto` and `java-encryption-tool` map primarily to `modules/crypto-core`.
- `java-lan-chat` maps to `modules/chat-core`.
- `java-file-transceiver` maps to `modules/file-transfer-core`.
- `java-audio-transceiver` is lower priority because current voice uses WebRTC.
- `webcam-catcher` is lower priority because current camera/video uses WebRTC.
- `java-steganography-tool` maps to `modules/stego-core`.

When migrating, extract reusable services/models and remove legacy CLI/UI orchestration.

---

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

---

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
- giant UI/controller classes;
- hidden framework magic;
- protocol or wire-format drift between desktop and Android;
- expanding experimental video features without preserving fallback and diagnostics.

---

## Documentation Maintenance Rules

When changing architecture, supported Java/Kotlin versions, module responsibilities, UI migration status, packaging flow, Android interoperability, or product status:

1. Update the relevant public docs in `README.md` or `docs/`.
2. Keep this file short and rule-focused.
3. Do not duplicate full planning or how-to content here.
4. Add only constraints that should affect future assistant decisions.

---

## Code Artifact Planning-Status Rule

- Do not add phase numbers, active-phase statements, roadmap/status-plan wording, migration-stage labels, launcher-stage labels, or temporary work-stage notes to source code, tests, resources, comments, metadata, or user-facing strings.
- Code artifacts must describe only the current technical behavior, state, constraints, and service boundaries.
- Keep historical planning, roadmap status, migration status, and future work sequencing only in documentation or task trackers.
