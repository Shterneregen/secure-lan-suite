package com.shterneregen.securelan.desktop.compose

internal fun formatComposeChatTimestamp(timestamp: java.time.Instant): String =
    com.shterneregen.securelan.desktop.compose.state.chat.formatComposeChatTimestamp(timestamp)

// Shell state typealiases
typealias AppMode = com.shterneregen.securelan.desktop.compose.state.shell.AppMode
typealias RoomState = com.shterneregen.securelan.desktop.compose.state.shell.RoomState
typealias SelectionState = com.shterneregen.securelan.desktop.compose.state.shell.SelectionState
typealias RightPanelMode = com.shterneregen.securelan.desktop.compose.state.shell.RightPanelMode
typealias ComposeWorkspaceMode = com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceMode
typealias ComposeWorkspaceLayoutContract = com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceLayoutContract
typealias ComposeWorkspaceState = com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceState
typealias ComposeProductScreenState = com.shterneregen.securelan.desktop.compose.state.shell.ComposeProductScreenState
typealias ComposeAppShellState = com.shterneregen.securelan.desktop.compose.state.shell.ComposeAppShellState
typealias ComposeWorkspaceConsistencyReviewArea = com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceConsistencyReviewArea
typealias ComposeWorkspaceConsistencyReviewItem = com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceConsistencyReviewItem
typealias ComposeWorkspaceConsistencyReviewState = com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceConsistencyReviewState
typealias ComposeOnboardingState = com.shterneregen.securelan.desktop.compose.state.shell.ComposeOnboardingState
typealias ComposeContextPanelCardKind = com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelCardKind
typealias ComposeContextPanelResponsiveMode = com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelResponsiveMode
typealias ComposeContextPanelResponsiveState = com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelResponsiveState
typealias ComposeContextPanelCard = com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelCard
typealias ComposeContextPanelState = com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelState
typealias ComposeGlobalStatusIndicatorState = com.shterneregen.securelan.desktop.compose.state.shell.ComposeGlobalStatusIndicatorState
typealias ComposeRoomStartupRoom = com.shterneregen.securelan.desktop.compose.state.shell.ComposeRoomStartupRoom
typealias ComposeRoomStartupState = com.shterneregen.securelan.desktop.compose.state.shell.ComposeRoomStartupState
typealias ComposeShellMetadata = com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata

// Connection state typealiases
typealias ComposeAdapterEventContract = com.shterneregen.securelan.desktop.compose.state.connection.ComposeAdapterEventContract
typealias ComposeAdapterEventKind = com.shterneregen.securelan.desktop.compose.state.connection.ComposeAdapterEventKind
typealias ComposeAdapterEventRouting = com.shterneregen.securelan.desktop.compose.state.connection.ComposeAdapterEventRouting
typealias ComposeConnectionActionState = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionActionState
typealias ComposeConnectionCommand = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionCommand
typealias ComposeConnectionCommandKind = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionCommandKind
typealias ComposeConnectionControlPlan = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionControlPlan
typealias ComposeConnectionEvent = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionEvent
typealias ComposeConnectionEventKind = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionEventKind
typealias ComposeConnectionEventPreview = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionEventPreview
typealias ComposeConnectionHubMessageTone = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMessageTone
typealias ComposeConnectionHubMode = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
typealias ComposeConnectionHubState = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubState
typealias ComposeConnectionJoinTarget = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
typealias ComposeConnectionLifecyclePlan = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionLifecyclePlan
typealias ComposeConnectionLifecycleState = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionLifecycleState
typealias ComposeConnectionLifecycleStep = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionLifecycleStep
typealias ComposeConnectionRuntimePlan = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionRuntimePlan
typealias ComposeConnectionTransitionIntent = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionTransitionIntent
typealias ComposeConnectionTransitionKind = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionTransitionKind
typealias ComposeConnectionTransitionPlan = com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionTransitionPlan
typealias ComposeStatusConnectionState = com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState

// Empty state typealias
typealias ComposeEmptyStateVisualWeight = com.shterneregen.securelan.desktop.compose.state.shell.ComposeEmptyStateVisualWeight

// Media state typealiases
typealias ComposeExperimentalVideoState = com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
typealias ComposeMediaVoiceState = com.shterneregen.securelan.desktop.compose.state.media.ComposeMediaVoiceState

// Peer state typealiases
typealias ComposePeerListAdapterEventContract = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListAdapterEventContract
typealias ComposePeerListAdapterEventKind = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListAdapterEventKind
typealias ComposePeerListAdapterEventRouting = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListAdapterEventRouting
typealias ComposePeerAvailabilityKind = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerAvailabilityKind
typealias ComposePeerCapabilityPresentation = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerCapabilityPresentation
typealias ComposePeerListItem = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem
typealias ComposePeerListItemPresentation = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItemPresentation
typealias ComposePeerListLifecyclePlan = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListLifecyclePlan
typealias ComposePeerListLifecycleState = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListLifecycleState
typealias ComposePeerListLifecycleStep = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListLifecycleStep
typealias ComposePeerListSectionPresentation = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListSectionPresentation
typealias ComposePeerListState = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
typealias ComposePeerListTransitionIntent = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListTransitionIntent
typealias ComposePeerListTransitionKind = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListTransitionKind
typealias ComposePeerListTransitionPlan = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListTransitionPlan
typealias ComposePeerTargetActions = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerTargetActions
typealias ComposePeerTargetCommand = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerTargetCommand
typealias ComposePeerTargetCommandKind = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerTargetCommandKind
typealias ComposePeerTargetControlPlan = com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerTargetControlPlan
typealias ComposeSelectedPeerQuickActionsState = com.shterneregen.securelan.desktop.compose.state.peer.ComposeSelectedPeerQuickActionsState

// Quick share state typealiases
typealias ComposeQuickShareRow = com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareRow
typealias ComposeQuickShareState = com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareState

// Steganography state typealiases
typealias ComposeSteganographyState = com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyState

// Transfer state typealiases
typealias ComposeAttachmentMenuLayoutContract = com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentMenuLayoutContract
typealias ComposeAttachmentToolItem = com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolItem
typealias ComposeAttachmentToolKind = com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolKind
typealias ComposeAttachmentToolsState = com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolsState
typealias ComposeChatAttachmentCard = com.shterneregen.securelan.desktop.compose.state.transfer.ComposeChatAttachmentCard
typealias ComposeFileTransferState = com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
typealias ComposeIncomingTransferPrompt = com.shterneregen.securelan.desktop.compose.state.transfer.ComposeIncomingTransferPrompt
typealias ComposeIncomingTransferPromptStatus = com.shterneregen.securelan.desktop.compose.state.transfer.ComposeIncomingTransferPromptStatus
typealias ComposeTransferRow = com.shterneregen.securelan.desktop.compose.state.transfer.ComposeTransferRow

// Chat state typealiases
typealias ComposeChatMessage = com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatMessage
typealias ComposeChatTranscriptLineKind = com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLineKind
typealias ComposeChatTranscriptLinePresentation = com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLinePresentation
typealias ComposeChatWorkspaceState = com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatWorkspaceState
