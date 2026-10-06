package tech.mmarca.openvitals.wear.features.measure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasureViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun result_endsInDone_andIsHandedOn() = runTest(dispatcher) {
        val results = mutableListOf<Pair<Measurement, Double>>()
        val viewModel = MeasureViewModel(
            measurement = Measurement.HEART_RATE,
            events = { flow { emit(MeasureEvent.Live(70.0)); emit(MeasureEvent.Result(71.0)) } },
            onResult = { measurement, value -> results += measurement to value },
        )

        viewModel.start()
        advanceUntilIdle()

        assertEquals(MeasureUiState.Done(71.0, null), viewModel.state.value)
        assertEquals(listOf(Measurement.HEART_RATE to 71.0), results)
    }

    @Test
    fun measureException_becomesItsError() = runTest(dispatcher) {
        val viewModel = MeasureViewModel(
            measurement = Measurement.HEART_RATE,
            events = { flow { throw MeasureException(MeasureError.OFF_BODY) } },
            onResult = { _, _ -> },
        )

        viewModel.start()
        advanceUntilIdle()

        assertEquals(MeasureUiState.Failed(MeasureError.OFF_BODY), viewModel.state.value)
    }

    @Test
    fun securityException_isAPermissionError() = runTest(dispatcher) {
        val viewModel = MeasureViewModel(
            measurement = Measurement.HRV,
            events = { flow { throw SecurityException("no") } },
            onResult = { _, _ -> },
        )

        viewModel.start()
        advanceUntilIdle()

        assertEquals(MeasureUiState.Failed(MeasureError.PERMISSION_DENIED), viewModel.state.value)
    }
}
