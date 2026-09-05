package tech.mmarca.openvitals.features.dashboard

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
import tech.mmarca.openvitals.domain.model.ExerciseData

/** The dashboard body's own derivations, pure functions extracted from `DashboardContent`. */
class DashboardContentLayoutTest {

    // STEPS and WEEKLY_CARDIO_LOAD fill the hero section; everything after them lands in the carousel.
    private val savedOrder = listOf(
        DashboardWidgetId.STEPS,
        DashboardWidgetId.WEEKLY_CARDIO_LOAD,
        DashboardWidgetId.DISTANCE,
        DashboardWidgetId.CALORIES_OUT,
        DashboardWidgetId.SLEEP,
        DashboardWidgetId.HYDRATION,
    )

    private fun widget(
        id: DashboardWidgetId,
        hasData: Boolean,
        isLoading: Boolean = false,
    ) = DashboardWidgetDisplayModel(
        id = id,
        value = if (hasData) DisplayValue("1", "") else null,
        hasValue = hasData,
        isLoading = isLoading,
        // The cardio-load tile reads its own field rather than `value`.
        weeklyCardioLoad = if (hasData && id == DashboardWidgetId.WEEKLY_CARDIO_LOAD) {
            DashboardWeeklyCardioLoad(
                currentScore = 120,
                targetScore = 200,
                todayScore = 30,
                confidence = CardioLoadConfidence.HIGH,
                targetSource = DashboardWeeklyCardioLoadTargetSource.RECENT_HISTORY,
            )
        } else {
            null
        },
    )

    /** DISTANCE and SLEEP have readings; CALORIES_OUT and HYDRATION do not. */
    private fun display(
        loading: Set<DashboardWidgetId> = emptySet(),
    ): DashboardDisplayState {
        val withData = setOf(
            DashboardWidgetId.STEPS,
            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
            DashboardWidgetId.DISTANCE,
            DashboardWidgetId.SLEEP,
        )
        return DashboardDisplayState(
            widgets = savedOrder.associateWith { id ->
                widget(id, hasData = id in withData, isLoading = id in loading)
            },
        )
    }

    private fun visibleIds(
        widgets: List<DashboardWidgetId> = savedOrder,
        display: DashboardDisplayState = display(),
        isEditingDashboard: Boolean = false,
        placedWidgetIds: Set<DashboardWidgetId> = emptySet(),
        sortEmptyTilesLast: Boolean = true,
    ) = dashboardVisibleWidgetIds(
        dashboardWidgets = widgets,
        specIds = savedOrder.toSet(),
        display = display,
        isEditingDashboard = isEditingDashboard,
        placedWidgetIds = placedWidgetIds,
        sortEmptyTilesLast = sortEmptyTilesLast,
    )

    // Tiles with no data sink below the ones with some.

    @Test
    fun `empty tiles come last in the carousel in saved order`() {
        val visible = visibleIds()

        assertEquals(
            listOf(
                // The fixed hero section keeps its geometry, untouched.
                DashboardWidgetId.STEPS,
                DashboardWidgetId.WEEKLY_CARDIO_LOAD,
                // Carousel: the two with data, in saved order…
                DashboardWidgetId.DISTANCE,
                DashboardWidgetId.SLEEP,
                // …then the two empties, also in saved order.
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.HYDRATION,
            ),
            visible,
        )
        // Once the empty tiles start, no tile with data follows.
        val firstEmpty = visible.indexOfFirst {
            display().widgets[it]?.showsNoDataMessage() == true
        }
        assertTrue(firstEmpty > 0)
        assertTrue(
            visible.drop(firstEmpty).all { display().widgets[it]?.showsNoDataMessage() == true },
        )
    }

    @Test
    fun `the edit grid keeps the true saved order`() {
        // A drag lands where the card is: edit mode must not show the partition.
        val plain = visibleIds()
        val editing = visibleIds(isEditingDashboard = true)

        assertEquals(savedOrder, editing)
        assertNotEquals(plain, editing)
    }

    @Test
    fun `a tile still loading holds its place instead of sinking`() {
        // Sinking a loading tile would reorder the carousel twice on every open.
        val held = visibleIds(display = display(loading = setOf(DashboardWidgetId.CALORIES_OUT)))

        assertEquals(
            listOf(
                DashboardWidgetId.STEPS,
                DashboardWidgetId.WEEKLY_CARDIO_LOAD,
                DashboardWidgetId.DISTANCE,
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.SLEEP,
                DashboardWidgetId.HYDRATION,
            ),
            held,
        )
    }

    @Test
    fun `no tile is demoted while any tile is still loading`() {
        // Metrics land one at a time, so nothing moves while anything is still reading.
        assertEquals(savedOrder, visibleIds(display = display(loading = setOf(DashboardWidgetId.SLEEP))))

        // The demotion still happens, once the last tile has spoken.
        assertEquals(
            listOf(
                DashboardWidgetId.STEPS,
                DashboardWidgetId.WEEKLY_CARDIO_LOAD,
                DashboardWidgetId.DISTANCE,
                DashboardWidgetId.SLEEP,
                DashboardWidgetId.CALORIES_OUT,
                DashboardWidgetId.HYDRATION,
            ),
            visibleIds(),
        )
    }

    @Test
    fun `a saved order that leads with an empty tile still sinks it`() {
        // The user's order puts an empty tile first; the partition still applies and the tile is not dropped.
        val reordered = listOf(
            DashboardWidgetId.STEPS,
            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
            DashboardWidgetId.HYDRATION,
            DashboardWidgetId.DISTANCE,
        )

        val visible = visibleIds(widgets = reordered)

        assertEquals(DashboardWidgetId.DISTANCE, visible[2])
        assertEquals(DashboardWidgetId.HYDRATION, visible.last())
        assertTrue(DashboardWidgetId.HYDRATION in visible)
    }

    @Test
    fun `an empty tile with recent history holds its place instead of sinking`() {
        // "Empty today" is not "unused": a sleep tile is empty every morning until the night syncs.
        // Without history the empty tile sinks, with it it holds.
        val reordered = listOf(
            DashboardWidgetId.STEPS,
            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
            DashboardWidgetId.CALORIES_OUT,
            DashboardWidgetId.DISTANCE,
            DashboardWidgetId.SLEEP,
            DashboardWidgetId.HYDRATION,
        )
        val base = display()
        val heldDisplay = base.copy(
            widgets = base.widgets.mapValues { (id, model) ->
                if (id == DashboardWidgetId.CALORIES_OUT) {
                    model.copy(hasRecentHistory = true)
                } else {
                    model
                }
            },
        )

        assertEquals(
            DashboardWidgetId.CALORIES_OUT,
            visibleIds(widgets = reordered, display = heldDisplay)[2],
        )
        assertEquals(
            DashboardWidgetId.HYDRATION,
            visibleIds(widgets = reordered, display = heldDisplay).last(),
        )
        // The control: with no history signal the same saved order sinks it.
        assertEquals(
            DashboardWidgetId.DISTANCE,
            visibleIds(widgets = reordered, display = base)[2],
        )
    }

    @Test
    fun `turning the sort off keeps the saved order verbatim`() {
        val visible = visibleIds(sortEmptyTilesLast = false)

        assertEquals(savedOrder, visible)
    }

    @Test
    fun `a hidden hero ring leaves the row and joins the tray`() {
        // The saved list is authoritative: a removed widget is absent from it, and the tray is the complement.
        val withoutSteps = savedOrder - DashboardWidgetId.STEPS

        val visible = visibleIds(widgets = withoutSteps, isEditingDashboard = true)

        assertEquals(withoutSteps, visible)
        assertEquals(
            setOf(DashboardWidgetId.STEPS),
            savedOrder.toSet() - visible.toSet(),
        )
    }

    // Unsupported metrics in edit mode. Outside edit mode the mapper drops them entirely.

    @Test
    fun `edit mode materialises an unsupported metric into the tray not the carousel`() {
        // HYDRATION is materialised only because edit mode asked for it.
        val display = display().copy(unsupportedIds = setOf(DashboardWidgetId.HYDRATION))

        val visible = visibleIds(
            display = display,
            isEditingDashboard = true,
            placedWidgetIds = emptySet(),
        )
        val tray = dashboardTrayWidgetIds(
            specIds = savedOrder,
            visibleIds = visible,
            isEditingDashboard = true,
        )

        assertTrue(DashboardWidgetId.HYDRATION !in visible)
        assertEquals(listOf(DashboardWidgetId.HYDRATION), tray)
    }

    @Test
    fun `adding an unsupported metric back is not a dead end`() {
        // Once the user places it, it stays in the grid like any other tile.
        val display = display().copy(unsupportedIds = setOf(DashboardWidgetId.HYDRATION))

        val visible = visibleIds(
            display = display,
            isEditingDashboard = true,
            placedWidgetIds = savedOrder.toSet(),
        )

        assertEquals(savedOrder, visible)
        assertEquals(
            emptyList<DashboardWidgetId>(),
            dashboardTrayWidgetIds(
                specIds = savedOrder,
                visibleIds = visible,
                isEditingDashboard = true,
            ),
        )
    }

    @Test
    fun `outside edit mode there is no tray and no unsupported materialisation`() {
        val visible = visibleIds()

        assertEquals(
            emptyList<DashboardWidgetId>(),
            dashboardTrayWidgetIds(
                specIds = savedOrder,
                visibleIds = visible,
                isEditingDashboard = false,
            ),
        )
    }

    // Activities.

    @Test
    fun `the workout list wins when it has entries`() {
        val data = DashboardData(
            date = LocalDate.of(2026, 1, 2),
            workouts = listOf(exercise("a"), exercise("b")),
            workout = exercise("c"),
        )

        assertEquals(listOf("a", "b"), dashboardActivitiesForDay(data).map { it.id })
    }

    @Test
    fun `a lone workout is the fallback`() {
        val data = DashboardData(
            date = LocalDate.of(2026, 1, 2),
            workout = exercise("c"),
        )

        assertEquals(listOf("c"), dashboardActivitiesForDay(data).map { it.id })
    }

    @Test
    fun `a day with neither has no activities`() {
        val data = DashboardData(date = LocalDate.of(2026, 1, 2))

        assertEquals(emptyList<ExerciseData>(), dashboardActivitiesForDay(data))
    }

    private fun exercise(id: String) = ExerciseData(
        id = id,
        title = null,
        exerciseType = 1,
        startTime = Instant.parse("2026-01-02T07:00:00Z"),
        endTime = Instant.parse("2026-01-02T08:00:00Z"),
        durationMs = 3_600_000,
        source = "test",
    )

    // A setup prompt is not an empty tile.

    @Test
    fun `a tile offering to set a feature up keeps its place`() {
        // Body Energy rendered on the last carousel page for anyone who had not set it up.
        // The tile is the only entry point to the feature, so demoting it keeps it undiscovered.
        val notSetUp = DashboardWidgetId.BODY_ENERGY
        val order = listOf(
            DashboardWidgetId.STEPS,
            DashboardWidgetId.WEEKLY_CARDIO_LOAD,
            DashboardWidgetId.DISTANCE,
            DashboardWidgetId.SLEEP,
            // carousel begins here
            notSetUp,
            DashboardWidgetId.HYDRATION,
            DashboardWidgetId.CALORIES_OUT,
        )
        val display = DashboardDisplayState(
            widgets = order.associateWith { id ->
                when (id) {
                    notSetUp -> DashboardWidgetDisplayModel(id = id, value = null, hasValue = false, isNotSetUp = true)
                    DashboardWidgetId.HYDRATION -> widget(id, hasData = true)
                    DashboardWidgetId.CALORIES_OUT -> widget(id, hasData = false)
                    else -> widget(id, hasData = true)
                }
            },
        )

        val visible = dashboardVisibleWidgetIds(
            dashboardWidgets = order,
            specIds = order.toSet(),
            display = display,
            isEditingDashboard = false,
        )

        // It stays ahead of the genuinely empty tile, in its saved position.
        assertTrue(
            "a not-set-up tile must not sink below an empty one",
            visible.indexOf(notSetUp) < visible.indexOf(DashboardWidgetId.CALORIES_OUT),
        )
        assertEquals(notSetUp, visible[4])
    }

    @Test
    fun `a genuinely empty tile is still demoted`() {
        // The counterweight: the demotion must survive the exception above.
        val visible = visibleIds()
        val empties = listOf(DashboardWidgetId.CALORIES_OUT, DashboardWidgetId.HYDRATION)

        for (empty in empties) {
            assertTrue(
                "$empty has no reading and should sink",
                visible.indexOf(empty) > visible.indexOf(DashboardWidgetId.SLEEP),
            )
        }
    }
}
