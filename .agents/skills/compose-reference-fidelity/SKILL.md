---
name: compose-reference-fidelity
description: >
  Implement or refine Kotlin Compose UI from a supplied mockup or screenshot,
  matching layout, density, typography and visual hierarchy through rendered
  comparisons. Use for requests such as "make it like this" or "closer to the
  mockup". Do not use for UI work without a visual reference or for read-only review.
---

# Compose Reference Fidelity

Turn the user's reference into the actual Compose surface, then verify the
rendered result. Passing interaction tests proves behavior; visual fidelity
requires inspecting captures of the implementation.

## Establish the target

- View the supplied images at their original resolution. Distinguish the intended
  mockup from screenshots of the current implementation. When a user supplies the
  current UI while asking to get closer to an earlier mockup, compare both; do not
  promote the current screenshot into the design target.
- Read repository instructions, relevant product guidance and the affected
  composables, tokens, state owners and tests. Follow existing UI skills when
  available; this skill supplies reference comparison, not a new product direction.
- Identify which details the user actually wants reproduced. Preserve existing
  operation/state contracts unless behavior changes are requested. An illustrated
  control is not authorization to invent networking, data or navigation semantics.
- If a required reference is inaccessible, request that image again. Continue
  independent inspection, but do not invent its appearance.

## Write a compact visual contract before editing

Record the important reference-to-code mappings in a short working table:

| Region | Reference constraint | Implementation / verification |
| --- | --- | --- |
| Container | Usable content area and edge padding | Window/scene size, density, outer insets |
| Header and tabs | Height, title scale, baseline and selection treatment | Typography and token mapping |
| Form groups | Column ratios, card header layout, gaps and alignment | Width constraints and breakpoint |
| Fields | Visible height, label placement and inner padding | Actual input component and measured bounds |
| Actions | Primary/secondary treatment and location | Real callbacks, footer visibility |

Include only relevant regions. Extract proportions and repeated spacing from
the image instead of guessing each value independently. Record uncertainties
as estimates rather than claiming exact measurements.

Compare app content with app content. Separate native title bars and shadows
from Compose bounds. A screenshot's physical pixels are not necessarily dp:
account for OS scaling, test density and capture size. If density is unknown,
use relative proportions and known controls; state the uncertainty.

## Implement the visual contract

- Start with geometry: content width, group heights, column ratios, alignment and
  action placement. Then refine typography, fields, icons, borders and colors.
  Reproducing only the cards and labels is not sufficient.
- Match density deliberately. Material defaults can make a compact reference
  substantially taller. Inspect real field min-height, label space, content
  padding and line height. Use an appropriate existing compact component or a
  focused local implementation; preserve text entry, focus, semantics and error
  visibility. Do not reduce height with clipping or tiny text.
- If the reference shows footer actions without scrolling at its target size,
  keep them visible at the comparable size. Adding scroll reachability alone
  does not satisfy that visual requirement. Narrow layouts may intentionally
  stack and scroll while retaining every action.
- Match header rows as rows: an inline subtitle, right-aligned action or fixed
  label must not silently become an extra full-width row that increases height.
  Use matching constraints rather than arbitrary spacers to align controls.
- Reuse semantic theme tokens and components. A shared component that cannot
  express the reference may be extended narrowly or replaced locally. Do not
  let an ill-fitting abstraction determine the layout, or restyle the whole app.
- Use semantically correct icons from the project's icon system. Do not silently
  substitute a document icon for DNS, a plug for a monitor, or a settings gear
  for a filter. If the matching symbol is missing, prefer a focused vector in
  that system when permitted; otherwise disclose the specific deviation.
- Keep existing themes functional. Match the reference theme closely and verify
  other supported themes without hardcoded colors or arbitrary global changes.
- Treat factual status and validation as real state. A mockup's success message
  or sample network data is not production data. Preserve unavailable, partial,
  failed, cancelled, empty and historical states.
- Preserve established return/cancel/dismiss semantics. Do not introduce data
  loss, duplicate actions or extra permanent explanatory paragraphs merely to
  fill the mockup. If a new action's semantics matter and cannot be inferred,
  clarify that behavior while continuing independent visual work.

## Render, compare, correct

1. Capture the real production composables at a viewport and density comparable
   to the reference, with the matching theme, input values, focus and operation
   state. Use deterministic fixtures in tests; never fake production inventory.
2. Verify the capture dimensions and bounds. In Compose Desktop, `requiredSize`
   can exceed the test scene and produce cropped screenshots. Set the scene or
   window viewport as well; do not mistake a requested layout size for evidence
   that it was rendered completely.
3. Open the resulting image. Compare reference and implementation region by
   region: edge insets, header/tab height, card boundaries, field height, title
   scale, line wrapping, icon meaning, button prominence and footer position.
   Compare the whole surface as well as local detail.
4. Fix material mismatches and capture again. Prioritize the differences that
   change density and hierarchy before polishing minor decoration. Do not stop
   solely because compilation or interaction tests pass.
5. Check the smallest supported size and a useful intermediate size. Inspect
   long names, validation errors and expanded controls where layout changes.
   Ensure intentional vertical scrolling and no hidden or overlapping controls.
6. Run focused behavioral tests and required lint/build gates. Add regression
   checks for defects actually found, such as lost focus, overlapping metadata,
   unreachable actions or wrong picker values. Do not assert exact screenshots
   or implementation constants unless the repository already uses that contract.

Use existing capture/comparison tools. Do not add image libraries, generate UI
assets or introduce screenshot infrastructure unless the task needs them.
Generated mockups are not evidence of what the application renders.

## Completion evidence

Report what changed, actual capture paths, tested viewport/density/theme and
checks run. List remaining visual deviations with their concrete cause. Distinguish
intentional responsive adaptations from missing fidelity. Do not claim an exact
match or complete accessibility validation without supporting evidence.

If rendering is unavailable, complete code and deterministic checks that are
possible, identify the rendering limitation, and mark visual comparison unverified.
Do not invent screenshots or claim visual acceptance from source inspection.
