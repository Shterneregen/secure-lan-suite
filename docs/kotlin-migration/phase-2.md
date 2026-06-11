# Phase 2: Low-risk modules first

[Back to Kotlin Migration Plan](kotlin-migration.md#phase-index)

- Migrate [`modules/audio-core`](../../modules/audio-core/build.gradle) first.
- Migrate [`modules/webcam-core`](../../modules/webcam-core/build.gradle) second.
- Recheck module dependency rules before changing these modules, because current build files depend on [`modules/webrtc-core`](../../modules/webrtc-core/build.gradle).
- Keep these modules small and profile-oriented; do not introduce UI code or Android-specific dependencies.

Status: completed. [`modules/audio-core`](../../modules/audio-core/build.gradle) and [`modules/webcam-core`](../../modules/webcam-core/build.gradle) now use Kotlin JVM and keep Java-callable record-style profile DTOs through Kotlin JVM records.

Compatibility note: Kotlin 2.2.21 does not yet emit JVM target 25 bytecode, so Kotlin JVM modules compile with JVM target 24 while the Java toolchain remains Java 25. The build explicitly ignores Kotlin/Java target validation for migrated JVM modules until Kotlin supports JVM target 25.
