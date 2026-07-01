---
name: securelan-packaging
license: MIT
description: >
  SecureLanSuite build, run, release, jpackage, portable ZIP, and Windows EXE
  packaging skill. Use when the user asks about Gradle validation, desktop
  distribution, WiX, installer creation, packaging failures, runtime images,
  app icons/resources, or release artifacts.
---

# SecureLanSuite Packaging Skill

Use this skill for build, run, validation, release, `jpackage`, portable ZIP, and Windows EXE installer work.

## Current packaging baseline

- Java baseline: JDK 25.
- Build system: Gradle multi-project with the Gradle Wrapper.
- Desktop packaging module: `apps/desktop-client`.
- Portable ZIP task: `:apps:desktop-client:buildPortable`.
- Windows EXE tasks: `:apps:desktop-client:buildExe` or `:apps:desktop-client:createExe`.
- Portable ZIP output: `apps/desktop-client/build/distributions/`.
- `jpackage` output: `apps/desktop-client/build/packaging/`.
- Windows EXE builds require WiX 5.0.2 plus `WixToolset.UI.wixext` and `WixToolset.Util.wixext`.

Read [`docs/development.md`](../../../docs/development.md) and [`docs/wix-installation.md`](../../../docs/wix-installation.md) before changing packaging tasks or troubleshooting installer builds.

## Rules

- Use `gradlew.bat` commands on Windows.
- Do not recommend WiX 7 unless packaging is revalidated for this project.
- Do not mention a `printPackagingEnvironment` Gradle task; it does not currently exist.
- Do not change `application.mainClass`, launcher behavior, manifest, runtime image inputs, duplicate dependency exclusions, or icon/resource paths without checking existing `apps/desktop-client/build.gradle` behavior.
- Do not claim portable ZIP, EXE, install, or runtime smoke validation unless those commands were actually run.
- Keep packaging docs synchronized with any task/output/version changes.

## Validation commands

- Environment checks: `java --version`, `jpackage --version`, `wix --version`, `wix extension list --global`.
- Desktop tests/build: `gradlew.bat :apps:desktop-client:test :apps:desktop-client:build --no-daemon`.
- Portable ZIP: `gradlew.bat :apps:desktop-client:buildPortable --no-daemon`.
- Windows EXE: `gradlew.bat :apps:desktop-client:buildExe --no-daemon`.

## Troubleshooting priorities

1. Confirm JDK 25 provides `jpackage`.
2. Confirm WiX version is exactly the verified 5.0.2 family.
3. Confirm required WiX extensions are installed globally.
4. Check `apps/desktop-client/build.gradle` for task inputs, output directories, duplicate-runtime exclusions, icons, and app version normalization.
5. Re-run the narrowest packaging task that reproduces the issue.

## Documentation rule

Update [`docs/development.md`](../../../docs/development.md), [`docs/wix-installation.md`](../../../docs/wix-installation.md), or [`README.md`](../../../README.md) when changing packaging tasks, output locations, required tool versions, artifact naming, or release validation status.
