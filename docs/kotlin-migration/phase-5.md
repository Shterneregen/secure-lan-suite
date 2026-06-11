# Phase 5: Chat and file transfer

[Back to Kotlin Migration Plan](kotlin-migration.md#phase-index)

- Migrate [`modules/chat-core`](../../modules/chat-core/build.gradle) after common-net and crypto-core are stable.
- Preserve UDP discovery wire format, handshake behavior, chat events, receive loops, service interfaces, and signaling transport.
- Migrate [`modules/file-transfer-core`](../../modules/file-transfer-core/build.gradle) after transport and crypto APIs are stable.
- Preserve file metadata format, encrypted transfer handshake, progress events, acceptance handling, and integration tests.
- Avoid changing desktop and Android interoperability protocols during this phase.

Status: completed. [`modules/chat-core`](../../modules/chat-core/build.gradle) and [`modules/file-transfer-core`](../../modules/file-transfer-core/build.gradle) now use Kotlin JVM for main sources. UDP discovery, secure chat handshake, RTC signaling transport, file metadata serialization, encrypted file-transfer handshake, quick-share, acceptance handling, progress events, existing Java integration tests, and the full repository build are preserved.

Post-migration Phase 10 update: [`chat-core`](../../modules/chat-core/build.gradle) now carries explicit peer capability metadata through [`PeerCapabilities.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/protocol/handshake/PeerCapabilities.kt), [`HandshakeRequest.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/protocol/handshake/HandshakeRequest.kt), [`HandshakeResponse.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/protocol/handshake/HandshakeResponse.kt), [`ChatClientConnectRequest.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/service/ChatClientConnectRequest.kt), [`ChatConnectedEvent.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/event/ChatConnectedEvent.kt), and [`ChatUserJoinedEvent.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/event/ChatUserJoinedEvent.kt). These classes are no longer all pure Kotlin JVM records, but they preserve Java-callable constructors/accessors and backward-compatible unknown-capability defaults. Treat future handshake changes as protocol-compatibility work, not a broad migration task.
