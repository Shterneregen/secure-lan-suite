# WebRTC Architecture in SecureLanSuite

This document describes the realtime layer built on the secure chat, discovery,
and file-transfer stack.

## Ownership

- `chat-core` transports realtime signaling envelopes over an established
  secure chat connection.
- `webrtc-core` owns session orchestration and state, signaling handlers,
  diagnostics, media-device discovery, camera preview, and native
  `webrtc-java` integration.
- `audio-core` and `webcam-core` provide media defaults and profile hints; they
  are not separate realtime transports.
- `desktop-client` owns call controls, media settings, video presentation, and
  conversion of runtime events into UI state.

## Supported flows

Primary flows:

- RTC data-channel sessions;
- voice sessions backed by native `webrtc-java`;
- SDP and ICE signaling over secure chat;
- microphone, speaker, and camera enumeration and selection;
- microphone and speaker tests;
- realtime state and diagnostic events.

Implemented experimental flows:

- camera preview;
- one-to-one video calls and inline video stage;
- local and remote frame propagation;
- preview conversion guards and failure diagnostics.

Video remains experimental until capture, preview, remote rendering, and
device/runtime compatibility have broader validation.

## Runtime behavior

`RtcSessionService` creates an `RtcEngine` through `RtcEngineProvider`. Startup
tries the native engine and falls back to `NoOpRtcEngine` when initialization
fails.

`WebRtcJavaEngine` provides:

- offer/answer negotiation and trickle ICE over `chat-core`;
- automatic data-channel creation for local outbound sessions;
- microphone capture and playout through `AudioDeviceModule`;
- local and remote audio-level events;
- camera capture through `MediaDevices`, `VideoDeviceSource`, and `VideoTrack`;
- selected capture-device IDs for sessions;
- remote audio/video track observation;
- diagnostic events and video preview conversion guards.

## Desktop experience

The desktop experience is chat-first and voice-first. A user selects a peer,
keeps the conversation available, and starts voice or experimental video from
the conversation header or contextual actions.

`LiveActionsColumn` builds the right context area from the current selection and
activity. It shows relevant call or transfer summaries and Quick Share activity;
it is not a permanent inventory of media controls, RTC tools, or diagnostics.
At narrow widths the same context moves into a drawer.

Microphone, speaker, camera, and preview controls live in **Settings → Audio &
Video**. The settings UI enumerates input and output devices, persists the
selected speaker through `media.outputDeviceId`, and supports microphone,
speaker, and camera tests. If no device is selected, the runtime uses an
available default.

Diagnostics primarily go to logs. Runtime failures may be summarized in the
affected call or settings flow, but no standalone Diagnostics screen is
required.

### Video preview flags

The desktop client reads these JVM system properties:

- `securelan.rtc.videoPreview.local.enabled`
- `securelan.rtc.videoPreview.remote.enabled`

Both are enabled by default. They can be disabled while investigating capture
or frame-conversion problems.

## Signaling flow

Signaling travels over the V2 password-authenticated chat record channel. The room
host can read SDP/ICE; this is not end-to-end encrypted signaling. See the
[security protocol](security-protocol.md) for its trust boundaries.

Desktop incoming offers wait for an explicit Accept/Decline decision before native
session creation or microphone/camera capture. The incoming dialog identifies the
requested media, and dismissal declines. Setup times out after 30 seconds; busy,
foreign, unexpected and retired-session signals cannot replace the active call.
Disconnect, host stop and peer departure close realtime resources. Native audio
recording/playout stops after the last session releases its device lease.

```mermaid
sequenceDiagram
    participant UI as Desktop UI
    participant RTC as webrtc-core
    participant Chat as chat-core
    participant Peer as Remote peer

    UI->>RTC: Start data, voice, or video session
    RTC->>Chat: Publish SDP offer or ICE candidate
    Chat->>Peer: Send signal over secure chat session
    Peer->>Chat: Return SDP answer or ICE candidate
    Chat->>RTC: Deliver inbound RTC signal
    RTC->>UI: Publish state, media, and diagnostic events
```

## Diagnostic coverage

The runtime reports native provider initialization, signaling and ICE progress,
selected and default devices, audio levels, video frames, preview conversion,
camera preview state, warnings, and failures. Logs must not contain room secrets,
private keys, passwords, or raw sensitive payloads.

## Current constraints and next work

- Video can fail on some Windows, JDK, camera, or driver combinations.
- The video experience is one-to-one rather than a conference grid.
- Chunked large-file transfer over `RTCDataChannel` is not implemented.
- Screen sharing is not implemented.
- Video needs broader cross-machine and cross-device validation before it can be
  presented as stable.
- Preview and capture flags may move from JVM properties to explicit
  experimental settings when that flow is designed.
