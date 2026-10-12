---
name: securelan-android
license: MIT
description: >
  Work on the SecureLanSuite Android client and desktop interoperability. Use
  for Android Compose UI, permissions, discovery, room hosting or joining,
  secure chat, file transfer, APK build/sign/install flows, and Android-specific
  diagnostics.
---

# SecureLanSuite Android

## Sources

- Read [`apps/android-client/android-readme.md`](../../../apps/android-client/android-readme.md)
  and inspect the affected implementation.
- Use the owning Gradle files for versions, SDK levels, signing, tasks, and
  artifact names.

## Boundaries

- Preserve desktop compatibility and keep Android dependencies out of reusable
  core modules.
- Keep foreground-service and notification behavior aligned with Android
  lifecycle requirements.
- Handle runtime permissions and unavailable network conditions explicitly.

## Verification

- Run the narrowest relevant test or build task. Use a device walkthrough when
  behavior depends on permissions, storage, notifications, or lifecycle.
- Treat desktop interoperability as unverified until both clients are exercised.
- Update the Android README when capability, setup, signing, artifact, or
  interoperability behavior changes.
