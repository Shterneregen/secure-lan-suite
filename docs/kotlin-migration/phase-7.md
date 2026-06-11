# Phase 7: Tests and documentation

[Back to Kotlin Migration Plan](kotlin-migration.md#phase-index)

- Migrate tests close to the corresponding production module, but do not rewrite tests and production behavior in the same large commit.
- Update [`README.md`](../../README.md) if Kotlin becomes part of the official core stack.
- Update [`docs/development.md`](../development.md) if build, run, or environment requirements change.
- Update packaging documentation if Kotlin runtime dependencies affect desktop distributions.

Status: completed for the reusable-module test and documentation migration scope. Remaining Java JUnit tests in migrated reusable modules were moved from `src/test/java` to `src/test/kotlin` without changing the covered protocol, crypto, stego, transport, chat, file-transfer, or quick-share behaviors. The public overview already lists Kotlin as part of the core stack, and [`docs/development.md`](../development.md) now notes that Kotlin core sources and tests are built through Gradle with no separate local Kotlin installation required. Targeted validation passed with `gradlew.bat :modules:common-model:test :modules:common-net:test :modules:crypto-core:test :modules:stego-core:test :modules:chat-core:test :modules:file-transfer-core:test`.
