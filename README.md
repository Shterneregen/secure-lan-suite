# Secure LAN Suite

Multi-module Gradle project for a secure LAN desktop application.

## Modules
- common-model
- common-net
- crypto-core
- chat-core
- file-transfer-core
- audio-core
- webcam-core
- stego-core

## App
- desktop-client

# Current MVP supports:
- start local chat server
- connect a client
- send and receive chat messages
- simple password-based handshake
- nickname uniqueness on the server

Run:
```bash
./gradlew :apps:desktop-client:run
```