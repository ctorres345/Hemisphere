# Code guidelines

Status: **proposed** (HEM-2).

Each rule says where it comes from: a section of [`architecture.md`](architecture.md) (`arch §n`),
an ADR, or [`design-system.md`](design-system.md) (`design §n`). The last column says what checks
it. "Review" means a reviewer checks it by hand. Lint tools are set up in the scaffold ticket;
anything a linter can check is left to the linter and listed only once here.

## Modules and boundaries

| Rule | From | Checked by |
| --- | --- | --- |
| A `feature:*` module never depends on another `feature:*` module. | arch §2 | Gradle module graph (scaffold adds a check task) |
| `core:fs`, `core:jobs` and `core:data` have no Compose dependency. | arch §2 | Gradle module graph |
| Outside `core:fs`, no `java.io.File`, `java.nio.file.Path`, path strings or `content://` URIs. Use `FsPath`. The only exception is passing `shareableUri` straight into an `Intent`. | ADR 0006 | Detekt `ForbiddenImport` for the types; review for strings and URIs |
| Check `provider.capabilities`, never the provider's type (`is SafProvider`). | arch §3 | Review |
| Each `core:*` module exposes interfaces; its implementations are `internal`. | arch §9 | Review |

## Errors

| Rule | From | Checked by |
| --- | --- | --- |
| `core:fs` returns `FsResult`; no exception crosses the module boundary. Catch inside the provider and map to `FsError`. | arch §3 | Review |
| Never catch `CancellationException` without rethrowing it; it is the only exception that crosses the `core:fs` boundary. | arch §3 | Detekt `SuspendFunSwallowedCancellation`, review |
| Every `FsError` shown to the user has a message and, where possible, an action (Retry, Grant access). No raw exception text in the UI. | arch §3 | Review |
| A failed or cancelled copy leaves no file under the final name, and its temp file is deleted. | arch §4 | `core:jobs` tests |

## State and Compose

| Rule | From | Checked by |
| --- | --- | --- |
| One immutable `UiState` per screen, exposed as `StateFlow`, collected with `collectAsStateWithLifecycle`. | arch §6 | Review |
| Input reaches a ViewModel through one `onAction(action)` with a sealed action type. | arch §6 | Review |
| One-off events are state fields the UI clears; no `Channel` or `SharedFlow` for UI events. | arch §6 | Review |
| Composables below screen level are stateless: state in, lambdas out. A `Modifier` parameter comes first among optional parameters. | arch §6 | Compose lint rules (slack/compose-lints) |
| No ViewModel passed into a non-screen composable. | arch §6 | Compose lint rules |
| Dispatchers are injected (`@IoDispatcher`), never `Dispatchers.IO` in app code. | arch §9 | Detekt `InjectDispatcher` |

## Design system and input

| Rule | From | Checked by |
| --- | --- | --- |
| UI code uses tokens only: no raw colors, `dp`/`sp` literals or font values outside `core:designsystem`. | design §5–7 | Detekt `ForbiddenMethodCall` on `Color(...)`, review for sizes |
| Every interactive element has a gamepad focus state, using the shared focus ring from `core:designsystem`. | design §3 | Roborazzi screenshots in gamepad mode, review |
| Every interactive element is reachable with the D-pad in a predictable order, with no focus trap. | design §3 | Instrumented focus tests, manual gamepad pass |
| Touch targets are at least 48dp in touch mode. | design §7 | Shared components use `minimumInteractiveComponentSize`; accessibility checks in Roborazzi tests |
| Semantic states (danger, warning, success, selection) carry an icon and a text label, never color alone. | design §5 | Review |
| Gamepad buttons go through `core:input` actions, never raw `KEYCODE_BUTTON_*` checks in features. | design §3 | Detekt `ForbiddenImport` / review |
| Long file names are cut in the middle with the shared text component. | design §6 | Review |

## Jobs and Trash

| Rule | From | Checked by |
| --- | --- | --- |
| Copy, move and delete always go through `JobQueue`, never a direct provider call from a ViewModel. | arch §4 | Review |
| Delete on a volume with the `trash` capability goes to Trash; permanent delete only from the Trash screen or on volumes without the capability, with the `danger` dialog. | arch §5 | `core:jobs` tests |
| A move inside one volume uses `move` when the provider has the `move` capability; copy plus delete only otherwise. | arch §4 | `core:jobs` tests |

## Persistence

| Rule | From | Checked by |
| --- | --- | --- |
| Every Room schema change ships a migration and a migration test. No destructive fallback. | arch §8 | Room schema export in CI, tests |
| Features read settings and saved data only through `core:data` repositories, never DataStore keys or DAOs. | arch §8 | Review |

## Privacy and dependencies

| Rule | From | Checked by |
| --- | --- | --- |
| No `INTERNET` permission, analytics, crash service, Firebase or Play Services in v1. | ADR 0008, 0003 | Manifest check in CI (scaffold), review |
| Every new library is GPLv3-compatible; its license is named in the PR. | ADR 0004 | Review (a license-report task can come later) |
| Never log file names or paths at info level or higher in release builds. | ADR 0008 | Review |

## Naming

| Rule | From | Checked by |
| --- | --- | --- |
| Use the words in [`GLOSSARY.md`](../GLOSSARY.md) for types and UI copy (`Pane`, `Volume`, `Job`, `Trash`), not their synonyms. | GLOSSARY | Review |
| Packages follow modules: `io.github.ctorres345.hemisphere.core.fs`, `...feature.browser`. | arch §2 | Review |
| Formatting and the rest of Kotlin style. | Kotlin coding conventions | ktlint |

## Tests

| Rule | From | Checked by |
| --- | --- | --- |
| `core:fs`, `core:jobs` and every ViewModel have JVM tests using `InMemoryProvider` and `runTest`. | arch §10 | `./gradlew test` in CI |
| Each shared component has a Roborazzi test in light, dark and AMOLED; phone and handheld; touch and gamepad. | design §11 | Roborazzi in CI |
| A bug fix starts with a failing test that shows the bug. | arch §10 | Review |
