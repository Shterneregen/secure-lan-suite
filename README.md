# Secure LAN Suite

Secure LAN Suite is a local-network secure communication suite with a JavaFX desktop client and an experimental Android client. The repository is a Gradle multi-module monorepo that keeps UI, networking, cryptography, file transfer, realtime media, Android, and future feature modules separated.

## Tech stack
- Java 25
- Gradle 9.1+ recommended
- JavaFX 25.0.2
- Android Gradle Plugin 8.13.2, Kotlin 2.2.21, and Jetpack Compose Material 3 for the experimental Android client
- `webrtc-java` 0.14.0 for realtime data, voice, and experimental video transport
- `jpackage` for native application images and installers
- WiX 5.0.2 for Windows EXE installers

## Project structure

### Applications
- `apps/desktop-client` — JavaFX desktop client and application packaging tasks
- `apps/android-client` — experimental native Android client for LAN discovery, secure chat, and encrypted file transfer interoperability with the desktop client

### Modules
- `modules/common-model` — shared DTO records, enums, app events, transfer models, RTC signaling models
- `modules/common-net` — shared network constants, TCP endpoint/socket helpers, reusable text/frame channels, server accept-loop utilities, and UDP broadcast address resolution
- `modules/crypto-core` — AES-GCM, RSA, hashing, signatures, key generation, file crypto workflows, keystore helpers
- `modules/chat-core` — secure chat server/client, handshake, message protocol, signaling transport, UDP peer discovery
- `modules/file-transfer-core` — encrypted file transfer client/server, secure handshake, progress events
- `modules/webrtc-core` — RTC session orchestration, WebRTC runtime/provider integration, data channels, voice, experimental video, diagnostics
- `modules/audio-core` — default audio profile hints used by desktop/realtime flows
- `modules/webcam-core` — default video profile hints used by desktop/realtime flows
- `modules/stego-core` — UI-free BMP steganography services for binary/text payload hide/extract workflows and password-based encrypt-then-hide flows

## Current product state

### Working now
- start and stop a local secure chat room
- automatically join the locally hosted room from the same desktop client
- connect manually to a remote room by host and port
- discover Secure LAN Suite peers on the LAN with UDP broadcast/listen mode
- control room visibility with the **Discoverable** checkbox
- connect to a discovered peer directly from the peer list
- complete an encrypted chat handshake using the shared room password
- send and receive chat messages in the shared room activity feed
- start a secure file-transfer listener together with the chat room or client connection
- send files from the desktop UI to a selected online peer
- receive files into a configurable downloads directory
- show transfer progress and transfer status in the main workspace
- publish temporary no-auth LAN browser links for files or text snippets with expiration and access limits
- route RTC signaling through `chat-core` into `webrtc-core`
- start voice sessions backed by native `webrtc-java`
- choose detected microphone and camera capture devices for RTC sessions
- test microphone capture and open a camera preview window from the desktop UI
- start experimental 1-to-1 video calls with an inline video stage
- use the desktop steganography tools panel to hide/extract text payloads in uncompressed BMP images, including password-encrypted payloads through `stego-core`
- build and install the experimental Android client as debug or release APK
- discover desktop peers from Android, connect to a desktop room, exchange encrypted chat messages, send Android-selected files to desktop, and receive encrypted files from desktop into `Downloads/SecureLan`
- monitor server, connection, selected peer, voice, transfer, runtime, and diagnostics state from the compact UI
- use the messenger-style desktop layout:
  - peer list on the left
  - chat/activity feed and inline video stage in the center
  - actions, media status, transfers, and advanced tools on the right

### Current UI layout
The desktop client uses a **messenger-style workspace**.

- **Top status bar** — compact colored indicators for server, connection, selected peer, voice state, and file transfers
- **Header** — local profile/hosting controls and manual connection fallback
- **Left column** — discovered/chat peers and contact status
- **Center column** — optional inline video stage, chat messages, system events, file events, and realtime messages
- **Right column** — quick actions, voice/media status, transfers, RTC data tools, diagnostics, and advanced/experimental controls

### Realtime status
- `RTCDataChannel` is integrated and available from the desktop client
- voice sessions are the primary supported realtime media flow
- microphone and camera capture device selection is exposed in the desktop UI
- camera preview and 1-to-1 video calls are implemented but remain **experimental**
- local and remote video preview behavior can be controlled with JVM system properties:
  - `securelan.rtc.videoPreview.local.enabled`
  - `securelan.rtc.videoPreview.remote.enabled`

## Screenshots

<img src="docs/images/app-main-0.3.17.png" alt="Secure LAN Suite main window" width="900">

## Development and packaging

Development, build, run, smoke-test, and packaging details are intentionally kept outside this overview:

- Development guide: [`docs/development.md`](docs/development.md)
- Android client build/signing/install guide: [`apps/android-client/android-readme.md`](apps/android-client/android-readme.md)
- Windows WiX installation guide: [`docs/wix-installation.md`](docs/wix-installation.md)
- Realtime/WebRTC architecture notes: [`docs/webrtc-architecture.md`](docs/webrtc-architecture.md)
- Migration and roadmap checklist: [`docs/migration-roadmap.md`](docs/migration-roadmap.md)

## Architecture notes

### Chat and discovery
- `chat-core` provides the secure room server/client, shared message protocol, and secure handshake integration
- `chat-core` also provides UDP broadcast/listen peer discovery through `PeerDiscoveryService`
- `desktop-client` starts discovery in listen-only mode by default and broadcasts when a hosted room is discoverable
- discovered peers are shown in the left peer list and can populate manual connection fields automatically

### File transfer
- chat uses the configured chat port, for example `5050`
- encrypted app-to-app file transfer uses a separate configured port, commonly `5051`
- encrypted app-to-app file transfer uses `crypto-core` with an ephemeral RSA key exchange and AES-GCM encrypted payload chunks
- transfer progress is exposed through shared progress models and desktop UI transfer entries
- no-auth LAN browser quick share uses a separate temporary HTTP server, commonly `5053`, in `file-transfer-core`
- browser quick-share payloads are not encrypted by the app because the receiver is a plain browser over local HTTP; use it only on trusted LANs
- the experimental Android MVP reimplements only the minimum desktop-compatible discovery, secure chat, AES-GCM/RSA handshake, file-send, and file-receive protocol code inside `apps/android-client` to avoid introducing Android UI dependencies into reusable core modules

### Realtime architecture
- `chat-core` transports realtime signaling envelopes between peers over the secure chat path
- `webrtc-core` owns RTC session state, signaling integration, diagnostics, and runtime/provider integration
- `webrtc-core` boots a native `webrtc-java` engine and reuses chat signaling for SDP and ICE exchange
- `RTCDataChannel` support remains in `webrtc-core`, but the desktop UI currently prioritizes voice/video controls and runtime diagnostics over data-channel test controls
- `audio-core` and `webcam-core` expose default media profile hints for desktop/realtime sessions
- implementation notes: [`docs/webrtc-architecture.md`](docs/webrtc-architecture.md)

### Steganography
- `stego-core` provides UI-agnostic BMP steganography services for uncompressed 24-bit and 32-bit BMP images
- payloads are embedded into color-channel least-significant bits with a compact SecureLanSuite header containing magic, version, flags, content type, and payload length
- service APIs support binary payloads, UTF-8 text convenience methods, and password-based encrypt-then-hide workflows through `crypto-core`
- the desktop client exposes a Steganography panel in the Actions column for selecting PNG/BMP/JPG/JPEG images, inspecting capacity, hiding text, extracting text, and using optional password encryption; non-BMP cover images are converted to BMP output before embedding
- no JavaFX code is present in `stego-core`; UI integration stays in `apps/desktop-client`

## Current limitations
- `common-net` still contains only the shared network baseline; richer reusable transport abstractions are not finished
- LAN discovery is implemented with UDP broadcast and may still require hardening for complex networks, firewalls, VPNs, and multi-adapter setups
- key management and advanced transfer controls are not fully exposed in the desktop UI yet
- video calls and preview are experimental and may fail on some Windows/JDK/camera combinations
- microphone and camera capture selection is exposed, but audio output device selection is not yet exposed
- desktop steganography currently targets text workflows over uncompressed 24-bit/32-bit BMP images; arbitrary binary payload UI is not exposed yet
- Android remains an experimental interoperability client and does not yet implement room hosting, voice, WebRTC data channels, camera/video, steganography tools, screen sharing, or no-auth browser quick share
- chunked large file transfer over `RTCDataChannel` is not implemented yet
- screen sharing is not implemented yet
- EXE packaging is Windows-only because `jpackage` does not cross-build Windows installers
