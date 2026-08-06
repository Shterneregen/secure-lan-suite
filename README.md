# Secure LAN Suite

Secure LAN Suite is a local-network communication suite for desktop and Android. Devices can discover each other, host or join password-protected rooms, exchange encrypted messages and files, and use experimental realtime media without relying on a cloud service.

The repository is a Gradle multi-module monorepo. The primary client is built with Compose Multiplatform; an experimental Android client provides desktop-compatible LAN interoperability. A separately packaged JavaFX client is retained as a deprecated fallback.

## Main features

- Host or join password-protected LAN chat rooms.
- Discover compatible peers with UDP broadcast or connect manually by host and port.
- Exchange encrypted chat messages and files with transfer progress and configurable downloads.
- Create temporary Quick Share browser links for files or text, with expiration and access limits. Quick Share is intended only for trusted LANs because browser transfers use plain local HTTP.
- Use voice sessions and experimental 1-to-1 video calls through WebRTC, including microphone and camera selection and diagnostics.
- Hide or extract text in BMP images with optional password-based encryption through the desktop Steganography tool.
- Persist desktop profile, appearance, network, media, download, window, and diagnostics settings.
- Build and use the experimental Android client for room hosting/joining, encrypted chat, and file transfer. Android supports adaptive layouts, light/dark themes, and English/Russian/system UI languages.

## Tech stack

- Java 25 and Kotlin 2.2.21
- Gradle Wrapper 9.4.1
- Compose Multiplatform for the primary desktop client
- Jetpack Compose and Android Gradle Plugin for the Android client
- JavaFX 25 for the deprecated desktop fallback
- `webrtc-java` for realtime data, voice, and experimental video transport
- `jpackage` and WiX for native application images and Windows EXE installers

## Repository structure

### Applications

- `apps/desktop-client` - primary Compose desktop client and packaging tasks
- `apps/android-client` - experimental Android client with desktop interoperability
- `apps/javafx-client` - deprecated JavaFX fallback kept for rollback and critical fixes

### Core modules

- `modules/common-model` and `modules/common-net` - shared models and networking utilities
- `modules/crypto-core` - encryption, hashing, signatures, key and file-crypto workflows
- `modules/chat-core` - secure rooms, handshake, messaging, signaling, and peer discovery
- `modules/file-transfer-core` - encrypted file transfer and Quick Share services
- `modules/webrtc-core`, `modules/audio-core`, and `modules/webcam-core` - realtime sessions and media profiles
- `modules/stego-core` - UI-independent BMP steganography services

## Screenshots

### Desktop

<img src="docs/images/app-main-0.5.0.png" alt="Secure LAN Suite 0.5.0 desktop client" width="900">

### Android

<p align="center">
  <img src="docs/images/android-devices-0.5.0.jpg" alt="Secure LAN Suite Android devices screen" width="30%">
  <img src="docs/images/android-chat-0.5.0.jpg" alt="Secure LAN Suite Android chat screen" width="30%">
  <img src="docs/images/android-files-0.5.0.jpg" alt="Secure LAN Suite Android files screen" width="30%">
</p>

## Documentation

- Development, build, run, and smoke-test guide: [`docs/development.md`](docs/development.md)
- Deprecated JavaFX fallback client: [`docs/javafx-client.md`](docs/javafx-client.md)
- Android build, signing, and installation: [`apps/android-client/android-readme.md`](apps/android-client/android-readme.md)
- Windows WiX installation: [`docs/wix-installation.md`](docs/wix-installation.md)
- WebRTC architecture and video-preview properties: [`docs/webrtc-architecture.md`](docs/webrtc-architecture.md)
- Migration and roadmap checklist: [`docs/migration-roadmap.md`](docs/migration-roadmap.md)

## Current limitations

- LAN discovery uses UDP broadcast and may require additional hardening for complex networks, firewalls, VPNs, and multi-adapter setups.
- Key management and advanced file-transfer controls are not fully exposed in the desktop UI.
- Voice/video behavior and device selection still need broader cross-device validation; video calls and preview remain experimental.
- Desktop steganography exposes text workflows for uncompressed 24-bit/32-bit BMP images. Binary payloads are supported by the core API but not by the UI.
- Android remains an experimental interoperability client and does not yet support voice, WebRTC data channels, camera/video, steganography, screen sharing, or Quick Share.
- Large file transfer over `RTCDataChannel` and screen sharing are not implemented.
- Windows EXE packaging is Windows-only because `jpackage` does not cross-build Windows installers.
