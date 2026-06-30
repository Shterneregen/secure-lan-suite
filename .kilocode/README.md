# SecureLanSuite Minimal Kilo Code AI Kit

This `.kilocode` kit is intentionally compact.

It does not duplicate the whole Product Specification inside rules and skills.
Instead, it works as an AI router:

1. Resolve the task type.
2. Read `knowledge/INDEX.md`.
3. Read only the relevant Product Specification chapters.
4. Follow the matching workflow.
5. Apply the rules.
6. Implement or review.
7. Score the result using the Product Scorecard.

## How to install

Copy the contents of this folder into your repository `.kilocode/` directory.

Recommended final structure:

```text
.kilocode/
├── knowledge/
├── rules/
├── skills/
├── workflows/
├── prompts/
└── checklists/
```

## Main rule

Do not let the AI start from Compose code.

The AI must start from product state, user intent, blueprint, approved pattern, and QA rubric.
