---
name: securelan-android
description: Develop and review the SecureLanSuite Android client and its desktop interoperability. Use for apps/android-client, Android Compose UI, LAN discovery, secure chat, encrypted file transfer, permissions, APK builds, signing, diagnostics, and Android-to-desktop protocol compatibility.
license: MIT
---

# SecureLanSuite Android

## Scope

Use this skill for work in `apps/android-client` and for changes that can affect
Android-to-desktop interoperability. Keep general Android guidance subordinate
to the repository's existing toolchain and architecture.

## Sources of truth

1. User request and accepted scope.
2. Current code and Gradle configuration.
3. [`apps/android-client/android-readme.md`](../../../apps/android-client/android-readme.md).
4. SecureLanSuite protocol and architecture documentation.
5. Current first-party Android documentation for version-sensitive APIs.

Inspect existing SDK levels, Android Gradle Plugin, Kotlin, Compose BOM, and
dependency versions before proposing configuration or dependency changes.

## Current product scope

- The Android client is an experimental client/interoperability MVP.
- It supports UDP discovery, secure chat, encrypted file send and receive,
  progress, dark theme, and diagnostics logs.
- It does not host a desktop-compatible room.
- It does not support voice, WebRTC data channels, camera/video, screen sharing,
  steganography tools, or no-auth browser Quick Share.
- It keeps an Android-local compatibility layer instead of depending on desktop UI code.

## Architecture and security boundaries

- Do not make Android depend on `apps/desktop-client`.
- Do not add Android UI dependencies to reusable `modules/*`.
- Preserve service boundaries between UI, state, protocol, crypto, and transport.
- Do not change wire formats, handshake behavior, or crypto semantics casually.
- Keep received files in safe Android-scoped paths; the default is
  `Downloads/SecureLan`.
- Do not log passwords, keys, message contents, file contents, or other secrets.
- Keep diagnostics useful for discovery, connection, handshake, transfer,
  permission, and recovery failures.

## Interoperability rules

- Preserve UDP discovery payload compatibility with desktop discovery.
- Preserve secure-chat handshake and shared room-password semantics.
- Preserve AES-GCM/RSA encrypted file-transfer behavior.
- Preserve the Android receive-port offset where required to avoid desktop receiver clashes.
- Add or update compatibility tests before changing protocol codecs or crypto interop.
- Run affected desktop/core tests when shared behavior changes.

## Android implementation rules

- Follow the existing project architecture and naming conventions.
- Keep durable screen state explicit and immutable; keep business logic outside composables.
- Hoist state and keep composables small enough to review and test.
- Use Material and Foundation semantics before adding custom accessibility metadata.
- Give meaningful standalone graphics localized descriptions and keep decorative graphics silent.
- Verify keyboard, focus, large-screen, orientation, and adaptive behavior when applicable.
- Use descriptive Android resource names; do not invent blanket reserved-name lists.
- Preserve lifecycle ownership for callbacks, coroutines, media, and platform resources.
- Inspect existing versions before adding dependencies; use current official docs for API details.

## Permissions

- Align permission handling with the configured SDK levels.
- Account for `NEARBY_WIFI_DEVICES` behavior on Android 13+.
- Request permissions in user context and handle denial and permanent denial.
- Do not claim discovery or file access works until the relevant permission path is verified.

## Validation

Use the Gradle Wrapper and start with the narrowest affected task on Windows:

- Unit tests: `gradlew.bat :apps:android-client:testDebugUnitTest --no-daemon`.
- Debug APK: `gradlew.bat :apps:android-client:assembleDebug --no-daemon`.
- Release APK: `gradlew.bat :apps:android-client:assembleRelease --no-daemon`
  when signing or release behavior changes.
- Run the nearest desktop/core tests for interoperability changes.

Do not claim emulator, device, cross-device, permission, install, or release
validation unless it was actually performed.

## Documentation

Update `apps/android-client/android-readme.md` when build, signing, installation,
permissions, smoke tests, artifact names, or known limitations change. Update the
repository `README.md` when Android product capability status changes.
