# SecureLanSuite Migration and Roadmap Checklist

## Purpose
This document is a practical migration and development checklist for **SecureLanSuite**.

Use it to:
- track migration of functionality from the legacy repositories
- mark completed work step by step
- keep implementation aligned with the target modular architecture
- track product development milestones

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
| [x] | Project setup | Create Gradle multi-module skeleton | root project | Java 21, Gradle multi-project, module registration |
| [x] | Project setup | Add `apps/desktop-client` | `apps/desktop-client` | Minimal runnable desktop app |
| [x] | Shared models | Create immutable records for peer/chat/file/event models | `modules/common-model` | Use records and enums |
| [ ] | Shared networking | Add transport abstractions and utilities | `modules/common-net` | TCP/UDP contracts, serialization helpers |
| [ ] | Standards | Establish package naming and module dependency rules | all modules | Prevent cyclic dependencies |

---

## 1.2 Crypto Migration

Source repositories:
- https://github.com/Shterneregen/java-crypto
- https://github.com/Shterneregen/java-encryption-tool

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [x] | java-crypto | Extract AES encryption service | `modules/crypto-core` | Reusable service API |
| [x] | java-crypto | Extract RSA encryption/decryption service | `modules/crypto-core` | Reusable service API |
| [x] | java-crypto | Extract key pair generation utilities | `modules/crypto-core` | RSA/AES key generation helpers |
| [x] | java-crypto | Extract hashing utilities | `modules/crypto-core` | SHA-based helpers |
| [x] | java-crypto | Extract digital signature utilities | `modules/crypto-core` | Sign/verify operations |
| [x] | java-crypto | Extract keystore/truststore helpers | `modules/crypto-core` | Reusable keystore access |
| [x] | java-encryption-tool | Extract file encryption workflow | `modules/crypto-core` | Convert CLI flow into service logic |
| [x] | java-encryption-tool | Extract file decryption workflow | `modules/crypto-core` | Reusable service logic |
| [x] | java-encryption-tool | Extract password-based encryption flow | `modules/crypto-core` | Keep implementation reusable |
| [x] | java-encryption-tool | Remove old CLI orchestration from migrated code | `modules/crypto-core` | No legacy `main()` logic in core |
| [x] | java-crypto + java-encryption-tool | Add unit tests for core crypto flows | `modules/crypto-core` | Encryption/decryption/sign/verify tests |

---

## 1.3 Chat Migration

Source repository:
- https://github.com/Shterneregen/java-lan-chat

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [x] | java-lan-chat | Extract peer connection model and session concepts | `modules/chat-core` | Rebuild as reusable services |
| [x] | java-lan-chat | Extract message send/receive flow | `modules/chat-core` | Remove UI coupling |
| [x] | java-lan-chat | Extract LAN peer communication logic | `modules/chat-core` | Service-level API |
| [x] | java-lan-chat | Implement secure handshake integration using `crypto-core` | `modules/chat-core` | Avoid crypto duplication |
| [ ] | java-lan-chat | Add peer discovery capability | `modules/chat-core` | Discovery should be modular |
| [x] | java-lan-chat | Add chat session management | `modules/chat-core` | State management in core |
| [x] | java-lan-chat | Add tests for message exchange and handshake | `modules/chat-core` | Prefer deterministic transport tests |

---

## 1.4 File Transfer Migration

Source repository:
- https://github.com/Shterneregen/java-file-transceiver

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [ ] | java-file-transceiver | Extract file sender service | `modules/file-transfer-core` | Reusable sending API |
| [ ] | java-file-transceiver | Extract file receiver service | `modules/file-transfer-core` | Reusable receiving API |
| [ ] | java-file-transceiver | Add transfer progress reporting | `modules/file-transfer-core` | Use shared progress models |
| [ ] | java-file-transceiver | Add secure transfer integration with `crypto-core` | `modules/file-transfer-core` | Encrypt/decrypt file content or metadata as needed |
| [ ] | java-file-transceiver | Extract SSL/TLS-related transport logic if useful | `modules/file-transfer-core` | Keep transport modular |
| [ ] | java-file-transceiver | Remove command-line orchestration from migrated code | `modules/file-transfer-core` | Core only |
| [ ] | java-file-transceiver | Add integration tests for file send/receive | `modules/file-transfer-core` | Include failure cases |

---

## 1.5 Audio Migration

Source repository:
- https://github.com/Shterneregen/java-audio-transceiver

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [ ] | java-audio-transceiver | Extract audio capture service | `modules/audio-core` | Input capture abstraction |
| [ ] | java-audio-transceiver | Extract audio playback service | `modules/audio-core` | Output playback abstraction |
| [ ] | java-audio-transceiver | Extract TCP audio sender/receiver logic | `modules/audio-core` | Service-based transport |
| [ ] | java-audio-transceiver | Extract UDP audio sender/receiver logic | `modules/audio-core` | Keep API consistent |
| [ ] | java-audio-transceiver | Add session management API | `modules/audio-core` | Voice session lifecycle |
| [ ] | java-audio-transceiver | Remove startup/CLI assumptions from migrated code | `modules/audio-core` | Core only |
| [ ] | java-audio-transceiver | Add tests for transport/session behavior where possible | `modules/audio-core` | Mock or isolate platform-specific pieces |

---

## 1.6 Webcam Migration

Source repository:
- https://github.com/Shterneregen/webcam-catcher

| Done | Source Repo | Step | Target Module | Notes |
|---|---|---|---|---|
| [ ] | webcam-catcher | Extract webcam capture service | `modules/webcam-core` | Main camera access abstraction |
| [ ] | webcam-catcher | Extract snapshot/photo functionality | `modules/webcam-core` | Save/capture snapshot use case |
| [ ] | webcam-catcher | Extract video recording support | `modules/webcam-core` | Recording service |
| [ ] | webcam-catcher | Extract frame stream access | `modules/webcam-core` | For preview or future streaming |
| [ ] | webcam-catcher | Isolate OpenCV/native integration | `modules/webcam-core` | Keep native coupling local |
| [ ] | webcam-catcher | Add tests around non-native logic | `modules/webcam-core` | Keep native-specific code thin |

---

## 1.7 Steganography Migration

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
| [x] | Desktop shell | Create JavaFX application shell | `apps/desktop-client` | Main window and navigation |
| [ ] | Desktop shell | Add tab/panel structure | `apps/desktop-client` | Chat, Files, Security, Logs |
| [ ] | MVP | Add peer list view | `apps/desktop-client` | Backed by `chat-core` |
| [x] | MVP | Add chat view | `apps/desktop-client` | Send/receive text messages |
| [ ] | MVP | Add file transfer view | `apps/desktop-client` | Send file and show progress |
| [ ] | MVP | Add security/keys view | `apps/desktop-client` | Key generation/loading actions |
| [x] | MVP | Add event log view | `apps/desktop-client` | Use shared event models |
| [x] | MVP | Connect desktop UI to core modules | `apps/desktop-client` | Keep controllers thin |
| [ ] | MVP | Smoke-test MVP end-to-end | `apps/desktop-client` | Peer + chat + file transfer |

---

## 2.2 Desktop Feature Expansion

| Done | Milestone | Step | Target | Notes |
|---|---|---|---|---|
| [ ] | Audio UI | Add voice session controls | `apps/desktop-client` | Start/stop/mute UI |
| [ ] | Camera UI | Add webcam preview panel | `apps/desktop-client` | Preview/snapshot UI |
| [ ] | Camera UI | Add recording controls | `apps/desktop-client` | Optional video capture controls |
| [ ] | Stego UI | Add steganography tools panel | `apps/desktop-client` | Hide/extract workflows |
| [ ] | Security UX | Add clearer secure state/status indicators | `apps/desktop-client` | Session security visibility |
| [ ] | UX | Improve error display and operation feedback | `apps/desktop-client` | User-friendly handling |

---

# 3. Product Development Roadmap

## 3.1 Milestones

| Done | Phase | Goal | Deliverable |
|---|---|---|---|
| [x] | Phase 0 | Bootstrap the monorepo | Gradle modules, Java 21, initial structure |
| [ ] | Phase 1 | Shared foundation | `common-model` + `common-net` ready |
| [x] | Phase 2 | Cryptographic base | `crypto-core` migrated and tested |
| [ ] | Phase 3 | Secure file transfer | `file-transfer-core` working |
| [x] | Phase 4 | LAN chat | `chat-core` working |
| [ ] | Phase 5 | First usable product | Desktop MVP: peers, chat, files, keys, log |
| [ ] | Phase 6 | Audio support | `audio-core` + basic UI integration |
| [ ] | Phase 7 | Webcam/media support | `webcam-core` + basic UI integration |
| [ ] | Phase 8 | Hidden-message workflows | `stego-core` + crypto integration |
| [ ] | Phase 9 | Stabilization | tests, packaging, documentation, UX polish |

---

## 3.2 Recommended Order of Work

| Done | Order | Work Item | Why |
|---|---|---|---|
| [x] | 1 | Finalize project structure | Prevent future architectural drift |
| [x] | 2 | Implement `common-model` | Shared DTO baseline |
| [ ] | 3 | Implement `common-net` | Shared networking baseline |
| [x] | 4 | Migrate `crypto-core` | Security foundation for other modules |
| [ ] | 5 | Migrate `file-transfer-core` | First practical secure feature |
| [x] | 6 | Migrate `chat-core` | Core communication feature |
| [ ] | 7 | Build desktop MVP | First usable application |
| [ ] | 8 | Migrate `audio-core` | Secondary communication channel |
| [ ] | 9 | Migrate `webcam-core` | Media capabilities |
| [ ] | 10 | Migrate `stego-core` | Advanced security feature |
| [ ] | 11 | Refine UX, packaging, tests | Productization |

---

## 3.3 Definition of Done for Each Module

| Done | Module | Definition of Done |
|---|---|---|
| [x] | common-model | Shared records/enums exist, compile cleanly, reused by other modules |
| [ ] | common-net | Transport abstractions compile cleanly and are reused by feature modules |
| [x] | crypto-core | AES/RSA/hash/signature/key APIs extracted, tested, UI-free |
| [x] | chat-core | Message flow, handshake, peer/session logic extracted, tested, UI-free |
| [ ] | file-transfer-core | Send/receive/progress logic extracted, tested, UI-free |
| [ ] | audio-core | Audio transport/services extracted behind clean APIs |
| [ ] | webcam-core | Camera/media services extracted, native dependencies isolated |
| [ ] | stego-core | Hide/extract services extracted, crypto integration possible, UI-free |
| [ ] | desktop-client | JavaFX shell integrates modules without embedding low-level logic |

---

## 3.4 Quality and Stabilization Checklist

| Done | Area | Step | Notes |
|---|---|---|---|
| [ ] | Testing | Add unit tests for all pure logic modules | Prefer deterministic tests |
| [ ] | Testing | Add integration tests for chat and file transfer | Use test environments/mocks |
| [ ] | Reliability | Add consistent exception model across modules | Avoid ad-hoc error handling |
| [ ] | Logging | Add structured internal logging strategy | Keep UI logging separate from core logging |
| [ ] | Packaging | Define runnable desktop packaging strategy | Distribution-ready app |
| [ ] | Documentation | Add architecture overview to repository | Explain module boundaries |
| [ ] | Documentation | Add migration notes per module | Track legacy-to-new mapping |
| [ ] | UX | Improve desktop usability and error feedback | Product polish |

---

# 4. Notes

- Prefer **incremental migration** over large rewrites.
- Refactor legacy repositories into reusable services instead of copying their original app structure.
- Keep the first goal focused on a usable **desktop MVP**:
  - peer list
  - LAN chat
  - secure file transfer
  - keys/security screen
  - event log
- Audio, webcam, and steganography should be added after the MVP unless explicitly prioritized.
