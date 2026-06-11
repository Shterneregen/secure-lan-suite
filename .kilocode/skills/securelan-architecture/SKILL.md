---
name: securelan-architecture
license: MIT
description: >
  SecureLanSuite architecture, module-boundary, Kotlin migration, and Gradle
  multi-project workflow skill. Use when the user asks about SecureLanSuite
  modules, dependency directions, core-vs-UI separation, Java/Kotlin interop,
  project structure, migration planning, or cross-module implementation rules.
---

# SecureLanSuite Architecture Skill

Use this skill for changes or analysis that affect the SecureLanSuite module graph, reusable core modules, Kotlin/Java migration, project-wide Gradle configuration, or architecture documentation.

## Mandatory first checks

1. Read the current project rules in [`rules.md`](../../rules/rules.md).
2. Read only the relevant public docs before proposing broad changes:
   - [`README.md`](../../../README.md) for current stack and module responsibilities.
   - [`docs/kotlin-migration.md`](../../../docs/kotlin-migration/kotlin-migration.md) for Kotlin migration state.
   - [`docs/desktop-client-kotlin-migration.md`](../../../docs/kotlin-migration/desktop-client-kotlin-migration.md) for desktop interop and Compose migration status.
   - [`docs/development.md`](../../../docs/development.md) for build and validation tasks.
3. Inspect the actual module build file before editing a module dependency.

## Architecture guardrails

- Do not introduce Spring or Spring Boot unless the user explicitly requests it.
- Keep reusable modules UI-agnostic.
- Do not put JavaFX, Android UI, or Compose UI code in `modules/*`.
- Do not make any `modules/*` project depend on `apps/desktop-client` or `apps/android-client`.
- Avoid cyclic dependencies.
- Keep cryptography logic outside UI modules.
- Keep network, file-transfer, and realtime orchestration behind focused service boundaries where practical.
- Prefer plain Java/Kotlin, constructor injection, explicit interfaces, immutable DTOs, and small focused classes.

## Allowed internal dependency directions

- `apps/*` may depend on `modules/*`.
- `modules/common-model` must not depend on internal modules.
- `modules/common-net` may depend only on `common-model`.
- `modules/crypto-core` may depend only on `common-model`.
- `modules/chat-core` may depend on `common-model`, `common-net`, and `crypto-core`.
- `modules/file-transfer-core` may depend on `common-model`, `common-net`, and `crypto-core`.
- `modules/webrtc-core` may depend on `common-model` and external `webrtc-java`.
- `modules/audio-core` may depend on `common-model` and `common-net`.
- `modules/webcam-core` may depend on `common-model`.
- `modules/stego-core` may depend on `common-model` and `crypto-core`.

## Kotlin migration rules

- Keep the repository buildable after every incremental change.
- Preserve public API contracts unless the user explicitly approves a breaking change.
- Preserve protocol behavior for LAN discovery, chat handshake, file transfer, quick share, RTC signaling, voice, and experimental video.
- Treat `webrtc-core` callback-heavy runtime code as high risk; prefer small, reversible changes with targeted tests.
- Kotlin JVM modules currently compile with JVM target 24 while the Java toolchain remains Java 25; do not “fix” this unless the Kotlin version supports JVM target 25.

## Gradle and validation defaults

On Windows, prefer `gradlew.bat` commands from the repository root.

- Whole repository validation: `gradlew.bat clean build --no-daemon`.
- Focused JVM module validation: `gradlew.bat :modules:<module-name>:test --no-daemon`.
- Desktop validation: `gradlew.bat :apps:desktop-client:test :apps:desktop-client:build --no-daemon`.
- Android debug validation: `gradlew.bat :apps:android-client:assembleDebug --no-daemon`.

Only run broader validation when the change justifies it. For small pure logic changes, prefer the closest module tests first.

## Documentation update rule

If a change alters architecture, supported Java/Kotlin versions, module responsibilities, migration status, packaging flow, or product status, update the relevant public docs in [`README.md`](../../../README.md) or [`docs`](../../../docs) in the same task.
