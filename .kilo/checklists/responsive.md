# Responsive Checklist

Use this checklist when a change affects layout, visibility, columns, dialogs,
drawers, the composer, or content that can grow horizontally or vertically.
Workspace thresholds are expressed in Compose `dp`, not physical pixels.

## Workspace widths

- [ ] The default window size, `1360 x 860 dp`, is usable.
- [ ] `1599/1600 dp` verifies the transition between collapsed-secondary and full context modes.
- [ ] `1439/1440 dp` verifies the transition between collapsed-history and collapsed-secondary modes.
- [ ] `1279/1280 dp` verifies the transition between drawer and inline context modes.
- [ ] The smallest window size reachable through current window constraints remains operable.

If the shell and context area are unaffected, test the default size and the
smallest relevant size instead of repeating every breakpoint.

## Layout behavior

- [ ] Conversation content and the composer remain visible and usable before secondary context is given space.
- [ ] Content does not clip, overlap, or introduce unintended horizontal scrolling.
- [ ] Long names, translated text, errors, and empty states wrap or truncate intentionally.
- [ ] The context entry control, drawer content, close behavior, and focus return work below `1280 dp`.
- [ ] Resizing preserves the room, selected peer, draft, transfer, call, and open context where applicable.
- [ ] Dialogs fit the available display or provide intentional scrolling at the tested height.
- [ ] Relevant OS display-scaling settings are reported for screenshot or runtime validation.

## Evidence

- [ ] Tested sizes, scaling, observed layout mode, and any untested boundary are reported.
- [ ] Screenshots are captured when layout behavior is material to the change.
