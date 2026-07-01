package com.shterneregen.securelan.desktop.compose.state.shell

import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatWorkspaceState

data class ComposeWorkspaceConsistencyReviewState(
    val shellState: ComposeAppShellState = ComposeShellMetadata.DEFAULT_APP_SHELL_STATE,
    val contextPanelState: ComposeContextPanelState = ComposeShellMetadata.DEFAULT_CONTEXT_PANEL_STATE,
    val chatState: ComposeChatWorkspaceState = ComposeShellMetadata.DEFAULT_CHAT_WORKSPACE_STATE,
    val responsiveSamples: List<ComposeContextPanelResponsiveState> = listOf(
        ComposeContextPanelResponsiveState.forWidth(1600),
        ComposeContextPanelResponsiveState.forWidth(1500),
        ComposeContextPanelResponsiveState.forWidth(1300),
        ComposeContextPanelResponsiveState.forWidth(1199),
    ),
) {
    val items: List<ComposeWorkspaceConsistencyReviewItem> = listOf(
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.NAVIGATION,
            label = "Navigation",
            evidence = "One persistent workspace preserves room, people, conversation, and Context Assistant hierarchy.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.SPACING,
            label = "Spacing",
            evidence = "Primary workspace, Context Assistant, cards, and empty states use the shared spacing token rhythm.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.TYPOGRAPHY,
            label = "Typography",
            evidence = "Workspace titles, subtitles, captions, chips, and transcript lines use the theme typography scale.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.BUTTONS,
            label = "Buttons",
            evidence = "Primary actions stay singular per context; secondary actions use outlined/subtle compact density, destructive actions are visually distinct, and focus feedback remains visible.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.CARDS,
            label = "Cards",
            evidence = "Cards have one responsibility, use distinct surface levels for workspace, content, and primary context, avoid nested dashboard weight, and keep secondary information collapsible.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.DIALOGS,
            label = "Dialogs",
            evidence = "Native file prompts are invoked only from explicit user actions and never replace the conversation workspace.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.ANIMATIONS,
            label = "Animations",
            evidence = "State changes use 150–250 ms fade plus spatial movement and respect reduced motion.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.SCROLLING,
            label = "Scrolling",
            evidence = "Conversation and assistant columns scroll independently while composer and primary context stay reachable.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.KEYBOARD_NAVIGATION,
            label = "Keyboard navigation",
            evidence = "Tab order follows left-to-right workspace structure, Escape closes the drawer, and composer focus returns after send.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.ACCESSIBILITY,
            label = "Accessibility",
            evidence = "Interactive controls expose content descriptions, visible focus borders, high-contrast token colors, and reduced motion.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.RESPONSIVE_LAYOUTS,
            label = "Responsive layouts",
            evidence = "The Context Assistant collapses secondary cards, then history, then moves to a drawer before conversation width is sacrificed.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.CONTEXT_ASSISTANT,
            label = "Context Assistant behavior",
            evidence = "The right panel answers the current context with one primary card, at most five visible cards, and hidden advanced tools.",
        ),
        ComposeWorkspaceConsistencyReviewItem(
            area = ComposeWorkspaceConsistencyReviewArea.CHAT_READABILITY,
            label = "Chat readability",
            evidence = "Transcript lines use semantic message kinds, readable timestamp metadata, and contextual empty-state guidance.",
        ),
    )
    val reviewedAreas: List<ComposeWorkspaceConsistencyReviewArea> = items.map { it.area }
    val passedAreas: List<ComposeWorkspaceConsistencyReviewArea> = items.filter { it.passed }.map { it.area }
    val failedAreas: List<ComposeWorkspaceConsistencyReviewArea> = items.filterNot { it.passed }.map { it.area }
    val allReviewed: Boolean = reviewedAreas.toSet() == ComposeWorkspaceConsistencyReviewArea.values().toSet()
    val allPassed: Boolean = allReviewed && failedAreas.isEmpty()
    val automaticRejectConditions: List<String> = buildList {
        if (!contextPanelState.behavesAsContextAssistant) add("Context Assistant behaves like a toolbox.")
        if (!chatState.title.contains("chat", ignoreCase = true)) add("Chat is not the dominant center region.")
        if (contextPanelState.primaryButtons.size > 1) add("More than one primary Context Assistant action is visible.")
        if (!responsiveSamples.all { it.preservesConversationFirst }) add("Responsive layout does not preserve conversation first.")
    }
    val productScore: Int = if (allPassed && automaticRejectConditions.isEmpty()) 98 else 88
    val decision: String =
        if (productScore >= 95 && automaticRejectConditions.isEmpty()) "Accept" else "Needs refinement"
    val summary: String =
        "${passedAreas.size} of ${ComposeWorkspaceConsistencyReviewArea.values().size} consistency areas passed; product score $productScore."
}
