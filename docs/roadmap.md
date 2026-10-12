# SecureLanSuite Roadmap

This file tracks open outcomes only. Completed migration work is recorded in
[`migration-history.md`](migration-history.md). Current implementation, tests,
and owning build files take precedence over this plan.

## Release readiness

| Outcome | Completion evidence |
| --- | --- |
| Qualify desktop communication across machines | Repeatable discovery, chat, encrypted file transfer, and voice checks on representative LANs; failures and recovery recorded |
| Qualify the Android interoperability client | Discovery, room join/host, chat, file send/receive, permissions, background hosting, and signed install/update exercised on representative devices |
| Qualify Windows distribution | Portable archive and EXE build, install, launch, upgrade, uninstall, signing, and release notes validated on supported Windows versions |

## Reliability and product hardening

| Outcome | Completion evidence |
| --- | --- |
| Harden LAN discovery | Useful behavior and recovery across firewalls, VPNs, multiple adapters, guest/client-isolated networks, and duplicate nicknames |
| Stabilize desktop video | Camera capture, local preview, negotiation, remote rendering, device changes, cleanup, and failure recovery validated across representative machines; product status updated only after evidence supports it |
| Finish desktop workflow hardening | Keyboard/focus, narrow-window behavior, composer and attachments, peer states, transfers, settings, errors, and recovery pass the project UI checklists and runtime review |
| Define key-management UX | A product decision and implemented workflow for key generation/loading where user control is required |
| Improve Android recovery UX | Permission rationale, discovery edge cases, file-receiver failures, hosting lifecycle, and LAN diagnostics provide actionable recovery |
| Normalize error contracts | Cross-module failures use consistent, testable domain errors instead of ad hoc exception handling |
| Strengthen deterministic coverage | Pure logic and protocol boundaries have focused unit or integration tests for material behavior |

Audio output enumeration, selection, persistence, and speaker testing are
already implemented in desktop Audio & Video settings and are not roadmap work.
Desktop steganography text workflows are also implemented.

## Candidate work

These items require a separate product decision before implementation:

- Android voice, video, or RTC data-channel support;
- chunked large-file transfer over `RTCDataChannel` with backpressure;
- screen sharing through desktop capture;
- video recording or standalone camera tooling;
- Android steganography or browser Quick Share workflows;
- a separate TLS transport for encrypted file transfer;
- revival of standalone non-WebRTC audio transport.

Candidate work should move into an active outcome only when scope, compatibility,
security boundaries, and acceptance evidence are defined.

## Updating this roadmap

- Add outcomes rather than sequential phases or implementation task dumps.
- Include observable completion evidence for every active item.
- Move completed architectural milestones to `migration-history.md`.
- Keep transient statements such as “tests are green” in change reports or CI,
  not in this file.
