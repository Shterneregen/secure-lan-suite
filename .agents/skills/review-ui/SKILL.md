---
name: review-ui
license: MIT
description: >
  Review SecureLanSuite UI implementations, screenshots, or pull requests for
  product, accessibility, and responsive regressions. Use for read-only product
  QA; implement fixes only when the user explicitly requests them.
---

# SecureLanSuite UI Review

1. Establish the requested behavior and review scope.
2. Read the relevant parts of `docs/product-ui.md`.
3. Inspect the complete diff and current implementation it changes.
4. Compare behavior before and after the change.
5. Apply `docs/quality/ui-review-checklist.md` and inspect relevant runtime or
   visual evidence.
6. Separate verified evidence from assumptions and missing evidence.

Report only actionable findings caused or exposed by the reviewed change. Each
finding must include severity, affected surface, concrete trigger, user impact,
evidence, and required correction. Order findings by severity and omit personal
style preferences.

Finish with verified behavior, validation evidence, material limitations, and
an `Accept`, `Needs Refinement`, or `Reject` recommendation.
