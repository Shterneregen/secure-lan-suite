
# SecureLanSuite Desktop Product Specification
## Version 1.0

# Chapter 14 — Visual Language & Design Tokens

> Purpose
>
> This chapter defines the visual grammar of SecureLanSuite.
> Colors, typography, spacing and elevation are described by semantic meaning,
> not by arbitrary values. The goal is to create a coherent visual language that
> AI agents can apply consistently across every screen.

---

# 1. Visual Language Principles

The interface must feel:

- Calm
- Precise
- Trustworthy
- Spacious
- Modern

The visual system communicates hierarchy before decoration.

---

# 2. Semantic Color Roles

Never refer to colors by hex values inside specifications.

Use semantic roles:

- Surface
- Surface Elevated
- Surface Floating
- Primary Accent
- Secondary Accent
- Success
- Warning
- Critical
- Information
- Disabled
- Outline
- Divider

Rule:

Meaning stays constant even if the theme changes.

---

# 3. Surface Hierarchy

Surface levels communicate depth.

Level 0
Application background.

Level 1
Primary workspace.

Level 2
Cards.

Level 3
Floating panels.

Level 4
Dialogs.

Higher surfaces must become rarer, not more common.

Avoid stacking more than three elevation levels.

---

# 4. Typography Hierarchy

Typography communicates importance.

Roles:

- Display
- Screen Title
- Section Title
- Card Title
- Body
- Secondary Text
- Caption
- Metadata

Rules:

- Never use size alone to indicate hierarchy.
- Weight, spacing and position work together.
- Metadata should never compete with conversation text.

---

# 5. Spacing Grammar

Spacing communicates relationships.

Recommended scale:

4, 8, 12, 16, 24, 32, 40, 48

Rules:

- Same concept → smaller spacing.
- Different concept → larger spacing.
- Replace borders with spacing whenever possible.

---

# 6. Corner Radius Semantics

Radius indicates friendliness and interaction.

Small:
Compact controls.

Medium:
Cards and inputs.

Large:
Dialogs and onboarding.

Never mix unrelated radius values on one screen.

---

# 7. Elevation Rules

Elevation indicates interaction, not decoration.

Use elevation for:

- Floating panels
- Dialogs
- Context menus

Do not elevate permanent layout regions.

---

# 8. Icon Semantics

Icons support labels.

Rules:

- Icons never replace clear text for primary actions.
- One icon represents one concept everywhere.
- Decorative icons are prohibited.

---

# 9. Density Modes

Support two density modes:

Comfortable

- Default
- Spacious
- Reading-focused

Compact

- High information density
- Keyboard-first workflows

Switching density must never change navigation.

---

# 10. Contrast Budget

Reserve strong contrast for:

- Current context
- Primary CTA
- Errors requiring attention

Avoid multiple competing accent colors.

The conversation should remain visually dominant.

---

# 11. Visual Rhythm

The interface should establish a repeatable rhythm.

Pattern:

Heading

↓

Content

↓

Action

↓

Whitespace

Repeat this rhythm consistently across cards and screens.

---

# 12. Token Naming Strategy

Use semantic names instead of implementation names.

Good:

SurfacePrimary

ConversationBackground

PeerOnline

TransferSuccess

ContextDivider

Avoid:

Blue500

Gray200

Radius12

Padding16

Tokens should describe purpose, not implementation.

---

# 13. Theme Invariants

Changing the theme must never change:

- Information hierarchy
- Interaction hierarchy
- Accessibility
- Component behaviour

Only appearance changes.

Meaning remains constant.

---

# 14. AI Visual Review Checklist

Before approving a screen verify:

- [ ] Semantic colors are used.
- [ ] Surface hierarchy is clear.
- [ ] Typography follows defined roles.
- [ ] Spacing expresses relationships.
- [ ] Elevation is meaningful.
- [ ] Accent color highlights only primary intent.
- [ ] Conversation remains visually dominant.
- [ ] Components share one visual language.

Reject the design if any answer is "No".

---

# Chapter 14 Acceptance Criteria

The chapter is complete when:

- Designers can implement light and dark themes without rewriting UX.
- Developers can map design tokens to any UI framework.
- AI agents produce visually consistent interfaces without relying on hard-coded colors or dimensions.
