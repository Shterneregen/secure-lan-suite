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

## Current MVP supports
- start a local secure chat server
- connect a client with encrypted chat handshake
- send and receive chat messages
- start a secure file transfer server automatically with the chat server
- send files from the JavaFX UI
- receive files into a configurable downloads directory
- view chat, transfer progress, and errors in the shared event log

## File transfer notes
- chat uses the configured port, for example `5050`
- file transfer uses `chat port + 1`, for example `5051`
- file transfer handshake uses `crypto-core` with ephemeral RSA key exchange and AES-GCM transport encryption

## Run
```bash
./gradlew :apps:desktop-client:run
```

## Current limitations
- there is no peer discovery yet
- the desktop client still uses a simple single-window layout instead of separate tabs/panels
- key management and advanced transfer controls are not exposed in the UI yet
