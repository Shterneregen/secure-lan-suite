# Accessibility Checklist

Use this checklist when a change affects interactive controls, focus, visual
states, animation, semantics, or user-facing status and error messages.

## Keyboard and focus

- [ ] The affected flow can be completed using only the keyboard.
- [ ] Focus order follows the logical reading and interaction order.
- [ ] Focus is visible in every supported theme and control state.
- [ ] Opening a menu, drawer, or dialog moves focus to a useful location.
- [ ] Closing a temporary surface returns focus to its trigger when practical.
- [ ] Escape closes the active dismissible surface where the product guide calls for it.

## Semantics and feedback

- [ ] Every icon-only control has a concise accessible name.
- [ ] Decorative icons are excluded from accessibility semantics when adjacent text already provides the label.
- [ ] Controls expose the correct role, enabled state, selected state, and value where applicable.
- [ ] Errors, progress, success, and selection are understandable without relying on color alone.
- [ ] Dynamic status changes have an accessible equivalent where the platform supports it.

## Visual and motion

- [ ] Text and essential UI indicators meet WCAG 2.2 AA contrast targets in the affected themes, with the measurement recorded when colors changed.
- [ ] Content remains understandable at the target OS display scaling and text size.
- [ ] Reduced-motion mode removes nonessential motion, and no information depends on animation.

## Evidence

- [ ] Automated checks, keyboard walkthroughs, and manual observations are reported.
- [ ] If screen-reader behavior is claimed or affected, the tested OS and screen reader are named; otherwise the limitation is stated.
