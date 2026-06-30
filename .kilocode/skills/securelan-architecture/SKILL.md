---
name: securelan-architecture
license: MIT
description: >
  SecureLanSuite architecture router skill. Use when the task affects module
  boundaries, dependency directions, Gradle multi-project structure, reusable
  core modules, Java/Kotlin migration, cross-module refactoring, or architecture
  documentation. This skill routes the agent to project rules and relevant docs;
  it does not duplicate the full architecture rule set.
---

# SecureLanSuite Architecture Skill

## Role

You are a SecureLanSuite architecture assistant.

Your job is to help with module boundaries, dependency directions, Gradle project
structure, reusable core modules, Kotlin/Java migration, and cross-module
implementation decisions without violating project rules.

This skill is intentionally short.

The authoritative architecture constraints live in:

- `.kilocode/rules.md`
- `.kilocode/rules/*.md`
- repository `README.md`
- repository `docs/`

Do not duplicate large rule sets inside this skill.

---

## Use this skill when

Use this skill for tasks involving:

- module graph changes;
- `modules/*` dependency changes;
- `apps/*` to `modules/*` boundaries;
- reusable core extraction;
- Java/Kotlin migration planning;
- cross-module refactoring;
- Gradle multi-project configuration;
- API compatibility between modules;
- architecture documentation updates.

Do not use this skill for ordinary Compose UI implementation unless the UI task
requires architecture or module-boundary changes.

---

## Mandatory first steps

Before proposing or implementing architecture changes:

1. Read `.kilocode/rules.md`.
2. Read only the relevant repository docs:
   - `README.md`
   - `docs/development.md`
   - `docs/kotlin-migration/kotlin-migration.md`
   - relevant feature or module docs under `docs/`
3. Inspect the actual `build.gradle.kts` files involved.
4. Identify the affected modules and dependency direction.
5. State the intended architecture change before editing code.

Do not start from assumptions about the module graph.

---

## Architecture decision pipeline

For every architecture task:

1. Identify the user goal.
2. Identify affected modules.
3. Check allowed dependency direction.
4. Check whether the change belongs in `apps/*` or `modules/*`.
5. Check protocol compatibility impact.
6. Check public API compatibility impact.
7. Choose the smallest reversible change.
8. Run the closest validation task.
9. Update docs if architecture, migration status, module responsibility, packaging, or product status changed.

---

## Non-negotiable reminders

Always preserve these project-level constraints:

- reusable modules remain UI-agnostic;
- UI framework code stays out of `modules/*`;
- `modules/*` must not depend on `apps/*`;
- crypto logic stays out of UI code;
- networking and transport orchestration stay behind service boundaries where practical;
- protocol and wire-format compatibility must not drift without explicit approval;
- large rewrites require explicit user approval.

For the full rule set, use `.kilocode/rules.md`.

---

## Kotlin / Java migration guidance

When working on migration:

- keep changes incremental;
- keep the repository buildable after each step;
- preserve Java-callable public API contracts unless a breaking change is explicitly approved;
- prefer small reversible slices over whole-module rewrites;
- avoid changing runtime-heavy WebRTC code unless the task specifically targets it;
- validate with the closest relevant Gradle task first.

Do not “fix” Java/Kotlin target details unless project rules and current Kotlin support allow it.

---

## Validation routing

Prefer focused validation first.

Examples:

- changed one JVM module → run that module tests;
- changed desktop app integration → run desktop client tests/build;
- changed Gradle configuration → run the affected project build;
- changed Android compatibility → run Android debug validation;
- changed packaging → run relevant packaging validation.

Run whole-repository validation only when the change justifies it.

---

## Documentation rule

If the change alters any of the following, update docs in the same task:

- architecture;
- module responsibility;
- dependency direction;
- supported Java/Kotlin versions;
- migration status;
- packaging flow;
- Android interoperability;
- product status.

Keep this skill short. Put durable rules in `.kilocode/rules.md` and detailed
explanations in repository documentation.

---

## Completion report

After an architecture task, report:

1. affected modules;
2. dependency direction checked;
3. public API compatibility impact;
4. protocol compatibility impact;
5. validation run;
6. docs updated or reason not needed;
7. risks and follow-up work.
