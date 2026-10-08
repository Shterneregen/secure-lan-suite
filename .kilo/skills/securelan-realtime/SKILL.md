---
name: securelan-realtime
license: MIT
description: >
  Work on SecureLanSuite RTC data, voice, experimental video, media devices,
  signaling, native runtime integration, and diagnostics. Use for webrtc-core,
  related chat signaling, desktop call controls, or audio/video settings.
---

# SecureLanSuite Realtime

Read [`docs/webrtc-architecture.md`](../../../docs/webrtc-architecture.md) and
inspect the current runtime and UI before changing realtime behavior.

## Boundaries

- Keep RTC signaling on the secure `chat-core` path.
- Keep provider and native runtime logic in `webrtc-core`.
- Keep reusable realtime modules free of UI dependencies.
- Preserve fallback to `NoOpRtcEngine` when provider initialization fails.
- Treat voice as the primary media flow and video as experimental until current
  runtime evidence changes that status.
- Preserve useful diagnostics for initialization, SDP/ICE, devices, levels,
  frames, preview conversion, and runtime failures without logging secrets.

## Change and verification

- Check callback threading, media-source lifetime, track ownership, device
  selection, and null/unavailable-device behavior.
- Prefer focused changes to event models, configuration, diagnostics, and
  service boundaries.
- Run `:modules:webrtc-core:test` for core changes and desktop tests when state
  or controls change; build the desktop client when runtime wiring changes.
- Report manual cross-device behavior only when it was actually exercised.
- Update the WebRTC document and root README when supported flows, stability,
  device behavior, or constraints change.
