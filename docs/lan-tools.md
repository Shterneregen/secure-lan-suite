# LAN Tools

`modules/lan-tools-core` provides room-independent Wake-on-LAN and diagnostics APIs. It is a
Kotlin/JVM module targeting Java 17, using `common-net` IP/interface values and
coroutines. It has no Compose, Android SDK, chat, file-transfer or crypto dependency.
Desktop exposes saved-device management, Wake-on-LAN and diagnostics in a standalone dialog.
Android integration and device browsing are not implemented. ICMP has no qualified backend.
See the [implementation plan](lantools-implementation-plan.md).

## Desktop workflow

Open **Tools → LAN Tools** in the top bar. Quick Share stays alongside Tools;
Steganography is another item in that menu. LAN Tools works while disconnected
and preserves the existing room, selected peer, chat draft, transfers and calls.

In **Devices**, choose **Add device**, enter a name and optionally an IP hint and
MAC address, then **Save device**. Without a MAC the device can be saved but Wake
up is unavailable. With a MAC you may override the broadcast and UDP port or
store a preferred platform interface key. Broadcast must belong to the source
subnet; leave it empty to use the subnet default. The IP hint is descriptive and
does not choose the WoL destination.

**Details** selects a saved device. Use **Edit device** to change its settings;
**Delete device** asks for confirmation and removes only saved settings. Choose
the current **Network interface** before **Wake up** if several source bindings
exist. Linux default-route recommendations are marked, never selected in place
of a user choice. **Refresh interfaces** reads current local metadata, not nearby
devices. There is no automatic device scan or connectivity check.

An outcome such as “Packet sent” means local submission only. A device may remain
off despite successful submission. MAC/broadcast/port/interface errors are shown
with recovery guidance. **Diagnostics** offers the checks described below.

The dialog resizes into a list/details layout when wide and a single scrollable
column when narrow. Close/Escape cancels network work and preserves the tab,
selection and unfinished device form in memory. Escape first dismisses a pending
delete confirmation. **Cancel editing** explicitly discards a form. On returning
to the tool, the form remains; after a process restart only successfully saved
devices are restored. A save already in progress may finish after the dialog
closes; it is owned by the application, not by the temporary dialog.

### Diagnostics workflow

Opening **Diagnostics** reads local interface metadata; it does not contact
other devices. **Refresh network information** repeats that one-shot read.
Cards show IPv4/prefix, interface state and available OS-reported gateways/DNS.
Unavailable, unsupported, denied and timed-out metadata are distinct from an
available but empty list. No gateway is inferred by replacing an address octet.

For **TCP connectivity**, enter a hostname or numeric IP, one port (1–65535) and
a timeout (1–30000 ms, default 5000). OS routing is the default; optionally choose
a source IPv4 binding. A disappeared selection fails rather than silently using
another source. A hostname check attempts only its first suitable resolved
address, without port/address sweeps. The connection is immediately closed
without sending application data. Connection time excludes name resolution;
success means this TCP port accepted a connection. Refusal, timeout and absence
of a route do not establish whether the device is powered on or offline.

For **DNS lookup**, enter a hostname or IP and choose the returned-address filter
(ALL/IPV4/IPV6). This is system name resolution, potentially using hosts/cache;
it does not guarantee a fresh DNS wire query or prove connectivity. Numeric input
bypasses DNS and is labelled accordingly. TCP source selection does not bind
name resolution to that interface or to the DNS servers shown in its card.

For **Gateway TCP check**, select a source interface, one of its OS-reported
gateways and an explicit TCP port. No port is preselected or scanned. The gateway
and source are checked against fresh metadata before connecting. A gateway may
route traffic while refusing the selected port; interpret the result as TCP only.
**ICMP ping is unavailable**: there is no reliable qualified platform backend,
and neither `InetAddress.isReachable()` nor TCP timing is presented as ICMP.

Checks have independent running/success/failure/cancelled states. **Cancel** or
closing the dialog cancels active work; forms/results remain in memory and late
answers cannot overwrite a later operation. TCP attempts may already have
reached an endpoint when cancelled. Form edits while a check runs do not relabel
its result: the submitted endpoint remains in the outcome.

### Diagnostics architecture and limits

Shared `NetworkDiagnosticsService` exposes `tcp(TcpCheckRequest)` and
`lookup(DnsLookupRequest)`, returning typed `LanResult<TcpCheckReceipt>` /
`LanResult<DnsLookupReceipt>`. `DefaultNetworkDiagnosticsService` takes injected
`HostResolver`, `LanNetworkInfoProvider`, `TcpConnector` and monotonic clock.
It validates before network access, admits four concurrent calls without a
queue, and includes resolution/snapshots/connect in the deadline. At most 32
resolver answers are admitted. Host validation accepts IDN hostnames and strict
numeric IPs; URLs, embedded ports, control characters, malformed addresses and
scoped IPv6 input are rejected. Multicast/unspecified/limited-broadcast TCP
targets are rejected, as is a directed broadcast of a selected source subnet.

`JvmTcpConnector` uses a nonblocking channel on IO, polls completion with
cancellation checks and closes on success, failure or cancellation. It sends no
credentials or application payload. As with WoL, short synchronous OS open/bind
and enumeration calls cannot promise forced interruption of a stuck kernel call.
Transport callers outside the service must supply their own deadline.

Desktop owns the platform adapters and `DesktopDiagnosticsController`, separate
from room orchestration. `DesktopHostResolver` wraps the JVM system resolver
with two daemon workers and no waiting queue. Timeout/cancellation promptly
releases the coroutine caller, but a native lookup may continue occupying its
worker until the OS returns. Saturation fails with `RESOURCE_LIMIT`; repeated
cancellation cannot create replacement threads. App shutdown cancels pending
callers and shuts down the executor; it cannot guarantee termination of native
DNS code. There is no process-wide DNS/routing change. See the
[JVM resolver/cache contract](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/InetAddress.html).

`DesktopNetworkInfoProvider` enriches the existing JVM snapshot with separate
diagnostic metadata, so route/DNS changes do not alter WoL snapshot equality.
On Windows, `WindowsNetworkInfoBackend` runs a fixed, local-only
[`Get-NetIPConfiguration -All`](https://learn.microsoft.com/en-us/powershell/module/nettcpip/get-netipconfiguration?view=windowsserver2025-ps)
query through the system PowerShell executable. No user input is interpolated.
The private numeric output format avoids localized display-table parsing and
has a 128 KiB / 512-row bound, strict decoding and a five-second deadline.
Cancellation/timeout destroys the child and closes pipes; cleanup can add up to
one second of bounded waiting. The UI bounds a complete local refresh to ten
seconds and includes a gateway metadata refresh in the submitted check timeout.
Unavailable cmdlets, permission failures and malformed output produce explicit
unavailable status; no fallback guesses are made.

| Capability | Windows Desktop | Linux/macOS Desktop | Android |
| --- | --- | --- | --- |
| Local IPv4/prefix/interface state | JVM snapshot | JVM snapshot | Integration pending |
| Gateway / configured DNS metadata | Local PowerShell backend | Unsupported backend | Integration pending |
| TCP / system lookup | Implemented | Shared JVM implementation; runtime unqualified | Integration pending |
| Gateway TCP check | Available with reported metadata | Unavailable | Integration pending |
| ICMP | Unavailable | Unavailable | Integration pending |

Interface inventory/source selection is IPv4-oriented. Numeric global IPv6 TCP
can use OS routing; link-local IPv6 needs a scope and is unsupported for TCP.
Configured scoped IPv6 DNS addresses can be displayed, but scoped resolver
answers cannot be represented as endpoint inputs by the current common IP
value. Full IPv6 inventory/scoped endpoint support is not claimed. Gateway/DNS
facts are a snapshot, not a guarantee of the route/resolver a later OS call uses.

### Device settings file and recovery

Devices are stored separately in portable `config/lan-tools-devices.properties`,
next to the existing application settings. There is no change to room settings
or passwords. The versioned store admits at most 256 devices and 1 MiB of data.
Writes use a temporary sibling file and atomic replacement when supported.

Read failures disable changes until **Reload saved devices** succeeds. A damaged
or unsupported schema is read-only; valid rows may still be displayed. Back up
and repair the file, then reload. Failed Save/Delete retains the original list,
form or confirmation so the action can be retried. An external change detected
before writing requires reloading. Concurrent external writers are unsupported;
the fingerprint check is not a cross-process file lock.

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
platform SDKs. The Desktop composition root injects this callback into the shared
JVM provider used for interface selection and WoL revalidation.

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

## Wake-on-LAN deadlines, cancellation and results

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

For the complete core and Desktop checks (including compilation and Kotlin lint):

```powershell
.\gradlew.bat :modules:lan-tools-core:check :apps:desktop-client:check
.\gradlew.bat :apps:desktop-client:lanToolsWindowSmoke
```

Diagnostics tests use a controlled loopback TCP server, injected resolver and
transport failures, virtual-time deadlines and fake process streams. They verify
no payload, channel release, source/topology validation, DNS filtering, resource
caps, cancellation and shutdown, including a native-like lookup that ignores
interruption. No test depends on external DNS servers or real gateway ports.
Compose tests cover validation, real shared service orchestration with fake
ports, cancellation, unsupported metadata and form restoration in light/dark
layouts. The explicit native-window probe additionally reads actual local
Windows/JVM metadata and checks Escape/workspace preservation with room-service
doubles; it does not perform remote checks. Screenshots are under
`apps/desktop-client/build/reports/lan-tools-ui/`.

Real gateway/firewall behavior, Wi-Fi loss, multi-platform runtime, screen-reader
and OS scaling behavior, physical WoL and simultaneous real chat/file/call traffic
still need manual qualification. Local/fake checks do not qualify those scenarios.
