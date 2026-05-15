# SecureLan Android Client

The Secure LAN Suite Android client is an experimental Android MVP for connecting to desktop clients on the same LAN. The app can discover desktop peers through UDP discovery, connect to a secure desktop chat room, exchange chat messages, send files, and receive incoming files through a desktop-compatible encrypted file-transfer protocol.

## Status and limitations

- The Android client is currently an experimental MVP, not yet a full feature peer of the desktop client.
- The UI is implemented with native Android + Jetpack Compose Material 3 inside `apps/android-client`.
- Protocol code is minimally reimplemented inside `apps/android-client` to avoid introducing Android/UI dependencies into reusable Java core modules.
- Supported flows include desktop-compatible UDP discovery, secure chat handshake, encrypted chat messages, AES-GCM/RSA file-transfer handshake, Android-to-desktop file sending, and desktop-to-Android file receiving.
- The app starts UDP discovery automatically on launch and requests `NEARBY_WIFI_DEVICES` on Android 13+.
- The UI exposes nickname/password setup, discovered peer selection, connect/disconnect actions, chat, document-picker file selection, send progress, receive progress, a dark-theme switch, and an in-app diagnostics log dialog.
- Voice, WebRTC data channels, camera/video, screen sharing, steganography tools, and the desktop no-auth browser quick-share HTTP flow are not implemented in the Android client yet.
- For discovery and file receiving, the phone and desktop must be on the same network. Firewalls, VPNs, guest Wi-Fi networks, client-isolated Wi-Fi, and mobile hotspots can block UDP broadcast or inbound TCP connections.

## Requirements

### Minimum requirements

- Android Studio Narwhal or newer, or the command-line Android SDK.
- Android SDK Platform 35.
- Android SDK Build Tools compatible with Android Gradle Plugin 8.13.2.
- Android SDK Platform Tools (`adb`).
- JDK 17 or newer for Android Gradle Plugin. JDK 25 is supported by the current Android toolchain used by this module.
- Internet access for the first build so Gradle, Android, Kotlin, and Compose dependencies can be downloaded.

The module currently uses Android Gradle Plugin 8.13.2, Kotlin 2.2.21, Compose compiler through the Kotlin Compose plugin, and Compose BOM 2024.12.01.

### When building the whole monorepo

JDK 25 is recommended for full repository builds because the desktop/core modules compile with Java 25. Android-only tasks also work on JDK 17+ when run with the Android Gradle Plugin-supported toolchain.

## Android SDK setup

Gradle must be able to find the Android SDK. Android Studio usually creates `local.properties` automatically.

Root project file:

```properties
sdk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
```

Typical Windows path:

```properties
sdk.dir=C\:\\Users\\admin\\AppData\\Local\\Android\\Sdk
```

macOS/Linux example:

```properties
sdk.dir=/Users/<user>/Library/Android/sdk
```

Do not commit `local.properties`; it is a local machine-specific configuration file.

## Verify the environment

Windows PowerShell:

```powershell
java --version
.\gradlew.bat --version
adb version
adb devices
```

macOS/Linux:

```bash
java --version
./gradlew --version
adb version
adb devices
```

`adb devices` should show a connected physical phone or a running emulator.

## Build the APK

The Android application id is `com.shterneregen.securelan.androidclient`. The APK filename is configured as `secure-lan-<version>.apk`; the current project version is `0.4.0`, so the expected filename is `secure-lan-0.4.0.apk`.

Debug APK on Windows:

```powershell
.\gradlew.bat :apps:android-client:assembleDebug
```

Debug APK on macOS/Linux:

```bash
./gradlew :apps:android-client:assembleDebug
```

Expected APK path:

```text
apps/android-client/build/outputs/apk/debug/secure-lan-0.4.0.apk
```

Release APK:

```powershell
.\gradlew.bat :apps:android-client:assembleRelease
```

Output path:

```text
apps/android-client/build/outputs/apk/release/secure-lan-0.4.0.apk
```

By default, release APKs fall back to the local Android debug key when no release signing configuration is provided. This keeps local release builds installable, but it is not a real production signature. For a real release, provide a private signing key through Gradle properties or environment variables.

Create a release keystore once:

```powershell
keytool -genkeypair -v -keystore secure-lan-release.jks -alias secure-lan -keyalg RSA -keysize 4096 -validity 10000
```

Keep this `.jks` file and its passwords private, do not commit them, and keep a backup. Android app updates must be signed with the same key as the already installed release app.

Release signing properties:

```properties
android.release.storeFile=C:/path/to/secure-lan-release.jks
android.release.storePassword=<store-password>
android.release.keyAlias=secure-lan
android.release.keyPassword=<key-password>
```

`android.release.storePassword` opens the keystore container. `android.release.keyPassword` unlocks the private key selected by `android.release.keyAlias`. They may be the same password for a simple one-key keystore, or different passwords if the keystore and individual key are protected separately.

Equivalent environment variables are `ANDROID_RELEASE_STORE_FILE`, `ANDROID_RELEASE_STORE_PASSWORD`, `ANDROID_RELEASE_KEY_ALIAS`, and `ANDROID_RELEASE_KEY_PASSWORD`.

Windows one-session example:

```powershell
$env:ANDROID_RELEASE_STORE_FILE="D:\Projects\My\keys\secure-lan-release.jks"
$env:ANDROID_RELEASE_STORE_PASSWORD="<store-password>"
$env:ANDROID_RELEASE_KEY_ALIAS="secure-lan"
$env:ANDROID_RELEASE_KEY_PASSWORD="<key-password>"
.\gradlew.bat :apps:android-client:assembleRelease
```

Verify an APK signature before copying it to a phone:

```powershell
$buildTools = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1
& "$($buildTools.FullName)\apksigner.bat" verify --verbose apps/android-client/build/outputs/apk/release/secure-lan-0.4.0.apk
```

A valid local result can look like this:

```text
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
Verified using v4 scheme (APK Signature Scheme v4): false
Number of signers: 1
```

For this app, `v2: true` is sufficient because the module has `minSdk 26`. `v1: false`, `v3: false`, `v4: false`, and `SourceStamp: false` are normal for a locally distributed APK unless those schemes are intentionally enabled. Java warnings from newer JDKs while running `apksigner` are also not a signing failure if the output contains `Verifies`.

To confirm that the APK was signed with the intended release certificate rather than the debug key, print the APK certificate and compare its SHA-256 digest with the keystore entry:

```powershell
& "$($buildTools.FullName)\apksigner.bat" verify --print-certs apps/android-client/build/outputs/apk/release/secure-lan-0.4.0.apk
keytool -list -v -keystore D:\Projects\My\keys\secure-lan-release.jks -alias secure-lan
```

For Google Play distribution, prefer an Android App Bundle:

```powershell
.\gradlew.bat :apps:android-client:bundleRelease
```

Expected bundle output directory:

```text
apps/android-client/build/outputs/bundle/release/
```

## Install on a device

Connect an Android device over USB or start an emulator, then check the device list:

```powershell
adb devices
```

Install the debug build through Gradle:

```powershell
.\gradlew.bat :apps:android-client:installDebug
```

Or install the generated APK directly:

```powershell
adb install -r apps/android-client/build/outputs/apk/debug/secure-lan-0.4.0.apk
```

Install the release APK directly:

```powershell
adb install -r apps/android-client/build/outputs/apk/release/secure-lan-0.4.0.apk
```

If a debug build is already installed and the release APK is signed with a different key, Android may reject the update. Remove the old package and install again:

```powershell
adb uninstall com.shterneregen.securelan.androidclient
adb install apps/android-client/build/outputs/apk/release/secure-lan-0.4.0.apk
```

If multiple devices are connected, specify the device serial:

```powershell
adb -s <device-serial> install -r apps/android-client/build/outputs/apk/debug/secure-lan-0.4.0.apk
```

## Run from Android Studio

1. Open the repository root in Android Studio: `secure-lan-suite`.
2. Wait for Gradle Sync to finish.
3. Select the run configuration for the `apps.android-client` / `android-client` module.
4. Select an emulator or a physical device.
5. Click Run.

If Android Studio cannot find the SDK, check `local.properties` in the repository root.

## Desktop client interoperability scenario

1. Start the desktop client on a computer in the same LAN:

```powershell
.\gradlew.bat :apps:desktop-client:run
```

2. In the desktop client, enter a nickname and a shared room password.
3. Click **Open room**.
4. Keep **Discoverable** enabled.
5. On Android, grant the `NEARBY_WIFI_DEVICES` permission if the system prompts for it.
6. In the Android client, enter the same session password.
7. Wait for the desktop peer to appear in the **Peers** list.
8. Select the peer and click **Connect**.
9. After the connection is established, send a test message.

The Android app acts as a chat client. It does not host a desktop-compatible chat room by itself yet.

## File transfer

### Android -> desktop

1. Connect to a desktop peer.
2. Click **Pick file**.
3. Select a file through the Android document picker.
4. Click **Send file**.
5. The desktop client must be ready to receive files through its encrypted file-transfer listener.

### Desktop -> Android

1. Enter the session password on Android.
2. Click **Receive files**.
3. Android starts listening on the incoming file-transfer port.
4. Send a file from the desktop client to the Android peer.
5. On Android 10+, received files are saved through `MediaStore` into public Downloads:

```text
Downloads/SecureLan/<file-name>
```

On Android 9 and older, the app uses public external storage under the Downloads directory and declares `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion=28`. The actual saved path is also shown in the Android UI after the receive operation completes.

## Ports

Default ports:

- chat TCP: `5050`
- encrypted file transfer TCP: `5051`
- UDP discovery: `5052`

When connected, the Android file receiver may use the remote file-transfer port + `1000`, usually `6051`, to avoid conflicts with the desktop receiver on `5051`.

## Android permissions

The manifest declares:

- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`
- `CHANGE_WIFI_MULTICAST_STATE`
- `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion=28`
- `NEARBY_WIFI_DEVICES`

On Android 13+, the app requests `NEARBY_WIFI_DEVICES` at startup. Without network permissions, discovery and LAN connections may not work.

## Tests

Android module unit tests:

```powershell
.\gradlew.bat :apps:android-client:testDebugUnitTest
```

Instrumented tests on a connected device or emulator:

```powershell
.\gradlew.bat :apps:android-client:connectedDebugAndroidTest
```

## Clean build artifacts

Android module only:

```powershell
.\gradlew.bat :apps:android-client:clean
```

Entire repository:

```powershell
.\gradlew.bat clean
```

## Troubleshooting

### SDK location not found

Create or fix the root `local.properties` file:

```properties
sdk.dir=C\:\\Users\\admin\\AppData\\Local\\Android\\Sdk
```

### Desktop peer does not appear in the list

- Make sure Android and desktop are on the same LAN.
- Disable VPN on both sides.
- Check that the desktop room is open and **Discoverable** is enabled.
- Allow inbound UDP `5052` and TCP `5050`/`5051` in the desktop firewall.
- Check that guest Wi-Fi or client isolation is not enabled on the router.

### Connect failed

- Check the shared room password.
- Check that the desktop room is still running.
- Make sure the firewall allows inbound TCP `5050`.
- Try restarting discovery and the desktop room.

### File send failed

- Make sure a peer and a file are selected.
- Check that both sides use the same session password.
- Check that the receiving side is listening on the file-transfer port.
- Allow inbound TCP `5051` on desktop or `6051` on Android when receiving desktop -> Android transfers.

### Release APK installs over debug APK failed

- Android requires the same signing certificate for app updates.
- If the installed APK was signed with the debug key and the release APK is signed with the release JKS, uninstall first:

```powershell
adb uninstall com.shterneregen.securelan.androidclient
adb install apps/android-client/build/outputs/apk/release/secure-lan-0.4.0.apk
```

### `apksigner` shows `v1: false`, `v2: true`, `v3: false`, `v4: false`

This is acceptable for the current app. The important part is that the command prints `Verifies` and `v2: true`. The module targets Android API 35 and has `minSdk 26`, so APK Signature Scheme v2 is enough for supported devices.
