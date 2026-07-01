
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 10 — Product QA & LLM Implementation Rules

> Purpose
>
> This chapter defines how every implementation is evaluated.
> It is the contract between Product Design, Engineering and AI agents.

---

# 1. Golden Rule

Every implementation must improve communication.

If a change makes communication harder, reject it regardless of technical quality.

---

# 2. Product Quality Gates

A screen is accepted only if all gates pass.

## Gate 1 — Intent

The user's next action is obvious within 3 seconds.

## Gate 2 — Focus

Only one visual hero exists.

## Gate 3 — Context

Only information relevant to the current task is visible.

## Gate 4 — Simplicity

Engineering concepts remain hidden from normal users.

## Gate 5 — Consistency

The screen follows the Design System.

---

# 3. LLM Decision Framework

Before generating UI, determine:

1. Application Mode
2. Current User Goal
3. Selected Object
4. Current Context
5. Primary CTA

Only then compose the screen.

Never start by placing widgets.

---

# 4. Implementation Priority

Always optimise in this order:

1. User understanding
2. Communication flow
3. Information hierarchy
4. Interaction quality
5. Visual polish

Never reverse this order.

---

# 5. Guardrails

LLM must NOT:

- recreate legacy JavaFX layouts
- expose ports or IPs in normal UI
- create dashboards
- duplicate actions
- invent new navigation patterns
- add permanent tool panels
- introduce engineering terminology outside Advanced/Diagnostics

---

# 6. PR Review Checklist

Every Pull Request must answer:

- What user problem does this solve?
- Which chapter(s) of the specification does it implement?
- Which acceptance criteria were verified?
- Which screenshots demonstrate the improvement?

Reject PRs without before/after evidence.

---

# 7. Visual Regression Rules

Reject if:

- Chat loses visual dominance.
- Right panel becomes heavier than conversation.
- Additional scrolling is introduced.
- New borders increase clutter.
- Multiple primary CTAs appear.

---

# 8. UX Regression Rules

Reject if users must:

- configure before communicating
- read documentation
- interpret engineering language
- search for the primary action

---

# 9. Self-Review Protocol for AI

Before completing a task the AI must verify:

- [ ] I identified the correct user context.
- [ ] I implemented only relevant UI.
- [ ] I removed unnecessary technical details.
- [ ] I preserved communication flow.
- [ ] I followed the Design System.
- [ ] I did not invent new interaction patterns.
- [ ] I can explain every visible component.

If any answer is "No", revise before submitting.

---

# 10. Acceptance Matrix

A feature is complete only when all are true:

- Functional behaviour works.
- UX matches this specification.
- Accessibility remains intact.
- Responsive behaviour is verified.
- Keyboard support is verified.
- Screenshots confirm visual quality.

---

# 11. Success Metrics

The specification succeeds when:

- New contributors understand the product without legacy UI.
- AI agents produce consistent layouts.
- UI reviews focus on product decisions rather than fixing random layouts.
- Communication remains the center of every implementation.

---

# Chapter 10 Acceptance Criteria

This chapter is complete when Product Designers, Developers and AI agents can evaluate the same implementation using identical criteria and reach the same conclusion.
