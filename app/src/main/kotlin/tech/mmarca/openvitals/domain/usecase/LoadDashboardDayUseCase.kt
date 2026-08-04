package tech.mmarca.openvitals.domain.usecase

import tech.mmarca.openvitals.data.repository.dashboard.DashboardDataLoader
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardQuery
import javax.inject.Inject

class LoadDashboardDayUseCase @Inject constructor(
    private val dashboardDataLoader: DashboardDataLoader,
) {
    suspend operator fun invoke(query: DashboardQuery): DashboardData =
        dashboardDataLoader.loadDashboard(query)
}
