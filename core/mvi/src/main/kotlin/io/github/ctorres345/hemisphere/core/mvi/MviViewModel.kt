package io.github.ctorres345.hemisphere.core.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A pure function from the current state and one result to the next state (architecture §6.1). It
 * does no I/O, starts no coroutines, calls no use case and reads no clock.
 */
typealias Reducer<S, R> = (state: S, result: R) -> S

/**
 * The shared strict-MVI base (ADR 0009). A screen's ViewModel extends it with its own
 * `XxxUiState`, `XxxIntent` and `XxxResult`, and passes its top-level `reduce` function.
 *
 * - Input arrives only through [onIntent]; a subclass adds no other public function.
 * - The subclass calls use cases and turns what they return into results with [emit].
 * - [emit] is the only write to [state], and it goes through [reducer].
 */
abstract class MviViewModel<S, I, R>(
    initialState: S,
    private val reducer: Reducer<S, R>,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState)

    /** Everything the screen shows. Collect it with `collectAsStateWithLifecycle`. */
    val state: StateFlow<S> = mutableState.asStateFlow()

    /** The one way in: everything the user does arrives here. */
    abstract fun onIntent(intent: I)

    /** Applies one result through the reducer. */
    protected fun emit(result: R) {
        mutableState.update { reducer(it, result) }
    }
}
