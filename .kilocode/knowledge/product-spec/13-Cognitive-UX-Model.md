
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 13 — Cognitive UX Model

> Purpose
>
> This chapter defines how the interface should match human perception,
> attention and decision-making. It is the cognitive foundation behind all
> layout, navigation and interaction decisions.

---

# 1. Core Principle

Users should think about communicating.

They should **not** think about how the application works.

Every UI decision must reduce cognitive effort.

---

# 2. Cognitive Budget

A screen has a limited decision budget.

Maximum simultaneously visible:

- 1 primary goal
- 1 primary CTA
- 1 visual hero
- 3–5 related actions
- 6 context cards

If a screen exceeds these limits, simplify it.

---

# 3. Attention Model

Visual attention follows this priority:

1. Primary content
2. Current task
3. Current person
4. Next action
5. Context
6. Supporting information

Diagnostics, settings and engineering details are always last.

---

# 4. Scanning Behaviour

Design for natural desktop scanning.

Eye flow:

Header

↓

Conversation header

↓

Conversation

↓

Composer

↓

Context panel (only if needed)

Never force users to search for the conversation.

---

# 5. Recognition over Recall

Users should recognize actions instead of remembering them.

Rules:

- Keep actions in consistent locations.
- Preserve icon meaning.
- Preserve terminology.
- Do not move primary actions between contexts without reason.

---

# 6. Cognitive Load Reduction

Prefer:

- Progressive disclosure
- Context-sensitive controls
- Meaningful defaults
- Clear grouping

Avoid:

- Simultaneous configuration
- Large forms
- Long technical text
- Repeated choices

---

# 7. Trust Model

Trust increases when:

- The interface is predictable.
- Feedback is immediate.
- Errors are recoverable.
- Security is understandable.
- Layout remains stable.

Trust decreases when:

- UI changes unexpectedly.
- Controls move.
- Technical jargon appears.
- Users lose context.

---

# 8. Error Prevention

Prevent mistakes before explaining them.

Examples:

- Disable impossible actions.
- Validate early.
- Explain consequences before destructive actions.
- Offer recovery instead of blame.

---

# 9. Calm Interface Model

A calm interface has:

- Stable layout
- Consistent spacing
- Few competing accents
- Moderate animation
- Clear hierarchy
- Predictable behaviour

Visual silence is a feature.

---

# 10. Decision Friction

Every additional decision has a cost.

Reduce decisions by:

- Choosing sensible defaults
- Remembering previous choices
- Hiding advanced options
- Automating safe behaviour

---

# 11. Mental Model

Users think in this order:

1. Where am I?
2. Who am I talking to?
3. What is happening?
4. What should I do next?

The UI must answer all four questions immediately.

---

# 12. Confidence Indicators

Use subtle indicators of confidence:

- Online status
- Secure connection badge
- Transfer progress
- Call quality
- Last activity

Do not overload the interface with status labels.

---

# 13. Cognitive Anti-Patterns

Reject designs that:

- Require reading before acting
- Expose implementation concepts
- Present multiple competing goals
- Hide the next action
- Frequently rearrange controls
- Interrupt communication with configuration

---

# 14. LLM Cognitive Review

Before approving a screen verify:

- [ ] Is the user's next decision obvious?
- [ ] Is unnecessary thinking removed?
- [ ] Can the screen be understood in under 5 seconds?
- [ ] Does visual hierarchy match attention hierarchy?
- [ ] Does the interface feel calm?
- [ ] Are advanced concepts hidden?
- [ ] Does the layout build trust?

Reject the design if any answer is "No".

---

# Chapter 13 Acceptance Criteria

The chapter is complete when every UI decision can be justified by reducing
cognitive load, increasing confidence or improving communication rather than
adding functionality.
