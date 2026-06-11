# Phase 8: Desktop Kotlin interop slice (completed and closed)

[Back to Kotlin Migration Plan](kotlin-migration.md#phase-index)

Goal: add Kotlin JVM support to [`apps/desktop-client`](../../apps/desktop-client/build.gradle) and extract low-risk desktop helpers without changing JavaFX behavior, LAN interoperability, or packaging configuration.

Finalized outcome:

- [`apps/desktop-client`](../../apps/desktop-client/build.gradle) applies the Kotlin JVM plugin while keeping the JavaFX plugin, Application plugin, Java 25 toolchain, packaging tasks, dependency graph, and main-class configuration unchanged.
- The Java launcher [`Main.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/Main.java) and JavaFX application boundary [`ChatApplication.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ChatApplication.java) remain in place so [`application.mainClass`](../../apps/desktop-client/build.gradle:8), JAR manifest generation, and `jpackage` tasks continue to resolve `com.shterneregen.securelan.desktop.Main`.
- The public desktop `MainView` JVM class is provided by [`MainView.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui/MainView.kt). It preserves JVM class identity `com.shterneregen.securelan.desktop.ui.MainView`, the no-argument constructor, and Java-callable `createContent()` / `shutdown()` lifecycle methods used by [`ChatApplication.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ChatApplication.java).
- The source-equivalent JavaFX implementation intentionally remains in package-private [`MainViewDelegate.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ui/MainViewDelegate.java). [`MainView.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui/MainView.kt) is a Kotlin compatibility shell, not a completed JavaFX UI rewrite.
- Safe desktop slices extracted transfer, quick-share, realtime, peer, media-device, list-cell, and low-risk main-view helper logic to Kotlin helpers with desktop-client tests.
- The attempted duplicate desktop nickname service was removed in favor of the shared [`DefaultRandomNicknameService`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/service/impl/DefaultRandomNicknameService.kt) from chat-core.
- Recorded validation is limited to `gradlew.bat :apps:desktop-client:test :apps:desktop-client:build`.

Closed decision:

- Phase 8 is completed, finalized, and closed.
- Further migration of JavaFX UI panels, delegate logic, or the Java launcher to Kotlin will not continue under Phase 8.
- Remaining JavaFX-to-Kotlin panel/delegate checklist items are superseded by Phase 9, which changes the direction from JavaFX Kotlin conversion to a controlled Compose Multiplatform UI migration.
- No Phase 8 result claims desktop launch, portable ZIP, or Windows EXE validation beyond the documented `test` and `build` Gradle tasks.

Final Phase 8 acceptance criteria:

- [`gradlew.bat :apps:desktop-client:test`](../../gradlew.bat) and [`gradlew.bat :apps:desktop-client:build`](../../gradlew.bat) pass for the desktop interop slice.
- The desktop client remains an application boundary only; no reusable core module depends on [`apps/desktop-client`](../../apps/desktop-client/build.gradle).
- No JavaFX code is moved into reusable core modules.
- The Java launcher, JavaFX application boundary, public Kotlin compatibility shell, and package-private JavaFX delegate boundaries are documented.

## Desktop-specific completed checklist migrated from the deleted desktop checklist

### Phase 8 desktop Kotlin interop

- [x] Enabled Kotlin JVM support in [`apps/desktop-client`](../../apps/desktop-client/build.gradle) without replacing JavaFX packaging or launcher behavior.
- [x] Kept [`Main.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/Main.java) and [`ChatApplication.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ChatApplication.java) as the JavaFX application boundary.
- [x] Replaced the public desktop `MainView` source with [`MainView.kt`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui/MainView.kt) as a Kotlin compatibility shell.
- [x] Kept the JavaFX implementation in [`MainViewDelegate.java`](../../apps/desktop-client/src/main/java/com/shterneregen/securelan/desktop/ui/MainViewDelegate.java) as the stable fallback baseline.
- [x] Extracted desktop helper models, formatters, list cells, and low-risk main-view helpers into [`apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui`](../../apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/ui).
- [x] Reused shared chat-core nickname generation through [`DefaultRandomNicknameService.kt`](../../modules/chat-core/src/main/kotlin/com/shterneregen/securelan/chat/service/impl/DefaultRandomNicknameService.kt).
- [x] Validated the desktop interop slice with [`gradlew.bat :apps:desktop-client:test :apps:desktop-client:build`](../../gradlew.bat).
- [x] Closed further JavaFX delegate/panel conversion to Kotlin as superseded by Compose UI migration.
