# Compose Multiplatform UI Migration Rules

Use these rules for JavaFX-to-Compose Multiplatform Desktop migration.

## Core rule

Do not invent a new UI. Recreate the existing JavaFX screen structure, visual hierarchy, spacing, behavior, and desktop UX unless the user explicitly asks for redesign.

## Required workflow

For every screen:
1. Read the existing JavaFX code, FXML, CSS, resources, and screenshots.
2. Summarize the current screen structure.
3. Identify reusable UI components.
4. Map JavaFX controls to Compose components.
5. Implement the Compose version.
6. Compare the result against JavaFX.
7. Refactor into smaller composables.

## JavaFX to Compose mapping

- VBox -> Column
- HBox -> Row
- StackPane -> Box
- BorderPane -> Scaffold-like shell using Row/Column/Box
- SplitPane -> custom resizable/split layout
- ListView -> LazyColumn
- TableView -> custom desktop table or LazyColumn rows
- Label -> Text
- Button -> Button / FilledTonalButton / IconButton
- TextField -> OutlinedTextField or project text field
- CSS variables/classes -> Compose theme tokens and modifiers
- Controller mutable state -> explicit immutable UiState + callbacks

## UI quality rules

- Use project design tokens for spacing, shapes, colors, and typography.
- Prefer compact desktop density over mobile spacing.
- Preserve toolbars, sidebars, status bars, panels, action groups, and dialogs.
- Preserve disabled, loading, error, empty, hover, and selected states.
- Do not use random colors or arbitrary dp values.
- Do not make all buttons huge.
- Do not put all UI into a single composable.
- Do not put business logic in composables.
