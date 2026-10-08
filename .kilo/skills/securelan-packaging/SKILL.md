---
name: securelan-packaging
license: MIT
description: >
  Build, run, and package SecureLanSuite. Use for Gradle validation, desktop
  application images, portable archives, Windows installers, jpackage, WiX,
  release artifacts, signing, and packaging failures.
---

# SecureLanSuite Packaging

## Sources

- Read [`docs/development.md`](../../../docs/development.md) for current build
  tasks and smoke checks.
- Read [`docs/wix-installation.md`](../../../docs/wix-installation.md) for the
  verified Windows installer toolchain.
- Inspect `gradle.properties` and the affected `build.gradle` before relying on
  a version, task, input, output path, launcher, or resource name.

## Workflow

1. Confirm the active Java, Gradle Wrapper, and platform tool environment.
2. Reproduce with the narrowest owning Gradle task.
3. Inspect task inputs, runtime-image dependencies, duplicate exclusions,
   icons, version normalization, and output directories when relevant.
4. Correct the owning build logic or documentation.
5. Re-run the focused task and inspect the produced artifact.

Do not claim a portable archive, installer, install, or runtime smoke test unless
that step was executed. Keep the development guide, Windows installer guide,
and root README synchronized when tasks, requirements, artifact names, or
release behavior change.
