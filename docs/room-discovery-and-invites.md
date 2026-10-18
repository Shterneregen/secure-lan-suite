# Nearby rooms and connection links

Desktop and Android combine the existing UDP broadcast protocol on port 5052
with DNS-SD/mDNS (`_securelan._tcp.local.`, multicast UDP 5353). Only an active
room host advertises. Joined participants do not register room services.
Desktop uses JmDNS 3.6.4; Android uses the platform NSD service.

The room list reconciles observations by source and endpoint. Losing a UDP
observation does not remove an independently live DNS-SD observation. UDP has a
45-second monotonic lease. Modern DNS-SD records are removed by the platform
cache/lost callback rather than an application 45-second timer. Legacy Android
resolution uses a 90-second application lease, refreshed during active search.
This is freshness guidance, not proof that TCP is reachable.

Android searches on Devices while the Activity is in the foreground, for up to
60 seconds per search window. Refresh starts another window. Stop search stays
stopped across network/lifecycle callbacks until Refresh. The foreground host
service owns advertisements independently of the search window.

## Host visibility and invitations

Discoverable announces through both providers. Hidden withdraws mDNS and stops
new UDP announcements while keeping the password-protected TCP room available.
Old discovery caches may retain entries temporarily. Hidden is not access
control, and does not revoke copied connection links.

On Desktop, use **Invite** in the hosted-room header to show/copy the room QR.
On Android Devices, use **Invite to room**. Select a suitable LAN address if
several are available. QR addresses update with the host network; stopped hosts
disable the invitation. Generated QR codes and copied links include the room
password and fill it in on the joining device. Share them only with trusted
recipients: percent encoding does not encrypt the password. Invitations work
with discovery unavailable or the room Hidden.

Desktop supports **Paste room link** in the connection form. Android supports
paste, an offline CameraX 1.5.3/ZXing scanner, and `securelan` VIEW intents.
Scanning requests CAMERA only; it requires no Google Play Services, Internet,
downloadable model, microphone or storage permission. Camera denial/unavailable
hardware leaves paste available. Importing a link opens a connection draft;
it never joins automatically or disconnects an existing room. A new target
does not reuse the previous room's password. Passwordless links leave the draft
password empty. The review password field filters whitespace from typing and
paste; choosing another address preserves this draft.

Quick Share remains a separate temporary unauthenticated local HTTP feature.
Its QR renderer is shared with invitations, but room invitations do not start
an HTTP server, open a browser, or contain Quick Share expiry/access controls.

## URI v1

```text
securelan://join?v=1&proto=2&host=192.168.1.42&port=5050
```

The authority is exactly `join`, with no path, userinfo, authority port or
fragment. Scheme/authority are case-insensitive on input. Query keys are
case-sensitive. Values are strict UTF-8 percent-encoded; `+` stays a plus and
decoding occurs once. Canonical order is `v`, `proto`, `host`, `port`, optional
`filePort`, `pid`, `rid`, `name`, repeated `alt`, then optional `password`.

| Field | Contract |
| --- | --- |
| `v`, `proto` | Required `1` and `2`; unsupported versions fail closed. |
| `host` | Required numeric IPv4/IPv6 literal; no DNS lookup. |
| `port` | Required ASCII decimal 1–65535 without leading zero. |
| `filePort` | Optional distinct valid port; a hint, never file authorization. |
| `pid`, `rid` | Optional canonical lowercase UUID hints; generated host links include both. |
| `name` | Optional NFC display hint, at most 128 UTF-8 bytes/64 code points, no controls/bidi formatting. |
| `alt` | Up to two other numeric IP addresses using the same ports; explicit selection only. |
| `password` | Optional non-empty room credential without whitespace/control characters; preserved exactly after UTF-8 percent decoding. Generated host invitations include it. |
| `x-*` | Up to four small ignored advisory extensions; duplicate keys fail. |

Input is bounded to 4096 ASCII characters, 16 pairs and 2048 decoded bytes;
generated output is at most 1024 bytes. Unknown ordinary fields and duplicate
singleton fields are rejected. Keys, tokens and cryptographic session identifiers
have no fields. Credentials remain in memory; descriptor diagnostics redact the
password and parser errors contain only a code/known field, never raw external
content. Raw invitation URIs must not be logged.

Both updated clients still accept links without `password`. Clients with the
earlier strict URI parser reject the new field; update the joining client to use
password-bearing links. The chat/file protocol and DNS-SD/UDP records are
unchanged; discovery advertisements never include the room password.

Unspecified, loopback, multicast, limited broadcast, ambiguous IPv4 spellings,
IPv4-mapped special addresses, sender IPv6 zones and portable IPv6 link-local
addresses are rejected. Known local directed broadcasts are rejected before
Join. Private IPv4, IPv4 link-local and IPv6 ULA syntax are supported. Addresses
outside detected LAN prefixes require explicit confirmation. No address probes
or fallback password handshakes run before the user confirms one destination.

Without `filePort`, Join uses its valid configured local/default value; this
does not claim that the remote file receiver is ready. mDNS publishes `fp`
only after the actual receiver is ready. Authenticated room presence and V2
file signatures remain the authority for file operations.

## DNS-SD TXT v1

SRV port is the actual chat listener. Connection addresses come from resolved
A/AAAA records plus receiver network context, never a TXT `host` or URL.
Publishers use generic random service/host labels, not OS usernames or nicknames.

| TXT | Contract |
| --- | --- |
| `v=1`, `proto=2` | Required application discovery schema and secure-room protocol. |
| `pid`, `rid` | Required canonical UUIDs; same pid as UDP, new rid per hosting lifetime. |
| `fp` | Optional ready file listener port. |
| `name` | Optional bounded display hint, same text restrictions as the URI. |
| `caps` | Bounded advisory flags; `files` only with a ready listener. |

TXT is at most 512 bytes including length prefixes, 12 entries, 255 bytes per
string and 16 ASCII key characters. Strict UTF-8, ports and versions are
validated. Case-insensitive duplicates are rejected when the raw representation
permits detection; platform maps may already have folded duplicates.

Discovery has bounded room/observation/event/resolve budgets. Partial provider
failure leaves the other source and an already running TCP room usable. Active
authenticated sessions and file targets are independent of discovery metadata.

## Network and support boundaries

The default mDNS/QR preference is physical IPv4 LAN. Desktop polls topology
every five seconds and owns one JmDNS per selected binding. Android tracks
underlying Wi-Fi/Ethernet networks without requiring Internet validation. A
selected route is captured for chat/files; the app never binds the entire
Android process to a network. VPN restrictions remain effective.

The mDNS interface filter does not restrict legacy UDP broadcasts or wildcard
TCP listeners. On old Android without network-scoped NSD, advertising pauses
for VPN/ambiguous multiple-network topology. IPv6 syntax is supported by shared
models/URI parsing; IPv6 mDNS is disabled pending physical qualification.
IPv6-only and scoped IPv6 cross-client operation are not release-qualified.

mDNS/UDP, UUIDs, names and QR addresses are unauthenticated availability hints.
Invitation links also carry a room credential. The unchanged SECURE_V2 SRP
handshake remains mandatory. Someone who knows the
room password can host another room; unsigned QR does not prove a permanent
owner identity. Links have no server-enforced expiry/revocation. IP reuse can
make an old link reach another host. No discovery method bypasses firewall,
subnet/VLAN boundaries, AP isolation or enterprise/VPN policy.

Native Windows/macOS/Linux, Android API/OEM, GMS-free first-install camera,
offline LAN and two-device encrypted chat/file qualification must be recorded
for the exact release artifacts. JVM tests and one-host multicast smoke do not
establish those results. macOS images include local-network privacy/Bonjour
declarations; Finder launch/permission behavior still requires macOS testing.
