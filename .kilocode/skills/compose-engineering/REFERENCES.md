# Compose Engineering References

Use official documentation first for version-specific APIs.

Recommended reference areas:
- Compose Multiplatform official documentation
- Jetpack Compose official documentation
- Kotlin Coroutines documentation
- Kotlin Multiplatform documentation
- Maven Central for dependency target verification
- AndroidX release notes for multiplatform artifacts
- Compose compiler stability and performance guidance
- Accessibility documentation for semantics and keyboard interaction

## When to fetch docs

Fetch current docs when:
- adding a dependency;
- upgrading a dependency;
- using a new API;
- relying on commonMain support;
- using navigation, persistence, image loading, or platform APIs.

## Reference Routing

Architecture/state:
- MVI/MVVM
- StateFlow
- Effects
- ViewModel lifecycle

UI:
- Compose essentials
- Lazy layouts
- Modifiers
- Focus
- Semantics
- Accessibility

Performance:
- Recomposition
- Stability
- Lazy keys
- Snapshot state

KMP:
- expect/actual
- source sets
- platform interfaces
- dependency target support

Testing:
- ViewModel tests
- Turbine
- pure reducer tests
- presentation mapper tests
