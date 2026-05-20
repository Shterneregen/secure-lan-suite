---
name: securelan-desktop-compose
license: MIT
description: >
  SecureLanSuite desktop Compose Multiplatform migration skill. Use when the
  user asks about the desktop Compose shell, Phase 9 UI migration, replacing
  JavaFX screens with Compose, Compose desktop Gradle setup, desktop UI state,
  or JavaFX fallback preservation in apps/desktop-client.
---

# SecureLanSuite Desktop Compose Skill

Use this skill for SecureLanSuite-specific Compose Multiplatform work in `apps/desktop-client`. For general Compose architecture details, also apply the existing project Compose guidance when the user explicitly requests that skill.

## Current migration status

- The stable desktop application entry remains the JavaFX launcher.
- The experimental Compose shell is isolated under `apps/desktop-client/src/main/kotlin/com/shterneregen/securelan/desktop/compose`.
- The Compose shell is launched by the dedicated Gradle task `runComposeShell`.
- JavaFX remains the fallback baseline until each Compose replacement screen is accepted.
- The Phase 9 migration order is recorded in [`docs/desktop-client-kotlin-migration.md`](../../../docs/desktop-client-kotlin-migration.md) and [`docs/kotlin-migration.md`](../../../docs/kotlin-migration.md).

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

For each migrated JavaFX screen, JavaFX is the source of truth.

Before writing Compose code:
- inspect the relevant JavaFX classes, FXML if present, CSS, resource files, and current screenshots;
- identify the screen shell, sidebars, panels, cards, toolbars, lists, forms, dialogs, and status indicators;
- write a short mapping table from JavaFX elements to Compose composables.

Compose implementation must:
- preserve existing layout hierarchy and desktop UX;
- preserve colors, typography, icons, spacing, and visual grouping where possible;
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

Update [`docs/desktop-client-kotlin-migration.md`](../../../docs/desktop-client-kotlin-migration.md) or [`docs/kotlin-migration.md`](../../../docs/kotlin-migration.md) when changing Phase 9 status, migration order, launcher strategy, validation status, or JavaFX fallback assumptions.
