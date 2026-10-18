# SecureLan Android Client

The Secure LAN Suite Android client is an experimental Android MVP for connecting to desktop clients on the same LAN. The app can discover desktop peers through UDP discovery, connect to a secure desktop chat room, exchange chat messages, send files, and receive incoming files through a desktop-compatible encrypted file-transfer protocol.

## Status and limitations

- The Android client is currently an experimental MVP, not yet a full feature peer of the desktop client.
- The UI is implemented with native Android + Jetpack Compose Material 3 inside `apps/android-client`.
- The UI uses one adaptive Compose layout for phones, tablets, landscape displays, and TV-like screens. Compact widths use labeled Devices, Chat, and Files bottom navigation; wider widths use a labeled navigation rail. At 840 dp the Devices screen becomes two-pane, and at 1200 dp chat/file screens gain a persistent device sidebar.
- TV-like usage is supported as a large-screen layout target without a separate Android TV launcher. Controls keep touch-friendly sizing and predictable keyboard/D-pad traversal, but the app is still distributed as the standard Android client.
- Android reuses shared V2 handshakes, record ciphers, wire models and stream rules. `ChatSession`, `WireMessage` and `FileTransferSession` are aliases to the core implementation. Android adapters own URI access, staging, MediaStore, permissions and lifecycle; its WebRTC engine and HTTP server remain platform-specific.
- Supported flows include desktop-compatible UDP discovery, mutually authenticated V2 chat, ordered encrypted messages/files, Android-to-desktop file sending, and desktop-to-Android file receiving. V1 endpoints must be updated; there is no protocol fallback.
- The Devices screen offers manual nearby-room search and an **Automatic search** checkbox, off by default. It requests `NEARBY_WIFI_DEVICES` on Android 13+.
- The UI uses a device-first connection flow, keeps the active desktop as the file recipient, starts the incoming file receiver automatically after connecting, and exposes transfer history, system/light/dark themes, file notifications, an in-app system/English/Russian language selector, and diagnostics logs.
- Android-to-Android and Android-to-desktop voice/video calls use WebRTC in the LAN, with the existing encrypted TCP connection for signaling. WebRTC data channels, screen sharing and steganography tools are not implemented yet. Browser Quick Share supports temporary file/text links and QR codes.
- For discovery and file receiving, the phone and desktop must be on the same network. Firewalls, VPNs, guest Wi-Fi networks, client-isolated Wi-Fi, and mobile hotspots can block UDP broadcast or inbound TCP connections.

## Requirements

### Voice and video calls

Connect the clients to the same room, then open **Chat → Call** on Android and choose a participant and audio/video mode. The menu uses advertised call capabilities, regardless of platform: audio requires supportsVoice, and video additionally requires supportsVideo. Clients without voice support are excluded. The room can be hosted on Android or desktop. Update the host as well: the updated server delivers RTC signaling only to its recipient and supplies the sender identity from the authenticated session.

Microphone permission is required; camera permission is requested for video. Denying camera access falls back to audio. Incoming video calls can also be answered with audio only. The call screen offers microphone mute, camera on/off, front/back camera switching, speaker/headset routing, a movable local preview and a duration timer. Active video calls enter Picture-in-Picture on supported devices when leaving the app with Home. A foreground notification provides microphone and end-call actions.

Media uses direct WebRTC DTLS-SRTP with host ICE candidates and no external STUN/TURN servers. Signaling uses V2 authenticated chat: **the room server can read SDP/ICE; signaling is not end-to-end encrypted**. Wi-Fi client isolation can prevent calls. Incoming notifications require notification permission and a surviving TCP connection; this is not a push service that wakes a killed app. Desktop-to-Android video calling has been confirmed by a user; the full cross-platform device matrix still needs acceptance testing.

Coordinator, codec and encrypted signaling have JVM tests; current build results belong to CI or change reports. Microphone/camera quality, Bluetooth routing, background/PiP behavior and OEM power management require acceptance testing on physical Android devices.

The [realtime architecture](../../docs/webrtc-architecture.md) and this guide describe the signaling boundary and remaining device verification.

### Minimum requirements

- Android Studio Panda 2 or newer with Android Gradle Plugin 9.1 support, or the
  command-line Android SDK. Check the official [Android Gradle Plugin
  compatibility table](https://developer.android.com/build/releases/about-agp)
  before changing either tool.
- Android SDK Platform 35.
- Android SDK Build Tools compatible with the configured Android Gradle Plugin.
- Android SDK Platform Tools (`adb`).
- JDK 17 or newer to run Gradle; the Android client and its shared JVM modules
  compile and run unit tests with a JDK 17 toolchain. Gradle can provision a
  missing toolchain through Foojay.
- Android 8.0 (API 26) or newer on the device. Android 9 and 10 remain supported.
  The build JDK, `compileSdk 35` and `targetSdk 35` do not raise `minSdk 26`.
- Internet access for the first build so Gradle, Android, Kotlin, and Compose dependencies can be downloaded.

Exact Android Gradle Plugin and Kotlin versions are defined in `settings.gradle`;
the Compose BOM is defined in `apps/android-client/build.gradle`. The current
Android Gradle Plugin is 9.1.1.

### When building the whole monorepo

JDK 25 is recommended for full repository builds because the desktop and
desktop-only modules target Java 25. `common-model`, `common-net`, `crypto-core`,
`chat-core` and `file-transfer-core` target Java 17 for Android reuse.
Android-only tasks can run Gradle on JDK 17 without requiring JDK 25.
The Android application uses AGP's built-in Kotlin support.
Shared SHA-256 hex formatting uses platform-independent lowercase encoding
instead of `java.util.HexFormat`, which requires Android API 34. Changing the
build JDK does not make newer Java APIs available on older Android devices.

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

The Android application id is `com.shterneregen.securelan.androidclient`. APK
filenames use `secure-lan-<version>.apk`, where `<version>` comes from
`secureLanVersion` in the root `gradle.properties`.

The assemble tasks export versioned APKs under `build/distributions/apk/` using
the Android Components artifact API. AGP's original APKs and installation
metadata remain under `build/outputs/apk/`; `installDebug` uses those originals.

`versionCode` is derived as `major * 1000000 + minor * 1000 + patch` from
`secureLanVersion` (each component must be 0–999). For example, `0.9.11` maps to
`9011`. Increase the application version for each release. If an existing
distribution uses a higher code, override with `-PandroidVersionCode=<integer>`
and keep future codes above it; the override must be 1–2100000000.

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
apps/android-client/build/distributions/apk/debug/secure-lan-<version>.apk
```

Release APK:

```powershell
.\gradlew.bat :apps:android-client:assembleRelease
```

Output path:

```text
apps/android-client/build/distributions/apk/release/secure-lan-<version>.apk
```

Local `assembleRelease` builds use the Android debug key when all release signing
values are absent. A partial signing configuration fails immediately. A local
release variant is not a production candidate. For production, supply all four
signing values and the independently trusted `ANDROID_RELEASE_CERT_SHA256`, then run:

```powershell
.\gradlew.bat :apps:android-client:productionApk --no-configuration-cache
```

This runs the quality gate and verifies the final APK certificate, application ID,
version, non-debuggable flag and 16 KB ZIP alignment. Missing credentials, debug
certificates and unexpected fingerprints fail the production task. Set
`ANDROID_PREVIOUS_VERSION_CODE` to enforce an increase over the previous release.
Production device/upgrade qualification and publication are described in
[the release guide](../../docs/release.md).

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
& "$($buildTools.FullName)\apksigner.bat" verify --verbose apps/android-client/build/distributions/apk/release/secure-lan-<version>.apk
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
& "$($buildTools.FullName)\apksigner.bat" verify --print-certs apps/android-client/build/distributions/apk/release/secure-lan-<version>.apk
keytool -list -v -keystore D:\Projects\My\keys\secure-lan-release.jks -alias secure-lan
```

The supported release channel is direct APK distribution on a trusted LAN.
Google Play readiness is not claimed for `targetSdk 35`. Before selecting Play,
perform a separate compile/target SDK migration against the
[current target API requirements](https://developer.android.com/google/play/requirements/target-sdk),
test behavior changes, validate an AAB and its generated APKs, and confirm the
application's status in Play Console. `bundleRelease` creates a development
bundle; successful assembly alone does not establish Play eligibility:

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
adb install -r apps/android-client/build/distributions/apk/debug/secure-lan-<version>.apk
```

Install the release APK directly:

```powershell
adb install -r apps/android-client/build/distributions/apk/release/secure-lan-<version>.apk
```

If a debug build is already installed and the release APK is signed with a different key, Android may reject the update. Remove the old package and install again:

```powershell
adb uninstall com.shterneregen.securelan.androidclient
adb install apps/android-client/build/distributions/apk/release/secure-lan-<version>.apk
```

If multiple devices are connected, specify the device serial:

```powershell
adb -s <device-serial> install -r apps/android-client/build/distributions/apk/debug/secure-lan-<version>.apk
```

## Run from Android Studio

1. Open the repository root in Android Studio: `secure-lan-suite`.
2. Wait for Gradle Sync to finish.
3. Select the run configuration for the `apps.android-client` / `android-client` module.
4. Select an emulator or a physical device.
5. Click Run.

If Android Studio cannot find the SDK, check `local.properties` in the repository root.

## Desktop client interoperability scenario

Nearby rooms use both UDP and platform NSD. **Search again** starts a manual
search for up to 60 seconds without enabling automatic search. The button stays
visible and is disabled while a search is running; a manual search can be stopped.
**Automatic search** is unchecked by default: launch, returning to Devices,
permission grants, and network recovery do not start search while it is off.
Checking it starts continuous search on foreground Devices; unchecking it stops
search immediately. Leaving Devices or backgrounding the app releases discovery,
and automatic search resumes on return only if the checkbox is checked.
The choice survives Activity recreation in memory but resets on process restart.
Devices separates invitation entry,
nearby discovery, and other connection options, with the current room first
when connected. Searching no longer shows a premature empty-result error.
Hosting advertisements belong to the foreground service and continue
independently of browsing. **Discoverable nearby** controls both sources.

Devices also offers **Scan QR** and **Paste room link**. The offline CameraX/ZXing
scanner requests CAMERA only, with paste as fallback. Imported/deep-linked
`securelan://join` links require review and prefill the password when the link
contains it; passwordless links require entry. The review field filters
whitespace from typing and paste and includes a show/hide password icon. An
external intent never disconnects an active room. An Android host can use
**Invite to room**, copy its link, and have Desktop paste it into Join. Generated
QR codes and links include the room password; share only with trusted people.
See [URI/TXT formats and limitations](../../docs/room-discovery-and-invites.md).

1. Start the desktop client on a computer in the same LAN:

```powershell
.\gradlew.bat :apps:desktop-client:run
```

2. In the desktop client, enter a nickname and a shared room password.
3. Click **Open room**.
4. Keep **Discoverable** enabled.
5. On Android, grant the `NEARBY_WIFI_DEVICES` permission if the system prompts for it.
6. In the Android client, enter the same session password.
7. Wait for the desktop computer to appear on **Devices**.
8. Select it, enter the nickname and password in the connection sheet, and click **Connect**.
9. After the connection is established, the app opens **Chat** automatically; send a test message.

If **Visible to nearby trusted devices** is disabled on desktop, click **Connect by IP** on Android instead. Enter the desktop IP address or host name, chat/file ports, nickname, and room password. The default ports are chat `5050` and files `5051`; the last manually entered address and ports are remembered locally.

The Android app can also host a desktop-compatible encrypted room. On **Devices**, choose **Host a room**, enter a nickname, non-empty room password, and distinct chat/file ports, then choose **Start hosting**. The app joins its own room automatically and advertises it to desktop clients on the LAN.

While hosting, the local room card shows each detected LAN endpoint as
`<phone-ip>:<chat-port>`. Use one of these endpoints for a manual connection
from another device. Wi-Fi, Ethernet, USB, and hotspot addresses are preferred
over VPN addresses; mobile-data and loopback addresses are not presented as LAN
room endpoints. The displayed endpoints update when the phone's network changes.

Hosting runs in a foreground service so the room remains available while the activity is in the background. Use **Stop hosting** in the app or in the persistent hosting notification to close the room and disconnect all participants.

## File transfer

V2 authenticates both file endpoints with their current room-certified signing keys;
knowing the room password or sharing an IP is insufficient to impersonate another
participant. File framing and the handshake are shared with Desktop. Exact zero
means an empty file. The default receiver budget is 1 GiB per file, 4 GiB and 256
files per receiver lifetime, with a 128 MiB free-space floor. Failed partial receives
release their budget; completed files retain it. Restarting the receiver starts a
new budget. The receiver handles four bounded handshakes and one authorized stream.
See [security guarantees and limits](../../docs/security-protocol.md).

### Android -> desktop

1. Connect to a desktop peer.
2. Open **Files** and use the file-selection action. The connected desktop is selected automatically unless a room participant was explicitly selected.
3. Select one or more files through the Android document picker. Review the recipient and list; add more files or remove unwanted entries before sending.
4. Start the batch with the send action showing the selected file count.
5. The desktop client must be ready to receive files through its encrypted file-transfer listener.

Files are sent sequentially, each through the existing encrypted single-file
protocol. There is one unfinished outgoing batch, bound to its original
recipient. Changing the selected device does not redirect queued files. The UI
shows individual outcomes and confirmed file counts; 100% bytes sent is not a
delivery confirmation until the receiver replies with `DONE`.

A failed file pauses the queue. Retry, skip and continue, or finish explicitly;
successful files are not automatically sent again. If final confirmation was
lost, the file may already be saved at the destination, and retry can create a
duplicate. **Stop after current file** waits for the current operation, then
skips remaining files. It is not immediate cancellation of that file. Desktop
with auto-save disabled continues to ask separately for every incoming file.

The picker uses Android's document access grants rather than broad storage
access. A provider can revoke or fail access before a queued item is read. If a
provider cannot report its file size, the app prepares that one file in private
temporary storage before sending; this requires sufficient free space. Files
are streamed, not loaded entirely into memory. Canceling the picker preserves
the current draft, and selecting the same URI again does not add it twice.

Drafts and the queue survive Activity configuration changes, but are kept only
in memory. File execution while the app is in the background is best effort;
the room-hosting foreground service is not a file-transfer service. A killed
process or app restart does not restore or resume the queue. Disconnecting
stops further files, and reconnecting never automatically resumes an old batch.

### Desktop -> Android

1. Enter the session password on Android.
2. Keep **Receive files automatically** enabled in Settings (the default).
3. Android starts listening on the incoming file-transfer port as soon as the secure chat connection is established.
4. Select the Android peer on desktop, choose one or more files through **Attach**, review the list, and send.
5. On Android 10+, received files are saved through `MediaStore` into public Downloads:

```text
Downloads/SecureLan/<file-name>
```

On Android 9 and older, the app uses public external storage under the Downloads directory and declares `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion=28`. The actual saved path is also shown in the Android UI after the receive operation completes.

Each incoming file has its own result. The receiver cannot know a sender-side
batch's total size or count, and does not claim that an entire batch arrived.
Incomplete new receives are cleaned up rather than published as successful
files; previous client versions may still leave partial files after an error.

When Android hosts the room, its receiver listens on the file port configured in the hosting sheet (normally `5051`). Select a connected room participant on **Devices** before sending a file from the Android host.

## Browser Quick Share

Open **Quick Share** to publish text or a selected document with an expiration
and optional access limit, then copy its link or show the QR code. Links use
**unauthenticated, unencrypted local HTTP**: anyone on the LAN who knows or
discovers a link can access it. Use this only on trusted networks. The index
page lists current shares, so link names are not access-control secrets.

Publication IDs include a fresh UUID so revoked links are not reused. HTTP request
headers and client workers are bounded. Cache files remain alive while an already
admitted download is using them; expiry or individual revocation rejects new
accesses, while an admitted stream may finish. Closing the server stops active
clients and revokes all publications. Document staging is limited to 1 GiB and
checks free cache space.

The server prefers TCP port `5053`, tries `5054` through `5063` when occupied,
then uses an available system-assigned port. It stages documents in private
cache and streams downloads without loading whole files into memory. A failed
creation removes its temporary file and reports an error in the workspace.
Content requests count against the access limit; file landing pages do not.
Shares are kept only in memory and end when the owning ViewModel is cleared
or the process exits. Activity configuration changes retain them; background
availability is best effort, without a dedicated Quick Share foreground service.

## Ports

Default ports:

- chat TCP: `5050`
- encrypted file transfer TCP: `5051`
- UDP discovery: `5052`

When connected, the Android file receiver may use the remote file-transfer port + `1000`, usually `6051`, to avoid conflicts with the desktop receiver on `5051`.

## Android permissions

The manifest declarations and runtime requests serve different purposes:

| Permissions | Purpose and runtime behavior |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | LAN sockets and network status; normal permissions, no runtime prompt. |
| `CHANGE_WIFI_MULTICAST_STATE`, `CHANGE_NETWORK_STATE`, `CHANGE_WIFI_STATE` | Discovery/network operations and connected-device foreground-service prerequisites; normal permissions. |
| `NEARBY_WIFI_DEVICES` (`neverForLocation`) | Android 13+ nearby-device permission. The UI tracks grant/denial and offers a request action; declaration does not grant access. |
| `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=28`) | Legacy Downloads writes on Android 8/9. On API 29+ the receiver publishes through MediaStore without this permission. |
| `CAMERA`, `RECORD_AUDIO` | Requested when starting/accepting the relevant call. Denial must leave chat usable; grant in system settings and retry. Camera/microphone hardware is optional. |
| `MODIFY_AUDIO_SETTINGS` | Call audio routing; normal permission. |
| `BLUETOOTH` (`maxSdkVersion=30`), `BLUETOOTH_CONNECT` | Legacy Bluetooth access and Android 12+ call-route permission. Denial must allow another audio route. |
| `POST_NOTIFICATIONS` | Android 13+ prompt when enabling notifications; denial does not grant notification access or substitute for service lifecycle checks. |
| `WAKE_LOCK`, `VIBRATE` | Call/background behavior and alerts; normal permissions. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Background room hosting, with the `connectedDevice` service type. |
| `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE` | Active-call service types; camera/microphone runtime grants and platform start restrictions still apply. |

For device acceptance, deny and then grant nearby access, camera, microphone,
Bluetooth and notifications; retry the affected action without restarting chat.
On API 26/28 verify legacy storage denial/recovery; on API 29+ verify pending
MediaStore cleanup and file publication. Recreate the Activity while hosting,
stop hosting and check that its port and notification are released. Exercise
background call start/stop and PiP separately. See the
[release qualification checklist](../../docs/release.md).

## Tests

Kotlin source hygiene and Android Lint:

```powershell
.\gradlew.bat :apps:android-client:lintKotlin :apps:android-client:lintDebug
.\gradlew.bat :apps:android-client:formatKotlin
```

`check` includes Kotlin lint. The module's `.editorconfig` uses the same focused
source-hygiene rules as desktop, preserving Compose names and existing layouts.
Android Lint reports are written to `build/reports/lint-results-debug.html`.
Do not replace synchronous receive-file recovery journal writes with asynchronous
preferences updates: the journal must survive process death before file IO.

Source documentation uses English **KDoc**, the Kotlin equivalent of Javadoc.
It describes ownership, threading, persistence, protocol compatibility and
recovery contracts. Java's `javadoc` task does not document Kotlin sources.

Android module unit tests:

```powershell
.\gradlew.bat :apps:android-client:testDebugUnitTest
```

The JVM suite includes loopback chat handshake/socket-cleanup tests, discovery
cancellation and port-reuse checks, and HTTP Quick Share tests for concurrency,
access limits, expiration validation and shutdown. These do not replace physical
device checks for permissions, Downloads providers, background services, PiP or
desktop interoperability.

Instrumented tests on a connected device or emulator:

```powershell
.\gradlew.bat :apps:android-client:connectedDebugAndroidTest
```

`PlatformBoundaryTest` exercises pending Downloads cleanup using the real
platform provider, camera permission denial/grant and Activity recreation, and
foreground host recreation/stop/port release. CI runs the suite on API 26 and 35.
The suite uses Android Test Orchestrator with `clearPackageData=true`; use a
dedicated test device/emulator because app data is cleared between cases.
Physical LAN/media, notification prompts, Bluetooth routes and PiP remain manual
qualification checks; a successful JVM suite does not establish those results.

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

### Hosted room has no local network address

- Connect the phone to Wi-Fi/Ethernet or enable its hotspot.
- If a VPN is active, try the physical LAN endpoint shown before the VPN endpoint.
- Wait a few seconds after changing networks; the hosting card refreshes its endpoints automatically.
- Mobile-data addresses are intentionally omitted because other LAN devices normally cannot connect to them directly.

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
adb install apps/android-client/build/distributions/apk/release/secure-lan-<version>.apk
```

### `apksigner` shows `v1: false`, `v2: true`, `v3: false`, `v4: false`

This is acceptable for the current app. The important part is that the command prints `Verifies` and `v2: true`. The module targets Android API 35 and has `minSdk 26`, so APK Signature Scheme v2 is enough for supported devices.

## Connection and transfer recovery

Updated desktop/Android endpoints negotiate encrypted idle health checks. Silent
network loss closes the session after roughly 30?40 seconds; EOF/read failures
release the session immediately so Connect can reuse the same client. Reconnect
is manual. Older V2 peers remain compatible but do not provide bounded idle loss
detection. Canceling a blocked RTC signaling write closes that chat transport to
avoid continuing a partially written encrypted frame.

Nearby-room discovery expires missing announcements after 45 seconds using a
monotonic clock. Explicit discovery stop removes discovered rows; manual entries
and the authenticated active room are preserved. Sending captures a draft revision
and permits one pending text send; its completion clears only that revision.

File receiver callbacks and job completion belong to a receiver generation.
Handshake/listener diagnostics do not fail an unrelated active file; authorized
transfer failures carry metadata for their own history record. Intermediate receive
progress publishes at most every 100 ms, with final byte counts and outcomes retained.

Quick Share removes orphaned staging files when its server is next constructed.
Download headers have a 10-second deadline and payload streaming renews a 60-second
inactivity budget after each successful chunk; there is no 60-second total limit
for progressing downloads. An admitted failed download still consumes one access;
a one-access share must be republished for retry. Links remain temporary,
unauthenticated local HTTP for trusted LANs.
