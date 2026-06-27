package tech.mmarca.openvitals.domain.preferences

data class ActivityRecordingDashboardLayout(
    val template: ActivityRecordingDashboardTemplate = ActivityRecordingDashboardTemplate.LARGE_TOP,
    val fields: List<ActivityRecordingDashboardField> = DefaultFields,
    val sizes: Map<ActivityRecordingDashboardField, ActivityRecordingDashboardItemSize> = emptyMap(),
) {
    val capacity: Int
        get() = RecordingDashboardTemplate.capacity

    val items: List<ActivityRecordingDashboardItem>
        get() = fields.mapIndexed { index, field ->
            ActivityRecordingDashboardItem(
                field = field,
                size = (sizes[field] ?: defaultItemSize(index)).coercedFor(RecordingDashboardTemplate),
            )
        }

    fun normalized(): ActivityRecordingDashboardLayout {
        val normalizedItems = fields
            .distinct()
            .mapIndexed { index, field ->
                ActivityRecordingDashboardItem(
                    field = field,
                    size = (sizes[field] ?: defaultItemSize(index)).coercedFor(RecordingDashboardTemplate),
                )
            }
            .placedIn(RecordingDashboardTemplate)
            .map { it.item }
            .ifEmpty {
                DefaultFields
                    .mapIndexed { index, field ->
                        ActivityRecordingDashboardItem(
                            field = field,
                            size = defaultItemSize(index).coercedFor(RecordingDashboardTemplate),
                        )
                    }
                    .placedIn(RecordingDashboardTemplate)
                    .map { it.item }
            }
        return copy(
            template = RecordingDashboardTemplate,
            fields = normalizedItems.map { it.field },
            sizes = normalizedItems.associate { item -> item.field to item.size },
        )
    }

    fun withTemplate(template: ActivityRecordingDashboardTemplate): ActivityRecordingDashboardLayout =
        copy(template = RecordingDashboardTemplate).normalized()

    fun withFieldSize(
        field: ActivityRecordingDashboardField,
        size: ActivityRecordingDashboardItemSize,
    ): ActivityRecordingDashboardLayout {
        val updated = copy(
            template = RecordingDashboardTemplate,
            sizes = sizes + (field to size.coercedFor(RecordingDashboardTemplate)),
        ).normalized()
        return if (field in updated.fields) updated else this
    }

    fun placements(): List<ActivityRecordingDashboardItemPlacement> =
        normalized().items.placedIn(RecordingDashboardTemplate)

    companion object {
        val DefaultFields = listOf(
            ActivityRecordingDashboardField.HEART_RATE,
            ActivityRecordingDashboardField.CADENCE,
            ActivityRecordingDashboardField.SPEED,
            ActivityRecordingDashboardField.DISTANCE,
            ActivityRecordingDashboardField.DURATION,
        )
    }
}

data class ActivityRecordingDashboardItem(
    val field: ActivityRecordingDashboardField,
    val size: ActivityRecordingDashboardItemSize = ActivityRecordingDashboardItemSize.SMALL,
)

data class ActivityRecordingDashboardItemPlacement(
    val item: ActivityRecordingDashboardItem,
    val row: Int,
    val column: Int,
    val rowSpan: Int,
    val columnSpan: Int,
)

data class ActivityRecordingDashboardItemSize(
    val columnSpan: Int,
    val rowSpan: Int,
) {
    init {
        require(columnSpan > 0) { "columnSpan must be positive" }
        require(rowSpan > 0) { "rowSpan must be positive" }
    }

    fun toPreferenceString(): String = "${columnSpan}x$rowSpan"

    companion object {
        val SMALL = ActivityRecordingDashboardItemSize(columnSpan = 1, rowSpan = 1)
        val WIDE = ActivityRecordingDashboardItemSize(columnSpan = 2, rowSpan = 1)
        val TALL = ActivityRecordingDashboardItemSize(columnSpan = 1, rowSpan = 2)
        val LARGE = ActivityRecordingDashboardItemSize(columnSpan = 2, rowSpan = 2)

        fun fromPreferenceString(value: String): ActivityRecordingDashboardItemSize? =
            when (value) {
                "SMALL" -> SMALL
                "WIDE" -> WIDE
                "TALL" -> TALL
                "LARGE" -> LARGE
                else -> value
                    .split('x', limit = 2)
                    .takeIf { it.size == 2 }
                    ?.mapNotNull { it.toIntOrNull() }
                    ?.takeIf { it.size == 2 }
                    ?.let { (columns, rows) ->
                        runCatching {
                            ActivityRecordingDashboardItemSize(
                                columnSpan = columns,
                                rowSpan = rows,
                            )
                        }.getOrNull()
                    }
            }
    }
}

enum class ActivityRecordingDashboardTemplate(
    val columns: Int,
    val rows: Int,
) {
    TWO_BY_FOUR(columns = 2, rows = 4),
    THREE_BY_FOUR(columns = 3, rows = 4),
    LARGE_TOP(columns = 4, rows = 6);

    val capacity: Int
        get() = columns * rows
}

enum class ActivityRecordingDashboardField {
    HEART_RATE,
    CADENCE,
    SPEED,
    DISTANCE,
    DURATION,
    MOVING_TIME,
    AVERAGE_SPEED,
    AVERAGE_MOVING_SPEED,
    MAX_SPEED,
    ELEVATION_GAIN,
    POWER,
    STEPS,
}

private val RecordingDashboardTemplate = ActivityRecordingDashboardTemplate.LARGE_TOP

private fun defaultItemSize(index: Int): ActivityRecordingDashboardItemSize =
    if (index == 0) {
        ActivityRecordingDashboardItemSize(columnSpan = 4, rowSpan = 2)
    } else {
        ActivityRecordingDashboardItemSize.SMALL
    }

private fun ActivityRecordingDashboardItemSize.coercedFor(
    template: ActivityRecordingDashboardTemplate,
): ActivityRecordingDashboardItemSize =
    ActivityRecordingDashboardItemSize(
        columnSpan = columnSpan.coerceIn(1, template.columns),
        rowSpan = rowSpan.coerceIn(1, template.rows),
    )

private fun List<ActivityRecordingDashboardItem>.placedIn(
    template: ActivityRecordingDashboardTemplate,
): List<ActivityRecordingDashboardItemPlacement> {
    val occupied = Array(template.rows) { BooleanArray(template.columns) }
    return buildList {
        this@placedIn.forEach { item ->
            val size = item.size.coercedFor(template)
            val position = firstOpenPosition(
                occupied = occupied,
                rowSpan = size.rowSpan,
                columnSpan = size.columnSpan,
            ) ?: return@buildList
            repeat(size.rowSpan) { rowOffset ->
                repeat(size.columnSpan) { columnOffset ->
                    occupied[position.row + rowOffset][position.column + columnOffset] = true
                }
            }
            add(
                ActivityRecordingDashboardItemPlacement(
                    item = item.copy(size = size),
                    row = position.row,
                    column = position.column,
                    rowSpan = size.rowSpan,
                    columnSpan = size.columnSpan,
                )
            )
        }
    }
}

private data class DashboardGridPosition(
    val row: Int,
    val column: Int,
)

private fun firstOpenPosition(
    occupied: Array<BooleanArray>,
    rowSpan: Int,
    columnSpan: Int,
): DashboardGridPosition? {
    val rows = occupied.size
    val columns = occupied.firstOrNull()?.size ?: return null
    for (row in 0..(rows - rowSpan)) {
        for (column in 0..(columns - columnSpan)) {
            val fits = (0 until rowSpan).all { rowOffset ->
                (0 until columnSpan).all { columnOffset ->
                    !occupied[row + rowOffset][column + columnOffset]
                }
            }
            if (fits) return DashboardGridPosition(row = row, column = column)
        }
    }
    return null
}
