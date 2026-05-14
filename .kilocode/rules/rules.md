# SecureLanSuite Assistant Rules

## Purpose

This file is a concise rule set for assistant decisions. It must not duplicate full project documentation.

For detailed human-readable project docs, use:
- [`README.md`](../../README.md)
- [`docs/migration-roadmap.md`](../../docs/migration-roadmap.md)
- [`docs/webrtc-architecture.md`](../../docs/webrtc-architecture.md)
- [`docs/wix-installation.md`](../../docs/wix-installation.md)

## Project Baseline

- Project: **SecureLanSuite**.
- Current version: `0.3.11-SNAPSHOT`.
- Language: **Java 25**.
- Build: **Gradle multi-project**.
- UI: **JavaFX 25.0.2** in `apps/desktop-client` only.
- Realtime: **webrtc-java 0.14.0** in `modules/webrtc-core`.
- Packaging: `jpackage`; WiX **5.0.2** for Windows EXE installers.
- Architecture: modular monorepo with UI-agnostic core modules.

## Non-Negotiable Architecture Rules

- Do not introduce Spring or Spring Boot unless explicitly requested.
- Keep reusable core modules UI-agnostic.
- Do not put JavaFX code in reusable core modules.
- Do not make any module depend on `apps/desktop-client`.
- Avoid cyclic dependencies.
- Keep crypto logic out of UI code.
- Keep network and transport orchestration behind service boundaries where practical.
- Prefer plain Java, constructor injection, explicit interfaces, records for immutable DTOs, and small focused classes.

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

## Current Product Constraints

Treat these as current constraints when planning or implementing:

- UDP LAN discovery is implemented, but still needs hardening for firewalls, VPNs, multi-adapter networks, and complex LANs.
- `common-net` currently provides shared network constants; richer reusable transport abstractions are still future work.
- File transfer is encrypted and progress-aware, but advanced transfer controls are not fully exposed in the UI.
- `webrtc-core` supports data channels and voice as primary realtime flows.
- Camera preview and 1-to-1 video exist, but video remains experimental.
- Microphone and camera capture selection is exposed; audio output device selection is not exposed yet.
- Chunked large-file transfer over `RTCDataChannel` is not implemented.
- Screen sharing is not implemented.
- `stego-core` is reserved for future steganography workflows.

## Realtime Rules

- Keep RTC signaling transport routed through `chat-core`.
- Keep provider/runtime logic inside `webrtc-core`.
- Treat voice as the primary stable realtime media flow.
- Treat video/camera preview as experimental until cross-device stability improves.
- Preserve diagnostics for provider initialization, SDP/ICE, media devices, audio levels, video frames, preview conversion, and runtime failures.
- Keep `audio-core` and `webcam-core` as profile/configuration modules unless standalone media workflows are explicitly prioritized.

## Legacy Migration Rules

Legacy repositories are source material, not final architecture references:

- `java-crypto` and `java-encryption-tool` map primarily to `modules/crypto-core`.
- `java-lan-chat` maps to `modules/chat-core`.
- `java-file-transceiver` maps to `modules/file-transfer-core`.
- `java-audio-transceiver` is lower priority because current voice uses WebRTC.
- `webcam-catcher` is lower priority because current camera/video uses WebRTC.
- `java-steganography-tool` maps to future `modules/stego-core`.

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

## Implementation Style Rules

Prefer:

- incremental, focused changes;
- small classes and explicit service boundaries;
- immutable shared models and Java records;
- validation of important constructor/input values;
- deterministic tests for pure logic;
- clear diagnostics for network, file-transfer, and realtime failures.

Avoid:

- large unstructured rewrites;
- copying legacy repositories as-is;
- giant UI/controller classes when reusable services are appropriate;
- hidden framework magic;
- expanding experimental video features without preserving fallback and diagnostics.

## Documentation Maintenance Rules

When changing architecture, supported Java version, module responsibilities, packaging flow, or product status:

1. Update the relevant public docs in [`README.md`](../../README.md) or [`docs`](../../docs).
2. Keep this file short and rule-focused.
3. Do not duplicate full roadmap or how-to content here.
4. Add only constraints that should affect future assistant decisions.
