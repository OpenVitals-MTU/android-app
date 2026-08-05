package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.model.CycleData

data class CyclePeriodData(
    val data: CycleData,
    val missingPermissions: Set<String>,
    val statistics: CycleStatistics? = null,
)
