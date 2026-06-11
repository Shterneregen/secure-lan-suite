# Phase 1: Build foundation and baseline validation

[Back to Kotlin Migration Plan](kotlin-migration.md#phase-index)

## Build foundation prerequisite

- Add the Kotlin JVM plugin to the root build in a way that does not affect [`apps/android-client`](../../apps/android-client/build.gradle).
- Keep Java Library configuration for JVM modules from [`build.gradle`](../../build.gradle:15).
- Configure Kotlin JVM toolchains consistently with the Java 25 baseline from [`build.gradle`](../../build.gradle:21).
- Verify that Kotlin compiler target settings are compatible with Java compile settings from [`build.gradle`](../../build.gradle:25).
- Prefer centralized version management for Kotlin plugins so the Android and JVM sides do not drift unnecessarily.
- Keep the module graph in [`settings.gradle`](../../settings.gradle:11) unchanged.

## Baseline validation

- Run a clean full build before migration.
- Record public API contracts for models, services, events, protocol classes, and exceptions.
- Run the desktop client through [`apps/desktop-client`](../../apps/desktop-client/build.gradle).
- Build the Android debug APK through [`apps/android-client`](../../apps/android-client/build.gradle).
- Keep the current protocol behavior unchanged for LAN discovery, chat handshake, file transfer, and RTC signaling.

Status: completed. The clean full build passed, Android debug assembly was covered by the full build, and the reusable module public API baseline is captured in [`docs/kotlin-api-baseline.md`](kotlin-api-baseline.md).
