---
name: review-pr
description: >
  Review a SecureLanSuite pull request for correctness, compatibility, and
  regressions. Use for repository code review; use review-ui when the requested
  scope is specifically product UI quality.
license: MIT
---

# Review Pull Request

1. Inspect the complete diff and the current code it changes.
2. Check `AGENTS.md`, relevant domain skills, module boundaries,
   compatibility, security, and documentation.
3. Apply `docs/product-ui.md` and the applicable `docs/quality/` checklists only
   when user-facing behavior is affected.
4. Evaluate tests and runtime evidence; run focused checks when practical.
5. Report only actionable findings caused or exposed by the change, ordered by
   severity and supported by file-level evidence.
6. State verified behavior, missing evidence, material risks, and the merge
   recommendation.
