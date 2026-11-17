package io.github.ctorres345.hemisphere.core.mvi

import org.junit.Assert.assertEquals
import org.junit.Test

class MviViewModelTest {
    private sealed interface CounterIntent {
        data class Add(
            val amount: Int,
        ) : CounterIntent
    }

    private sealed interface CounterResult {
        data class Added(
            val amount: Int,
        ) : CounterResult
    }

    private class CounterViewModel : MviViewModel<Int, CounterIntent, CounterResult>(0, ::reduceCounter) {
        override fun onIntent(intent: CounterIntent) {
            when (intent) {
                is CounterIntent.Add -> emit(CounterResult.Added(intent.amount))
            }
        }
    }

    @Test
    fun `starts from the initial state`() {
        assertEquals(0, CounterViewModel().state.value)
    }

    @Test
    fun `each result goes through the reducer`() {
        val viewModel = CounterViewModel()
        viewModel.onIntent(CounterIntent.Add(2))
        viewModel.onIntent(CounterIntent.Add(3))
        assertEquals(5, viewModel.state.value)
    }

    private companion object {
        fun reduceCounter(
            state: Int,
            result: CounterResult,
        ): Int =
            when (result) {
                is CounterResult.Added -> state + result.amount
            }
    }
}
