---
name: securelan-realtime
license: MIT
description: >
  SecureLanSuite realtime, WebRTC, voice, video, RTC data-channel, media-device,
  and diagnostics skill. Use when the user asks about webrtc-core, signaling,
  SDP/ICE, voice sessions, experimental video, camera preview, microphone or
  camera selection, RTC diagnostics, or realtime desktop controls.
---

# SecureLanSuite Realtime Skill

Use this skill for work touching `modules/webrtc-core`, `modules/audio-core`, `modules/webcam-core`, realtime signaling in `modules/chat-core`, or desktop realtime controls.

## Current architecture

- `chat-core` transports RTC signaling envelopes over the secure chat path.
- `webrtc-core` owns RTC session state, signaling integration, diagnostics, runtime/provider integration, data channels, voice, and experimental video.
- `audio-core` and `webcam-core` provide profile/configuration hints, not standalone realtime transports.
- Desktop UI exposes voice as the primary stable media flow and video as experimental.

Read [`docs/webrtc-architecture.md`](../../../docs/webrtc-architecture.md) before making realtime architecture changes.

## Non-negotiable rules

- Keep RTC signaling transport routed through `chat-core`.
- Keep provider/runtime logic inside `webrtc-core`.
- Treat voice as the primary stable realtime media flow.
- Treat camera preview and 1-to-1 video as experimental.
- Preserve diagnostics for provider initialization, SDP/ICE, media devices, audio levels, video frames, preview conversion, and runtime failures.
- Do not add UI dependencies to reusable realtime modules.
- Do not introduce chunked large-file transfer over `RTCDataChannel` unless the user explicitly requests that feature.
- Do not introduce screen sharing unless the user explicitly requests it.

## Change strategy

- Prefer small changes around event models, configuration, diagnostics, and service boundaries.
- Be cautious around native `webrtc-java` initialization, callback threading, media source lifetimes, track ownership, and device enumeration.
- Preserve fallback behavior to `NoOpRtcEngine` when native provider initialization fails.
- Avoid swallowing runtime errors; surface them through diagnostics.
- Keep media-device selection explicit and null-safe.
- Preserve JVM system-property behavior for local and remote video preview toggles unless replacing it with an explicit UI setting.

## Validation defaults

- Realtime module tests: `gradlew.bat :modules:webrtc-core:test --no-daemon`.
- Desktop tests when UI state or controls change: `gradlew.bat :apps:desktop-client:test --no-daemon`.
- Full desktop build when runtime wiring changes: `gradlew.bat :apps:desktop-client:build --no-daemon`.
- Manual cross-device smoke checks are recommended for voice/video behavior but should not be claimed unless actually performed.

## Documentation rule

Update [`docs/webrtc-architecture.md`](../../../docs/webrtc-architecture.md) and, when product status changes, [`README.md`](../../../README.md), if a change alters supported realtime flows, diagnostics, media-device behavior, video stability assumptions, or future realtime constraints.
