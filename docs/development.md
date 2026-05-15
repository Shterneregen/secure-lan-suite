# SecureLanSuite Development Guide

This guide contains local development, build, run, packaging, and smoke-test notes for SecureLanSuite. The root [`README.md`](../README.md) intentionally stays focused on general project information.

## Requirements

- JDK 25 installed and active.
- Gradle 9.1 or newer is recommended for Java 25. The repository uses the Gradle Wrapper, so normal builds should use `gradlew` or `gradlew.bat` from the repository root.
- Internet access on the first Gradle build so dependencies can be downloaded.
- Android SDK Platform 35 and Android SDK Platform Tools are required when building or installing `apps/android-client`.
- Windows only: WiX 5.0.2 installed and available in `PATH` for EXE packaging.
- For WiX 5, the required extensions must also be installed:
  - `WixToolset.UI.wixext`
  - `WixToolset.Util.wixext`

## Verify the environment

Desktop/core development:

```powershell
java --version
jpackage --version
wix --version
```

`wix --version` is only required when building the Windows EXE installer.

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

Run the desktop client:

```bash
./gradlew :apps:desktop-client:run
```

On Windows, use `gradlew.bat`:

```powershell
.\gradlew.bat clean build
.\gradlew.bat :apps:desktop-client:run
```

## Android client build

Build Android debug and release APKs:

```powershell
.\gradlew.bat :apps:android-client:assembleDebug
.\gradlew.bat :apps:android-client:assembleRelease
```

Android outputs:

- `apps/android-client/build/outputs/apk/debug/secure-lan-0.4.0.apk`
- `apps/android-client/build/outputs/apk/release/secure-lan-0.4.0.apk`

Detailed Android SDK setup, release signing, APK verification, install, troubleshooting, and desktop interoperability notes are in [`apps/android-client/android-readme.md`](../apps/android-client/android-readme.md).

## Desktop workflow smoke test

1. Enter a nickname and shared room password.
2. Click **Open room** to host locally, or wait for discovered peers in the left column.
3. Keep **Discoverable** enabled if this room should be advertised through UDP discovery.
4. Select a discovered peer and click **Connect**, or use the manual host/port fields as a fallback.
5. Exchange chat messages in the center feed.
6. Use right-side quick actions to send files, start a voice call, start an experimental video call, or end an active call.

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

- it is a client/interoperability MVP and does not host a desktop-compatible chat room yet;
- it uses a small Android-local protocol compatibility layer rather than depending on JavaFX or UI-specific desktop code;
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

All desktop packaging tasks live in `apps/desktop-client`.

### Portable build

Build a portable application image and ZIP archive:

```bash
./gradlew :apps:desktop-client:buildPortable
```

Example output:

- `apps/desktop-client/build/distributions/SecureLanSuite-<version>-portable.zip`

The intermediate application image is created under:

- `apps/desktop-client/build/packaging/SecureLanSuite/`

This task uses `jpackage --type app-image`, so it does not require WiX.

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

Notes:

- this task must be run on Windows;
- `jpackage` must come from JDK 25;
- WiX 5.0.2 must be installed and available in `PATH`;
- WiX extensions `WixToolset.UI.wixext` and `WixToolset.Util.wixext` must be installed globally;
- WiX 7 is not recommended for this project because the working `jpackage` setup was verified with WiX 5.0.2.

## Installing WiX on Windows

Use the detailed instructions in [`docs/wix-installation.md`](wix-installation.md).

Short version:

```powershell
dotnet nuget add source https://api.nuget.org/v3/index.json -n nuget.org
dotnet tool install --global wix --version 5.0.2
wix extension add --global WixToolset.UI.wixext/5.0.2
wix extension add --global WixToolset.Util.wixext/5.0.2
wix extension list --global
wix --version
```
