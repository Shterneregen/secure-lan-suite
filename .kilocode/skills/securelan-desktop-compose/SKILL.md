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

1. App shell, theme, resources, and layout scaffolding.
2. Status and connection controls.
3. Peer list.
4. Chat workspace.
5. File transfer.
6. Quick share.
7. Steganography panel.
8. Media devices and voice controls.
9. Experimental camera and video last.
10. Runtime validation, portable ZIP validation, and Windows EXE validation.

## Visual migration contract

For each strict parity migration, JavaFX is the source of truth. For UX modernization tasks, JavaFX is the accepted behavioral baseline, not a strict visual target.

Before writing Compose code:
- inspect the relevant JavaFX classes, FXML if present, CSS, resource files, and current screenshots;
- identify the screen shell, sidebars, panels, cards, toolbars, lists, forms, dialogs, and status indicators;
- write a short mapping table from JavaFX elements to Compose composables.

Compose implementation must:
- preserve existing layout hierarchy and desktop UX in parity mode; improve hierarchy and workflows in UX modernization mode;
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
