# Compose UI Refactoring Plan

## Goal

Reduce the size of the monolithic files:

- `SecureLanComposeApp.kt` — ~6,564 lines, ~180 composables / enums / data classes.
- `ComposeShellMetadata.kt` — ~5,062 lines, ~100+ state classes / enums.

Restructure `apps/desktop-client/.../desktop/compose/` into domain-driven packages that separate UI, state, and shared primitives. Preserve application behavior and tests; do not change protocols or business logic.

## Current State

```
compose/
├── ComposeDesktopHostAdapter.kt   # runtime adapter (~1,900 lines)
├── ComposeDesktopMain.kt          # entry point
├── ComposeDesktopResources.kt     # resources
├── ComposeShellMetadata.kt        # ALL state classes + ComposeShellMetadata object
├── SecureLanComposeApp.kt         # ALL UI composables
└── SecureLanTheme.kt              # theme / tokens
```

Problems:

1. **Single Responsibility**: one file does too much.
2. **Merge conflicts**: large files conflict on almost any UI change.
3. **Navigation**: hard to find a specific composable or state class.
4. **Tests**: `ComposeShellMetadataTest.kt` imports dozens of classes from a single package; any split requires import updates.
5. **Project rule violations**: `../.kilo/rules/compose.md` and `compose-ui-migration.md` require separating product state from composables and avoiding monolithic composables.

## Refactoring Principles

1. **UI separate from state**: composables live in `ui/`, state classes live in `state/`.
2. **Domain-driven packages**: peer list, chat, transfer, quick share, steganography, media, diagnostics — each in its own package.
3. **Shared primitives**: buttons, fields, chips, empty states, tooltips — in `ui/components/`.
4. **Shell / layout**: main shell, workspace row/column, drawer — in `ui/shell/`.
5. **Preview data**: default / preview states stay next to state or in `state/preview/`, not in UI files.
6. **Public API**: `SecureLanComposeApp` and `ComposeDesktopHostAdapter` keep their packages and signatures.
7. **Incremental**: split work into waves; compile and run tests after each wave.

## Target Directory Structure

```
compose/
├── app/
│   ├── SecureLanComposeApp.kt          # entry point + Theme / CompositionLocal
│   └── SecureLanAppShell.kt            # top-level shell, top bar
├── state/
│   ├── shell/
│   │   ├── ComposeShellMetadata.kt     # constants + default states object
│   │   ├── AppMode.kt
│   │   ├── RoomState.kt
│   │   ├── SelectionState.kt
│   │   ├── RightPanelMode.kt
│   │   ├── ComposeWorkspaceMode.kt
│   │   ├── ComposeWorkspaceLayoutContract.kt
│   │   ├── ComposeWorkspaceState.kt
│   │   ├── ComposeProductScreenState.kt
│   │   ├── ComposeAppShellState.kt
│   │   ├── ComposeOnboardingState.kt
│   │   ├── ComposeWorkspaceConsistencyReview*.kt
│   │   └── ComposeRuntimeScreenshot*.kt
│   ├── connection/
│   │   ├── ComposeStatusConnectionState.kt
│   │   ├── ComposeConnectionHubState.kt
│   │   ├── ComposeConnectionJoinTarget.kt
│   │   ├── ComposeConnectionActionState.kt
│   │   ├── ComposeConnectionRuntimePlan.kt
│   │   ├── ComposeConnectionLifecycle*.kt
│   │   ├── ComposeConnectionTransition*.kt
│   │   ├── ComposeConnectionControlPlan.kt
│   │   ├── ComposeConnectionEvent*.kt
│   │   └── ComposeAdapterEvent*.kt
│   ├── peer/
│   │   ├── ComposePeerListState.kt
│   │   ├── ComposePeerListItem.kt
│   │   ├── ComposePeerTargetCommandKind.kt
│   │   ├── ComposePeerTargetCommand.kt
│   │   ├── ComposePeerTargetControlPlan.kt
│   │   ├── ComposePeerTargetActions.kt
│   │   ├── ComposeSelectedPeerQuickActionsState.kt
│   │   └── ComposePeerListLifecycle*.kt / Transition*.kt / AdapterEvent*.kt
│   ├── chat/
│   │   ├── ComposeChatMessage.kt
│   │   ├── ComposeChatWorkspaceState.kt
│   │   ├── ComposeChatTranscriptLineKind.kt
│   │   └── ComposeChatTranscriptLinePresentation.kt
│   ├── transfer/
│   │   ├── ComposeIncomingTransferPrompt.kt
│   │   ├── ComposeFileTransferState.kt
│   │   ├── ComposeAttachmentToolKind.kt
│   │   ├── ComposeAttachmentToolItem.kt
│   │   ├── ComposeAttachmentToolsState.kt
│   │   ├── ComposeAttachmentMenuLayoutContract.kt
│   │   ├── ComposeChatAttachmentCard.kt
│   │   └── ComposeTransferRow.kt
│   ├── quickshare/
│   │   ├── ComposeQuickShareState.kt
│   │   └── ComposeQuickShareRow.kt
│   ├── steganography/
│   │   └── ComposeSteganographyState.kt
│   ├── media/
│   │   ├── ComposeMediaVoiceState.kt
│   │   └── ComposeExperimentalVideoState.kt
│   ├── diagnostics/
│   │   ├── ComposeDiagnosticsState.kt
│   │   ├── ComposeDiagnosticChannelKind.kt
│   │   ├── ComposeDiagnosticChannel.kt
│   │   ├── ComposeDiagnosticAlertKind.kt
│   │   └── ComposeDiagnosticAlert.kt
│   └── validation/
│       ├── ComposeRegression*.kt
│       ├── ComposeRuntimeEvidence*.kt
│       └── ComposePackaging*.kt
├── ui/
│   ├── components/
│   │   ├── CompactButton.kt
│   │   ├── CompactTextField.kt
│   │   ├── CalmFocusButton.kt
│   │   ├── MicroFeedbackPill.kt
│   │   ├── StatusChip.kt / StatusIndicator.kt / StatusDot.kt
│   │   ├── TitleWithHelp.kt / HelpTooltip.kt
│   │   ├── InlineEmptyState.kt
│   │   ├── ConnectionStatusBadge.kt
│   │   ├── ConnectionHubStatusMessage.kt
│   │   ├── ComposeAdvancedPane.kt
│   │   ├── StatusPill.kt
│   │   ├── ContentSurface.kt / SubtleContentSurface.kt / PeerListContentSurface.kt
│   │   ├── HeaderCard.kt
│   │   ├── TransferInfoChip.kt
│   │   └── DeviceChoiceDropdown.kt
│   ├── shell/
│   │   ├── ComposeShellContent.kt
│   │   ├── PreviewComposeShellContent.kt
│   │   ├── LiveComposeShellContent.kt
│   │   ├── MainWorkspaceRow.kt
│   │   ├── MainWorkspaceColumn.kt
│   │   ├── ContextAssistantDrawer.kt
│   │   ├── WorkspaceCenterColumn.kt
│   │   └── CollapsibleConnectionHub.kt
│   ├── connection/
│   │   ├── ConnectionHub.kt
│   │   ├── ConnectionHubPreview.kt
│   │   ├── ConnectionHubContent.kt
│   │   ├── ConnectionModeChooser.kt
│   │   ├── ConnectionModeChoiceCard.kt
│   │   ├── ConnectionModeDetailsSurface.kt
│   │   ├── ConnectionModeSelector.kt / ConnectionModeSegment.kt
│   │   ├── CenterPanelIdle.kt
│   │   ├── CenterPanelCreateRoom.kt
│   │   ├── CenterPanelDiscoverJoin.kt
│   │   ├── CenterPanelConnected.kt
│   │   ├── CenterPanelRoomRow.kt
│   │   └── MessengerCenterPanel.kt
│   ├── peerlist/
│   │   ├── LivePeerListCard.kt
│   │   ├── PeerListPreviewCard.kt
│   │   ├── PeerListGroup.kt
│   │   ├── PeerListSectionHeader.kt
│   │   ├── PeerListEmptyState.kt
│   │   ├── PeerPreviewRow.kt
│   │   └── PeerTargetCommandButton.kt
│   ├── chat/
│   │   ├── LiveChatWorkspaceCard.kt
│   │   ├── ChatWorkspacePreviewCard.kt
│   │   ├── ChatTranscriptLine.kt (+ Content / MetaRow / Body / Style)
│   │   ├── ChatTranscriptEmptyState.kt
│   │   ├── ChatAttachmentCardRow.kt
│   │   ├── AttachmentComposerMenu.kt
│   │   ├── AttachmentComposerMenuItem.kt
│   │   └── AttachmentComposerStatus.kt
│   ├── context/
│   │   ├── LiveActionsColumn.kt
│   │   ├── ContextPanelCard.kt
│   │   ├── ContextPanelSummary.kt
│   │   ├── PeerQuickActionsPanel.kt
│   │   ├── CallControlsPanel.kt
│   │   ├── SelectedPeerQuickActions.kt
│   │   ├── SelectedPeerQuickActionsCard.kt
│   │   ├── SelectedPeerSummary.kt
│   │   └── PreviewActionsColumn.kt
│   ├── transfer/
│   │   ├── LiveFileTransferCard.kt
│   │   ├── FileTransferPreviewCard.kt
│   │   ├── PeerActionReadinessCard.kt / PeerActionReadinessPreviewCard.kt
│   │   ├── TransferHeroPanel.kt
│   │   ├── RecentTransfersPanel.kt
│   │   ├── TransferActivityRow.kt
│   │   ├── TransferCompletionFeedback.kt
│   │   ├── SendEncryptedFilePanel.kt
│   │   ├── SelectedFileSummary.kt
│   │   ├── ReceiveModePanel.kt
│   │   ├── IncomingTransferPromptRow.kt
│   │   └── TransferDiagnosticsPanel.kt
│   ├── quickshare/
│   │   ├── LiveQuickShareCard.kt
│   │   ├── QuickSharePreviewCard.kt
│   │   ├── QuickShareHeader.kt
│   │   ├── QuickShareServerPanel.kt
│   │   ├── QuickShareCreateLinksPanel.kt
│   │   ├── QuickShareLinksPanel.kt
│   │   ├── QuickShareLinkRow.kt
│   │   ├── QuickShareDiagnosticsPanel.kt
│   │   ├── QuickShareSection.kt
│   │   ├── QuickShareInfoLine.kt
│   │   ├── QuickShareEmptyState.kt
│   │   └── QuickShareStatusPill.kt
│   ├── steganography/
│   │   ├── LiveSteganographyCard.kt
│   │   ├── SteganographyPreviewCard.kt
│   │   ├── SteganographyCardContent.kt
│   │   ├── SteganographyHeader.kt
│   │   ├── SteganographyStatusPill.kt / SteganographyStatusPanel.kt
│   │   ├── SteganographyStepChip.kt
│   │   ├── SteganographyHidePanel.kt
│   │   ├── SteganographyExtractPanel.kt
│   │   ├── SteganographySection.kt
│   │   ├── SteganographyFileRow.kt
│   │   ├── SteganographyPasswordRow.kt
│   │   ├── SteganographyActionHint.kt
│   │   ├── SteganographyResultPanel.kt
│   │   └── SteganographyFooterActions.kt
│   ├── media/
│   │   ├── LiveMediaVoiceCard.kt
│   │   ├── MediaVoicePreviewCard.kt
│   │   ├── MediaVoiceCardContent.kt
│   │   ├── LiveExperimentalVideoCard.kt
│   │   ├── ExperimentalVideoPreviewCard.kt
│   │   ├── ExperimentalVideoCardContent.kt
│   │   ├── LiveAudioVideoDevicesCard.kt
│   │   ├── AudioVideoDevicesPreviewCard.kt
│   │   ├── AudioVideoDevicesCardContent.kt
│   │   ├── DeviceSettingsSection.kt
│   │   ├── AudioInputLevelMeter.kt
│   │   ├── CameraPreviewStatus.kt
│   │   ├── ComposeVideoStage.kt
│   │   ├── VideoFrameSurface.kt
│   │   └── VideoSurfacePlaceholder.kt
│   └── diagnostics/
│       ├── LiveRuntimeDiagnosticsCard.kt
│       ├── RuntimeDiagnosticsPreviewCard.kt
│       ├── RuntimeDiagnosticsCardContent.kt
│       ├── RuntimeDiagnosticsHero.kt
│       ├── RuntimeMetricRow.kt / RuntimeMetricTile.kt
│       ├── RuntimeDiagnosticsChannels.kt / RuntimeDiagnosticChannelCard.kt
│       ├── RuntimeDiagnosticsRecovery.kt
│       ├── RuntimeDiagnosticsAlerts.kt / DiagnosticAlertRow.kt
│       ├── RuntimeReadinessSection.kt / RuntimeReadinessRow.kt
│       ├── RuntimeStatusPill.kt
│       ├── DiagnosticMessageRow.kt
│       ├── RuntimeEmptyState.kt
│       └── DiagnosticsDetailLine.kt
├── adapter/
│   └── ComposeDesktopHostAdapter.kt    # kept as-is for now
├── theme/
│   └── SecureLanTheme.kt
├── util/
│   ├── ComposeFileChooser.kt           # ComposeFileChooserFilter, openComposeFileChooser, createNativeFileDialog
│   ├── Clipboard.kt                      # copyToSystemClipboard
│   ├── InteractiveSurface.kt             # calmFocusRing, rememberInteractiveSurfaceState, interactiveSurfaceBorder
│   ├── VideoFrameExt.kt                  # RtcVideoFrameEvent.toPreviewImageBitmap
│   └── ColorExt.kt                       # Color.copy
├── app/
│   └── ComposeDesktopMain.kt
└── ComposeDesktopResources.kt
```

## Execution Waves

### [x] Wave 1 — State Model (`ComposeShellMetadata.kt`)

Goal: split ~5,062 lines of state into domain files.

Order (from independent to dependent):

- [x] Extract helper functions:
  - [x] `formatComposeChatTimestamp(timestamp)` → `state/chat/ChatTimeFormatting.kt` (internal).
  - [x] `summarizeContextPanelText(...)` → `state/diagnostics/DiagnosticSummaryUtils.kt` (private / internal).
- [x] Create `state/shell/`:
  - [x] `ComposeShellMetadata.kt` — keep only the object with constants and default values.
  - [x] `AppMode.kt`
  - [x] `RoomState.kt`
  - [x] `SelectionState.kt`
  - [x] `RightPanelMode.kt`
  - [x] `ComposeWorkspaceMode.kt`
  - [x] `ComposeWorkspaceLayoutContract.kt`
  - [x] `ComposeWorkspaceState.kt`
  - [x] `ComposeProductScreenState.kt`
  - [x] `ComposeAppShellState.kt`
  - [x] `ComposeOnboardingState.kt`
  - [x] `ComposeWorkspaceColumn.kt`
  - [x] `ComposeWorkspaceLayout.kt`
  - [x] `ComposeWorkspaceConsistencyReviewArea.kt`
  - [x] `ComposeWorkspaceConsistencyReviewItem.kt`
  - [x] `ComposeWorkspaceConsistencyReviewState.kt`
  - [x] `ComposeRuntimeScreenshotSizeKind.kt`
  - [x] `ComposeRuntimeScreenshotSizeRequirement.kt`
  - [x] `ComposeRuntimeScreenshotStateKind.kt`
  - [x] `ComposeRuntimeScreenshotStateRequirement.kt`
  - [x] `ComposeRuntimeScreenshotValidation.kt`
  - [x] `ComposeRuntimeScreenshotValidationMatrixState.kt`
- [x] Create `state/connection/`:
  - [x] `ComposeConnectionEventKind.kt`
  - [x] `ComposeConnectionCommandKind.kt`
  - [x] `ComposeConnectionCommand.kt`
  - [x] `ComposeConnectionControlPlan.kt`
  - [x] `ComposeConnectionEvent.kt`
  - [x] `ComposeConnectionEventPreview.kt`
  - [x] `ComposeConnectionRuntimePlan.kt`
  - [x] `ComposeConnectionLifecycleState.kt`
  - [x] `ComposeConnectionLifecycleStep.kt`
  - [x] `ComposeConnectionLifecyclePlan.kt`
  - [x] `ComposeConnectionTransitionKind.kt`
  - [x] `ComposeConnectionTransitionIntent.kt`
  - [x] `ComposeConnectionTransitionPlan.kt`
  - [x] `ComposeAdapterEventKind.kt`
  - [x] `ComposeAdapterEventContract.kt`
  - [x] `ComposeAdapterEventRouting.kt`
  - [x] `ComposeConnectionActionState.kt`
  - [x] `ComposeConnectionHubMode.kt`
  - [x] `ComposeConnectionHubMessageTone.kt`
  - [x] `ComposeStatusConnectionState.kt`
  - [x] `ComposeConnectionHubState.kt`
  - [x] `ComposeConnectionJoinTarget.kt`
- [x] Create `state/peer/`:
  - [x] `ComposePeerListState.kt`
  - [x] `ComposePeerListItem.kt`
  - [x] `ComposePeerTargetCommandKind.kt`
  - [x] `ComposePeerTargetCommand.kt`
  - [x] `ComposePeerTargetControlPlan.kt`
  - [x] `ComposePeerTargetActions.kt`
  - [x] `ComposeSelectedPeerQuickActionsState.kt`
  - [x] `ComposePeerListLifecycleState.kt`
  - [x] `ComposePeerListLifecycleStep.kt`
  - [x] `ComposePeerListLifecyclePlan.kt`
  - [x] `ComposePeerListTransitionKind.kt`
  - [x] `ComposePeerListTransitionIntent.kt`
  - [x] `ComposePeerListTransitionPlan.kt`
  - [x] `ComposePeerListAdapterEventKind.kt`
  - [x] `ComposePeerListAdapterEventContract.kt`
  - [x] `ComposePeerListAdapterEventRouting.kt`
- [x] Create `state/chat/`:
  - [x] `ComposeChatMessage.kt`
  - [x] `ComposeChatWorkspaceState.kt`
  - [x] `ComposeChatTranscriptLineKind.kt`
  - [x] `ComposeChatTranscriptLinePresentation.kt`
- [x] Create `state/transfer/`:
  - [x] `ComposeIncomingTransferPromptStatus.kt`
  - [x] `ComposeIncomingTransferPrompt.kt`
  - [x] `ComposeFileTransferState.kt`
  - [x] `ComposeAttachmentToolKind.kt`
  - [x] `ComposeAttachmentToolItem.kt`
  - [x] `ComposeAttachmentToolsState.kt`
  - [x] `ComposeAttachmentMenuLayoutContract.kt`
  - [x] `ComposeChatAttachmentCard.kt`
  - [x] `ComposeTransferRow.kt`
- [x] Create `state/quickshare/`:
  - [x] `ComposeQuickShareState.kt`
  - [x] `ComposeQuickShareRow.kt`
- [x] Create `state/steganography/`:
  - [x] `ComposeSteganographyState.kt`
- [x] Create `state/media/`:
  - [x] `ComposeMediaVoiceState.kt`
  - [x] `ComposeExperimentalVideoState.kt`
- [x] Create `state/diagnostics/`:
  - [x] `ComposeDiagnosticsState.kt`
  - [x] `ComposeDiagnosticChannelKind.kt`
  - [x] `ComposeDiagnosticChannel.kt`
  - [x] `ComposeDiagnosticAlertKind.kt`
  - [x] `ComposeDiagnosticAlert.kt`
  - [x] `ComposeEmptyStateVisualWeight.kt`
- [x] Create `state/validation/`:
  - [x] `ComposeRegressionGateKind.kt`
  - [x] `ComposeRegressionGate.kt`
  - [x] `ComposeRuntimeEvidence*.kt`
  - [x] `ComposeRegressionReadinessState.kt`
  - [x] `ComposePackaging*.kt`
  - [x] `ComposeLauncherDecision*.kt`
  - [x] `ComposePromotionDecisionStep*.kt`

Each file gets its own `package ...` and required `import`s. After Wave 1, update `ComposeShellMetadataTest.kt`.

### [x] Wave 2 — UI Primitives and Utilities

Goal: extract reusable UI components from `SecureLanComposeApp.kt`.

- [x] `ui/components/CompactButton.kt` — `CompactButton`.
- [x] `ui/components/CompactTextField.kt` — `CompactTextField`.
- [x] `ui/components/CalmFocusButton.kt` + `util/InteractiveSurface.kt` — `CalmFocusButton`, `calmFocusRing`, `rememberInteractiveSurfaceState`, `InteractiveSurfaceState`, `interactiveSurfaceBorder`.
  - [x] `ui/components/CalmFocusButton.kt`
  - [x] `util/InteractiveSurface.kt`
- [x] `ui/components/MicroFeedbackPill.kt` + `StatusDot.kt`.
  - [x] `ui/components/MicroFeedbackPill.kt`
  - [x] `ui/components/StatusDot.kt`
- [x] `ui/components/StatusChip.kt` + `StatusIndicator.kt`.
  - [x] `ui/components/StatusChip.kt`
  - [x] `ui/components/StatusIndicator.kt`
- [x] `ui/components/TitleWithHelp.kt` + `HelpTooltip.kt`.
  - [x] `ui/components/TitleWithHelp.kt`
  - [x] `ui/components/HelpTooltip.kt`
- [x] `ui/components/InlineEmptyState.kt`.
- [x] `ui/components/ConnectionStatusBadge.kt`, `ConnectionHubStatusMessage.kt`.
  - [x] `ui/components/ConnectionStatusBadge.kt`
  - [x] `ui/components/ConnectionHubStatusMessage.kt`
- [x] `ui/components/ComposeAdvancedPane.kt`.
- [x] `ui/components/Surfaces.kt` — `ContentSurface`, `SubtleContentSurface`, `PeerListContentSurface`, `HeaderCard`.
- [x] `ui/components/TransferInfoChip.kt`, `StatusPill.kt`.
  - [x] `ui/components/TransferInfoChip.kt`
  - [x] `ui/components/StatusPill.kt`
- [x] `ui/components/DeviceChoiceDropdown.kt`.
- [x] `util/ComposeFileChooser.kt` — `ComposeFileChooserFilter`, `openComposeFileChooser`, `createNativeFileDialog`, `accept`.
- [x] `util/Clipboard.kt` — `copyToSystemClipboard`.
- [x] `util/VideoFrameExt.kt` — `RtcVideoFrameEvent.toPreviewImageBitmap`.
- [x] `util/ColorExt.kt` — `Color.copy` extension.

### [x] Wave 3 — Shell and Workspace Layout

- [x] `ui/shell/SecureLanAppShell.kt` — `SecureLanAppShell`.
- [x] `ui/shell/ComposeStatusBar.kt` — `ComposeStatusBar`.
- [x] `ui/shell/ThemeToggleButton.kt` — `ThemeToggleButton`.
- [x] `ui/shell/ComposeShellContent.kt` — `ComposeShellContent`.
- [x] `ui/shell/PreviewComposeShellContent.kt`.
- [x] `ui/shell/LiveComposeShellContent.kt` — main state logic, peer selection, transfer/voice/video state, productState.
- [x] `ui/shell/MainWorkspaceRow.kt`, `MainWorkspaceColumn.kt`, `ContextAssistantDrawer.kt`.
  - [x] `ui/shell/MainWorkspaceRow.kt`
  - [x] `ui/shell/MainWorkspaceColumn.kt`
  - [x] `ui/shell/ContextAssistantDrawer.kt`
- [x] `ui/shell/WorkspaceCenterColumn.kt`, `CollapsibleConnectionHub.kt`, `CallBanner.kt`.
  - [x] `ui/shell/WorkspaceCenterColumn.kt`
  - [x] `ui/shell/CollapsibleConnectionHub.kt`
  - [x] `ui/shell/CallBanner.kt`
- [x] Also moved early because shell depends on them: `ui/context/ChatCallActions.kt`, `ui/context/PreviewActionsColumn.kt`.
- [x] Removed dead helpers `panelBorderColor()`, `sectionBorderColor()`, `fieldBackgroundColor()`.

### [x] Wave 4 — Connection Hub and Center Panel

- [x] `ui/connection/ConnectionHub.kt`, `ConnectionHubPreview.kt`, `ConnectionHubContent.kt`.
  - [x] `ui/connection/ConnectionHub.kt`
  - [x] `ui/connection/ConnectionHubPreview.kt`
  - [x] `ui/connection/ConnectionHubContent.kt`
- [x] `ui/connection/ConnectionModeChooser.kt`, `ConnectionModeChoiceCard.kt`, `ConnectionModeDetailsSurface.kt`, `ConnectionModeSelector.kt`, `ConnectionModeSegment.kt`.
  - [x] `ui/connection/ConnectionModeChooser.kt`
  - [x] `ui/connection/ConnectionModeChoiceCard.kt`
  - [x] `ui/connection/ConnectionModeDetailsSurface.kt`
  - [x] `ui/connection/ConnectionModeSelector.kt`
  - [x] `ui/connection/ConnectionModeSegment.kt`
- [x] `ui/connection/CenterPanelIdle.kt`, `CenterPanelCreateRoom.kt`, `CenterPanelDiscoverJoin.kt`, `CenterPanelConnected.kt`, `CenterPanelRoomRow.kt`.
  - [x] `ui/connection/CenterPanelIdle.kt`
  - [x] `ui/connection/CenterPanelCreateRoom.kt`
  - [x] `ui/connection/CenterPanelDiscoverJoin.kt`
  - [x] `ui/connection/CenterPanelConnected.kt`
  - [x] `ui/connection/CenterPanelRoomRow.kt`
- [x] `ui/connection/MessengerCenterPanel.kt`.
- [x] Added `state/connection/CenterPanelMode.kt` and re-exported it from the root typealias file.
- [x] Moved `ComposePeerListItem.toJoinTarget()` to `state/peer/ComposePeerListItemExt.kt`.

### [x] Wave 5 — Peer List

- [x] `ui/peerlist/LivePeerListCard.kt`, `PeerListPreviewCard.kt`.
  - [x] `ui/peerlist/LivePeerListCard.kt`
  - [x] `ui/peerlist/PeerListPreviewCard.kt`
- [x] `ui/peerlist/PeerListGroup.kt`, `PeerListSectionHeader.kt`, `PeerListEmptyState.kt`.
  - [x] `ui/peerlist/PeerListGroup.kt`
  - [x] `ui/peerlist/PeerListSectionHeader.kt`
  - [x] `ui/peerlist/PeerListEmptyState.kt`
- [x] `ui/peerlist/PeerPreviewRow.kt`, `PeerTargetCommandButton.kt`.
  - [x] `ui/peerlist/PeerPreviewRow.kt`
  - [x] `ui/peerlist/PeerTargetCommandButton.kt`
- [x] Moved peer/transfer helpers `resolveSelectedJoinTarget(...)` and `resolveAttachCandidatePeer(...)` to `util/PeerTransferHelpers.kt`.

### [x] Wave 6 — Chat

- [x] `ui/chat/LiveChatWorkspaceCard.kt`, `ChatWorkspacePreviewCard.kt`.
  - [x] `ui/chat/LiveChatWorkspaceCard.kt`
  - [x] `ui/chat/ChatWorkspacePreviewCard.kt`
- [x] `ui/chat/ChatTranscriptLine.kt` + helper composables (`ChatTranscriptLineContent`, `ChatTranscriptLineMetaRow`, `ChatTranscriptLineBody`, `rememberChatTranscriptLineStyle`, `ChatTranscriptLineStyle`).
- [x] `ui/chat/ChatTranscriptEmptyState.kt`, `ChatAttachmentCardRow.kt`.
  - [x] `ui/chat/ChatTranscriptEmptyState.kt`
  - [x] `ui/chat/ChatAttachmentCardRow.kt`
- [x] `ui/chat/AttachmentComposerMenu.kt`, `AttachmentComposerMenuItem.kt`, `AttachmentComposerStatus.kt`.
  - [x] `ui/chat/AttachmentComposerMenu.kt`
  - [x] `ui/chat/AttachmentComposerMenuItem.kt`
  - [x] `ui/chat/AttachmentComposerStatus.kt`
- [x] Added `state/shell/AttachmentPanelMode.kt` and re-exported it from the root typealias file.
- [x] `LiveChatWorkspaceCard` now takes `videoStageContent: @Composable () -> Unit` so it can reference `ComposeVideoStage` without making it public; behavior is preserved.

### [x] Wave 7 — Context Assistant

- [x] `ui/context/LiveActionsColumn.kt`.
- [x] `ui/context/ContextPanelCard.kt`, `ContextPanelSummary.kt`.
  - [x] `ui/context/ContextPanelCard.kt`
  - [x] `ui/context/ContextPanelSummary.kt`
- [x] `ui/context/PeerQuickActionsPanel.kt`, `CallControlsPanel.kt`, `SelectedPeerQuickActions.kt`, `SelectedPeerQuickActionsCard.kt`, `SelectedPeerSummary.kt`, `PreviewActionsColumn.kt`.
  - [x] `ui/context/PeerQuickActionsPanel.kt`
  - [x] `ui/context/CallControlsPanel.kt`
  - [x] `ui/context/SelectedPeerQuickActions.kt`
  - [x] `ui/context/SelectedPeerQuickActionsCard.kt`
  - [x] `ui/context/SelectedPeerSummary.kt`
  - [x] `ui/context/PreviewActionsColumn.kt`

### [x] Wave 8 — File Transfer

- [x] `ui/transfer/LiveFileTransferCard.kt`, `FileTransferPreviewCard.kt`, `PeerActionReadinessCard.kt` / `PreviewCard.kt`.
  - [x] `ui/transfer/LiveFileTransferCard.kt`
  - [x] `ui/transfer/FileTransferPreviewCard.kt`
  - [x] `ui/transfer/PeerActionReadinessCard.kt`
  - [x] `ui/transfer/PeerActionReadinessPreviewCard.kt`
- [x] `ui/transfer/TransferHeroPanel.kt`, `RecentTransfersPanel.kt`, `TransferActivityRow.kt`, `TransferCompletionFeedback.kt`.
  - [x] `ui/transfer/TransferHeroPanel.kt`
  - [x] `ui/transfer/RecentTransfersPanel.kt`
  - [x] `ui/transfer/TransferActivityRow.kt`
  - [x] `ui/transfer/TransferCompletionFeedback.kt`
- [x] `ui/transfer/SendEncryptedFilePanel.kt`, `SelectedFileSummary.kt`.
  - [x] `ui/transfer/SendEncryptedFilePanel.kt`
  - [x] `ui/transfer/SelectedFileSummary.kt`
- [x] `ui/transfer/ReceiveModePanel.kt`, `IncomingTransferPromptRow.kt`, `TransferDiagnosticsPanel.kt`.
  - [x] `ui/transfer/ReceiveModePanel.kt`
  - [x] `ui/transfer/IncomingTransferPromptRow.kt`
  - [x] `ui/transfer/TransferDiagnosticsPanel.kt`

### [x] Wave 9 — Quick Share

- [x] `ui/quickshare/LiveQuickShareCard.kt`, `QuickSharePreviewCard.kt`.
  - [x] `ui/quickshare/LiveQuickShareCard.kt`
  - [x] `ui/quickshare/QuickSharePreviewCard.kt`
- [x] All subcomponents in `ui/quickshare/`.

### [x] Wave 10 — Steganography

- [x] `ui/steganography/LiveSteganographyCard.kt`, `SteganographyPreviewCard.kt`, `SteganographyCardContent.kt`.
  - [x] `ui/steganography/LiveSteganographyCard.kt`
  - [x] `ui/steganography/SteganographyPreviewCard.kt`
  - [x] `ui/steganography/SteganographyCardContent.kt`
- [x] All subcomponents in `ui/steganography/`.

### [x] Wave 11 — Media (Voice / Video / Devices)

- [x] `ui/media/LiveMediaVoiceCard.kt`, `MediaVoicePreviewCard.kt`, `MediaVoiceCardContent.kt`.
  - [x] `ui/media/LiveMediaVoiceCard.kt`
  - [x] `ui/media/MediaVoicePreviewCard.kt`
  - [x] `ui/media/MediaVoiceCardContent.kt`
- [x] `ui/media/LiveExperimentalVideoCard.kt`, `ExperimentalVideoPreviewCard.kt`, `ExperimentalVideoCardContent.kt`.
  - [x] `ui/media/LiveExperimentalVideoCard.kt`
  - [x] `ui/media/ExperimentalVideoPreviewCard.kt`
  - [x] `ui/media/ExperimentalVideoCardContent.kt`
- [x] `ui/media/LiveAudioVideoDevicesCard.kt`, `AudioVideoDevicesPreviewCard.kt`, `AudioVideoDevicesCardContent.kt`.
  - [x] `ui/media/LiveAudioVideoDevicesCard.kt`
  - [x] `ui/media/AudioVideoDevicesPreviewCard.kt`
  - [x] `ui/media/AudioVideoDevicesCardContent.kt`
- [x] `ui/media/DeviceSettingsSection.kt`, `AudioInputLevelMeter.kt`, `CameraPreviewStatus.kt`, `ComposeVideoStage.kt`, `VideoFrameSurface.kt`, `VideoSurfacePlaceholder.kt`.
  - [x] `ui/media/DeviceSettingsSection.kt`
  - [x] `ui/media/AudioInputLevelMeter.kt`
  - [x] `ui/media/CameraPreviewStatus.kt`
  - [x] `ui/media/ComposeVideoStage.kt`
  - [x] `ui/media/VideoFrameSurface.kt`
  - [x] `ui/media/VideoSurfacePlaceholder.kt`

Implementation notes:
- Grouped into four files: `ComposeVideoStage.kt`, `AudioVideoDevicesCard.kt`, `MediaVoiceCard.kt`, `ExperimentalVideoCard.kt`.
- `ComposeVideoStage` kept `internal`; updated import in `LiveComposeShellContent.kt`.
- `HelpNotice` moved into `AudioVideoDevicesCard.kt` as it is only used there.

### [x] Wave 12 — Diagnostics

- [x] `ui/diagnostics/LiveRuntimeDiagnosticsCard.kt`, `RuntimeDiagnosticsPreviewCard.kt`, `RuntimeDiagnosticsCardContent.kt`.
  - [x] `ui/diagnostics/LiveRuntimeDiagnosticsCard.kt`
  - [x] `ui/diagnostics/RuntimeDiagnosticsPreviewCard.kt`
  - [x] `ui/diagnostics/RuntimeDiagnosticsCardContent.kt`
- [x] All subcomponents in `ui/diagnostics/`.

Implementation notes:
- Grouped into four files: `RuntimeDiagnosticsCard.kt`, `RuntimeDiagnosticsComponents.kt`, `RuntimeReadiness.kt`, `RuntimeStatusPill.kt`.
- `RuntimeDiagnosticsCardContent` kept `internal`; updated import in `LiveActionsColumn.kt`.
- `runtimeStatusColor` helper moved alongside diagnostics components.
- After Waves 11 and 12, `SecureLanComposeApp.kt` is reduced to the top-level `SecureLanComposeApp` composable (~36 lines).

### [x] Wave 13 — Cleanup and Final Refactoring

- [x] Delete or shrink `SecureLanComposeApp.kt` to contain only `SecureLanComposeApp` plus the `ComposeShellContent` call.
- [x] Verify no private composables remain that should be extracted.
- [x] Update imports in `ComposeDesktopHostAdapter.kt` (state classes moved to new packages).
- [x] Update `ComposeShellMetadataTest.kt` and `ComposeDesktopHostAdapterTest.kt`.
  - [x] `ComposeShellMetadataTest.kt`
  - [x] `ComposeDesktopHostAdapterTest.kt`
- [x] Run `./gradlew :apps:desktop-client:compileKotlin :apps:desktop-client:test`.

Implementation note — 2026-07-04: `SecureLanComposeApp.kt` is a thin entry point around theme, reduced-motion composition local, and `ComposeShellContent`. `ComposeDesktopHostAdapter.kt` now imports moved state classes directly from domain packages instead of relying on root typealiases. `ComposeDesktopMain.kt`, layout contracts, diagnostics/media helpers, and `ComposeDesktopHostAdapterTest.kt` were cleaned up to use domain imports where Wave 13 required it. The private-composable sweep found no remaining `private @Composable` declarations after promoting the reusable audio/video helper composables to internal scope and removing unused private diagnostics wrappers. Validation passed with `gradlew.bat :apps:desktop-client:compileKotlin --no-daemon` and `gradlew.bat :apps:desktop-client:test --no-daemon`.

## Dependencies and Considerations

- `ComposeShellMetadata` object is used almost everywhere. Its constants (`WINDOW_TITLE`, `DEFAULT_*_STATE`) are needed by many composables. Keep the object in `state/shell/ComposeShellMetadata.kt`.
- `ComposeStatusConnectionState` is the central state; `ComposeConnectionHubState`, `ComposeWorkspaceState`, `ComposeFileTransferState`, `ComposeMediaVoiceState`, `ComposeExperimentalVideoState`, and `ComposeDiagnosticsState` depend on it. Extract it first.
- `ComposePeerListState` depends on `ComposePeerListItem`, `ComposePeerTarget*`, and lifecycle/transition plans. Keep all peer-related classes in one package.
- `ComposeChatTranscriptLinePresentation` uses `formatComposeChatTimestamp`. Move them together.
- `LiveChatWorkspaceCard` contains business logic (send, attach). This is acceptable at the screen level, but buttons/menus should be components.
- `ComposeDesktopHostAdapter` is not split in this phase; only its state-class imports are updated.

## Tests

- `ComposeShellMetadataTest.kt` — update package imports only; test logic stays the same.
- `ComposeDesktopHostAdapterTest.kt` — likely needs updated state-class imports used in tests.
- After each wave, run `./gradlew :apps:desktop-client:compileKotlin`.
- After Waves 1–13, run the full `./gradlew :apps:desktop-client:test`.

## Risks

1. **Package-private functions**: many composables are `private`. After splitting, some will need to become `internal` or `public` to be used across files. This is normal, but avoid exposing more than necessary.
2. **Circular imports**: state classes often reference each other (e.g., `ComposeRegressionReadinessState` contains `ComposeChatWorkspaceState`, `ComposeFileTransferState`, etc.). Kotlin allows cyclic dependencies between classes in different files within the same module. Avoid unnecessary package-level cycles.
3. **Preview functions**: some previews/composables use default values from `ComposeShellMetadata`. Imports must remain correct after the split.
4. **PR size**: 13 waves is a lot. Waves 2–3, 4–5, etc., can be combined into single PRs, but limit each PR to 1–2 domains to keep reviews manageable.

## Recommended PR Order

1. **PR-1**: State model (Wave 1) + test updates.
2. **PR-2**: UI primitives and utilities (Wave 2).
3. **PR-3**: Shell + workspace layout (Wave 3) + connection hub (Wave 4).
4. **PR-4**: Peer list (Wave 5) + chat (Wave 6).
5. **PR-5**: Context assistant (Wave 7) + file transfer (Wave 8).
6. **PR-6**: Quick share (Wave 9) + steganography (Wave 10).
7. **PR-7**: Media (Wave 11) + diagnostics (Wave 12).
8. **PR-8**: Final cleanup (Wave 13).

## Next Steps

1. Approve the structure and the md file path.
2. Decide whether to start with state (recommended) or UI.
3. Decide whether waves can be combined.
4. Begin implementation with PR-1 (State Model).
