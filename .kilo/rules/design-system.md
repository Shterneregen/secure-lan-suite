# Design System Rule

Use semantic design tokens and approved components.

Do not hard-code arbitrary colors, spacing, radius, elevation, typography, or icons.

Use meaning-based roles:

- Surface
- Surface Elevated
- Primary Accent
- Success
- Warning
- Critical
- Metadata
- Divider

Every component must have one responsibility.

## Icon System

All product icons must come from `SecureLanIcons`.

Rules:

- One icon represents one product concept everywhere.
- Icons support labels; they never replace primary-action text.
- Decorative icons use `contentDescription = null` because the adjacent label is sufficient.
- Do not import Material icons directly into composables; route every icon through `SecureLanIcons`.
- When a new product concept needs an icon, add it to `SecureLanIcons` before using it in UI.
- Icon tint follows semantic color roles (text secondary, primary accent, success, etc.), never arbitrary colors.
