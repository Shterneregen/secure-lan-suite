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

Read [`docs/development.md`](../../../docs/development.md), the applicable
platform guide, and the owning Gradle files. Verify versions, tasks, inputs, and
output paths there instead of copying them into this skill.

## Workflow

1. Confirm the Java, Gradle Wrapper, and required platform tools.
2. Reproduce with the narrowest owning task.
3. Inspect inputs, runtime-image dependencies, duplicate exclusions,
   icons, version normalization, and output directories when relevant.
4. Correct the owner, rerun the focused task, and inspect the artifact.

Report only packaging and runtime steps actually executed. Update public
documentation when requirements, tasks, artifacts, or release behavior change.
