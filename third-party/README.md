# Distribution notices

`generateThirdPartyNotices` records each resolved runtime artifact, version,
classifier, SHA-256 and POM license declarations, copies POMs and extracts embedded
license/notice/copyright files (including `classes.jar` inside Android AARs).
It fails when license metadata cannot be established. Application modules and
build-only tools are not misrepresented as external runtime dependencies.
Gradle explicitly resolves the exact runtime POMs and any parents needed for
inherited license declarations. These files are task inputs and are passed to
the offline generator through a metadata index; notice generation does not scan
or depend on incidental files in Gradle's cache.

Checked-in upstream texts:

- Apache-2.0: <https://www.apache.org/licenses/LICENSE-2.0.txt>.
- webrtc-java 0.14.0 LICENSE and NOTICE:
  <https://github.com/devopvoid/webrtc-java/tree/v0.14.0>.
- Bouncy Castle 1.86 licence:
  <https://github.com/bcgit/bc-java/blob/r1rv86/LICENSE.html>.
- Android WebRTC SDK 150.7871.01 LICENSE and complete upstream WebRTC notice list:
  <https://github.com/webrtc-sdk/android/tree/v150.7871.01/Licenses>.

The Android SDK repository's wrapper license is MIT; its POM declares BSD-3-Clause
for WebRTC. Both upstream texts are retained. The Android native notices must not
be treated as a version-matched inventory of desktop WebRTC's native binary.
Before public desktop distribution, review the desktop native supplier's complete
third-party inventory and Skiko/Skia notices against the exact binaries.
Generated inventory and bundled texts make that review inspectable; they do not
establish completeness of an upstream binary with missing embedded notices.

Vendored Material icons retain the Android Open Source Project Apache-2.0 headers.
JDK legal files remain inside each jpackage runtime image. The release manifest
records the actual JDK vendor/runtime; changing vendor requires another notices
review. Do not remove `runtime/legal` from portable images.
