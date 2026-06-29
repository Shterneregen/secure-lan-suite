---
name: securelan-desktop-compose
license: MIT
description: >
  SecureLanSuite desktop Compose Multiplatform migration skill. Use when the
  user asks about the desktop Compose shell, desktop UI migration, replacing
  JavaFX screens with Compose, Compose desktop Gradle setup, desktop UI state,
  or JavaFX fallback preservation in apps/desktop-client.
---

# SecureLanSuite Desktop Compose Skill

Use this skill for SecureLanSuite-specific Compose Multiplatform work in `apps/desktop-client`. For general Compose architecture details, also apply the existing project Compose guidance when the user explicitly requests that skill.

## Current migration status

- The stable packaged desktop application entry remains the deprecated JavaFX launcher until explicit Compose promotion/removal is accepted.
- New desktop UI/UX improvements target Compose first; JavaFX receives only critical fixes and fallback preservation.
- The experimental Compose shell is isolated under `apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose`.
- The Compose shell is launched by the dedicated Gradle task `runComposeShell`.
- JavaFX is deprecated and remains the fallback baseline only until each Compose replacement screen is accepted and packaging/promotion gates justify removal.
- The current desktop Compose migration order and status are recorded in [`docs/kotlin-migration.md`](../../../docs/kotlin-migration/kotlin-migration.md).

## Boundaries

- Keep all JavaFX and Compose UI code inside `apps/desktop-client`.
- Do not move desktop UI dependencies into `modules/*`.
- Do not change protocol behavior as part of UI migration.
- Do not remove JavaFX fallback code unless the user explicitly asks and acceptance criteria are met.
- Do not rewrite runtime-heavy chat, file-transfer, or WebRTC services for UI convenience.
- Keep reusable formatting, state mapping, and presentation helpers testable and UI-adjacent.

## Recommended migration order

For generic Compose parity/migration work, keep changes small and preserve the existing behavior.

For Phase 11 messenger-first redesign work, use this order:

1. Theme tokens, resources, density, focus/hover states.
2. Product mode state: Welcome, HostSetup, JoinSetup, Messenger, Settings, Diagnostics.
3. App shell and one compact global status indicator.
4. Full desktop welcome screen.
5. Separate HostSetup and JoinSetup flows.
6. Messenger layout without connection setup panels.
7. Left sidebar for room conversation, online peers, nearby peers, recent peers.
8. Conversation header, timeline, and composer.
9. Attach menu entry points for file transfer, Quick Share, encryption, and steganography.
10. Inline transfer cards in the chat timeline.
11. Contextual right panel for peer, room, transfer, call, diagnostics, or advanced connection.
12. Voice call banner and experimental video surface.
13. Advanced Connection, Diagnostics, Settings, and final QA.
14. Runtime validation, portable ZIP validation, and Windows EXE validation when launcher/packaging changes require it.

Do not create permanent Quick Share, Steganography, Audio Devices, or Transfers dashboard panels in Phase 11. Those features must be contextual: Attach, More, Settings, Diagnostics, or active transfer/call state.

## Visual migration contract

For each strict parity migration, JavaFX is the source of truth. For UX modernization tasks, JavaFX is the accepted behavioral baseline, not a strict visual target. For Phase 11 redesign, the Phase 11 checklist is the visual/product baseline; JavaFX is only a behavior, protocol, diagnostics, and rollback reference.

Before writing Compose code:
- inspect the relevant JavaFX classes, FXML if present, CSS, resource files, and current screenshots;
- identify the screen shell, sidebars, panels, cards, toolbars, lists, forms, dialogs, and status indicators;
- write a short mapping table from existing behavior/runtime state to the new Compose product modes; only map JavaFX elements directly when the task is strict parity.

Compose implementation must:
- preserve existing layout hierarchy and desktop UX in parity mode; replace the old dashboard hierarchy with the messenger-first Phase 11 hierarchy in redesign mode;
- preserve colors, typography, icons, spacing, and visual grouping where possible in parity mode; intentionally refine them in UX modernization mode using project tokens;
- use shared design tokens instead of ad-hoc dp/color values;
- avoid Android-looking mobile layouts;
- avoid one giant composable;
- keep composables mostly stateless;
- support resizing and compact desktop density;
- keep JavaFX fallback intact.

Recommended local reference files:
- `docs/ai/ui-migration-guide.md`
- `docs/ai/javafx-screenshots/`
- `docs/ai/compose-examples/`
- `apps/desktop-client/src/main/kotlin/.../compose/theme/`


## UX modernization mode

Use this mode when the user asks for a user-friendly Kotlin Compose interface, better UX, clearer layout, modernization, simplification, or redesign.

In this mode:
- preserve business behavior, protocols, diagnostics availability, service boundaries, and JavaFX fallback;
- improve information architecture, visual hierarchy, primary actions, navigation, wording, validation, and empty/error/loading states;
- keep diagnostics reachable, but move non-critical technical details into expandable sections;
- prefer user intent and task flow over literal JavaFX control order;
- keep desktop density compact and avoid Android/mobile assumptions;
- document meaningful deviations from JavaFX when they affect QA or acceptance.

User-friendly checklist:
- clear primary action;
- grouped related controls;
- readable labels;
- visible progress and errors;
- keyboard/focus/hover support;
- predictable resizing;
- dangerous actions separated;
- advanced diagnostics available but not visually dominant.

## Implementation rules

- Prefer small Compose surfaces that can be validated independently.
- Model UI state explicitly with immutable Kotlin data classes.
- Keep business rules in existing services or dedicated UI-adapter classes, not in composables.
- Keep composables mostly stateless: render state and emit callbacks.
- Preserve diagnostics for network, file transfer, RTC, media devices, audio levels, video frames, preview conversion, and runtime failures.
- Preserve existing desktop helper tests when moving behavior toward Compose.
- Add tests for non-visual mapping and formatting logic instead of trying to unit-test every composable.

## Validation defaults

- Compose shell tests: `gradlew.bat :apps:desktop-client:test --no-daemon`.
- Desktop build: `gradlew.bat :apps:desktop-client:build --no-daemon`.
- Manual shell run when UI behavior changes: `gradlew.bat :apps:desktop-client:runComposeShell --no-daemon`.
- Packaging validation is required only when launcher, runtime image, resources, distributions, or packaging tasks change.

## Documentation rule

Update [`docs/kotlin-migration.md`](../../../docs/kotlin-migration/kotlin-migration.md) when changing Compose migration status, migration order, launcher strategy, validation status, or JavaFX fallback assumptions.
