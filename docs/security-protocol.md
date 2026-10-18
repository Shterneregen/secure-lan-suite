# Security model and V2 LAN protocols

The implementation uses the same chat/file handshake and record code on Desktop
and Android. V2 is intentionally incompatible with the former RSA key-transport
protocol. Update both endpoints; neither client retries with V1 after a failure.
Quick Share remains a separate, unauthenticated HTTP feature for trusted LANs.

## Trust and identity

The room password is a shared membership credential. SRP mutually authenticates
knowledge of that credential without transmitting the password or a recoverable
password/key envelope. A person who already knows the password can host a room
with that credential; it is not a certificate identifying a unique room owner.
Discovery and IP addresses are availability hints, not authentication.

DNS-SD TXT contains no password/key/token. Generated `securelan://join` QR links
include the room password in percent-encoded plaintext, so treat them as shared
membership credentials and share only with trusted recipients. Their addresses
and identity hints remain untrusted. Explicit Join uses the unchanged V2
handshake and file receivers still require authenticated presence and signatures;
links have no enforced expiry or revocation, and their UUIDs do not prove the
room owner's identity. See
[discovery and invitation boundaries](room-discovery-and-invites.md).

The room host is trusted: it decrypts chat and routes readable SDP/ICE. Chat and
signaling are not end-to-end encrypted between room participants. Nicknames are
unique within a live room, not permanent identities across reconnects.

Each client instance creates an in-memory P-256 signing identity. Its public key
is included in capabilities, authenticated by the chat handshake, and announced
by the host in USER_JOINED frames. File receivers resolve keys exclusively from
that authenticated presence. Discovery records and address/name equality cannot
substitute for these keys. Disconnect and USER_LEFT remove the authorization.
Keys are not persisted, and applications must not log passwords or raw payloads.
File authorization also captures the current room generation. A prior listener
cannot reuse keys from a later connection, and local server-side presence is not
treated as membership in the client's joined room. Stop a hosted room before
joining another room; the Desktop application owns one active room context.

## Password-authenticated key exchange

`crypto-core` uses Bouncy Castle's SRP-6a implementation with the fixed RFC 5054
3072-bit group, SHA-256, fresh 32-byte salt and fresh ephemeral values per
connection. Neither peer supplies arbitrary group parameters. Public values and
proofs are bounded before BigInteger parsing. Both evidence messages must verify
before the application enables the record channel.

Chat starts with HELLO containing `SECURE_V2`, nickname and encoded capabilities.
Its SRP identity binds the protocol token, nickname and exact capabilities text.
SERVER_KEY carries `base64(salt):hex(B)`, CLIENT_KEY carries `hex(A):hex(M1)`, and
the next SERVER_KEY carries `hex(M2)`. These enum names remain wire envelope
names; V2 does not send RSA keys or encrypted room passwords. ACCEPTED/REJECTED
then use authenticated records.

Files start with the modified-UTF banner `SECURE_FILE_TRANSFER_V2`. SRP challenge,
response and server proof use bounded modified-UTF fields, with that banner as
the domain context. Old banners and old chat HELLO versions fail explicitly.

The handshake key is expanded using HMAC-SHA256 extract/expand, bound to the
protocol/identity context, salt and both ephemeral public values. This yields
separate client/server traffic keys, nonce prefixes and a channel-binding value.
The exchange is an application protocol, not TLS or a claim of certification.

## Authenticated records

Each record contains a one-byte version (2), an eight-byte big-endian sequence
number, ciphertext and a 16-byte GCM tag. AES-256-GCM uses a four-byte derived
direction prefix plus the sequence number as its 12-byte nonce. The handshake
context and complete header are AAD. Client/server directions use different keys.

The reader requires the exact next sequence number and increments only after tag
verification. Duplicate, reordered, reflected, tampered and cross-session records
fail closed; an authentication/order failure permanently fails that reader.
Secure mode is enabled once and cannot be reset to plaintext. Plaintext payloads
are capped at 64 KiB; encrypted records add 25 bytes. At 2^32 records the channel
must be replaced with a fresh handshake rather than reuse a nonce or key.

## Files

After PAKE, the sender transmits metadata and its signature over that metadata,
the channel binding, protocol and sender role. The receiver verifies the current
room-certified sender key and its own exact recipient ID, then returns its own
receiver-role signature. The sender verifies the addressed receiver's certified
key before sending a final PEER_VERIFIED confirmation. Only then does the receiver
consult its acceptance policy. A room-password holder cannot substitute a different
participant's signing identity in this flow.

All data chunks and the empty EOF record participate in the ordered record stream.
Size is exact: zero means an empty file, with no legacy unknown-size bypass.
Publication requires that the complete received length equal the authenticated
metadata. DONE is sent after publication; losing that confirmation can still
produce an unknown outcome and retry duplicates. Exactly-once delivery is not
claimed. Filename sanitization and no-overwrite publication remain in place.

Default receiver bounds are 1 GiB per file, 4 GiB and 256 completed/reserved files
per receiver lifetime, and a 128 MiB free-space floor. Failed partial transfers
release their lifetime charge; completed files keep it. Core also limits concurrent
reserved bytes to twice its configured per-file limit. Core configuration can
set both per-file and total byte budgets explicitly. Restarting with a new
receiver configuration starts a new lifetime budget; previous saved files remain.
Android permits one authorized file stream, with four bounded handshake workers.

Desktop retains its ASK/auto-accept policy; Android auto-receive still depends on
its setting. Both policies now operate after mutual peer proof. Leaving a room
stops the remote receiver and invalidates further peer authorization.

## Availability bounds

| Resource | Bound |
| --- | --- |
| Default TCP handler pool / live room connections | 32, no waiting connection queue |
| File handler pool | 4 |
| Plain chat handshake line / file handshake field | 4 KiB |
| Chat wire line | 90,000 characters, before Base64 decoding |
| File/record plaintext | 64 KiB |
| Cryptographic handshake | 10-second absolute deadline |
| Partial line / chat write | 10-second absolute deadline |
| File body read/write | 60-second absolute per-operation deadline |
| File approval wait | Up to 5 minutes, also bounded by caller watchdog |
| Per-peer chat delivery queue | 32 messages, separate writer per recipient |
| In-memory room/Android message history | 1,000 entries |
| Desktop RTC operation queue | 128 operations, one worker |
| Pending/session ICE candidates | 256 |
| Quick Share active worker pool | 8 |
| Quick Share retained publications | 100 |
| Android HTTP headers | 4 KiB per line, 16 KiB total, 32 lines, 10-second deadline |

Slow chat recipients cannot synchronously block the broadcast loop: their delivery
queues and writes are bounded, and overflow/write failure closes the offending
connection. TCP shutdown closes accepted sockets, including blocked readers.
These bounds reduce process resource exhaustion; they cannot prevent network-level
DoS or make an untrusted LAN available under every hostile traffic pattern.

Desktop Quick Share sets finite JDK HTTP-provider connection/header/request/response
limits before provider startup, including 64 connections. Those provider settings
are process-wide and initialized by the JDK. The owning runtime is JDK 25; duration
values follow its implementation's seconds conversion. A host embedding this service
after starting another JDK HTTP server must configure provider limits at JVM startup.

## Idle chat health and local shutdown

Updated clients append the optional eighth compact-capabilities token `health:v1`
to HELLO. The full HELLO remains bound into the existing SRP transcript. An updated
server confirms health support with the sender `server-health-v1` on the encrypted
ACCEPTED frame; the payload remains the assigned nickname. Older V2 decoders ignore
the extra capabilities token. Without this agreement neither side sends health frames.

Negotiated sessions exchange encrypted PING/PONG frames every 10 seconds. They are
consumed by `ChatSocketSession`, never broadcast as chat. Any valid received frame
renews a monotonic 30-second liveness budget, checked every 10 seconds. Loss of both
data and FIN/RST therefore closes an idle session within about 30?40 seconds, subject
to OS scheduling. Both clients release membership/file/call ownership and permit a
manual reconnect. There is no automatic reconnect or durable message retry. Older V2
peers retain the previous idle behavior; update both ends for bounded detection.

Chat write deadlines include waiting for the serialization lock. Local desktop
Disconnect closes the socket immediately without waiting to write DISCONNECT.
RTC notifications use a bounded background outbox; native cleanup does not wait
for that outbox. Android signaling cancellation closes the specific chat session
when a blocking encrypted write must be aborted: a partially written frame cannot
be safely skipped while continuing the same encrypted byte stream.

## Quick Share lifecycle

Anyone who can reach the listener can discover the index and access active shares.
HTTP offers no confidentiality or peer authentication. UUID suffixes prevent a
revoked URL from becoming a later publication with the same display name; they
do not make the publicly listed links an access-control secret.

Expiry, revocation and access-count admission are synchronized. An admitted download
may finish after individual revocation or expiry. Android keeps its cache file alive
until the last admitted stream closes, then deletes it. Stopping the server closes
its listeners/connections and revokes all shares; starting again does not restore
previous publications. Android stages at most 1 GiB per file and checks cache space.
Desktop publishes the chosen source path, so it still does not promise a file snapshot.
Android download headers have a 10-second deadline; payload read/write chunks renew
a 60-second inactivity budget. A progressing large download has no 60-second total
limit. Admission consumes an access even if the connection subsequently fails;
retrying a one-access publication requires its owner to publish a new link.
On construction, Android Quick Share removes orphaned `quick-share-*.tmp` cache
files left by a prior process, while preserving files owned by live server instances.

Listeners bind available interfaces. Public/VPN exposure depends on routing and
firewall; the application does not prove that an interface belongs to a trusted LAN.

## Realtime consent and isolation

An inbound Desktop OFFER creates an INCOMING state only. The user explicitly accepts
or declines it; the dialog names the media that acceptance enables. Dismissal rejects
the call. No native session or media tracks are prepared before acceptance. Incoming
and outgoing setup time out after 30 seconds. A busy call is preserved instead of
being replaced by another signal.

Signals require the matching session ID, local/remote peer, mode, label and allowed
state/type. Unknown, foreign and retired-session signals cannot replace the snapshot.
Pending ICE and native queues are bounded. Stale callbacks and outbound signals are
ignored. Disconnect, host stop, peer departure and explicit hangup close the session;
native microphone/playout leases stop the device when its last session releases it.
Camera sessions also close their capture sources. Unexpected data-channel labels or
additional channels are rejected.

WebRTC media relies on DTLS-SRTP and the trusted signaling identity/fingerprint path.
This does not authenticate a permanent person independently of the room host.
Android retains its explicit call acceptance, permissions and generation checks.

## Verification and limits

JVM tests cover wrong credentials/context, substituted/invalid challenges, record
replay/reorder/reflection/tampering, peer signature substitution, exact file sizes,
quotas, parser bounds, concurrent Quick Share admission/cleanup, Desktop consent
and RTC lifecycle. A loopback test exercises Android chat identity distribution
through a core host followed by V2 file transfer to the Desktop receiver.

Builds and JVM tests do not verify physical Android crypto providers, microphone/
camera permissions, JNI hardware cleanup, screen-reader behavior, or the complete
Desktop/Android device matrix. Those require device acceptance testing.

Protocol references: [Bouncy Castle SRP-6a API](https://downloads.bouncycastle.org/java/docs/bcprov-jdk18on-javadoc/org/bouncycastle/crypto/agreement/srp/SRP6Client.html),
[RFC 5054](https://www.rfc-editor.org/rfc/rfc5054.html),
[JDK HTTP provider limits](https://docs.oracle.com/en/java/javase/25/docs/api/jdk.httpserver/module-summary.html).

## Shared API and signaling versions

`SecureFileTransferHandshake` is in `filetransfer.protocol`. V2 request, receiver
configuration and handshake entry points require `FileTransferPeerAuthentication`;
there is no unauthenticated constructor or unused CryptoServices parameter.
`FileTransferStreams` centralizes exact-size chunk/EOF and publish-before-DONE rules;
applications still own sources, deadlines, storage, quota commit and cancellation.
These source API changes do not alter signed fields or encrypted V2 bytes.

RTC envelopes require explicit `v=1`; absent, malformed and unknown versions are
rejected. Hosts discard invalid signals before routing/serialization and keep text
chat available. Application version metadata comes from the same Gradle
`secureLanVersion` used by packaging; protocol version remains a separate field.
