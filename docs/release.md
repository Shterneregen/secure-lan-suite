# Release and qualification

Local packaging is available without production credentials. Public release
uses the repository workflows and exact artifacts qualified on devices.
The supported Android channel is direct APK distribution. Google Play requires
a separate target-SDK/AAB migration and platform qualification.

## Required checks and trust boundaries

`Quality` runs on PRs, master pushes and as a reusable workflow:

- JVM tests, actual Compose host/join interaction tests, Kotlin hygiene,
  Android lint, debug APK and instrumentation APK assembly.
- Windows, Linux x64 and macOS ARM64 portable assembly/startup checks.
- Android device contracts on API 26 and API 35 emulators.
- Strict dependency checksums, platform lockfiles, resolved Maven inventory
  and scoped OSV advisory checks. Failed tests, unexplained skipped JVM tests,
  missing suites and untriaged/expired advisories fail the checks.
- Reports retained for 14 days even on failure. Fork PR caches are read-only;
  no signing secrets are available to those jobs. Actions use full commit SHAs.

Configure branch protection to require every `Quality` build/device check.
Configure the `production` GitHub environment with protected master-only
deployment and reviewers. These are hosted repository settings; adding workflow
files cannot establish or prove them. No workflow uses `pull_request_target`.

## Toolchain baseline

The workflow pins Temurin 17.0.20.1+1 and 25.0.4.1+1, Android Platform 35 and
Build Tools 36.0.0, and Windows WiX/UI/Util extensions 5.0.2. Wrapper/plugin
versions remain in their owning configuration files. `writeToolchainManifest`
records the actual JDK vendors/runtime versions, OS/architecture, Gradle,
Kotlin, AGP, Compose, SDK selection and WiX/extensions. The signing job sets
`SECURELAN_VERIFY_RELEASE_TOOLCHAIN=true` to reject a different JDK/WiX baseline.
Update pinned tools deliberately after security review and repeat qualification.
The local JDK can differ; a local manifest must not be presented as evidence
that the hosted baseline ran successfully.

## Dependency maintenance

Wrapper distribution SHA-256 is pinned. `gradle/verification-metadata.xml`
verifies downloaded artifact and module/POM content. Locks cover application
compile/runtime, JVM test and Kotlin lint configurations. Desktop/WebRTC locks
are separated by native target; Android AGP internal/device-test graphs are
checksum-verified rather than forcibly locked because AGP chooses tested
variants through its own artifact views.

After reviewing a dependency change, update Windows inventory/locks/checksums:

```powershell
.\gradlew.bat resolveDependencyInventory --write-locks --write-verification-metadata sha256 --no-configuration-cache
.\gradlew.bat qualityGate --write-verification-metadata sha256 --no-configuration-cache
```

Update each other desktop graph without cross-building:

```powershell
.\gradlew.bat resolveDependencyInventory -PsecureLanDependencyTarget=linux-x86_64 --write-locks --write-verification-metadata sha256 --no-configuration-cache
```

Repeat for `linux-aarch64`, `macos-x86_64` and `macos-aarch64`. The target override
is restricted to inventory tasks. Review metadata/lock diffs and independently
check newly trusted artifacts against upstream releases/checksums before accepting
them. Generated bootstrap checksums alone do not establish upstream provenance.
CI never regenerates trust metadata. Run the final checks with
`--dependency-verification=strict` and no metadata-writing option.

`python scripts/audit_dependencies.py` scans resolved inventory files. OSV
network failure blocks the check; it is not interpreted as zero vulnerabilities.
`gradle/advisory-exceptions.json` is empty initially. Any necessary exception must
name its exact coordinate/advisory, owner, reason, reachability and `reviewBy`
date. Runtime, test and build scopes remain separate in the report. Native
binaries, JDK/SDK/WiX and unknown advisories require separate review.

## Build a candidate

Run `Release candidate` on master. It first requires the complete reusable
quality workflow. Only its signing job has production secrets; Gradle caching
and configuration cache are disabled there. The job builds from a fresh checkout,
verifies the final APK, signs or explicitly designates the EXE, and uploads a
30-day immutable `release-candidate` artifact with hashes and toolchain manifest.
It does not publish a release.

Environment secrets:

- `ANDROID_RELEASE_STORE_BASE64`, `ANDROID_RELEASE_STORE_PASSWORD`,
  `ANDROID_RELEASE_KEY_ALIAS`, `ANDROID_RELEASE_KEY_PASSWORD`.
- For Authenticode: `WINDOWS_CERT_BASE64`, `WINDOWS_CERT_PASSWORD`.

Environment variables:

- `ANDROID_RELEASE_CERT_SHA256`: independently trusted certificate fingerprint.
- `ANDROID_PREVIOUS_VERSION_CODE`: previously published versionCode, when one exists.
- `WINDOWS_CERT_SHA256`: independently trusted Authenticode certificate fingerprint.

Production fails with missing/partial signing values, an unexpected or Android
Debug certificate, wrong APK ID/version, a debuggable APK, or failed alignment.
Windows Authenticode requires a valid timestamp. An unsigned pilot requires the
explicit workflow choice and a reason published in release notes. No new upgrade
UUID is introduced without checking previous installer identity.

For local APK candidates, use `:apps:android-client:productionApk` with the same
credentials/fingerprint and `--no-configuration-cache`. `assembleRelease`
without any release values deliberately remains a local debug-signed build.

## Qualify the exact downloaded artifacts

Create a JSON record with the exact `manifest.json` commit and artifact hashes:

```json
{
  "commit": "full candidate commit",
  "artifacts": {"exact-artifact-name.apk": "SHA-256 from candidate manifest"},
  "devices": ["Windows VM, OS/build, no installed Java", "Android model/API and network"],
  "tester": "maintainer",
  "testedAt": "ISO date/time",
  "windowsSigning": "authenticode",
  "checks": {
    "desktopLaunchWithoutJava": "not-tested",
    "hostJoinChat": "not-tested",
    "filesBothDirections": "not-tested",
    "disconnectReconnect": "not-tested",
    "voiceStartStop": "not-tested",
    "androidUpgrade": "not-tested",
    "windowsInstallUpgradeUninstall": "not-tested",
    "thirdPartyNoticesReviewed": "not-tested"
  }
}
```

Copy **all** artifacts/hashes from the manifest, not just the example APK.
Record `pass` only after executing each check. `not-tested`, missing checks,
wrong commit or differing hashes fail promotion. For an unsigned pilot also set
`windowsSigning` to `unsigned-pilot` and provide `unsignedPilotReason`.

Exercise room hosting/joining in both client directions, chat, encrypted files
in both directions (including zero-byte/multiple files and interruption),
disconnect/reconnect and voice start/stop/recovery on the same trusted LAN.
Verify Windows installation, upgrade from the previous production installer,
settings/download preservation and uninstall on a disposable VM. Verify Android
upgrade with the same production certificate without uninstalling the old app.
An existing debug installation is not evidence of production upgrade support.

Additionally check permissions denial/recovery, Downloads providers, background
host/call lifecycle, Bluetooth routing and PiP on the supported Android APIs.
Check Unix extraction/launcher/runtime permissions, GUI/native initialization,
DPI/focus/dialog context and OS integration for every published desktop target.
Keep experimental video and 16 KB native runtime qualification explicit.
An emulator or process-liveness smoke does not substitute for physical LAN/media
or installer lifecycle checks.

Review the generated third-party inventory and exact native/JDK notices before
public distribution; see [notice sources and limitations](../third-party/README.md).

## Publish without rebuilding

Create the version tag pointing to the qualified commit. Run `Publish qualified
release` on that same master commit, passing the candidate run ID and completed
JSON. It verifies successful workflow origin, branch/repository/commit, downloaded
artifact hashes, qualification, signing policy, final APK certificate/identity,
Authenticode status when applicable and the version tag. Only then does it
publish those downloaded bytes with SHA-256 sums, manifest and qualification.
It never replaces them with a new build. Device qualification remains a truthful
maintainer record, not a claim inferred from green unit tests.
