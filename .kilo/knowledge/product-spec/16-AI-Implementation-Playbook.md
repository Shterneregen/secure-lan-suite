
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 16 — AI Implementation Playbook

> Purpose
>
> This chapter defines the mandatory workflow every AI coding agent must follow
> before changing the UI. It replaces improvisation with a deterministic process.

---

# 1. Core Rule

Never start by writing code.

Always start by understanding the product problem.

Code is the final step, not the first.

---

# 2. Implementation Pipeline

Every UI task follows exactly this sequence:

1. Read the request.
2. Identify the user problem.
3. Identify the current application mode.
4. Identify the active context.
5. Read the relevant chapters of this specification.
6. Select an approved design pattern.
7. Apply the Decision Matrices.
8. Check the Forbidden Patterns catalog.
9. Build the UI from approved components.
10. Validate against Product QA.
11. Only then implement code.

Skipping any step is considered a specification violation.

---

# 3. Context Resolution Algorithm

Determine:

- Application mode
- Current screen
- Selected object
- Active activity
- Screen size
- User intent

If any item is unknown:

Do not guess.

Request clarification or preserve existing behaviour.

---

# 4. Screen Construction Order

Always compose screens in this order:

1. Workspace
2. Navigation
3. Primary content
4. Context assistant
5. Primary CTA
6. Secondary actions
7. Recovery UI
8. Diagnostics

Never begin with side panels or utilities.

---

# 5. Component Selection Rules

Only use components defined in Chapter 07.

If no suitable component exists:

- extend the specification
- do not invent a one-off component

---

# 6. Change Scope

Every implementation must declare:

- What changes
- What stays unchanged
- Which specification chapters are affected
- Which acceptance criteria are verified

Avoid unrelated UI refactoring.

---

# 7. When NOT to Change the UI

Do not redesign when the task is only:

- bug fixing
- performance optimisation
- accessibility correction
- localisation
- visual polish

Unless explicitly requested.

---

# 8. Legacy Protection

The legacy JavaFX UI is:

- behavioural reference
- migration reference

It is NOT:

- visual source of truth
- layout reference
- design target

Always prefer this specification.

---

# 9. Self Review

Before marking a task complete verify:

- [ ] User goal is preserved.
- [ ] Correct context selected.
- [ ] Approved pattern reused.
- [ ] Decision matrix followed.
- [ ] Forbidden patterns absent.
- [ ] Cognitive UX respected.
- [ ] Product QA passed.

If any answer is "No", continue refining.

---

# 10. Pull Request Checklist

Every UI PR should include:

- Summary
- User problem
- Implemented specification chapters
- Before screenshots
- After screenshots
- Accessibility impact
- Responsive verification
- Risks

---

# 11. Escalation Rules

Ask for clarification instead of guessing when:

- Multiple workflows conflict.
- Two chapters prescribe different behaviour.
- Product intent is unclear.
- A requested change violates the specification.

---

# 12. Success Criteria

A successful AI implementation:

- Feels native to SecureLanSuite.
- Requires no explanation.
- Introduces no new interaction model.
- Preserves communication-first philosophy.
- Can be justified by this specification.

---

# Chapter 16 Acceptance Criteria

This chapter is complete when two independent AI agents following this playbook produce substantially equivalent UI decisions for the same task.
