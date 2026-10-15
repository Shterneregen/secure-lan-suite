# SecureLanSuite Migration History

This document records the completed consolidation of the legacy SecureLanSuite
repositories. It is historical context, not a statement that every current
feature has completed release validation. Active work lives in
[`roadmap.md`](roadmap.md).

## Legacy sources

| Legacy repository | Primary destination |
| --- | --- |
| [java-crypto](https://github.com/Shterneregen/java-crypto) | `modules/crypto-core` |
| [java-encryption-tool](https://github.com/Shterneregen/java-encryption-tool) | `modules/crypto-core` |
| [java-lan-chat](https://github.com/Shterneregen/java-lan-chat) | `modules/chat-core` |
| [java-file-transceiver](https://github.com/Shterneregen/java-file-transceiver) | `modules/file-transfer-core` |
| [java-audio-transceiver](https://github.com/Shterneregen/java-audio-transceiver) | superseded by `modules/webrtc-core` for active voice transport |
| [webcam-catcher](https://github.com/Shterneregen/webcam-catcher) | superseded by `modules/webrtc-core` for active camera transport |
| [java-steganography-tool](https://github.com/Shterneregen/java-steganography-tool) | `modules/stego-core` |

## Foundation

The migration established a Gradle multi-project repository with shared models
and transport helpers. Reusable modules remain independent of desktop and
Android UI frameworks, while application modules own platform UI and lifecycle
integration.

| Area | Completed result |
| --- | --- |
| `common-model` | Shared immutable chat, peer, transfer, event, and realtime models |
| `common-net` | Reusable TCP framing, endpoints, socket helpers, accept loop, close helpers, and UDP broadcast resolution |
| `crypto-core` | AES-GCM, RSA, hashing, signatures, key/keystore helpers, and password/public-key file crypto with tests |
| `chat-core` | Secure handshake, sessions, messaging, RTC signaling envelopes, and UDP peer discovery with tests |
| `file-transfer-core` | Encrypted send/receive, progress events, and integration tests |
| `stego-core` | BMP text/binary hide and extract plus password encryption workflows and tests |
| `webrtc-core` | RTC sessions, signaling, data channels, native voice, experimental video, media devices, and diagnostics |
| `audio-core` / `webcam-core` | Standalone profile/configuration DTOs; default services are not used by active capture |

The standalone TCP/UDP audio transport and OpenCV-style standalone camera path
were intentionally not migrated. The active product direction uses WebRTC.
TLS-specific file transport was also left out. The earlier migration used an
RSA/AES-GCM key-transport handshake; this is historical behavior. Current chat
and files use mutually authenticated SRP V2, room-certified peer signatures and
ordered AES-GCM records, described in [security-protocol.md](security-protocol.md).

## Desktop applications

`apps/desktop-client` became the sole Compose Multiplatform client. The
migration delivered the messenger workspace, discovery, secure chat, encrypted
file transfer, voice, experimental video and preview, media-device settings,
Quick Share, steganography tools, persisted settings, tray lifecycle, desktop
notifications, portable packaging, and Windows installer tasks.

The initial migration retained `apps/javafx-client` as an isolated fallback
during Compose stabilization; that module has now been retired and removed.

## Android application

`apps/android-client` was added as a native Kotlin and Compose application. The
completed interoperability baseline includes desktop-compatible discovery,
secure room joining and hosting, encrypted chat, Android-to-desktop file send,
desktop-to-Android automatic file receive, transfer progress, lifecycle-aware
foreground hosting, configurable release signing, and in-app logs.

Android keeps a small platform-local compatibility layer while reusing the
shared modules that are safe for Android. Desktop-specific UI and runtime code
is not pulled into the Android application.

## Continuing validation

Migration completion means the legacy capabilities have an intentional home in
the monorepo. Cross-machine reliability, release qualification, experimental
video, network edge cases, and platform polish remain active concerns tracked in
the [roadmap](roadmap.md).
