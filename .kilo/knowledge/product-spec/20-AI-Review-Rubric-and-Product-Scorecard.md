
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 20 — AI Review Rubric & Product Scorecard

> Purpose
>
> This chapter defines an objective review framework for every UI implementation.
> All reviewers—human or AI—should reach substantially the same conclusion when
> evaluating the same change.

---

# 1. Review Pipeline

Every review follows this order:

1. Functional correctness
2. Product compliance
3. UX compliance
4. Visual consistency
5. Accessibility
6. Responsiveness
7. Cognitive load
8. Performance
9. Final score

Do not skip stages.

---

# 2. Product Scorecard

| Category | Max |
|----------|----:|
| Product Vision | 10 |
| User Intent | 10 |
| Information Architecture | 10 |
| Navigation | 10 |
| Visual Hierarchy | 10 |
| Context Awareness | 10 |
| Design System Compliance | 10 |
| Cognitive UX | 10 |
| Accessibility | 10 |
| Implementation Quality | 10 |

Maximum Score: **100**

---

# 3. Scoring Rules

## 95–100

Excellent

Ready for merge.

## 90–94

Minor improvements recommended.

## 85–89

Needs refinement before merge.

## 70–84

Major UX review required.

## Below 70

Reject.

---

# 4. Category Evaluation

## Product Vision

Verify:

- Communication-first
- Calm interface
- Technical complexity hidden
- Messenger-first experience

Automatic fail if the UI resembles an administration tool.

---

## Information Architecture

Verify:

- Correct application mode
- Correct state
- Correct blueprint
- Clear hierarchy

Penalty:

- Mixed workflows
- Duplicate navigation
- Broken context

---

## Navigation

Verify:

- Predictable flow
- Back navigation
- Keyboard support
- Command Palette integration

---

## Context Awareness

Verify:

- Only relevant panels visible
- Correct context assistant
- Correct primary CTA

Penalty:

- Unrelated UI
- Competing contexts

---

## Visual Hierarchy

Verify:

- One visual hero
- Conversation dominance
- Balanced whitespace
- Correct spacing rhythm

Automatic fail:

Multiple visual heroes.

---

## Design System Compliance

Verify:

- Approved components
- Approved patterns
- Semantic tokens
- Consistent interaction

Penalty:

Custom one-off components.

---

## Cognitive UX

Verify:

- One primary goal
- Low cognitive load
- Recognition over recall
- Stable layout

Penalty:

Configuration before communication.

---

## Accessibility

Verify:

- Keyboard navigation
- Visible focus
- Screen reader support
- High contrast
- Responsive scaling

---

## Implementation Quality

Verify:

- Clean implementation
- No specification violations
- Maintainability
- Feature boundaries respected

---

# 5. Automatic Reject Conditions

Reject immediately if:

- Dashboard syndrome detected
- Engineering leakage present
- More than one primary CTA
- More than one visual hero
- Chat is no longer dominant
- Legacy JavaFX layout recreated
- Context is lost
- Communication blocked by configuration

---

# 6. Evidence Requirements

Every UI pull request must include:

- Before screenshots
- After screenshots
- Responsive screenshots
- Chapters implemented
- Accessibility notes
- Self-review score

---

# 7. AI Self-Assessment

Before completion answer:

- What user problem was solved?
- Which product chapter guided the solution?
- Which approved pattern was used?
- Which forbidden patterns were checked?
- What is the final score?

---

# 8. Definition of Done

A UI implementation is complete only if:

- Product Score >= 95
- Zero automatic reject conditions
- Product State Machine preserved
- Canonical Blueprint followed
- Approved Design Pattern used
- Product QA passed
- Accessibility verified
- Responsive behavior verified

---

# Chapter 20 Acceptance Criteria

The chapter is complete when independent reviewers consistently produce nearly identical
scores for the same implementation and the score reliably predicts overall UI quality.
