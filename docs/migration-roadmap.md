# SecureLanSuite Migration and Roadmap Checklist

## Purpose
This document is a practical migration and development checklist for **SecureLanSuite**.

Use it to:
- track migration of functionality from the legacy repositories
- mark completed work step by step
- keep implementation aligned with the target modular architecture
- track product development milestones

---

## Current baseline

- Current runtime target: Java 25
- Current Android build target: Android SDK 35 with Android Gradle Plugin 8.13.2, Kotlin 2.2.21, and Jetpack Compose Material 3
- Current product direction: secure chat + encrypted files + LAN discovery + voice-first realtime, experimental video, and Android desktop-interoperability MVP
- Current desktop UX: messenger-style JavaFX workspace with peers, chat/activity feed, transfers, RTC controls, diagnostics, and inline video stage
- Current Android UX: native Compose MVP with peer discovery, secure chat, encrypted file send/receive, transfer progress, dark theme toggle, and diagnostics logs

---

## Legacy Repositories

| Legacy Repository | URL | Primary Target Module |
|---|---|---|
| java-crypto | https://github.com/Shterneregen/java-crypto | `modules/crypto-core` |
| java-encryption-tool | https://github.com/Shterneregen/java-encryption-tool | `modules/crypto-core` |
| java-lan-chat | https://github.com/Shterneregen/java-lan-chat | `modules/chat-core` |
| java-file-transceiver | https://github.com/Shterneregen/java-file-transceiver | `modules/file-transfer-core` |
| java-audio-transceiver | https://github.com/Shterneregen/java-audio-transceiver | `modules/audio-core` |
| webcam-catcher | https://github.com/Shterneregen/webcam-catcher | `modules/webcam-core` |
| java-steganography-tool | https://github.com/Shterneregen/java-steganography-tool | `modules/stego-core` |

---

# 1. Functional Migration Plan

## 1.1 Foundation and Shared Modules

| Done | Area | Step | Target Module | Notes |
|---|---|---|---|---|
| [x] | Project setup | Create Gradle multi-module skeleton | root project | Java 25, Gradle multi-project, module registration |
| [x] | Project setup | Add `apps/desktop-client` | `apps/desktop-client` | Runnable JavaFX desktop app |
| [x] | Project setup | Add `apps/android-client` | `apps/android-client` | Experimental Android application module using Android Gradle Plugin, Kotlin, and Compose |
| [x] | Shared models | Create immutable records for peer/chat/file/event/realtime models | `modules/common-model` | Records and enums reused across modules |
| [x] | Shared networking | Add reusable transport abstractions and utilities | `modules/common-net` | Shared network constants, TCP endpoint/socket factories, text and frame channels, reusable accept loop, close helper, and UDP broadcast address resolver are available and reused by chat/file-transfer modules |
| [x] | Standards | Establish package naming and module dependency rules | all modules | Current modules follow the `com.shterneregen.securelan` package structure and acyclic Gradle wiring |

---

## 1.2 Crypto Migration

Source repositories:
- https://github.com/Shterneregen/java-crypto
- https://github.com/Shterneregen/java-encryption-tool

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [x] | java-crypto | Extract AES encryption service | `modules/crypto-core` | AES-GCM reusable service API |
| [x] | java-crypto | Extract RSA encryption/decryption service | `modules/crypto-core` | Reusable service API |
| [x] | java-crypto | Extract key pair generation utilities | `modules/crypto-core` | RSA/AES key generation helpers |
| [x] | java-crypto | Extract hashing utilities | `modules/crypto-core` | SHA-based helpers |
| [x] | java-crypto | Extract digital signature utilities | `modules/crypto-core` | Sign/verify operations |
| [x] | java-crypto | Extract keystore/truststore helpers | `modules/crypto-core` | PKCS12 keystore access |
| [x] | java-encryption-tool | Extract file encryption workflow | `modules/crypto-core` | Public-key hybrid file crypto workflow |
| [x] | java-encryption-tool | Extract file decryption workflow | `modules/crypto-core` | Reusable service logic |
| [x] | java-encryption-tool | Extract password-based encryption flow | `modules/crypto-core` | Password file crypto workflow |
| [x] | java-encryption-tool | Remove old CLI orchestration from migrated code | `modules/crypto-core` | No legacy `main()` logic in core |
| [x] | java-crypto + java-encryption-tool | Add unit tests for core crypto flows | `modules/crypto-core` | Encryption/decryption/sign/verify and key encoding tests |

---

## 1.3 Chat Migration

Source repository:
- https://github.com/Shterneregen/java-lan-chat

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [x] | java-lan-chat | Extract peer connection model and session concepts | `modules/chat-core` | Rebuilt as reusable services |
| [x] | java-lan-chat | Extract message send/receive flow | `modules/chat-core` | UI-free core logic |
| [x] | java-lan-chat | Extract LAN peer communication logic | `modules/chat-core` | Service-level API |
| [x] | java-lan-chat | Implement secure handshake integration using `crypto-core` | `modules/chat-core` | Avoid crypto duplication |
| [x] | java-lan-chat | Add LAN peer discovery capability | `modules/chat-core` | UDP broadcast/listen discovery is implemented and wired into the desktop peer list |
| [x] | java-lan-chat | Add chat session management | `modules/chat-core` | State management in core |
| [x] | java-lan-chat | Add tests for message exchange and handshake | `modules/chat-core` | Deterministic transport tests |
| [ ] | java-lan-chat | Harden peer discovery for complex LAN environments | `modules/chat-core` | Firewalls, VPNs, multi-adapter networks, duplicate nicknames, and richer UX feedback still need polish |

---

## 1.4 File Transfer Migration

Source repository:
- https://github.com/Shterneregen/java-file-transceiver

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [x] | java-file-transceiver | Extract file sender service | `modules/file-transfer-core` | Reusable sending API |
| [x] | java-file-transceiver | Extract file receiver service | `modules/file-transfer-core` | Reusable receiving API |
| [x] | java-file-transceiver | Add transfer progress reporting | `modules/file-transfer-core` | Shared progress models and events |
| [x] | java-file-transceiver | Add secure transfer integration with `crypto-core` | `modules/file-transfer-core` | Ephemeral RSA handshake plus AES-GCM chunk encryption |
| [ ] | java-file-transceiver | Extract SSL/TLS-related transport logic if useful | `modules/file-transfer-core` | Keep transport modular if this path remains relevant |
| [x] | java-file-transceiver | Remove command-line orchestration from migrated code | `modules/file-transfer-core` | Core only |
| [x] | java-file-transceiver | Add integration tests for file send/receive | `modules/file-transfer-core` | Includes transfer behavior coverage |
| [x] | product integration | Add Android-compatible encrypted file send/receive MVP | `apps/android-client` | Android reimplements the minimum desktop-compatible RSA/AES-GCM handshake and chunk protocol locally to avoid Android dependencies in core modules |

---

## 1.5 Realtime / WebRTC Integration

| Done | Area | Step | Target Module | Notes |
|---|---|---|---|---|
| [x] | Realtime foundation | Add RTC session models, signal envelopes, and session states | `modules/common-model` | Shared realtime DTO baseline |
| [x] | Signaling | Route realtime signaling through `chat-core` | `modules/chat-core` | SDP/ICE envelopes over secure chat |
| [x] | Runtime | Add `webrtc-core` session service and engine/provider abstraction | `modules/webrtc-core` | `RtcSessionService`, `RtcEngine`, `RtcEngineProvider` |
| [x] | Data | Add `RTCDataChannel` support | `modules/webrtc-core` | Outbound/inbound realtime data sessions |
| [x] | Voice | Add native voice transport backed by `webrtc-java` | `modules/webrtc-core` | Current primary realtime media flow |
| [x] | Diagnostics | Add runtime warnings, audio levels, video events, and console diagnostics | `modules/webrtc-core` | Includes richer troubleshooting output |
| [x] | Device selection | Add manual audio/video capture device selection | `modules/webrtc-core` + `apps/desktop-client` | Microphone and camera selectors are exposed; audio output selection remains open |
| [x] | Camera preview | Add desktop camera test/preview flow | `modules/webrtc-core` + `apps/desktop-client` | Preview window uses runtime camera capture and frame events |
| [ ] | Video stabilization | Stabilize camera/video transport for normal user-facing use | `modules/webrtc-core` | Video call UI exists, but video remains experimental |

---

## 1.6 Audio Migration

Source repository:
- https://github.com/Shterneregen/java-audio-transceiver

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [ ] | java-audio-transceiver | Extract standalone audio capture service | `modules/audio-core` | Current voice path is provided through `webrtc-core` |
| [ ] | java-audio-transceiver | Extract standalone audio playback service | `modules/audio-core` | Current voice path is provided through `webrtc-core` |
| [ ] | java-audio-transceiver | Extract TCP audio sender/receiver logic | `modules/audio-core` | Lower priority after WebRTC voice |
| [ ] | java-audio-transceiver | Extract UDP audio sender/receiver logic | `modules/audio-core` | Lower priority after WebRTC voice |
| [ ] | java-audio-transceiver | Add standalone session management API | `modules/audio-core` | Reassess if still needed |
| [ ] | java-audio-transceiver | Remove startup/CLI assumptions from migrated code | `modules/audio-core` | Core only |
| [ ] | java-audio-transceiver | Add tests for transport/session behavior where possible | `modules/audio-core` | Platform-specific pieces should stay isolated |
| [x] | product integration | Provide default audio profile hints | `modules/audio-core` | Used by desktop/realtime status UI |

---

## 1.7 Webcam Migration

Source repository:
- https://github.com/Shterneregen/webcam-catcher

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [ ] | webcam-catcher | Extract standalone webcam capture service | `modules/webcam-core` | Current camera transport path is provided through `webrtc-core` |
| [ ] | webcam-catcher | Extract snapshot/photo functionality | `modules/webcam-core` | Future desktop tooling |
| [ ] | webcam-catcher | Extract video recording support | `modules/webcam-core` | Future desktop tooling |
| [ ] | webcam-catcher | Extract frame stream access | `modules/webcam-core` | Useful for non-WebRTC preview/processing |
| [ ] | webcam-catcher | Isolate OpenCV/native integration | `modules/webcam-core` | Keep native coupling local if this path is revived |
| [ ] | webcam-catcher | Add tests around non-native logic | `modules/webcam-core` | Native-specific code should stay thin |
| [x] | product integration | Provide default video profile hints | `modules/webcam-core` | Used by desktop/realtime status UI |

---

## 1.8 Steganography Migration

Source repository:
- https://github.com/Shterneregen/java-steganography-tool

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [x] | java-steganography-tool | Extract BMP encode/hide service | `modules/stego-core` | UI-free service hides binary and UTF-8 text payloads in uncompressed BMP images |
| [x] | java-steganography-tool | Extract BMP decode/extract service | `modules/stego-core` | Validates SecureLanSuite stego header and extracts exact payload bytes |
| [x] | java-steganography-tool | Remove JavaFX-specific code from migrated logic | `modules/stego-core` | Core only; desktop tools panel remains a separate UI task |
| [x] | java-steganography-tool | Integrate with `crypto-core` for encrypt-then-hide flow | `modules/stego-core` | Password-based encrypt-then-hide and decrypt-after-extract workflows use `crypto-core` |
| [x] | java-steganography-tool | Add tests for encode/decode roundtrip | `modules/stego-core` | Capacity, text, binary, encrypted, wrong-password, and oversized payload tests |

---

# 2. Desktop Client Development Plan

## 2.1 Desktop MVP

| Done | Milestone | Step | Target | Notes |
|---|---|---|---|---|
| [x] | Desktop shell | Create JavaFX application shell | `apps/desktop-client` | Main window and lifecycle |
| [x] | Desktop shell | Replace old tabbed flow with messenger-style workspace layout | `apps/desktop-client` | Status bar + peer list + chat feed + action sidebar |
| [x] | MVP | Add peer list view | `apps/desktop-client` | Populated from LAN discovery and chat/session activity |
| [x] | MVP | Add chat view | `apps/desktop-client` | Send/receive text messages |
| [x] | MVP | Add file transfer view integrated into main workspace | `apps/desktop-client` | Send file and show progress/status |
| [ ] | MVP | Add security/keys view | `apps/desktop-client` | Key generation/loading actions still limited |
| [x] | MVP | Add event/activity log surface | `apps/desktop-client` | User-facing activity feed plus advanced diagnostics |
| [x] | MVP | Connect desktop UI to core modules | `apps/desktop-client` | Keep UI orchestration focused on wiring and state |
| [ ] | MVP | Smoke-test MVP end-to-end | `apps/desktop-client` | Chat + discovery + file transfer + voice across multiple machines |

---

## 2.2 Desktop Feature Expansion

| Done | Milestone | Step | Target | Notes |
|---|---|---|---|---|
| [x] | Discovery UI | Add discoverable hosting and discovered peer connect flow | `apps/desktop-client` | UDP discovery is integrated into the peer list |
| [x] | Audio UI | Add voice session controls | `apps/desktop-client` | Start/hang up, levels, voice state |
| [x] | Media UI | Add capture device selectors and test controls | `apps/desktop-client` | Microphone selector, camera selector, mic test, camera preview |
| [x] | Camera UI | Add experimental webcam/video call panel | `apps/desktop-client` | Inline video stage exists but remains experimental |
| [ ] | Camera UI | Stabilize webcam/video call panel for normal use | `apps/desktop-client` | Needs broader runtime/device validation |
| [ ] | Camera UI | Add recording controls | `apps/desktop-client` | Optional future feature |
| [x] | Stego UI | Add steganography tools panel | `apps/desktop-client` | Hide/extract text workflows with optional password encryption |
| [x] | Security UX | Add clearer session/status indicators | `apps/desktop-client` | Compact top status bar and voice status area |
| [x] | UX | Improve error display and diagnostics feedback | `apps/desktop-client` | Advanced/experimental panel plus richer logs |

---

# 3. Android Client Development Plan

## 3.1 Android MVP

| Done | Milestone | Step | Target | Notes |
|---|---|---|---|---|
| [x] | Android shell | Create Android application module | `apps/android-client` | Native Android app with Kotlin and Compose Material 3 |
| [x] | Build setup | Configure Android SDK 35 build and APK naming | `apps/android-client` | Outputs `secure-lan-<version>.apk` for debug and release variants |
| [x] | Release signing | Add configurable release APK signing | `apps/android-client` | Uses Gradle properties or `ANDROID_RELEASE_*` environment variables; falls back to debug signing for local installable builds |
| [x] | Permissions | Declare LAN/network/storage permissions | `apps/android-client` | Includes `INTERNET`, Wi-Fi/network permissions, `NEARBY_WIFI_DEVICES`, and legacy `WRITE_EXTERNAL_STORAGE` for API 28 and lower |
| [x] | Discovery | Add desktop-compatible UDP discovery listener | `apps/android-client` | Starts on launch and shows discovered desktop rooms as peers |
| [x] | Chat | Add secure desktop chat client flow | `apps/android-client` | Connects to a desktop room using the shared password and encrypted handshake |
| [x] | Chat UI | Add Compose chat and connection UI | `apps/android-client` | Nickname/password, peer selection, connect/disconnect, inbound/outbound messages |
| [x] | File send | Add Android-to-desktop encrypted file sending | `apps/android-client` | Uses Android document picker, desktop-compatible handshake, and send progress |
| [x] | File receive | Add desktop-to-Android encrypted file receiving | `apps/android-client` | Listens on the normal file port or connected peer file port + `1000`; saves into `Downloads/SecureLan` on Android 10+ |
| [x] | Diagnostics | Add Android in-app status and log surface | `apps/android-client` | Recent logs are available from the settings panel |
| [ ] | Hosting | Add Android-hosted desktop-compatible chat room | `apps/android-client` | Not implemented; Android currently acts as a client/interoperability peer |
| [ ] | Realtime | Add Android WebRTC/voice/data support | `apps/android-client` + future Android media integration | Not implemented; desktop remains the only realtime media client |
| [ ] | Advanced tools | Add Android steganography or quick-share workflows if needed | `apps/android-client` | Not implemented; desktop-only today |

---

# 4. Product Development Roadmap

## 4.1 Milestones

| Done | Phase | Goal | Deliverable |
|---|---|---|---|
| [x] | Phase 0 | Bootstrap the monorepo | Gradle modules, Java 25, initial structure |
| [x] | Phase 1 | Shared foundation | `common-model` is ready; `common-net` provides reusable TCP/UDP transport utilities reused by feature modules |
| [x] | Phase 2 | Cryptographic base | `crypto-core` migrated and tested |
| [x] | Phase 3 | Secure file transfer | `file-transfer-core` working |
| [x] | Phase 4 | LAN chat and discovery | `chat-core` working with secure chat and UDP peer discovery |
| [ ] | Phase 5 | First usable product | Messenger-style desktop MVP is mostly wired; cross-machine validation and polish remain |
| [x] | Phase 6 | Realtime data + voice | `webrtc-core` with signaling, data, voice, diagnostics, and native runtime integration |
| [ ] | Phase 7 | Stable webcam/media support | Video exists experimentally but is not yet stable enough to call complete |
| [x] | Phase 8 | Hidden-message workflows | `stego-core` + crypto integration |
| [x] | Phase 9 | Android interoperability MVP | Android app can discover desktop peers, connect to secure chat, send files, receive files, and build signed release APKs |
| [ ] | Phase 10 | Stabilization | tests, packaging polish, documentation, UX hardening, cross-device validation |

---

## 4.2 Recommended Order of Work

| Done | Order | Work Item | Why |
|---|---|---|---|
| [x] | 1 | Finalize project structure | Prevent future architectural drift |
| [x] | 2 | Implement `common-model` | Shared DTO baseline |
| [x] | 3 | Implement richer `common-net` abstractions | Shared networking baseline beyond constants |
| [x] | 4 | Migrate `crypto-core` | Security foundation for other modules |
| [x] | 5 | Migrate `file-transfer-core` | First practical secure feature |
| [x] | 6 | Migrate `chat-core` | Core communication feature |
| [x] | 7 | Build messenger-style desktop workspace | First coherent user-facing UX |
| [x] | 8 | Integrate LAN discovery | Easier peer connection flow |
| [x] | 9 | Integrate realtime data + voice through `webrtc-core` | Practical realtime layer without waiting for stable video |
| [ ] | 10 | Stabilize video and output device selection | Needed before positioning video as a normal feature |
| [x] | 11 | Migrate `stego-core` | Advanced security feature |
| [x] | 12 | Add Android desktop-interoperability MVP | Enables mobile LAN chat/file-transfer testing against the desktop client |
| [ ] | 13 | Refine UX, packaging, tests | Productization |

---

## 4.3 Definition of Done for Each Module

| Done | Module | Definition of Done |
|---|---|---|
| [x] | common-model | Shared records/enums exist, compile cleanly, reused by other modules |
| [x] | common-net | Transport abstractions compile cleanly and are reused by feature modules |
| [x] | crypto-core | AES/RSA/hash/signature/key/file-crypto APIs extracted, tested, UI-free |
| [x] | chat-core | Message flow, handshake, signaling integration, discovery, peer/session logic extracted, tested, UI-free |
| [x] | file-transfer-core | Send/receive/progress/encryption logic extracted, tested, UI-free |
| [ ] | audio-core | Audio-specific services either extracted or intentionally superseded by the WebRTC runtime path |
| [ ] | webcam-core | Camera/media services either extracted or intentionally superseded by the WebRTC runtime path |
| [x] | stego-core | Hide/extract services extracted, crypto integration possible, UI-free |
| [x] | webrtc-core | Session state, signaling, `RTCDataChannel`, voice, experimental video, device enumeration, diagnostics, and native runtime integration are wired in |
| [ ] | desktop-client | JavaFX client delivers a stable day-to-day workflow for chat, discovery, files, and voice across target machines |
| [ ] | android-client | Android client can be considered stable only after cross-device validation of discovery, chat, file send/receive, permissions, signed APK install/update behavior, and failure diagnostics |

---

## 4.4 Quality and Stabilization Checklist

| Done | Area | Step | Notes |
|---|---|---|---|
| [ ] | Testing | Add unit tests for all pure logic modules | Prefer deterministic tests |
| [x] | Testing | Add integration tests for chat and file transfer | Use test environments/mocks |
| [ ] | Testing | Add cross-machine smoke checks for desktop flows | Discovery, chat, file transfer, voice, and video diagnostics |
| [ ] | Reliability | Add consistent exception model across modules | Avoid ad-hoc error handling |
| [x] | Logging | Add richer runtime diagnostics for realtime troubleshooting | Core and UI logging still need a cleaner long-term strategy |
| [x] | Packaging | Define runnable desktop packaging strategy | Portable app image/ZIP and Windows EXE tasks are defined |
| [x] | Packaging | Define Android APK build and release signing flow | Debug/release APK tasks, JKS signing properties, `apksigner` verification, and install notes are documented |
| [ ] | Packaging | Polish distribution-ready installer behavior | Signing, upgrade behavior, release notes, and platform validation remain open |
| [x] | Documentation | Add architecture overview to repository | Include messenger UI and realtime notes |
| [x] | Documentation | Add migration notes for realtime layer | Track current WebRTC-first direction |
| [x] | Documentation | Add Android client build and interoperability notes | Android README covers SDK setup, release APK signing, verification, install, permissions, and LAN scenarios |
| [ ] | UX | Improve desktop usability and error feedback | Continue polishing peer handling, discovery edge cases, and advanced flows |
| [ ] | UX | Improve Android usability and error feedback | Continue polishing permission rationale, discovery edge cases, file receive setup, and LAN diagnostics |

---

# 5. Notes

- Prefer **incremental migration** over large rewrites.
- The current product direction is **chat + secure files + discovery + voice-first realtime**.
- Keep video **experimental** until capture, preview, and remote video behavior are stable across machines.
- UDP LAN discovery is implemented, but complex-network hardening remains a stabilization task.
- Audio, webcam, and steganography migrations can be revisited after the messenger-style MVP is more stable.
- `audio-core` and `webcam-core` currently provide profile hints; realtime media capture is handled primarily by `webrtc-core`.
- The Android client is an experimental interoperability MVP: keep Android-specific UI/platform code inside `apps/android-client` and do not introduce Android dependencies into reusable core modules.
- Android release APKs can be built and signed today, but broader device/install/update validation remains part of stabilization.
