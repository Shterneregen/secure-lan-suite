# UI Checklist

Use this checklist for every user-facing Compose change. Apply items only to the
affected workflow and report any intentionally inapplicable section.

## Behavior and state

- [ ] The user goal, current state, and available capabilities are clear.
- [ ] Every visible action works in the current state; unavailable actions are hidden or disabled with useful context.
- [ ] Existing room, peer, draft, scroll, transfer, and call context is preserved where the workflow requires it.
- [ ] Relevant loading, empty, disabled, success, and error states are handled.
- [ ] Errors explain the problem in user language and provide a realistic recovery action.
- [ ] The change does not add navigation, shortcuts, settings, or capabilities that are not implemented.

## Product fit

- [ ] Communication flows keep the conversation and composer easy to find and use.
- [ ] Host and Join controls do not remain in the connected messenger workspace.
- [ ] The context area reflects the selected peer or current activity instead of listing unrelated tools.
- [ ] Ports, adapters, protocol details, and raw logs appear only in settings, advanced controls, or recovery details where they help.
- [ ] Quick Share explains its trusted-LAN and unauthenticated HTTP boundary when relevant.
- [ ] Experimental video is labelled honestly when relevant.

## Interaction and presentation

- [ ] Action hierarchy is clear without imposing an arbitrary one-action limit.
- [ ] Interactions provide visible feedback and do not reset unrelated state.
- [ ] Existing shell, components, semantic tokens, and `SecureLanIcons` are reused where appropriate.
- [ ] New visual constants or one-off components have a concrete reason.
- [ ] Empty states guide the next action instead of showing disabled tool collections.
- [ ] JavaFX is used only as a behavioral reference during migration or parity work.

Complete the accessibility checklist when interaction, focus, semantics, color,
or motion changes. Complete the responsive checklist when layout, visibility,
dialogs, or content sizing changes. Report focused test or runtime evidence.
