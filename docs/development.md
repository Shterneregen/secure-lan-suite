# SecureLanSuite Development Guide

This guide is the command index and smoke-test reference for local development.
The owning Gradle files define exact dependency and plugin versions.

## Requirements

- JDK 25 for full repository and desktop builds.
- The checked-in Gradle Wrapper; no separate Gradle or Kotlin installation is
  needed.
- Internet access for the first dependency download.
- Android SDK Platform 35, compatible Build Tools, and Platform Tools for the
  Android client.

For exact Gradle, Kotlin, Compose, Android plugin, and application versions,
inspect `gradle/wrapper/gradle-wrapper.properties`, `settings.gradle`, and
`gradle.properties`.

## Verify the environment

```powershell
java --version
.\gradlew.bat --version
jpackage --version
adb version
adb devices
```

`jpackage` is needed only for desktop packaging. `adb` and a connected device or
emulator are needed only for Android install and device tests.

## Build, test, and run

On macOS or Linux, replace `.\gradlew.bat` with `./gradlew`.

```powershell
.\gradlew.bat test
.\gradlew.bat clean build
.\gradlew.bat :apps:desktop-client:run
```

Use `test` for a fast repository verification and `build` when assembled outputs
also matter. `:apps:desktop-client:runComposeShell` remains a compatibility
alias for the primary desktop run task.

### Desktop Kotlin checks and documentation

```powershell
.\gradlew.bat :apps:desktop-client:lintKotlin
.\gradlew.bat :apps:desktop-client:formatKotlin
.\gradlew.bat :apps:desktop-client:check
```

The desktop `check` task includes unit tests and ktlint source-hygiene checks.
The module's `.editorconfig` explicitly enables import order, unused imports,
KDoc placement, modifier order, string templates, and whitespace checks while
preserving existing Compose naming and layout conventions. `formatKotlin`
applies the corresponding automatic corrections. Checked-in upstream Material
icons under `src/main/kotlin/androidx/` retain their generated formatting and
are excluded; application sources and tests are checked.

Desktop source documentation uses English KDoc because the module is written
in Kotlin. Document service boundaries, threading, persistence, state ownership,
and recovery contracts rather than restating function names. The Java `javadoc`
task has no sources in this module and does not generate Kotlin documentation.

## Android client

```powershell
.\gradlew.bat :apps:android-client:assembleDebug
.\gradlew.bat :apps:android-client:assembleRelease
.\gradlew.bat :apps:android-client:testDebugUnitTest
.\gradlew.bat :apps:android-client:lintKotlin :apps:android-client:lintDebug
.\gradlew.bat :apps:android-client:formatKotlin
.\gradlew.bat :apps:android-client:connectedDebugAndroidTest
```

APK names use `secureLanVersion` from `gradle.properties`:

- `apps/android-client/build/outputs/apk/debug/secure-lan-<version>.apk`
- `apps/android-client/build/outputs/apk/release/secure-lan-<version>.apk`

See the [Android client guide](../apps/android-client/android-readme.md) for SDK
setup, signing, install, lifecycle, permissions, and troubleshooting.

## Desktop workflow smoke test

1. Enter a nickname and shared room password.
2. Select **Open room** to host, or select a discovered room and connect.
3. Keep **Discoverable** enabled when the room should be advertised over LAN.
4. Use manual host and port fields only when discovery is unavailable.
5. Exchange messages, send a file, and exercise voice or experimental video as
   required by the change.
6. Verify affected settings, tray lifecycle, and notifications on a supported
   desktop environment.

The connected workspace keeps peers on the left, conversation in the center,
and contextual actions or activity on the right or in a drawer. Media device
selection and tests are in **Settings → Audio & Video**.

The **Actions** panel keeps active file transfers and incoming approval requests
expanded. Completed or failed transfers are under **Recent files**, collapsed by
default; **Auto-save: on/off** expands to explain and change the receive setting.
Start a new file transfer with **Attach** in chat after selecting its recipient.

### Multiple files

Select one or more files from **Attach**, review the explicit recipient and file
list, then start the batch. Adding files extends the draft; canceling the system
picker does not clear it. Removing a queued file does not undo a completed
transfer. The batch stays bound to its original recipient even if another peer
is selected in the workspace.

There is one unfinished outgoing batch per application. Files use independent
`SECURE_FILE_TRANSFER_V1` connections in order; a batch does not add metadata or
messages to the chat protocol. The next file waits for the receiver's final
`DONE`, not merely 100% local byte progress. A failure pauses the queue for an
explicit retry, skip, or finish action. **Stop after current file** leaves the
current operation running and skips the remaining queue after it finishes.

Desktop receiver approval remains per file when auto-save is off. No action
implicitly enables auto-save or approves the remaining files. A missing final
acknowledgement has an uncertain outcome: the receiver may already have saved
the file. Retrying is explicit and can create a duplicate. Disconnecting does not
silently resume the batch in a new room or redirect it to another person.

Leaving a remote room or losing its chat connection stops that room's file
receiver. The next successful connection starts it with the new room password,
receive port, and configured download directory. A locally hosted room retains
its receiver until hosting stops, even if its local chat client disconnects.

Focused validation:

```powershell
.\gradlew.bat :modules:file-transfer-core:test :apps:desktop-client:test
.\gradlew.bat :apps:android-client:testDebugUnitTest :apps:android-client:assembleDebug
```

Exercise 1, 3, and 25 files in both desktop/Android directions, then repeat with
desktop auto-save disabled. Reject the second file and verify that the third is
not offered automatically. Include a zero-byte file, same-named files from
different folders, a changed/unavailable source, disconnect, stop-after-current,
and a file larger than available RAM. Confirm that successful files are not
repeated and that the summary is independent of the short recent-files history.

On desktop, check the review dialog, keyboard focus, and access to batch details
at the default size and below the 1280 dp drawer breakpoint. On Android, check
document-provider errors, unknown-size preparation, rotation, storage failures,
and large text. JVM tests do not substitute for these UI/device checks or real
interoperability with the previous single-file client.

## Android interoperability smoke test

1. Install or run the Android client and start a desktop room on the same LAN.
2. Keep desktop discovery enabled and grant `NEARBY_WIFI_DEVICES` on Android 13+
   when requested.
3. Enter the same password, select the discovered room, and connect.
4. Exchange chat messages and send a file from Android to desktop.
5. Keep **Receive files automatically** enabled on Android, which is the default.
   After the secure chat connection is established, send a file from desktop;
   Android starts its receiver automatically.
6. When relevant, host a room from Android and connect from desktop. Android
   hosting runs in a foreground service and is advertised through LAN discovery.

Android supports LAN audio/video calls and browser Quick Share. RTC data
channels, screen sharing and steganography are not implemented. See the Android
guide for call permission/device checks and the unauthenticated HTTP trust boundary.

## Ports

- secure chat: `5050` TCP
- encrypted file transfer: `5051` TCP
- LAN discovery: `5052` UDP
- desktop browser Quick Share: `5053` TCP

An Android client connected to a desktop room may receive on the peer file port
plus `1000`, normally `6051`, to avoid a local conflict with `5051`.

## Browser Quick Share smoke test

Publish a file or text snippet and open its generated
`http://<lan-ip>:5053/s/<share-name>` link from another LAN device. Verify the
expiration and access limit, then stop the share.

For both file and text links, verify the QR action while the link is active,
including the corresponding **File link created** and **Text link created**
events in chat.
On a desktop with multiple network adapters, the displayed, copied, opened, and
QR-encoded link should prefer the primary local address shown in the desktop
network status, when that address is available among the link URLs.

Quick Share intentionally uses unauthenticated local HTTP. Anyone on the LAN who
knows or discovers an active link can access it, so use it only on a trusted LAN.

Desktop Quick Share logs record lifecycle metadata rather than shared text,
text-derived display names, share IDs, or share URLs. Names and links remain
available in the sharing UI.

## Desktop packaging

Build the primary Compose portable image and ZIP:

```powershell
.\gradlew.bat :apps:desktop-client:buildPortable
```

The archive is written under `apps/desktop-client/build/distributions/`; the
intermediate application image is under
`apps/desktop-client/build/packaging/SecureLanSuite/`. This `app-image` flow does
not require WiX.

Build the Windows EXE installer:

```powershell
.\gradlew.bat :apps:desktop-client:buildExe
```

Installer output is written under `apps/desktop-client/build/packaging/`. The
`:apps:desktop-client:createExe` task remains available as the direct packaging
task. Follow the
[Windows installer guide](wix-installation.md) for the verified WiX setup and
troubleshooting. The deprecated fallback client has separate instructions in
[the JavaFX guide](javafx-client.md).
