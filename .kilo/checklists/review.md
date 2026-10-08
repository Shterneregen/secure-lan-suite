# Review Checklist

Use this checklist to review a completed UI change. It aggregates the product,
engineering, accessibility, and responsive checks without duplicating them.

## Scope and correctness

- [ ] The implemented behavior matches the request and current product capabilities.
- [ ] The relevant sections of `.kilo/knowledge/PRODUCT-UI.md` were identified.
- [ ] Existing service boundaries, protocols, and unrelated behavior remain intact.
- [ ] State restoration and failure recovery were checked for the affected workflow.
- [ ] No unrelated redesign, dependency, or feature expansion was introduced.

## Evidence and decision

- [ ] The applicable UI, accessibility, responsive, and engineering checks were completed.
- [ ] Tests, build commands, and runtime checks are listed with their results or an explicit reason for omission.
- [ ] Screenshots are included when visual comparison or responsive behavior is material.
- [ ] Findings cite concrete files, states, dimensions, or reproduction steps.
- [ ] Remaining limitations, unverified claims, and deliberate tradeoffs are stated.
- [ ] The final decision is `Accept`, `Needs Refinement`, or `Reject`, supported by the findings.
