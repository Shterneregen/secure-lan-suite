# SecureLanSuite Minimal Kilo Code AI Kit

This `.kilo` kit routes UI work through one concise product guide:

1. Resolve the task type.
2. Read `.kilo/knowledge/PRODUCT-UI.md`.
3. Inspect the current implementation.
4. Follow the matching workflow and rules.
5. Implement or review.
6. Verify concrete behavior and report tradeoffs.

## How to install

Copy the contents of this folder into your repository `/` directory.

Recommended final structure:

```text
.kilo/
├── knowledge/
├── rules/
├── skills/
├── workflows/
├── prompts/
└── checklists/
```

## Main rule

Do not let the AI start from Compose code.

The AI must start from user intent, current product state, and verified behavior.

## Checklist routing

- Use `.kilo/checklists/ui.md` for every user-facing Compose change.
- Add `.kilo/checklists/accessibility.md` when interaction, focus, semantics, color, or motion changes.
- Add `.kilo/checklists/responsive.md` when layout, visibility, dialogs, or content sizing changes.
- Use `.kilo/checklists/review.md` to review the completed change and its evidence.
