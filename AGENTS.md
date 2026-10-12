# SecureLanSuite Agent Instructions

These repository rules apply to Kilo, Codex, Antigravity, and other coding
agents. Read only the supporting documents relevant to the task. Keep stable
project constraints here; keep current behavior, commands, and status in the
owning code and documentation.

## Sources of truth

- Current behavior is defined by the implementation and its tests.
- Module topology and dependencies are defined by `settings.gradle` and the
  relevant Gradle build files. Inspect them before changing dependencies.
- Desktop product intent and UX constraints are defined by
  `docs/product-ui.md`.
- Current features, build commands, packaging, and operational guidance are
  defined by `README.md` and `docs/`.
- If these sources disagree, preserve implemented behavior unless the task
  explicitly changes it. Do not silently implement roadmap or aspirational
  behavior.

## Architecture

- Application and platform UI code belongs in `apps/*`; reusable behavior
  belongs in `modules/*`.
- Reusable modules must not depend on application modules or UI frameworks such
  as Compose, JavaFX, or Android UI.
- Application modules may depend on reusable modules. Do not introduce cyclic
  dependencies.
- Keep cryptography out of UI code and keep network, transport, and runtime
  orchestration behind explicit service boundaries.
- Do not introduce Spring or Spring Boot unless the task explicitly requests
  it.
- Prefer explicit interfaces, constructor injection, immutable shared models,
  and deterministic tests for pure logic.

## Compatibility and security

- A UI change must not alter protocols, wire formats, cryptographic behavior,
  or desktop/Android interoperability unless the task explicitly requests it.
- When a compatibility change is requested, update both affected clients,
  focused cross-client tests, and the relevant public documentation.
- Quick Share links are temporary unauthenticated local HTTP links for trusted
  LANs. Preserve this boundary and communicate it wherever users publish links.
- Keep secrets, private keys, passwords, and raw sensitive payloads out of logs
  and user-facing diagnostics.

## Desktop UI

- Compose Multiplatform in `apps/desktop-client` is the primary desktop UI.
- The deprecated JavaFX fallback is isolated in `apps/javafx-client`. Change it
  only when a task explicitly targets fallback behavior or a critical fix.
- Before desktop UI work, read `docs/product-ui.md` and inspect the
  affected implementation and state.
- Use JavaFX only as a behavioral reference. Do not copy its deprecated visual
  layout unless the user explicitly requests visual parity.
- Keep business logic out of composables. Model UI state explicitly and route
  behavior through callbacks, adapters, services, or focused helpers.
- Reuse existing semantic tokens, components, and `SecureLanIcons` before
  introducing new visual primitives.
- Preserve relevant room, peer, conversation, draft, transfer, and call context
  when opening temporary UI such as dialogs, drawers, tools, or settings.
- Keep technical details available where they support configuration, diagnosis,
  or recovery; do not let them dominate normal communication flows.

## Task-specific guidance

- For Android work, read `apps/android-client/android-readme.md` and, when
  interoperability is affected, verify behavior with both clients.
- For realtime work, read `docs/webrtc-architecture.md`.
- For build, packaging, or installation work, read `docs/development.md` and
  the applicable platform guide.
- For user-facing Compose changes, apply `docs/quality/ui-checklist.md`. Apply
  `docs/quality/accessibility-checklist.md` when interaction, focus, semantics,
  color, or motion changes. Apply `docs/quality/responsive-checklist.md` when
  layout, visibility, dialogs, or content sizing changes.
- For UI reviews, use `docs/quality/ui-review-checklist.md` and report concrete
  evidence, limitations, and tradeoffs.
- `.agents/skills/` contains optional task workflows following the Agent Skills
  format. Use a relevant skill when the task matches its description.

## Verification and maintenance

- Verify the smallest relevant behavior with focused tests or a runtime check.
- Update public documentation when a change affects behavior, architecture,
  compatibility, build or packaging instructions, or supported platforms.
- Keep phase numbers, roadmap status, migration stages, and temporary planning
  notes in documentation or task trackers, not in source code, tests, resources,
  metadata, or user-facing strings.
- Update this file when a stable cross-agent rule changes. Do not copy shared
  rules into tool-specific configuration.
