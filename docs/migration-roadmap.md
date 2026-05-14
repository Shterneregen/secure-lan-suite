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

- Current project version: `0.3.11-SNAPSHOT`
- Current runtime target: Java 25
- Current product direction: secure chat + encrypted files + LAN discovery + voice-first realtime, with experimental video
- Current desktop UX: messenger-style JavaFX workspace with peers, chat/activity feed, transfers, RTC controls, diagnostics, and inline video stage

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
| [x] | Shared models | Create immutable records for peer/chat/file/event/realtime models | `modules/common-model` | Records and enums reused across modules |
| [ ] | Shared networking | Add reusable transport abstractions and utilities | `modules/common-net` | Current baseline contains shared network constants; richer TCP/UDP contracts remain open |
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
| [ ] | java-steganography-tool | Extract BMP encode/hide service | `modules/stego-core` | Hide text or payload in BMP |
| [ ] | java-steganography-tool | Extract BMP decode/extract service | `modules/stego-core` | Extract hidden payload |
| [ ] | java-steganography-tool | Remove JavaFX-specific code from migrated logic | `modules/stego-core` | Core only |
| [ ] | java-steganography-tool | Integrate with `crypto-core` for encrypt-then-hide flow | `modules/stego-core` | Advanced secure workflow |
| [ ] | java-steganography-tool | Add tests for encode/decode roundtrip | `modules/stego-core` | Verify extracted payload integrity |

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
| [ ] | Stego UI | Add steganography tools panel | `apps/desktop-client` | Hide/extract workflows |
| [x] | Security UX | Add clearer session/status indicators | `apps/desktop-client` | Compact top status bar and voice status area |
| [x] | UX | Improve error display and diagnostics feedback | `apps/desktop-client` | Advanced/experimental panel plus richer logs |

---

# 3. Product Development Roadmap

## 3.1 Milestones

| Done | Phase | Goal | Deliverable |
|---|---|---|---|
| [x] | Phase 0 | Bootstrap the monorepo | Gradle modules, Java 25, initial structure |
| [ ] | Phase 1 | Shared foundation | `common-model` is ready; `common-net` still needs reusable transport abstractions |
| [x] | Phase 2 | Cryptographic base | `crypto-core` migrated and tested |
| [x] | Phase 3 | Secure file transfer | `file-transfer-core` working |
| [x] | Phase 4 | LAN chat and discovery | `chat-core` working with secure chat and UDP peer discovery |
| [ ] | Phase 5 | First usable product | Messenger-style desktop MVP is mostly wired; cross-machine validation and polish remain |
| [x] | Phase 6 | Realtime data + voice | `webrtc-core` with signaling, data, voice, diagnostics, and native runtime integration |
| [ ] | Phase 7 | Stable webcam/media support | Video exists experimentally but is not yet stable enough to call complete |
| [ ] | Phase 8 | Hidden-message workflows | `stego-core` + crypto integration |
| [ ] | Phase 9 | Stabilization | tests, packaging polish, documentation, UX hardening |

---

## 3.2 Recommended Order of Work

| Done | Order | Work Item | Why |
|---|---|---|---|
| [x] | 1 | Finalize project structure | Prevent future architectural drift |
| [x] | 2 | Implement `common-model` | Shared DTO baseline |
| [ ] | 3 | Implement richer `common-net` abstractions | Shared networking baseline beyond constants |
| [x] | 4 | Migrate `crypto-core` | Security foundation for other modules |
| [x] | 5 | Migrate `file-transfer-core` | First practical secure feature |
| [x] | 6 | Migrate `chat-core` | Core communication feature |
| [x] | 7 | Build messenger-style desktop workspace | First coherent user-facing UX |
| [x] | 8 | Integrate LAN discovery | Easier peer connection flow |
| [x] | 9 | Integrate realtime data + voice through `webrtc-core` | Practical realtime layer without waiting for stable video |
| [ ] | 10 | Stabilize video and output device selection | Needed before positioning video as a normal feature |
| [ ] | 11 | Migrate `stego-core` | Advanced security feature |
| [ ] | 12 | Refine UX, packaging, tests | Productization |

---

## 3.3 Definition of Done for Each Module

| Done | Module | Definition of Done |
|---|---|---|
| [x] | common-model | Shared records/enums exist, compile cleanly, reused by other modules |
| [ ] | common-net | Transport abstractions compile cleanly and are reused by feature modules |
| [x] | crypto-core | AES/RSA/hash/signature/key/file-crypto APIs extracted, tested, UI-free |
| [x] | chat-core | Message flow, handshake, signaling integration, discovery, peer/session logic extracted, tested, UI-free |
| [x] | file-transfer-core | Send/receive/progress/encryption logic extracted, tested, UI-free |
| [ ] | audio-core | Audio-specific services either extracted or intentionally superseded by the WebRTC runtime path |
| [ ] | webcam-core | Camera/media services either extracted or intentionally superseded by the WebRTC runtime path |
| [ ] | stego-core | Hide/extract services extracted, crypto integration possible, UI-free |
| [x] | webrtc-core | Session state, signaling, `RTCDataChannel`, voice, experimental video, device enumeration, diagnostics, and native runtime integration are wired in |
| [ ] | desktop-client | JavaFX client delivers a stable day-to-day workflow for chat, discovery, files, and voice across target machines |

---

## 3.4 Quality and Stabilization Checklist

| Done | Area | Step | Notes |
|---|---|---|---|
| [ ] | Testing | Add unit tests for all pure logic modules | Prefer deterministic tests |
| [x] | Testing | Add integration tests for chat and file transfer | Use test environments/mocks |
| [ ] | Testing | Add cross-machine smoke checks for desktop flows | Discovery, chat, file transfer, voice, and video diagnostics |
| [ ] | Reliability | Add consistent exception model across modules | Avoid ad-hoc error handling |
| [x] | Logging | Add richer runtime diagnostics for realtime troubleshooting | Core and UI logging still need a cleaner long-term strategy |
| [x] | Packaging | Define runnable desktop packaging strategy | Portable app image/ZIP and Windows EXE tasks are defined |
| [ ] | Packaging | Polish distribution-ready installer behavior | Signing, upgrade behavior, release notes, and platform validation remain open |
| [x] | Documentation | Add architecture overview to repository | Include messenger UI and realtime notes |
| [x] | Documentation | Add migration notes for realtime layer | Track current WebRTC-first direction |
| [ ] | UX | Improve desktop usability and error feedback | Continue polishing peer handling, discovery edge cases, and advanced flows |

---

# 4. Notes

- Prefer **incremental migration** over large rewrites.
- The current product direction is **chat + secure files + discovery + voice-first realtime**.
- Keep video **experimental** until capture, preview, and remote video behavior are stable across machines.
- UDP LAN discovery is implemented, but complex-network hardening remains a stabilization task.
- Audio, webcam, and steganography migrations can be revisited after the messenger-style MVP is more stable.
- `audio-core` and `webcam-core` currently provide profile hints; realtime media capture is handled primarily by `webrtc-core`.
