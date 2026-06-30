
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 12 — Forbidden Patterns Catalog

> Purpose
>
> This chapter defines UI and UX patterns that are prohibited.
> AI agents and developers must actively detect and eliminate these patterns during implementation and review.

---

# How to Use This Catalog

Each anti-pattern contains:

- Description
- Symptoms
- Why it is harmful
- Correct alternative
- Automatic review rules

Any occurrence must be treated as design debt.

---

# FP-01 Dashboard Syndrome

## Description

The application resembles an administration dashboard instead of a messenger.

## Symptoms

- Multiple unrelated panels
- Many statistics
- Equal visual weight everywhere
- No obvious focal point

## Correct Alternative

Conversation becomes the visual hero.
Everything else becomes contextual.

Review Rule:

Reject if users need more than three seconds to identify the primary activity.

---

# FP-02 Engineering Leakage

## Description

Implementation concepts appear in normal workflows.

Examples:

- Port
- Adapter
- Runtime
- Listener
- Socket
- Discovery service

Correct Alternative:

Expose only business concepts.

Review Rule:

Engineering terminology is allowed only in Advanced and Diagnostics.

---

# FP-03 Multiple Primary CTAs

Symptoms

- Host Room
- Join Room
- Quick Share
- Diagnostics

all compete visually.

Correct Alternative

Exactly one primary action per context.

---

# FP-04 Configuration Before Communication

Description

Users configure networking before they can communicate.

Correct Alternative

Communicate first.
Configure only when necessary.

---

# FP-05 Permanent Toolbox

Description

Quick Share, Steganography, Audio Devices and Diagnostics remain permanently visible.

Correct Alternative

Reveal tools only when requested or contextually relevant.

---

# FP-06 Competing Visual Heroes

Description

Several areas demand equal attention.

Examples

Conversation

Transfer

Diagnostics

Call

Correct Alternative

Exactly one visual hero.

---

# FP-07 Scroll Before Communication

Description

Users must scroll before reaching the primary workflow.

Correct Alternative

Conversation and primary CTA fit into the initial viewport.

---

# FP-08 Legacy Recreation

Description

The Compose UI reproduces the old JavaFX layout instead of the product specification.

Correct Alternative

Follow this specification, not historical screenshots.

---

# FP-09 Empty Technical Panels

Description

Large panels display placeholders or disabled engineering controls.

Correct Alternative

Replace with guidance, contextual information or hide the panel entirely.

---

# FP-10 Feature-First Layout

Description

The layout is organised around features instead of user goals.

Correct Alternative

Organise the interface around user intent and current context.

---

# FP-11 Duplicate Actions

Symptoms

The same action appears in multiple regions.

Examples

- Invite
- Send File
- Settings

Correct Alternative

Each primary action has one canonical location.

---

# FP-12 Hidden Navigation Logic

Description

Users cannot predict where navigation will lead.

Correct Alternative

Navigation always preserves context and has an obvious return path.

---

# Automatic Detection Checklist

Reject a screen if:

- [ ] More than one visual hero exists.
- [ ] More than one primary CTA exists.
- [ ] Chat is not the dominant region.
- [ ] Engineering terminology appears outside Advanced/Diagnostics.
- [ ] The right panel behaves like a toolbox.
- [ ] Communication requires scrolling.
- [ ] Users must configure before communicating.
- [ ] Empty states lack guidance.
- [ ] Legacy JavaFX composition is reproduced.
- [ ] Navigation breaks context.

---

# AI Review Prompt

Before completing any UI implementation ask:

1. Does this resemble a modern messenger?
2. Does this resemble an administration tool?
3. What unnecessary UI can be removed?
4. Which panel can be hidden until needed?
5. Is communication still the obvious purpose?
6. Am I preserving the specification instead of the legacy implementation?

If any answer indicates regression, redesign the screen before submitting.

---

# Chapter 12 Acceptance Criteria

The chapter is complete when reviewers, developers and AI agents can identify forbidden patterns objectively and correct them using the prescribed alternatives.
