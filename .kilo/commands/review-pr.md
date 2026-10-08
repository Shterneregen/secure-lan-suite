---
description: Review a pull request for correctness, compatibility, and regressions
agent: code
---

# Review Pull Request

Task context: $ARGUMENTS

1. Inspect the complete diff and the current code it changes.
2. Check `.kilo/rules/rules.md`, relevant domain skills, module boundaries,
   compatibility, security, and documentation.
3. Apply Product UI guidance and UI checklists only when user-facing behavior is
   affected.
4. Evaluate tests and runtime evidence; run focused checks when practical.
5. Report only actionable findings caused or exposed by the change, ordered by
   severity and supported by file-level evidence.
6. State verified behavior, missing evidence, material risks, and the merge
   recommendation.
