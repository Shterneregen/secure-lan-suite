# Compose Engineering Checklists

## Implementation Checklist

1. [ ] Feature specification or task scope is clear.
2. [ ] Existing architecture pattern is identified.
3. [ ] Screen state is explicit and immutable.
4. [ ] Composables are mostly stateless.
5. [ ] Business logic is outside composables.
6. [ ] UI-local state is only visual.
7. [ ] Loading, empty, error, disabled and success states exist.
8. [ ] Keyboard/focus behavior is preserved.
9. [ ] Accessibility semantics are present.
10. [ ] Validation commands were run or skipped with reason.

## Compose Desktop Checklist

1. [ ] Layout is desktop-first.
2. [ ] Density is compact but readable.
3. [ ] Resize behavior is stable.
4. [ ] Hover states exist where useful.
5. [ ] Focus states are visible.
6. [ ] Controls are not oversized.
7. [ ] No Android-looking mobile shell unless requested.

## State Checklist

1. [ ] Raw editable input is preserved separately from parsed value.
2. [ ] Derived values are not stored redundantly.
3. [ ] Effects are not encoded as consume-once booleans.
4. [ ] Platform objects are not stored in screen state.
5. [ ] Stable list keys are used.

## Performance Checklist

1. [ ] No heavy work in composables.
2. [ ] Lazy lists use stable keys.
3. [ ] State reads are as narrow as practical.
4. [ ] Old content remains during refresh where possible.
5. [ ] No unnecessary full-screen spinners.

## Accessibility Checklist

1. [ ] Keyboard navigation works.
2. [ ] Focus is visible.
3. [ ] Semantics describe controls.
4. [ ] Touch/click targets are reasonable.
5. [ ] Contrast is adequate.
6. [ ] Reduced motion is respected where animations exist.

## Review Checklist

1. [ ] Implementation matches spec.
2. [ ] Scope was not expanded.
3. [ ] Architecture convention was preserved.
4. [ ] No business logic in composables.
5. [ ] No random visual constants.
6. [ ] No unnecessary dependency added.
7. [ ] Tests/validation are reported.
