# Secure LAN Suite

Secure LAN Suite is a local-network communication suite for desktop and Android. Devices can discover each other, host or join password-protected rooms, exchange encrypted messages and files, and use experimental realtime media without relying on a cloud service.

The repository is a Gradle multi-module monorepo. The primary desktop client is built with Compose Multiplatform; an experimental Android client provides desktop-compatible LAN interoperability.

## Main features

- Host or join password-protected LAN chat rooms.
- Discover compatible peers with UDP broadcast or connect manually by host and port.
- Exchange encrypted chat messages and files with transfer progress and configurable downloads.
- Select multiple files on Compose desktop or Android, review the recipient and
  send them as a sequential queue with per-file results and explicit retries.
- Create temporary Quick Share browser links for files or text, with expiration and access limits. Quick Share is intended only for trusted LANs because browser transfers use plain local HTTP.
- Use voice sessions and experimental 1-to-1 video calls through WebRTC,
  including microphone, speaker, and camera selection and diagnostics.
- Hide or extract text in BMP images with optional password-based encryption through the desktop Steganography tool.
- Persist desktop profile, appearance, network, media, download, window, and diagnostics settings.
- Build and use the experimental Android client for room hosting/joining, encrypted chat, and file transfer. Android supports adaptive layouts, light/dark themes, and English/Russian/system UI languages.

## Implemented client capabilities

| Capability | Desktop | Android |
| --- | --- | --- |
| Discovery, room host/join, V2 chat and encrypted files | Implemented | Implemented, experimental client |
| LAN WebRTC voice / one-to-one video | Implemented / experimental video | Implemented, experimental voice/video |
| Browser file/text Quick Share, TTL/count/revoke, QR | Implemented | Implemented, experimental |
| RTC data channels | Implemented | Unavailable |
| BMP steganography | Implemented | Unavailable |
| Screen sharing | Unavailable | Unavailable |

Implementation does not imply device qualification. See the [Android guide](apps/android-client/android-readme.md)
and [realtime architecture](docs/webrtc-architecture.md) for platform owners and remaining hardware checks.
Build checks, production signing and artifact qualification are described in
[Release and qualification](docs/release.md).

## Security and compatibility

Chat and encrypted files use the mutually authenticated V2 protocol on both
clients. V1 clients must be updated; there is no insecure fallback. The room host
can read chat and signaling. See [security guarantees and limits](docs/security-protocol.md)
for peer identities, replay protection, receiver budgets and Quick Share boundaries.
Updated desktop and Android peers negotiate encrypted idle health probes; a silent
connection loss closes the session after roughly 30?40 seconds and can be retried
manually. Older V2 peers remain compatible without the bounded idle-health guarantee.
Desktop media selections apply before the next outgoing or accepted incoming call;
changing a selection preserves the active call.

## Tech stack

- Java 25 for desktop, Java 17 for Android and its shared modules, and Kotlin
- Gradle Wrapper
- Compose Multiplatform for the desktop client
- Jetpack Compose and Android Gradle Plugin for the Android client
- `webrtc-java` for realtime data, voice, and experimental video transport
- `jpackage` and WiX for native application images and Windows EXE installers

## Repository structure

### Applications

- `apps/desktop-client` - Compose desktop client and packaging tasks
- `apps/android-client` - experimental Android client with desktop interoperability

### Core modules

- `modules/common-model` and `modules/common-net` - shared models and networking utilities
- `modules/crypto-core` - encryption, hashing, signatures, key and file-crypto workflows
- `modules/chat-core` - secure rooms, handshake, messaging, signaling, and peer discovery
- `modules/file-transfer-core` - encrypted V2 file transfer, stream contracts, and shared Quick Share models/HTML; HTTP backends belong to the applications
- `modules/webrtc-core` - desktop realtime services and private native runtime
- `modules/audio-core` and `modules/webcam-core` - standalone profile DTOs; their default services are not used by capture
- `modules/stego-core` - UI-independent BMP steganography services

## Screenshots

### Desktop

<img src="docs/images/app-main-0.9.3.png" alt="Secure LAN Suite 0.9.3 desktop client" width="900">

### Android

<p align="center">
  <img src="docs/images/android-devices-0.9.3.jpg" alt="Secure LAN Suite Android devices screen" width="30%">
  <img src="docs/images/android-chat-0.9.3.jpg" alt="Secure LAN Suite Android chat screen" width="30%">
  <img src="docs/images/android-files-0.9.3.jpg" alt="Secure LAN Suite Android files screen" width="30%">
</p>

## Documentation

- Shared instructions for Kilo, Codex, Antigravity, and other coding agents: [`AGENTS.md`](AGENTS.md)
- Desktop product and UX guidance: [`docs/product-ui.md`](docs/product-ui.md)
- UI quality checklists: [`docs/quality/`](docs/quality/)
- Development, build, run, and smoke-test guide: [`docs/development.md`](docs/development.md)
- Android build, signing, and installation: [`apps/android-client/android-readme.md`](apps/android-client/android-readme.md)
- Windows WiX installation: [`docs/wix-installation.md`](docs/wix-installation.md)
- WebRTC architecture and video-preview properties: [`docs/webrtc-architecture.md`](docs/webrtc-architecture.md)
- Security model and V2 chat/file protocol: [`docs/security-protocol.md`](docs/security-protocol.md)
- Active outcomes and release work: [`docs/roadmap.md`](docs/roadmap.md)
- Completed legacy consolidation: [`docs/migration-history.md`](docs/migration-history.md)

## Current limitations

- LAN discovery uses UDP broadcast and may require additional hardening for complex networks, firewalls, VPNs, and multi-adapter setups.
- Key management and advanced file-transfer controls are not fully exposed in the desktop UI.
- File batches are local sender queues of independent encrypted transfers, not
  atomic deliveries. Receiver approval remains per file. Stopping a batch waits
  for its current file; missing final confirmation can mean the file was saved,
  so retrying can create a duplicate. Queues are not restored after app restart,
  and Android background execution is not guaranteed.
- Voice/video behavior and device selection still need broader cross-device validation; video calls and preview remain experimental.
- Desktop steganography exposes text workflows for uncompressed 24-bit/32-bit BMP images. Binary payloads are supported by the core API but not by the UI.
- Android implements experimental LAN voice/video calls and temporary browser Quick Share. Microphone/camera, Bluetooth, background/PiP and OEM behavior still require physical-device qualification. RTC data channels, steganography and screen sharing are unavailable on Android.
- Large file transfer over `RTCDataChannel` and screen sharing are not implemented.
- Windows EXE packaging is Windows-only because `jpackage` does not cross-build Windows installers.
