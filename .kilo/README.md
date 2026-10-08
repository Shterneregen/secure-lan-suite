# SecureLanSuite Kilo Kit

This directory contains project commands, skills, checklists, and product
guidance for work in SecureLanSuite.

## Setup

Keep the complete `.kilo/` directory at the repository root.
`.kilo/kilo.jsonc` loads `.kilo/rules/rules.md`; merge its `instructions` entry
when combining this kit with an existing Kilo configuration.

Kilo discovers Markdown workflows in `.kilo/commands/` and skills under
`.kilo/skills/`. After changing rules, commands, or skills, run `/reload` or
start a new session before relying on the updated instructions.

## Routing

| Work | Command or skill | Supporting source |
| --- | --- | --- |
| Product flow or layout | `securelan-ui-design` | `knowledge/PRODUCT-UI.md` |
| Compose implementation | `compose-engineering` | `checklists/ui.md` |
| UI review | `/review-ui`, `securelan-ui-review` | `checklists/review.md` |
| Android interoperability | `securelan-android` | Android README and Gradle files |
| Modules and dependencies | `securelan-architecture` | `settings.gradle` and affected build files |
| Build and distribution | `securelan-packaging` | `docs/development.md`, `docs/wix-installation.md` |
| Voice, video, and RTC | `securelan-realtime` | `docs/webrtc-architecture.md` |

Commands define workflows and accept task context through `$ARGUMENTS`.
Skills define domain-specific boundaries. The product guide owns desktop UX
intent, while current code and public documentation own implemented behavior.

## Maintenance

- Keep stable repository constraints in `rules/rules.md`.
- Keep current feature and tool status in public documentation or owning code.
- Avoid copying version numbers and implementation constants into skills.
- Use accessibility and responsive checklists only when their applicability
  conditions are met.
