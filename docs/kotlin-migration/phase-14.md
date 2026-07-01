# Phase 14 — Product Polish & UX Refinement

> Goal: complete the remaining core functionality, simplify the user experience, and prepare SecureLanSuite for everyday use.

## Checklist

1. [x] Fix voice and video calls
   - Restore stable audio communication.
   - Restore stable video communication.
   - Verify desktop ↔ desktop.
   - Verify desktop ↔ Android.
   - Verify reconnect and disconnect scenarios.
   - Handle network interruptions gracefully.

2. [x] Redesign the room startup workflow
   - Replace the current "Host / Join" dual layout with a room-centric workflow.
   - Show nearby secure rooms first.
   - Allow creating a new room as a primary action.
   - Display only the controls relevant to the selected scenario.
   - Reduce first-screen cognitive load.

3. [x] Simplify the main screen
   - Remove competing UI elements.
   - Focus the first screen on starting or joining a conversation.
   - Keep advanced networking options hidden by default.
   - Preserve power-user functionality inside expandable sections.

4. [x] Refine diagnostics UX
   - Reduce visual dominance of the Diagnostics panel.
   - Move detailed diagnostics into expandable cards.
   - Keep only health summary visible by default.
   - Make diagnostics contextual instead of always visible.

5. [x] Improve visual hierarchy
   - Introduce clearer elevation levels for cards.
   - Reduce visual noise.
   - Differentiate primary, secondary, and informational surfaces.
   - Improve spacing consistency across the application.

6. [x] Refine button hierarchy
   - Keep only one primary action per screen.
   - Convert secondary actions to outlined or subtle buttons.
   - Make destructive actions visually distinct.

7. [x] Improve chat presentation
   - Separate system events from user messages.
   - Make presence events lightweight.
   - Improve message spacing and readability.
   - Reduce the "log viewer" appearance.

8. [x] Compact conversation header
   - Reduce vertical space usage.
   - Show peer capabilities (Voice, Video, File).
   - Display online status more prominently.

   **Implementation note — 2026-07-07**

   The Compose conversation pane now renders user messages as the primary transcript content while lightweight presence and system events use subdued centered presentation instead of full log-style rows. Presence normalization handles join/leave messages even when they arrive through system-prefixed chat events, and transcript spacing is increased for readability. A compact in-pane conversation header shows the selected person or shared room, a prominent online/room status badge, and selected-peer capability chips for Voice, Video, and File without changing JavaFX fallback behavior or any chat/file/RTC protocol contracts.

9. [x] Improve peer list
   - Better distinguish online/offline users.
   - Display supported capabilities.
   - Reduce unused empty space.
   - Prepare for grouping and filtering.

   **Implementation note — 2026-07-08**

   The Compose peer list now uses dedicated presentation models for peer rows and sections so future grouping and filtering can operate on display data without rewriting list UI. Peer rows show stronger online/offline distinction with status badges, filled/outline presence glyphs, color and non-color indicators, accessible row descriptions, compact capability chips, and a chat-only fallback when voice/video/file capabilities are absent. The list layout uses tighter row padding and section spacing while preserving stable nickname-based selection across refreshes and existing JavaFX fallback/service behavior.

10. [x] Modernize the attachment menu
    - Replace the current popup with a richer action menu.
    - Add icons.
    - Improve grouping of actions.
    - Keep privacy-related tools visually separated.

    **Implementation note — 2026-07-09**

    The Compose attachment popup now uses `compose.materialIconsExtended` to show a leading icon for every action. Actions are grouped into "Send & share" (secure file, Quick Share) and "Privacy tools" (encrypted text/file, hide message, extract message), separated by a subtle divider. The item rows use the project design tokens for spacing, icon tint, hover/focus feedback, and the existing status pill. The underlying state model, item ordering, and selection callbacks are unchanged, so keyboard access, screen-reader labels, and disabled-status behavior remain intact.

11. [x] Introduce a consistent icon system
    - Voice
    - Video
    - File transfer
    - Encryption
    - LAN
    - Diagnostics
    - Presence
    - Quick Share

    **Implementation note — 2026-07-09**

    Added `SecureLanIcons` in `apps/desktop-client/.../compose/ui/icons/SecureLanIcons.kt` as the single canonical icon set for product concepts. Each concept maps to one Material icon: Voice (`Mic`), Video (`Videocam`), File (`Description`), Encryption (`Lock`), LAN (`Wifi`), Diagnostics (`MonitorHeart`), Presence (`Circle` filled/outlined), Quick Share (`Share`), plus hang-up (`CallEnd`) and attach (`AttachFile`). The icon set is now used by the attachment menu, peer-list capability chips, conversation-header capability chips, selected-peer quick actions, the Diagnostics header button, the Quick Share preview header, and context-panel card titles. State labels and test assertions are unchanged; only presentation consumes the new icons.

12. [x] Reduce informational text
   - Replace long descriptions with concise summaries.
   - Move detailed explanations into tooltips or expandable sections.
   - Keep screens focused on actions.

    **Implementation note — 2026-07-09**

    Shortened user-facing copy across the Compose desktop client. The peer-list empty state, context-panel guidance, diagnostics health summary, recovery guidance, channel descriptions, and channel empty states now use concise summaries instead of multi-sentence explanations. The `InlineEmptyState` component no longer forces a repetitive "next action" line, so diagnostic channel cards avoid the boilerplate "Continue from the conversation workspace" footer. Responsive context-panel summaries, selected-peer quick-action copy, file-transfer hero subtitles, and media/peer-target readiness summaries were also tightened while preserving the key phrases covered by tests.

13. [ ] Improve status visualization
   - Use colored status badges.
   - Highlight healthy, warning, and error states.
   - Improve readability of runtime state.

14. [ ] Add application status bar
   - Connection state.
   - Encryption status.
   - Active room.
   - Connected peers.
   - Active transports.
   - Network information.

15. [x] Transform Context Assistant into a proactive assistant
   - Show contextual recommendations.
   - Suggest the next logical actions.
   - Display peer capabilities.
   - Surface only information relevant to the current conversation.

   **Implementation note — 2026-07-09**

   The Compose Context Assistant no longer duplicates the main action bar. The "Quick actions" card has been removed from the right panel because Attach, Voice call, Video call, and End call already have canonical locations in the conversation header and composer. Each context now surfaces only relevant cards: room context shows guidance plus room status; peer context shows a primary peer profile with presence and capability chips, collapsed recent files, and collapsed security; transfer context shows transfer details as the primary card with the peer profile collapsed below; call context shows call status as the primary card with the peer profile collapsed below. All context cards now have canonical icons from `SecureLanIcons`, the card layout wraps its height instead of stretching to fill, and capability chips render the selected peer's supported features visually. The drawer mode also avoids duplicating the summary text by relying on `ContextPanelSummary`.

16. [ ] Final UI consistency pass
   - Spacing.
   - Alignment.
   - Typography.
   - Animations.
   - Corner radius consistency.
   - Hover effects.
   - Scroll behavior.
   - Keyboard navigation.

   **Implementation note — 2026-07-29 (Quick Share and Steganography)**

   Quick Share advanced settings now use a single-column responsive layout with field-level validation, expiration and download presets, true `Until stopped` and `Unlimited` policies, an opt-in custom port, and a readable policy summary. Running shares present their settings read-only until the user explicitly chooses to edit them; port changes remain disabled until the server is stopped, and active links retain the policy captured when they were created. The quick-share core model, browser pages, desktop formatting, and tests now represent optional expiration and access limits directly instead of using artificial sentinel values.

   Steganography no longer renders inside the narrow Context Assistant column. It is exposed through one `Steganography` action in the `Attach` menu, which opens the shared resizable modal on the `Hide message` tab; `Extract message` remains available as the second tab inside that modal. The modal adds image previews, file drag-and-drop, clipboard image paste, automatic capacity inspection, UTF-8 byte counting, progressive password fields, field-specific readiness/error/completion states, one primary action per mode, copy/save extraction results, and open-folder/secure-send actions for generated BMP files. Empty result panels, duplicate choose actions, readiness chips, the old combined long form, and the redundant Context Assistant entry were removed; Escape closes the modal, a minimum modal size prevents control collapse, and native controls preserve keyboard navigation.

   Audio & video setup now lives in a dedicated resizable `Settings` window opened from the top bar next to the theme control. The window has a persistent settings navigation area ready for future sections and a wide, scrollable Audio & video workspace: microphone and speaker controls share one row when space allows, camera preview receives the full content width, device tests stay next to their status, and the layout falls back to one column at smaller sizes. Refresh, device selection, input metering, speaker/microphone/camera tests, camera preview, permission summaries, and recovery guidance continue to use the existing live media state. Call start/end actions remain in the conversation controls instead of being duplicated inside settings. The old Audio & video card and its metadata were removed from every Context Assistant mode; Escape and the visible Close action dismiss the settings window.

   The obsolete JavaFX workspace-parity layer was removed after parity checks were retired. Runtime layout data now lives in the minimal `ComposeWorkspaceLayout` and `ComposeWorkspaceColumn` models, containing only the titles and weights consumed by `MainWorkspaceRow`. JavaFX source mappings, fallback labels, parity summaries/checks, action-section presentation metadata, and the unused `ActionsColumnSection` composable were deleted.
