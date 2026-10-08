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

## Android client

```powershell
.\gradlew.bat :apps:android-client:assembleDebug
.\gradlew.bat :apps:android-client:assembleRelease
.\gradlew.bat :apps:android-client:testDebugUnitTest
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

Android does not currently provide voice, RTC data channels, camera/video,
screen sharing, steganography, or browser Quick Share.

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

Quick Share intentionally uses unauthenticated local HTTP. Anyone on the LAN who
knows or discovers an active link can access it, so use it only on a trusted LAN.

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
