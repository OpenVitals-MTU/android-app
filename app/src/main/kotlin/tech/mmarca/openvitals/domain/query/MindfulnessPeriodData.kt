package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.model.MindfulnessSession

data class MindfulnessPeriodData(
    val sessions: List<MindfulnessSession> = emptyList(),
    val previousSessions: List<MindfulnessSession> = emptyList(),
    val baselineSessions: List<MindfulnessSession> = emptyList(),
)
