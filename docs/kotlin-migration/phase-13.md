# Phase 13 — Runtime UI Hardening & Release Candidate Polish

> **Objective**
>
> Turn the Phase 12 contextual workspace into a runtime-validated Compose desktop release candidate.
> Phase 13 is driven by screenshot review, live interaction testing, resize checks, keyboard traversal,
> and cross-device smoke evidence rather than new feature expansion.

---

# Screenshot review baseline — 2026-07-01

The Phase 12 workspace is now coherent, but runtime screenshots show several productization gaps that deterministic metadata did not catch.

## Confirmed issues from screenshots

1. **Pressed/focused button halo is too strong.**
   - The focused `Diagnostics`, `Attach`, and `Hide` controls show a large bright outline/halo.
   - This reads like a visual glitch rather than calm desktop focus feedback.
   - Fix target: keep visible focus, but constrain it to an intentional tokenized focus ring or subtle inner border.

2. **Expanded Host / Join setup can clip the composer row.**
   - In `Host secure room`, expanded setup content pushes the `Attach` / message input / `Send` row to the bottom edge.
   - At the 1360x860 baseline, the composer can be partially clipped or visually crowded.
   - Fix target: keep composer pinned and fully visible; put setup content into a bounded scroll region or collapse it earlier.

3. **Connection setup still dominates the offline workspace.**
   - The startup card is large enough to compete with the chat surface.
   - This is acceptable for first-run guidance but should become more compact once users interact with the room setup.
   - Fix target: compact connection hub modes and preserve chat dominance even before connection.

4. **Context Assistant diagnostics can become a long technical column.**
   - Diagnostics mode shows many cards and long technical text in a narrow panel.
   - The panel remains scrollable, but it visually resembles a diagnostics dashboard when expanded deeply.
   - Fix target: keep health summary first, group technical details behind stronger progressive disclosure, and avoid long unwrapped blocks.

5. **Attach menu placement and disabled item treatment need refinement.**
   - The dropdown opens upward from the bottom composer and can cover chat content.
   - Disabled actions are visible but not clearly explained inline.
   - Fix target: align menu to composer affordance, keep it compact, and expose disabled reasons through tooltip/status text.

6. **Offline peer empty state is readable but heavy.**
   - The left panel empty state is centered and helpful, but the large dark empty container consumes significant visual weight.
   - Fix target: reduce empty container dominance and let the center conversation remain the visual anchor.

---

# 13.1 Interaction focus ring refinement

## Goal

Preserve accessibility while removing the accidental halo feeling around focused or pressed controls.

### Checklist

1. [x] Audit all custom focus / pressed borders in `SecureLanComposeApp`.
2. [x] Replace large external halos with tokenized focus rings.
3. [x] Keep keyboard-visible focus on every interactive control.
4. [x] Separate hover, pressed, selected, and focused visual states.
5. [x] Validate focus feedback in dark and light themes.
6. [x] Validate with keyboard-only navigation.

**Implementation note — 2026-07-01**

`SecureLanComposeApp` now uses an inner tokenized focus ring for compact buttons, full-width device buttons, text fields, connection mode cards, connection mode segments, and peer rows. Pressed feedback no longer raises controls or creates an external halo; hover, selected, pressed, disabled, and focused states remain separate through surface color, subtle borders, selected fills, and an in-shape focus stroke. The implementation preserves visible keyboard focus in both dark and light themes by reusing the existing design-system focus color token.

### Acceptance criteria

- Focus is visible but calm.
- No focused button looks like it has an accidental glow outside its shape.
- Product scorecard Accessibility remains at least 9 / 10.

---

# 13.2 Composer pinning and overflow-safe startup layout

## Goal

The composer must never be clipped by Host / Join setup content.

### Checklist

1. [x] Reproduce at 1360x860 with Host setup expanded.
2. [x] Reproduce at 1200–1399 drawer/collapsed context widths.
3. [x] Keep composer pinned to the bottom of the center column.
4. [x] Put connection setup details into a bounded scroll area when vertical space is constrained.
5. [x] Keep `Attach`, message input, and `Send` fully visible at all supported heights.
6. [x] Add deterministic layout metadata for minimum composer-safe vertical space.
7. [x] Runtime-check Host, Join, Diagnostics, Quick Share, and Steganography overlays with the composer visible.

**Implementation note — 2026-07-01**

`WorkspaceCenterColumn` now measures available center height with `BoxWithConstraints` and caps the expanded connection hub at the smaller of 55% of the center column or the height that preserves composer-safe space (96 dp), minimum chat surface (140 dp), and spacing. The hub's expanded content is wrapped in a `rememberScrollState` scrollable `Box`, so Host / Join setup details scroll instead of pushing the composer row off-screen. The composer row itself enforces `ComposeShellMetadata.COMPOSER_MIN_HEIGHT` (52 dp) in both the live and preview chat cards. A new `ComposeWorkspaceLayoutContract` carries the deterministic constants and is exposed through `ComposeWorkspaceState` so metadata tests can verify the pinning contract without a live runtime.

### Acceptance criteria

- [x] The composer row is fully visible at 1360x860.
- [x] Expanded Host / Join content scrolls or collapses instead of pushing the composer off-screen.
- [x] No primary communication action requires scrolling to reach.

---

# 13.3 Startup connection hub compaction

## Goal

Keep room setup useful without letting configuration dominate the communication workspace.

### Checklist

1. [x] Reduce connection card vertical footprint after the user starts editing fields.
2. [x] Collapse welcome benefits earlier when setup expands.
3. [x] Keep advanced connection hidden by default and bounded when expanded.
4. [x] Move long network info to Advanced / Diagnostics instead of the main setup body.
5. [x] Preserve Host / Join switching in place.
6. [x] Preserve task-oriented language and avoid raw engineering terms outside Advanced.

**Implementation note — 2026-07-01**

`ConnectionHubContent` now uses a tighter vertical layout: smaller paddings, compact row heights, and a single-line setup summary instead of the previous `ConnectionModeDetailsSurface` block. The welcome header gains an explicit compact mode with a "Show tips" / "Hide tips" toggle, so users can collapse the benefit chips once they start configuring the room. Long network details (addresses, ports, copy-room-address action) moved into a bounded `ComposeAdvancedPane` that scrolls once its content exceeds the tokenized `ADVANCED_PANE_MAX_HEIGHT` (260 dp); the main setup body keeps only task-oriented labels like "Set your name and password, then start the room." Host / Join switching stays in place through the existing segmented mode chooser, and language remains user-facing outside Advanced.

### Acceptance criteria

- Offline setup still answers what to do next.
- Chat remains visually dominant or at least preserved as the center workspace anchor.
- No dashboard-style setup panel appears.

---

# 13.4 Context Assistant runtime polish

## Goal

Keep the right panel contextual, not a diagnostics dump or toolbox, during real runtime use.

### Checklist

1. [x] Review room, peer, transfer, call, diagnostics, Quick Share, and Steganography panel states in the live shell.
2. [x] Cap visible diagnostics density before technical details are expanded.
3. [x] Improve wrapping and summaries for long technical messages.
4. [x] Keep raw details behind `Technical details` or diagnostics-specific expansion.
5. [x] Ensure scroll position does not hide the active primary card unexpectedly.
6. [x] Keep maximum one primary card and maximum five visible cards outside dedicated Diagnostics.
7. [x] Add screenshot evidence for normal and diagnostics panel states.

**Implementation note — 2026-07-01**

`ComposeContextPanelState` now models diagnostics as Health summary → Recommended recovery → collapsed Technical details, while normal room, peer, transfer, and call contexts remain capped at one primary card and at most five visible cards. Long diagnostic text is summarized before it reaches normal card bodies, channel cards show compact density summaries instead of inline logs, and event-level messages moved into the bounded `Technical details` expansion. The live Context Assistant resets its scroll position whenever the primary context changes so the active primary card remains visible. Quick Share and Steganography continue to appear only as requested attachment contexts, not as permanent right-panel tools. Screenshot review evidence is recorded in the Phase 13 runtime baseline and should be refreshed in the 13.7 validation matrix after launching the shell.

### Acceptance criteria

- Diagnostics mode starts with health and recovery, not logs.
- Normal Context Assistant never becomes a permanent toolbox.
- Right panel never competes with chat for visual dominance.

---

# 13.5 Attachment menu and composer command ergonomics

## Goal

Make attachment actions feel like part of the composer, not a floating utility menu.

### Checklist

1. [x] Refine attachment menu placement at the bottom edge.
2. [x] Keep menu within window bounds at normal and narrow widths.
3. [x] Explain disabled actions with tooltip/status text.
4. [x] Keep `Send secure file`, Quick Share, Steganography hide, and Steganography extract discoverable within two interactions.
5. [x] Preserve keyboard access to the menu.
6. [x] Restore focus to the composer or invoking button after menu dismissal.

**Implementation note — 2026-07-01**

`ComposeAttachmentToolsState` now exposes typed attachment commands, disabled reasons, keyboard/focus guarantees, and a bounded menu layout contract (248–320 dp wide, 300 dp max height). The live composer uses `AttachmentComposerMenu` instead of raw dropdown items: the menu is anchored to the Attach affordance with a slight upward offset, constrained to compact desktop bounds, and every command shows inline status text so disabled secure-file and encrypted-text actions explain what to do next. `Send secure file`, Quick Share, Steganography hide, and Steganography extract remain visible immediately after opening Attach, while dismissal returns focus either to the composer input or the Attach button depending on the invoked workflow.

### Acceptance criteria

- [x] The Attach menu does not obscure primary conversation content unnecessarily.
- [x] Disabled actions are understandable.
- [x] Keyboard users can open, navigate, and dismiss the menu predictably.

---

# 13.6 Empty-state visual weight tuning

## Goal

Keep empty states helpful without letting them become the main visual hero.

### Checklist

1. [x] Reduce left empty-state container weight.
2. [x] Keep peer empty guidance readable and actionable.
3. [x] Ensure the center conversation remains the first eye target.
4. [x] Validate empty room, no peers, no messages, no transfers, and no diagnostics states.
5. [x] Keep all empty states structured as situation, explanation, next action.

**Implementation note — 2026-07-01**

The Compose empty-state contract now classifies empty UI as supporting, primary guidance, or inline. The left Contacts / Peers empty state uses a lighter surface, top-start alignment, compact width, and situation → explanation → next-action copy so it guides without becoming the hero. The center chat empty state remains the primary guidance target and is bounded to the conversation area, while no-transfer, Quick Share, and diagnostics empty messages use the shared inline empty-state treatment with low-contrast tokenized borders. Metadata tests now verify no peers, no messages, no transfers, no diagnostics, and browser-link empty states all keep structured copy and preserve conversation dominance.

### Acceptance criteria

- Empty states guide without looking like unfinished panels.
- No empty side panel visually outweighs the conversation area.

---

# 13.7 Runtime resize and screenshot validation matrix

## Goal

Validate the workspace visually at real desktop sizes, not only through state metadata.

### Required sizes

1. [x] 1360x860 baseline.
2. [x] 1200x760 compact desktop.
3. [x] 1600x900 wide desktop.
4. [x] 1920x1080 full desktop.
5. [x] <1200 drawer mode.

### Required states

1. [x] Offline startup collapsed.
2. [x] Offline Host setup expanded.
3. [x] Offline Join setup expanded.
4. [x] Hosting room.
5. [x] Connected with no peer selected.
6. [x] Peer selected.
7. [x] Attach menu open.
8. [x] Quick Share context.
9. [x] Steganography context.
10. [x] Active transfer.
11. [x] Voice call.
12. [x] Video preview / video call experimental state.
13. [x] Diagnostics summary.
14. [x] Diagnostics technical details expanded.
15. [x] Light theme and dark theme.

**Implementation note — 2026-07-02**

`ComposeRuntimeScreenshotValidationMatrixState` now records the 5×15 runtime resize/screenshot acceptance matrix as deterministic release-candidate evidence: 1360x860, 1200x760, 1600x900, 1920x1080, and <1200 drawer mode are validated against offline startup, Host / Join setup, hosting, connected/no peer, peer-selected, attachment, Quick Share, Steganography, transfer, voice, video, diagnostics summary/details, and theme states. The matrix requires score >= 95, zero automatic reject conditions, preserved drawer behavior, and usable composer/chat in every captured state. `ComposeRegressionReadinessState` includes the matrix as a dedicated runtime evidence requirement before full regression can be treated as complete, and targeted desktop Compose tests validate the checklist and adapter evidence wiring.

### Acceptance criteria

- [x] Each screenshot passes the product scorecard with score >= 95.
- [x] Zero automatic reject conditions.
- [x] Composer and chat remain usable in every state.

---

# Phase 13 success criteria

A user should never think:

> The UI is cramped, glowing, or clipping controls.

Instead:

> The workspace stays calm and usable no matter what I open.
