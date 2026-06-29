package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.model.SleepData

data class SleepPeriodData(
    val sessions: List<SleepData> = emptyList(),
    val previousSessions: List<SleepData> = emptyList(),
    val baselineSessions: List<SleepData> = emptyList(),
)
