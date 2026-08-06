# SecureLanSuite Development Guide

This guide contains local development, build, run, packaging, and smoke-test notes for SecureLanSuite. The root [`README.md`](../README.md) intentionally stays focused on general project information.

## Requirements

- JDK 25 installed and active.
- The repository uses Gradle Wrapper 9.4.1 and Java 25 toolchains. Do not install a separate Gradle or Kotlin compiler for normal development.
- Kotlin 2.2.21 is used by the Android client and migrated JVM core modules, including Kotlin test sources; use the Gradle Wrapper rather than installing a separate local Kotlin compiler.
- Internet access on the first Gradle build so dependencies can be downloaded.
- Android SDK Platform 35, Android SDK Build Tools, and Android SDK Platform Tools are required when building or installing `apps/android-client`.
- The Android module currently uses Android Gradle Plugin 9.1.1 and Kotlin 2.2.21.

## Verify the environment

Desktop/core development:

```powershell
java --version
.\gradlew.bat --version
jpackage --version
```

`jpackage --version` is only required for desktop packaging.

Android development:

```powershell
adb version
adb devices
```

`adb devices` should show a connected physical phone or a running emulator when Android install/test tasks are needed.

## Build and run

Build the whole project:

```bash
./gradlew clean build
```

Run the primary Compose desktop client:

```bash
./gradlew :apps:desktop-client:run
```

On Windows, use `gradlew.bat`:

```powershell
.\gradlew.bat clean build
.\gradlew.bat :apps:desktop-client:run
```

Run the repository test suite:

```bash
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

Use `test` for a fast verification pass and `build` when packaging or validating all assembled outputs.

The previous Compose-specific task remains as a compatibility alias for scripts that still use it:

```powershell
.\gradlew.bat :apps:desktop-client:runComposeShell
```

## Android client build

Build Android debug and release APKs:

```powershell
.\gradlew.bat :apps:android-client:assembleDebug
.\gradlew.bat :apps:android-client:assembleRelease
```

Android outputs use the version from `gradle.properties` (`secureLanVersion`, currently `0.5.1`):

- `apps/android-client/build/outputs/apk/debug/secure-lan-<version>.apk`
- `apps/android-client/build/outputs/apk/release/secure-lan-<version>.apk`

Detailed Android SDK setup, release signing, APK verification, install, troubleshooting, and desktop interoperability notes are in [`apps/android-client/android-readme.md`](../apps/android-client/android-readme.md).

## Desktop workflow smoke test

Use `apps/desktop-client` for Compose UI/UX validation.

1. Enter a nickname and shared room password.
2. Click **Open room** to host locally, or wait for discovered peers in the left column.
3. Keep **Discoverable** enabled if this room should be advertised through UDP discovery.
4. Select a discovered peer and click **Connect**, or use the manual host/port fields as a fallback.
5. Exchange chat messages in the center feed.
6. Use right-side quick actions to send files, start a voice call, start an experimental video call, or end an active call.
7. Verify settings persistence, tray hide/show/exit behavior, and desktop notifications when running on a supported desktop environment.

Default ports:

- chat: `5050`
- encrypted file transfer: `5051`
- UDP discovery: `5052`
- no-auth LAN browser quick share: `5053`

When the Android client receives files while connected to a desktop room, it may listen on the remote file-transfer port plus `1000`, usually `6051`, to avoid clashing with the desktop receiver on `5051`.

## Android interoperability smoke test

1. Build and install the Android APK, or run `apps/android-client` from Android Studio.
2. Start the desktop client on a computer in the same LAN and click **Open room**.
3. Keep desktop **Discoverable** enabled.
4. On Android, grant `NEARBY_WIFI_DEVICES` on Android 13+ if prompted.
5. Enter the same room password, select the discovered desktop peer, and tap **Connect**.
6. Exchange chat messages, use **Pick file** / **Send file** for Android-to-desktop transfer, or tap **Receive files** before sending a desktop-to-Android file.

Android client notes:

- it can host a desktop-compatible encrypted chat room from the Devices screen; hosting runs as a foreground service and is advertised over LAN discovery;
- it uses a small Android-local protocol compatibility layer rather than depending on desktop UI code;
- it supports UDP discovery, secure chat, encrypted file send, encrypted file receive, progress indicators, a dark-theme toggle, and in-app diagnostics logs;
- it does not support voice, WebRTC data channels, camera/video, screen sharing, steganography tools, or no-auth browser quick share yet.

## No-auth LAN browser quick share smoke test

The desktop client can publish temporary browser-accessible LAN shares for a file or a text snippet. The receiver does not need Secure LAN Suite installed: they open the generated `http://<lan-ip>:5053/s/<share-name>` link in a browser, then download the file or copy the text.

Safety constraints:

- there is intentionally no login and no random URL token;
- anyone on the same LAN who knows or discovers the link can access an active share;
- each share should have an expiration and access limit;
- stop the share server or stop individual shares when finished;
- Windows/macOS/Linux firewalls may need to allow inbound TCP on the quick-share port.

## Desktop packaging

The primary Compose desktop client owns the current packaging tasks. The deprecated fallback client has separate documentation in [`javafx-client.md`](javafx-client.md).

### Portable build

Build the primary Compose portable application image and ZIP archive:

```bash
./gradlew :apps:desktop-client:buildPortable
```

On Windows:

```powershell
.\gradlew.bat :apps:desktop-client:buildPortable
```

Example output:

- `apps/desktop-client/build/distributions/SecureLanSuite-<version>-portable.zip`

The intermediate application image is created under:

- `apps/desktop-client/build/packaging/SecureLanSuite/`

This task uses `jpackage --type app-image` and does not require the Windows EXE installer toolchain.

`:apps:desktop-client:buildComposePortable` remains an alias for the primary `buildPortable` task.

### Windows EXE installer

Build the Windows EXE installer:

```powershell
.\gradlew.bat :apps:desktop-client:buildExe
```

or directly:

```powershell
.\gradlew.bat :apps:desktop-client:createExe
```

Output directory:

- `apps/desktop-client/build/packaging/`

Example output file:

- `apps/desktop-client/build/packaging/SecureLanSuite-<version>.exe`

For Windows EXE prerequisites, setup, and troubleshooting, use the dedicated [Windows installer guide](wix-installation.md).
