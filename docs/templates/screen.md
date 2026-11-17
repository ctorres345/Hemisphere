# Adding a screen

Copy these files to add a screen that follows strict MVI (ADR 0009, architecture §6). The Browser
screen in `feature/browser` is the worked example; read it next to this page.

```
 user ──► XxxIntent ──► XxxViewModel.onIntent ──► use case (core:domain) ──► data layer
                                  │
                                  ▼
                          emit(XxxResult)
                                  │
                                  ▼
               reduce(XxxUiState, XxxResult) ──► new XxxUiState ──► XxxScreen
```

## Files

Replace `Xxx` with the screen name (`Trash`, `Settings`). One package per screen, in its
`feature:*` module.

| File | Holds | Browser example |
| --- | --- | --- |
| `XxxContract.kt` | `XxxUiState` (one immutable data class), `XxxIntent` and `XxxResult` (sealed interfaces) | `BrowserContract.kt` |
| `XxxReducer.kt` | `fun reduce(state: XxxUiState, result: XxxResult): XxxUiState`, pure | `BrowserReducer.kt` |
| `XxxViewModel.kt` | `class XxxViewModel(...use cases) : MviViewModel<XxxUiState, XxxIntent, XxxResult>(XxxUiState(), ::reduce)` | `BrowserViewModel.kt` |
| `XxxScreen.kt` | `XxxRoute(viewModel)` collects the state; `XxxScreen(state, onIntent)` is stateless | `BrowserScreen.kt` |
| `XxxReducerTest.kt` | Plain JUnit: given a state and a result, check the new state | `BrowserReducerTest.kt` |
| `XxxViewModelTest.kt` | Real use cases over fakes from `core:testing`, `MainDispatcherRule`, `runTest` | `BrowserViewModelTest.kt` |

Each new action also gets a `VerbNounUseCase` in `core:domain` with one `operator fun invoke` and a
test over the fakes, even when it only passes a call through.

## Skeleton

```kotlin
// XxxContract.kt
data class XxxUiState(val loading: Boolean = true)

sealed interface XxxIntent {
    data object Retry : XxxIntent
}

sealed interface XxxResult {
    data class Loaded(val items: List<Item>) : XxxResult
}

// XxxReducer.kt
fun reduce(state: XxxUiState, result: XxxResult): XxxUiState =
    when (result) {
        is XxxResult.Loaded -> state.copy(loading = false)
    }

// XxxViewModel.kt
class XxxViewModel(
    private val observeItems: ObserveItemsUseCase,
) : MviViewModel<XxxUiState, XxxIntent, XxxResult>(XxxUiState(), ::reduce) {
    override fun onIntent(intent: XxxIntent) {
        when (intent) {
            XxxIntent.Retry -> load()
        }
    }

    private var loading: Job? = null // declared above init: Kotlin runs them in order

    init {
        load() // the first load starts here; later loads come only through onIntent
    }

    private fun load() {
        loading?.cancel() // a Retry replaces the old collector, so a stale listing never wins
        loading = viewModelScope.launch { observeItems().collect { emit(XxxResult.Loaded(it)) } }
    }
}

// XxxScreen.kt
@Composable
fun XxxRoute(viewModel: XxxViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    XxxScreen(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
fun XxxScreen(state: XxxUiState, onIntent: (XxxIntent) -> Unit, modifier: Modifier = Modifier) { /* ... */ }
```

## Wiring

Until Hilt lands, `app/AppContainer.kt` builds the use cases and `MainActivity` creates the
ViewModel with `viewModelFactory { initializer { ... } }`. With Hilt, the ViewModel gets
`@HiltViewModel` and an `@Inject constructor`, and the container goes away.

## Checklist

- [ ] The ViewModel's constructor takes only use cases (and `SavedStateHandle` or `@Assisted` values).
- [ ] The ViewModel's only public function is `onIntent`; state changes only through `emit`.
- [ ] `reduce` is pure: no I/O, coroutines, use cases or clock.
- [ ] One-off events (a snackbar) are state fields cleared by an intent, never a `Channel`.
- [ ] Composables below `XxxRoute` take state and lambdas, never the ViewModel.
- [ ] UI uses design tokens (`HemisphereSpacing`, theme colors), no `dp` literals or raw colors.
- [ ] Every `FsError` shown has a message from string resources, and Retry where a retry can help.
- [ ] Reducer, use-case and ViewModel tests pass: `./gradlew test`.
- [ ] `./gradlew check` is green (Konsist checks most of the rules above).
