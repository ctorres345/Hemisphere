---
status: accepted
---

# Strict MVI in the UI layer and a required domain layer of use cases

Hemisphere will grow past the MVP (archives, search, viewers, network shares). We follow
Google's guide to app architecture (UI, domain and data layers, unidirectional data flow) and go
further in two ways. The domain layer, optional in the guide, is required: every ViewModel
reaches data only through use cases in a `core:domain` module. And every ViewModel uses strict
MVI (intents, results, a pure reducer), our stricter form of the guide's UDF; the guide itself
does not prescribe reducers. Cesar chose this on
2026-11-17 so that the screen logic and the app's rules are both testable on the JVM and stay
in one place as features are added.

## Considered options

- **Plain unidirectional data flow** (one `UiState`, an `onAction` function, the ViewModel
  updates state wherever it likes, repositories called directly). Less code, and it is
  Google's baseline. Rejected: state changes spread across the ViewModel, and rules such as
  "delete goes to Trash when the volume supports it" would be copied into each screen that
  deletes.
- **Domain layer only where logic is shared** (Google's default: the layer is optional).
  Rejected: "only where needed" is a judgment call each time, and the code drifts into two
  styles. One rule for every ViewModel is easier to check with a test.
- **An MVI library** (Orbit, Mavericks). Rejected for now: a pure `reduce` behind a
  `MutableStateFlow` is a few lines, and it keeps one less dependency to license-check (ADR 0004 rule).

## Consequences

- Each ViewModel has four types (`XxxUiState`, `XxxIntent`, `XxxResult`, `reduce`), and each
  action has a use case, even a one-line pass-through. More files; each one is small.
- Reducers are tested as pure functions, use cases with fakes, ViewModels by sending intents
  and reading the `StateFlow`. Fakes (`InMemoryProvider`, a fake `JobQueue`, fake repositories),
  not a mocking library.
- Shared value types (`FsPath`, `Entry`, `FsError`, job states) live in a pure Kotlin
  `core:model` module. `core:domain` exposes only that as `api` and takes the data modules as
  `implementation`, so providers, the `JobQueue` and repositories are not on a feature's
  classpath. Gradle enforces the layer boundary.
- Konsist architecture tests (Apache 2.0, test-only) check what Gradle cannot: ViewModel
  constructors, use-case names and shape, no `android.*` in `core:domain`.
- The Browser screen has one `BrowserViewModel` holding both panes and the shell, because
  paste, copy to the other pane and back order need all of them in one state. Pane logic is a
  shared `reducePane`.
- Moving off this later means rewriting every ViewModel, so the cost of the choice grows with
  each screen.
