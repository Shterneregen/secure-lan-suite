# WebRTC architecture in SecureLanSuite

This document describes the realtime layer built on top of the existing secure chat and file-transfer stack.

## Current architecture

- `chat-core` transports realtime signaling envelopes over the already established secure chat connection.
- `webrtc-core` owns realtime session orchestration, session state, signaling handlers, diagnostics, and the native `webrtc-java` runtime integration.
- `audio-core` and `webcam-core` currently provide runtime-facing media defaults and profile hints rather than a separate transport stack.
- `desktop-client` surfaces realtime controls inside the messenger-style UI:
  - peer selection on the left
  - chat/activity feed in the center
  - quick actions, voice status, transfers, and advanced tools on the right

## What is supported now

### Stable / primary flows
- `RTCDataChannel` sessions
- voice sessions backed by native `webrtc-java`
- signaling exchange through the existing secure chat path
- realtime state and diagnostics in the desktop client

### Experimental flows
- camera/video transport
- inline 1-to-1 video stage in the desktop client
- remote preview event propagation through `VideoBufferConverter`
- self preview disabled by default for stability, with an opt-in runtime flag
- advanced runtime diagnostics for video capture and frame conversion

Video-related controls are intentionally hidden from the main UX until the implementation is stable enough for normal use.

## Runtime behavior

The default `RtcSessionService` creates a concrete `RtcEngine` through `RtcEngineProvider`.

Startup behavior:
- try to initialize a native `webrtc-java` engine
- fall back to `NoOpRtcEngine` only if native initialization fails

`WebRtcJavaEngine` currently provides:
- offer/answer negotiation through the existing secure chat signaling path
- trickle ICE candidate exchange over `chat-core`
- automatic `RTCDataChannel` creation for local outbound sessions
- microphone capture and playout through `AudioDeviceModule`
- audio level events for local and remote streams
- camera capture through `MediaDevices` + `VideoDeviceSource` + `VideoTrack`
- remote track observation for audio/video state reporting
- diagnostic logging and preview conversion guards for video troubleshooting

## Desktop UX model

The current desktop UX is voice-first and chat-first.

### Main workflow
1. start or connect to a secure chat session
2. select a peer from the left peer list
3. exchange messages in the center feed
4. trigger quick actions from the right panel:
   - send file
   - start voice
   - hang up

### Advanced / Experimental section
The right panel also contains a place for:
- provider/runtime details
- diagnostics and debug status
- notes about preview stability and runtime toggles

## Diagnostics

The runtime now exposes more diagnostics than the initial architecture draft:
- signaling and ICE progression
- selected/default audio devices
- detected cameras and selected video capability
- local/remote audio level events
- local/remote video frame events
- preview conversion failures
- console logging for runtime warnings and errors

## Current constraints

- device selection still uses the first/default device in most flows
- video may still fail on some Windows/JDK/camera combinations
- the current desktop UX is an inline 1-to-1 video stage rather than a multi-peer conference grid
- the project currently prioritizes reliable text + file transfer + voice over a video-first experience
- chunked large file transfer over `RTCDataChannel` has not been implemented yet
- screen sharing is not implemented yet

## Recommended next steps

- stabilize video capture across more devices and JDK/runtime combinations
- expose manual input/output device selection
- add safe experimental toggles for video preview and capture modes
- add chunked transfer with backpressure awareness for `RTCDataChannel`
- add screen-sharing mode backed by desktop capture
