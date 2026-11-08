# LAN Tools

`modules/lan-tools-core` provides room-independent Wake-on-LAN, diagnostics and discovery APIs. It is a
Kotlin/JVM module targeting Java 17, using `common-net` IP/interface values and
coroutines. It has no Compose, Android SDK, chat, file-transfer or crypto dependency.
`modules/neighbor-core` provides an optional desktop MAC suggestion from the
operating system's IPv4 ARP/neighbor table; `lan-tools-core` remains free of
native or JNA dependencies.
Desktop exposes saved devices, Wake-on-LAN, diagnostics and an advertised-device browser in a standalone dialog.
Android exposes the same core operations through platform socket/NSD adapters and
a separate Material 3 screen. ICMP has no qualified backend.
The [Verification](#verification) section describes automated coverage, local
runtime probes and physical scenarios awaiting qualification.

## Desktop workflow

Open **Tools → LAN Tools** in the top bar. Quick Share stays alongside Tools;
Steganography is another item in that menu. LAN Tools works while disconnected
and preserves the existing room, selected peer, chat draft, transfers and calls.

The compact toolbar groups search, **Show**, manual Refresh/Cancel and **Add device**.
An empty inventory offers Add and Refresh; filtered-empty, searching, no-services,
partial and error results remain distinct. Saved and discovered categories stay
separate. The editor has a bounded content width and keeps its existing validation.

**Diagnostics** starts with a network overview. The explicitly selected interface,
or otherwise the first active LAN interface, is shown first; **Other interfaces**
expands every remaining adapter. This display ordering never selects a route.
TCP and DNS forms share a row at wide sizes and stack below 760 dp of pane width.
TCP host and port stay adjacent; all sections use one scroll surface. Source
selection remains in the gateway group and applies to TCP/gateway; timeout and
ICMP/system-routing explanations are available through **Advanced settings**.
Incomplete OS gateway/DNS metadata is labelled Partial with its actual reason and
Refresh recovery. Windows metadata timeouts remain a backend limitation, not a
successful metadata query. Running results retain their submitted target without
a duplicated phase prefix.

In **Devices**, choose **Add device**, enter a name and optionally an IP hint and
MAC address, then **Save device**. Without a MAC the device can be saved but Wake
up is unavailable. With a MAC you may override the broadcast and UDP port or
store a preferred platform interface key. Broadcast must belong to the source
subnet; leave it empty to use the subnet default. The IP hint is descriptive and
does not choose the WoL destination.

The device editor groups **Basics** and **Wake-on-LAN** settings. Fields pair on
wide windows and stack on narrow ones, with one scrolling surface. Compact fields
and inline group headings keep the action row visible at the reference desktop
size (1336x720 dp, density 1); smaller windows scroll intentionally. The optional
network interface field accepts a manually entered key or one copied from the
current interface list; picking a key does not select a route or send a packet.
It is used for MAC lookup and Wake up. Leave it empty to use the only matching
interface; choose one when the IP exists on more than one network. **Cancel
editing** and **Back to devices** discard the unsaved form, as stated in the
editor. Closing LAN Tools preserves the unfinished form for the current session.

The optional MAC field has an explicit lookup action for a valid unicast IPv4 hint
on a current LAN subnet. If more than one interface contains the IP, lookup asks
for an explicit network interface instead of choosing one. It reads the operating
system's ARP/neighbor table and
performs at most one local probe (a UDP datagram or an ARP request); it does not
scan the LAN. The suggestion is
labelled with its source and requires review before saving: it can be stale or
randomized (for example private Wi-Fi addresses). Lookup never persists a MAC,
does not run automatically and never establishes trusted identity. It is
desktop-only; Android keeps manual MAC entry.

Selecting a saved device row opens grouped details with its name, address and
saved state kept visible while the settings scroll. Use **Edit device** to change
its settings; **Delete device** asks for confirmation and removes only saved
settings. Choose
the current **Network interface** before **Wake up** if several source bindings
exist. Linux default-route recommendations are marked, never selected in place
of a user choice. **Refresh interfaces** reads current local metadata, not nearby
devices. There is no automatic device scan or connectivity check.
An explicit selection is retained if its interface disappears. The picker shows
that it is unavailable; sending fails until the user chooses a current interface
or explicitly restores automatic/saved selection.

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

### Device Explorer

Choose **Refresh devices** in Devices to browse supported mDNS/DNS-SD services
for up to thirty seconds. Opening the app/dialog does not start browsing.
**Cancel discovery**, Close and Escape cancel the current run. There is no
automatic restart or background inventory. Discovery can miss silent devices,
sleeping devices, unsupported services and networks that filter multicast;
an empty list does not prove an empty LAN or an offline device.

The fixed browse types are `_http._tcp.local.`, `_https._tcp.local.`,
`_ipp._tcp.local.`, `_ipps._tcp.local.`, `_ssh._tcp.local.` and `_smb._tcp.local.`.
These describe advertised services, not hardware types. **Type: Unknown** is
intentional; even an IPP advertisement does not establish a trusted printer
identity. No SSDP, ARP sweep, subnet scan, port scan or service-type enumeration
is performed. The existing SecureLan UDP room discovery and `_securelan._tcp`
codec/lifecycle are unchanged and are not used to classify arbitrary devices.
See the [DNS-SD naming model](https://www.rfc-editor.org/rfc/rfc6763.html).

**Show: All/Saved/Discovered** filters the list. Search matches names, hostname
and addresses; discovered records also match advertised service types and
network binding. Selecting a row opens grouped details with source, exact
service endpoints, hostname when validated, all accepted IPv4 addresses and a
local observation time. A fixed header keeps the selected device, its status and
**Save as new device** visible while details scroll. Unconfirmed fields stay
Unknown. Observations are untrusted, including any name that resembles a
product, user or room name.

Records from different network bindings never merge. Services on one binding
are grouped only when their advertised SRV target hostname matches and their
address sets overlap (including transitive overlaps). Conflicting disjoint
claims stay separate; missing hostnames do not cause IP-only merging. This is
an ephemeral grouping, not identity validation. Row IDs belong to one browse
session and are never saved as device identities. Another refresh creates a
new session and reconciles observations rather than retaining old rows as
current. Source loss retires that source's observations and ignores its late
callbacks, even if the replacement binding has the same key.
Both browsers recheck topology at completion as well as while listening. Losing
all browsed sources returns a network-change error; losing one Desktop source
retains only the others and marks the result partial.

Goodbyes remove observations immediately when delivered by JmDNS. A thirty-second
monotonic application lease limits records whose removal was missed; this is
a conservative local observation lifetime, not the DNS record's wire TTL.
**Last observed** is callback receipt time, not time of successful connectivity.
Completed, cancelled and failed runs retain only explicitly historical results.
Saved devices survive refresh, expiry and service/network loss.

**Save as new device** copies a name, one address and hostname into an editable
form. Review and Save explicitly to create a new local UUID. The application
does not merge with existing saved records by IP, overwrite their names, infer
a MAC or import TXT data. Add a valid MAC yourself to enable Wake-on-LAN on the
saved device. No discovered device is an authorized room participant.

**Diagnostics** opens a prefilled TCP form; it does not connect immediately.
For a discovered service it uses that exact advertised address/port and source
binding. Saved devices prefill their address/hostname and require an explicit
port. **Check TCP** then performs one check. Last completed results retain the
submitted endpoint and timestamp; changing a form cannot relabel an older
result. Manually changing the target/source clears its row association. At
most 256 row check results are kept in memory, with no persisted online flag.

### Explorer resources and platform limits

Core owns `LanDeviceDiscovery`, observation/snapshot models and the single-owner
`LanDeviceRegistry`; Desktop owns `DesktopLanDeviceDiscovery`, `LanDnsSdBrowser`
and `DesktopExplorerController`. The existing JmDNS library is reused with new
independently owned handles, without changing the room facade or introducing
core dependencies on JmDNS/chat/UI. The
[JmDNS API](https://jmdns.sourceforge.net/apidocs/javax/jmdns/JmDNS.html) binds an
instance to one interface address and supports service-type listeners.

One run admits at most four up, multicast-capable LAN IPv4 interfaces, one
binding per interface, six fixed types, 512 service observations and 256 device
groups. VPN/virtual interfaces are excluded by existing classification
heuristics; these heuristics are not an OS routing guarantee. Additional
bindings and source failures produce **Partial results**. Accepted records have
at most sixteen local-subnet unicast addresses and a 63-byte instance name;
external/broadcast/unsupported addresses are excluded. Hostnames are validated
local names. TXT is limited to 4 KiB at the callback boundary, then discarded
without parsing, logging or displaying it, including malformed binary TXT.

Callback delivery uses a 256-entry queue, drops excess events and marks results
partial. Topology is rechecked every two seconds and updates are processed in
bounded batches. There are no application per-device resolve workers or blocking
`requestServiceInfo` calls: JmDNS's own collector resolves its subscribed types.
JmDNS internally owns its packet cache, resolver tasks and callback executor;
their internal allocation is not capped by these application-level limits.
The implementation therefore bounds subscriptions/handles, accepted data,
event queue and active run duration; it does not claim a hard memory bound on
third-party mDNS internals under hostile LAN traffic.

Library open/close calls run on IO, never Compose's UI thread. Logical browse
duration is at most thirty seconds; cleanup may take additional library timeout
time per handle. Cancellation invalidates UI callbacks immediately, but the
single service permit is released only after cleanup actually finishes. Refresh
during cleanup returns a busy/resource-limit result rather than opening more
handles. Short synchronous OS/library calls cannot promise forced kernel
interruption. A binding change detected by the monitor retires old data; a
network replacement with identical interface/address/prefix is not distinguishable
by this JVM snapshot. No topology cache or room-level identity is used to fill
that gap. Scoped IPv6 browsing is unavailable; Android uses the separate NSD
adapter described below.

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
| Local IPv4/prefix/interface state | JVM snapshot | JVM snapshot | Shared LAN registry / LinkProperties |
| Gateway / configured DNS metadata | Local PowerShell backend | Unsupported backend | LinkProperties when available |
| TCP / system lookup | Implemented | Shared JVM implementation; runtime unqualified | Network-bound sockets / system resolver |
| Gateway TCP check | Available with reported metadata | Unavailable | Available with reported IPv4 gateway |
| ICMP | Unavailable | Unavailable | Unavailable |

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

## Android workflow

Open **Devices → Network utilities → LAN Tools**. The four primary destinations
remain Devices, Chat, Files and Quick Share. This secondary full-screen surface
keeps the room workspace composed and uses its own ViewModel; opening/closing
it never changes membership, selected peer, draft or transfer/call owners.
The existing app language and Material 3 light/dark theme are reused.
Phones use one scrollable column; at 840 dp the list and details are separate.

**Devices** supports the same manual save/edit/confirmed delete, interface
selection, search/filter, explicit copy from discovery and Wake-on-LAN actions.
Settings are private to this installation, separately from room preferences.
MAC must be entered manually; Android hardware identifiers are not read.
An invalid/future/oversized settings payload disables writes until a valid
reload, preserving the original content. Failed saves retain the form/list.
SharedPreferences updates memory before its disk result: a failed commit restores
the last acknowledged payload. If that rollback also fails, Save/Reload requires
a successful recovery commit before the cached value can be accepted as durable.
Disk failure is reported, never converted into a successful save.
There is no sync/import of desktop devices or implicit trusted identity by IP.

**Refresh devices** is the only start of a browse. On API 33+ it requests the
existing Nearby devices permission when missing; after granting, press Refresh
again. Opening the app/tool, a permission grant or network recovery never
starts discovery. Denial has an actionable message; app settings can be used
to restore a denied permission. No manifest permission or SDK level is added.

The Android browser subscribes to the six fixed service types through its own
`NsdFacade`. Secure room browsing still calls the room-only wrapper and codec.
Generic browsing deliberately does not copy TXT data. Each run chooses one LAN:
with multiple networks select an explicit current binding. Capabilities decide
whether network-scoped APIs (API 33/extensions) and service updates (API
34/extensions) can be used. Legacy NSD requires one LAN without a VPN. Its
uncancellable native resolves share one gate across all types/generations in
this tool facade. A stuck resolve may keep the tool busy until Android returns;
repeated refresh cannot create replacement resolver workers.

The Android backend does not extract the SRV hostname from
[NsdServiceInfo](https://developer.android.com/reference/android/net/nsd/NsdServiceInfo).
Android hostname/type stays Unknown. Records without hostname do
not merge solely by IP; several services may appear as separate device rows.
The core registry still supplies lease expiry, loss handling, bounds and stale
snapshots. The app bounds the callback queue to 256, candidate lists to 32 per
type, one run to thirty seconds and stop acknowledgements to two extra seconds.
The facade additionally bounds data callbacks to 512; terminal acknowledgements
are dispatched separately so a data flood cannot drop them. The 32 service-info
registration limit includes retiring callbacks, not just visible candidates.
If an acknowledgement exceeds the cleanup budget, subsequent runs return busy
until both discovery and all service-info registrations acknowledge stopping.
A stop failure/exception is not an acknowledgement. A missing acknowledgement
can leave discovery busy for the rest of this process; restarting the app is the
recovery when Android never completes cleanup. Repeated refresh cannot admit
replacement native resources in that state. The legacy resolve gate is unchanged.
Binding loss clears observations; late callbacks from an old run are ignored.
There is no continuous background browsing or room-discovery coupling.
Permission denial ends the browse promptly and closes its owned handles; it
does not wait for the full thirty-second listening period.

**Diagnostics** reads IPv4/prefix and gateway/DNS facts from `LinkProperties`.
Unavailable facts are shown explicitly; no gateway or MAC is guessed. Broadcast
must be reported by the interface; if unavailable WoL fails with an unsuitable
interface result. Source selections are revalidated for each operation.
[`Network.bindSocket`](https://developer.android.com/reference/android/net/Network)
binds only the current TCP/UDP socket, before connect/send, never the process.
If two current networks have the same source IP, the TCP adapter rejects the
ambiguous source instead of choosing a different Network silently. WoL carries
the exact binding and is not subject to this source-IP ambiguity.

DNS uses [`DnsResolver`](https://developer.android.com/reference/android/net/DnsResolver)
and `CancellationSignal` on API 29+, with four concurrent requests/no queue;
API 26–28 uses `JvmHostResolver`, extracted from the existing Desktop adapter,
with two bounded native workers. The Desktop wrapper preserves its constructor
and behavior. DNS uses the default system network regardless of the TCP source
picker. Numeric input bypasses lookup. TCP/gateway messages describe one port,
WoL messages describe submission, and neither is interpreted as ICMP or power
state. ICMP, IPv6 discovery and scoped IPv6 endpoints remain unavailable.
TCP, gateway and DNS outcomes retain their submitted target/name when the form
is edited, including on failure, timeout and cancellation.

Back/closing/background cancels tool network jobs and releases only its network
subscription, browsers and multicast lease. Cancellation can include up to two
seconds of NSD acknowledgement cleanup; native legacy DNS/NSD can finish later.
Completed/failed/cancelled observations are historical. Forms, selection and
results survive Activity recreation and temporary closure; only committed
devices survive process death. Saves may finish after closing the tool.
See the [Android guide](../apps/android-client/android-readme.md#lan-tools).

## Verification

```powershell
.\gradlew.bat :modules:lan-tools-core:check :modules:neighbor-core:test :modules:common-net:test
.\gradlew.bat :apps:desktop-client:test --tests '*LinuxDefaultRouteHintProviderTest' :apps:desktop-client:lintKotlin
```

Tests cover MAC formats/policy, exact packet bytes, validation before network
access, subnet/interface selection, network changes, typed errors, concurrency,
timeouts and cancellation cleanup. Fakes control failures/backpressure and use
virtual time. One UDP test sends only to loopback and checks source-port release;
tests never broadcast onto the physical LAN. Successful JVM tests do not qualify
Android devices, operating-system broadcast routing or physical wake-up.

MAC lookup tests parse synthetic `/proc/net/arp` and macOS `sysctl` fixtures and
fake the Windows `SendARP` surface; they do not exercise a real neighbor table.
The macOS backend has no runtime qualification, and randomized Wi-Fi MACs or
DHCP address reuse can make any suggestion stale.

Additional tests use local procfs fixtures for default-route metrics, ambiguity,
malformed input, size limits and topology refresh. They do not require Linux or
access the host's actual route table. CIDR tests include non-octet-aligned prefixes
and verify that route hints never bypass explicit source selection.

For the complete core and Desktop checks (including compilation and Kotlin lint):

```powershell
.\gradlew.bat :modules:lan-tools-core:check :apps:desktop-client:check
.\gradlew.bat :apps:desktop-client:lanToolsWindowSmoke
.\gradlew.bat :apps:desktop-client:lanExplorerMdnsSmoke
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

Explorer adds deterministic registry, fake-browser multi-NIC/topology/race/queue/
cleanup tests, binary/oversized TXT boundary tests, separate-room lifecycle tests,
and Compose filtering/details/save/diagnostics-history/cancel tests. The explicit
`lanExplorerMdnsSmoke` additionally uses real JmDNS on one eligible LAN binding:
it publishes local HTTP/SSH fixtures, verifies their resolution/grouping and
goodbye removal, then verifies a separate SecureLan room advertisement still
updates after Explorer closes. It never opens TCP connections or authenticates
a room. This controlled multicast probe is opt-in, separate from `check`, and
is not physical third-party-device or live chat/file/call qualification.

Android instrumentation also starts the actual foreground host, joins it with a
local shared V2 client, exchanges messages while LAN Tools is open and after it
closes, transfers and verifies an encrypted 256 KiB file into Android Downloads,
and fetches a temporary Quick Share text over local HTTP. It verifies that draft,
peer selection, outgoing queue, receiver and room instance remain intact. This
is one-emulator service integration, separate from two-machine Desktop/Android
interoperability or physical call/Wi-Fi qualification. See
[Release and qualification](release.md) for candidate evidence requirements.
