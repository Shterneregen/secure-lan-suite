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
  for current capabilities and procedures.
- Read `settings.gradle`, `gradle.properties`, and
  `apps/android-client/build.gradle` for current versions, SDK levels, signing,
  tasks, and artifact naming.
- Inspect the affected Android implementation before changing behavior.

## Boundaries

- Preserve desktop protocol compatibility for discovery, chat, room hosting,
  and encrypted file transfer.
- Keep Android and UI dependencies out of reusable core modules.
- Keep foreground-service and notification behavior aligned with Android
  lifecycle requirements.
- Handle runtime permissions and unavailable network conditions explicitly.
- Do not infer current capabilities or versions from this skill; verify them in
  the sources above.

## Verification

- Run the narrowest relevant Android unit or build task.
- Use instrumented tests or a physical-device walkthrough when the behavior
  depends on permissions, storage, notifications, or lifecycle.
- Treat desktop interoperability as unverified until both clients are exercised.
- Update the Android README when capability, setup, signing, artifact, or
  interoperability behavior changes.
