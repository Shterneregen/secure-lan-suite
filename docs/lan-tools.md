# LAN Tools core

`modules/lan-tools-core` provides a room-independent Wake-on-LAN API. It is a
Kotlin/JVM module targeting Java 17, using `common-net` IP/interface values and
coroutines. It has no Compose, Android SDK, chat, file-transfer or crypto dependency.
Neither application exposes LAN Tools in its UI yet. Saved devices currently
have a shared model; persistent stores, diagnostics and device browsing are not
implemented. See the [implementation plan](lantools-implementation-plan.md).

## Wake-on-LAN API

The caller owns its coroutine scope. `DefaultWakeOnLanService` takes a
`LanNetworkInfoProvider`, a `DatagramSender` and an optional `Clock`. The default
sender uses nonblocking JVM UDP; `JvmLanNetworkInfoProvider` offers one-shot IPv4
interface enumeration. A platform may replace both ports without changing the
packet generator or selection policy. Android integration must provide its
network-bound adapter rather than change process-wide routing.

```kotlin
val mac = requireNotNull(MacAddress.parse("02:ab:cd:ef:12:34"))
val service = DefaultWakeOnLanService(JvmLanNetworkInfoProvider())
val result = service.send(
    WakeRequest(
        configuration = WakeConfiguration(mac),
        bindingKey = selectedBindingKey, // from a fresh LanNetworkSnapshot
    ),
)
when (result) {
    is LanResult.Success -> reportPacketSent(result.value)
    is LanResult.Failure -> reportFailure(result.code, result.field)
}
```

The snippet assumes an existing suspend scope, imports from
`com.shterneregen.securelan.lantools.model` and `.wol`, and application callbacks.
Reuse one service per feature owner to share its four-operation limit. No room,
server or authenticated peer is required. Core code does not log MACs, packets,
credentials or raw OS errors.

`MacAddress.parse` accepts six colon/hyphen-separated octets, twelve hex digits,
or dotted `aabb.ccdd.eeff`. It returns null for malformed input (including
whitespace or mixed separators). `MagicPacket.create` rejects zero/multicast/
broadcast MACs, accepts locally administered unicast addresses, and generates
exactly 102 bytes: six `FF` bytes and sixteen repetitions of the target MAC.
Returned byte arrays are independent of the immutable MAC value.

`SavedLanDevice` uses a local UUID and optional address/hostname hints and WoL
configuration. Hints are not verified identities. Its name is trimmed,
nonempty, control-free and at most 128 characters; hostname hints are bounded
to 253 characters without whitespace/control characters. Applications must
apply these constraints when decoding future persistent records.

## Interface and broadcast policy

- Eligible sources are up LAN IPv4 bindings with a valid directed broadcast
  and prefix length 1–30. Multicast capability is not required. Loopback,
  unspecified, multicast, subnet/broadcast source addresses, VPN, virtual and
  cellular bindings are excluded. `/0`, `/31` and `/32` are unsupported.
- One eligible binding can be selected automatically. Multiple source bindings,
  including aliases on one NIC, return `INTERFACE_SELECTION_REQUIRED`.
- `bindingKey` selects a source explicitly; otherwise a saved
  `preferredInterfaceKey` is resolved. Missing/unsuitable preferences fail;
  they never silently select another NIC. An explicit caller choice can
  override a saved preference.
- The default target is the selected subnet's broadcast. An explicit target
  must be that subnet's directed broadcast or `255.255.255.255`. Limited
  broadcast additionally requires an explicit binding/interface preference.
  A device's unicast IP is not accepted as a broadcast override.
- `Ipv4Broadcast.directed(IpPrefix(...))` derives the subnet broadcast using only
  numeric bytes and the prefix. Missing OS broadcast metadata may be derived
  only when the platform independently confirms broadcast capability. Conflicting
  OS metadata is rejected; `/0`, `/31`, `/32` and IPv6 are not derived.
- The UDP port defaults to 9 and may be set to 1–65535. The sender binds the
  selected source address, enables broadcast, sends one datagram and closes.
  A fresh snapshot is checked immediately before sending. A network change
  after that check is still possible; binding/send errors are reported.

The JVM provider uses the existing desktop-style interface-name heuristics for
VPN/virtual classification. These are not a guarantee of physical topology or
OS route selection. It retains no platform handles and does not start discovery.
At most 256 interfaces / 512 IPv4 bindings are admitted; exceeding these limits
returns `RESOURCE_LIMIT`, without silently using a partial inventory.

### Optional default-route recommendation

`LanNetworkSnapshot.defaultRouteInterfaceKey` is informational: it can mark a
recommended interface in a future picker but does not authorize source selection.
Multiple eligible bindings still require an explicit choice; a saved user
preference always takes priority over a platform recommendation.

The JVM provider accepts an application-owned callback, evaluated on each snapshot:

```kotlin
val hint = LinuxDefaultRouteHintProvider()
val networks = JvmLanNetworkInfoProvider(defaultRouteInterfaceKey = hint::interfaceKey)
```

`LinuxDefaultRouteHintProvider` belongs to the Desktop application, under
`desktop.network.discovery`; the core module does not read procfs or import
platform SDKs. The adapter is ready for future Desktop service composition; it
is not wired to the main application before LAN Tools UI integration.

On Linux it reads at most 64 KiB / 512 route rows from `/proc/net/route`, checks
UP/non-reject default routes, and recommends the unique lowest-metric interface.
Equal metrics across different interfaces, malformed/unavailable data, and
Windows/macOS return no hint. No commands, network probes, background work or
process-wide route changes are performed. The core discards hints for absent,
down or unsuitable interfaces; a failed optional read does not invalidate usable
interface information. Successful detections are never cached across snapshots.

This adopts the useful default-route/CIDR ideas from the
[home bot's LAN resolver](https://github.com/Shterneregen/telegram-home-bot/blob/master/src/main/java/random/telegramhomebot/monitor/network/LanNetworkResolver.kt),
without its Spring configuration or permanently cached topology. Kotlin API
documentation uses English KDoc (the Kotlin equivalent of Javadoc), with inline
comments for packet layout, route policy, resource ownership and cancellation.

## Deadlines, cancellation and results

Default operation timeout is 2 seconds; valid caller values are 1–30000 ms. The
deadline covers snapshots, selection and send. A service admits at most four
concurrent calls; excess calls return `RESOURCE_LIMIT` immediately, without
creating a queue. Cancellation propagates as `CancellationException`, including
an enclosing caller timeout; the service's own timeout returns `TIMEOUT`.

The nonblocking sender suspends for 10 ms when the OS reports backpressure,
checks cancellation on each attempt and closes its channel on success, failure,
timeout or cancellation. It does not retry a successfully submitted datagram.
Low-level `DatagramSender.send` callers must supply a deadline; the service does
this automatically. Injected providers/senders must cooperate with cancellation.
Short OS calls to enumerate interfaces, open, configure and bind a channel are
synchronous on the IO dispatcher; the API does not promise to interrupt a stuck
kernel call or forcibly terminate a noncooperative third-party implementation.

Failures distinguish invalid input, unavailable/changed networks, missing or
unsuitable interfaces, required selection, permissions, unsupported operations,
resource limits and IO errors. Field identifiers are stable technical keys for
application localization, not raw exception messages.

`WakeReceipt` confirms only that the OS accepted the packet, with its source,
target, port, byte count and timestamp. It does **not** confirm network delivery,
that the device supports WoL, or that it turned on. BIOS/UEFI, NIC, power state,
router/client isolation, firewall and Wi-Fi behavior require physical testing.

## Verification

```powershell
.\gradlew.bat :modules:lan-tools-core:check :modules:common-net:test
.\gradlew.bat :apps:desktop-client:test --tests '*LinuxDefaultRouteHintProviderTest' :apps:desktop-client:lintKotlin
```

Tests cover MAC formats/policy, exact packet bytes, validation before network
access, subnet/interface selection, network changes, typed errors, concurrency,
timeouts and cancellation cleanup. Fakes control failures/backpressure and use
virtual time. One UDP test sends only to loopback and checks source-port release;
tests never broadcast onto the physical LAN. Successful JVM tests do not qualify
Android devices, operating-system broadcast routing or physical wake-up.

Additional tests use local procfs fixtures for default-route metrics, ambiguity,
malformed input, size limits and topology refresh. They do not require Linux or
access the host's actual route table. CIDR tests include non-octet-aligned prefixes
and verify that route hints never bypass explicit source selection.
