# Phase 3: Foundation modules

[Back to Kotlin Migration Plan](kotlin-migration.md#phase-index)

- Migrate [`modules/common-net`](../../modules/common-net/build.gradle) before higher-level networking modules.
- Preserve socket lifecycle behavior, transport exceptions, text channels, frame channels, TCP server utilities, and UDP broadcast address resolution.
- Migrate [`modules/common-model`](../../modules/common-model/build.gradle) carefully because it defines shared DTOs and events used across clients and core modules.
- Decide per model whether to keep Java records temporarily or replace them with Kotlin data classes after checking Java caller compatibility.
- Preserve RTC signaling payload behavior from the common model package.

Status: completed. [`modules/common-net`](../../modules/common-net/build.gradle) and [`modules/common-model`](../../modules/common-model/build.gradle) now use Kotlin JVM. Common model DTOs use Kotlin JVM records where the constructor contract is record-compatible; [`RtcSignalEnvelope`](../../modules/common-model/src/main/kotlin/com/shterneregen/securelan/common/model/rtc/RtcSignalEnvelope.kt) remains a Java-callable Kotlin class to preserve null-normalization and factory behavior.
