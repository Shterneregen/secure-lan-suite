---
name: securelan-android-interop
license: MIT
description: >
  SecureLanSuite Android client and desktop interoperability skill. Use when the
  user asks about apps/android-client, Android LAN discovery, Android secure
  chat, Android file transfer, protocol compatibility with desktop, APK builds,
  Android permissions, or mobile UI behavior.
---

# SecureLanSuite Android Interop Skill

Use this skill for work in `apps/android-client` or for changes that could affect Android-to-desktop interoperability.

## Current Android scope

- The Android client is experimental.
- It is a client/interoperability MVP and does not host a desktop-compatible chat room.
- It supports UDP discovery, secure chat, encrypted file send, encrypted file receive, progress indicators, dark theme toggle, and diagnostics logs.
- It does not support voice, WebRTC data channels, camera/video, screen sharing, steganography tools, or no-auth browser quick share.
- It intentionally keeps a small Android-local protocol compatibility layer instead of depending on desktop UI code.

Read [`apps/android-client/android-readme.md`](../../../apps/android-client/android-readme.md) before changing Android build, install, signing, or interoperability workflows.

## Boundaries

- Do not make Android depend on `apps/desktop-client`.
- Do not put Android UI dependencies in reusable `modules/*`.
- Do not change desktop wire protocols casually; Android compatibility depends on them.
- Keep permission handling aligned with Android API level behavior, especially `NEARBY_WIFI_DEVICES` on Android 13+.
- Keep file receive paths safe and Android-scoped; default downloads belong under `Downloads/SecureLan`.
- Keep diagnostics useful for discovery, connection, handshake, transfer, and permission failures.

## Protocol compatibility rules

- Preserve UDP discovery payload compatibility with desktop discovery.
- Preserve secure chat handshake behavior and shared room password semantics.
- Preserve AES-GCM/RSA encrypted file transfer behavior.
- Preserve Android receive-port offset behavior when needed to avoid desktop receiver clashes.
- Prefer adding compatibility tests before changing protocol codec or crypto interop code.

## Validation defaults

- Android unit tests: `gradlew.bat :apps:android-client:testDebugUnitTest --no-daemon`.
- Android debug APK: `gradlew.bat :apps:android-client:assembleDebug --no-daemon`.
- Android release APK: `gradlew.bat :apps:android-client:assembleRelease --no-daemon` when signing/release behavior changes.
- Desktop interop-affecting core changes should also run the nearest affected desktop/core module tests.

## Documentation rule

Update [`apps/android-client/android-readme.md`](../../../apps/android-client/android-readme.md) when changing Android build, signing, install, permissions, smoke tests, output APK names, or known limitations. Update [`README.md`](../../../README.md) if Android product capability status changes.
