# Compose Multiplatform UI Migration Rules

Use these rules for SecureLanSuite JavaFX-to-Compose Multiplatform Desktop work in `apps/desktop-client`.

## Current stage

- Compose-first desktop UX hardening is the current implementation priority.
- JavaFX is deprecated for desktop UI evolution and remains only as packaged launcher, rollback fallback, and critical-fix path until runtime parity, UX stabilization, portable ZIP validation, Windows EXE validation, rollback planning, and explicit approval are complete.

## Core rule

Default to the mode implied by the user task. If the user asks for parity, migration, porting, or matching the old UI, use JavaFX parity mode. If the user asks for a user-friendly UI, UX improvement, modernization, redesign, better Compose UI, or usability fixes, use UX modernization mode.

In UX modernization mode, JavaFX is a functional and behavioral reference, not a visual prison. Preserve accepted behavior, protocols, service boundaries, diagnostics availability, and JavaFX fallback. Improve information architecture, layout hierarchy, navigation, grouping, labels, empty/error/loading states, validation feedback, accessibility, responsiveness, and desktop ergonomics.

Do not change discovery, chat, file-transfer, quick-share, stego, RTC, voice, video, or Android interoperability protocols as part of UI modernization unless the user explicitly asks for protocol changes.

When a task references Phase 11 UI/UX redesign or `docs/kotlin-migration/phase-11.md`, use UX modernization mode. Phase 11 is not JavaFX parity. The old dashboard must be treated as a behavioral reference and rollback baseline only; the visual target is the messenger-first Phase 11 checklist.

New desktop UI/UX improvements must target Compose UI. Do not add new JavaFX screens or non-critical JavaFX polish.

## UI work modes

### 1. JavaFX parity mode

Use this mode when the task says: migrate, port, match JavaFX, preserve the old screen, keep parity, or fix a Compose regression.

- Recreate the existing JavaFX screen structure, visual hierarchy, spacing, behavior, desktop density, diagnostics, and service boundaries.
- Preserve colors, typography, icons, grouping, disabled states, dialogs, and status indicators where possible.
- Compare Compose against JavaFX after coding.

### 2. UX modernization mode

Use this mode by default when the task says: user-friendly, improve UX, modernize UI, make the interface clearer, simplify, redesign, make better Compose UI, or improve usability.

- Preserve business behavior, protocols, data flow, feature availability, diagnostics availability, and JavaFX fallback.
- Improve the screen structure if JavaFX copied literally would be confusing, noisy, cramped, or technically worded.
- Prefer clear user journeys, obvious primary actions, better grouping, progressive disclosure for advanced diagnostics, and human-readable labels.
- Keep advanced diagnostics reachable, but do not let logs/raw protocol details dominate the primary workflow unless the screen is explicitly diagnostic.
- Document important UX deviations from JavaFX in the implementation note or checklist.

## Required workflow

For every screen or runtime UI area:
1. Read the existing JavaFX code, CSS, resources, screenshots, and current Compose implementation.
2. Summarize the current JavaFX and Compose structure.
3. Identify reusable UI components, state holders, adapter logic, diagnostics, and formatter helpers.
4. For parity work, map JavaFX controls and states to Compose components. For Phase 11 modernization, map JavaFX/runtime behavior into the new product modes and explicit immutable UI state without recreating old panels.
5. Implement the Compose change inside `apps/desktop-client` without moving UI dependencies into reusable modules.
6. Compare the result against JavaFX for parity work, or against the accepted Compose modernization baseline such as the Phase 11 checklist.
7. Refactor into smaller composables and keep business logic in adapters/services/helpers.
8. Update the desktop checklist when migration status, validation status, launcher strategy, or JavaFX fallback assumptions change.

## JavaFX to Compose mapping

- VBox -> Column
- HBox -> Row
- StackPane -> Box
- BorderPane -> Scaffold-like shell using Row/Column/Box
- SplitPane -> custom resizable/split layout
- TitledPane -> compact collapsible section
- ListView -> LazyColumn or compact desktop list surface
- TableView -> custom desktop table or LazyColumn rows
- Label -> Text
- Button -> Button / FilledTonalButton / IconButton / compact project button
- TextField -> OutlinedTextField or compact project text field
- PasswordField -> password text field with visual transformation
- CheckBox -> Checkbox with explicit label and state
- ComboBox -> exposed dropdown/menu with selected item state
- ProgressBar -> LinearProgressIndicator or compact progress row
- Dialog / Alert -> Compose dialog or clearly modeled prompt state
- CSS variables/classes -> Compose theme tokens and modifiers
- Controller mutable state -> explicit immutable UiState + callbacks

## User-friendly UI checklist

For every Compose screen in UX modernization mode:

- Make the primary user action obvious.
- Group related controls into clear sections.
- Reduce visual noise, duplicated status text, and raw technical terminology.
- Show clear empty, loading, error, disabled, success, validation, and progress states.
- Use human-readable labels before internal class names, protocol names, raw IDs, or status codes.
- Use progressive disclosure for advanced diagnostics and troubleshooting details.
- Keep dangerous or destructive actions visually separated and clearly confirmed.
- Support keyboard navigation, focus states, hover states, window resizing, and compact desktop density.
- Keep diagnostics available for troubleshooting, but place non-critical details in expandable/details areas.
- Prefer task-oriented wording: what the user can do next, what happened, and how to fix it.

## UI quality rules

- Use project design tokens for spacing, shapes, colors, and typography.
- Prefer compact desktop density over mobile spacing.
- Preserve or intentionally improve toolbars, sidebars, status bars, panels, action groups, dialogs, and diagnostics. In UX modernization mode, prefer clearer workflows over literal JavaFX structure.
- Preserve disabled, loading, error, empty, hover, focus, selected, active-call, active-transfer, stopped-share, and validation states.
- Keep diagnostics visible for discovery, chat, file transfer, quick share, stego, RTC provider initialization, SDP/ICE, media devices, audio levels, video frames, preview conversion, and runtime failures.
- Do not use random colors or arbitrary dp values.
- Do not make all buttons huge.
- Do not put all UI into a single composable.
- Do not put business logic in composables.
- Do not change `application.mainClass`, manifest behavior, jpackage main class, or JavaFX fallback files unless Compose promotion has explicit approval.
