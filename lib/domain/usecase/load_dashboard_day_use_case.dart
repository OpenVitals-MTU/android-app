import '../../core/result/result.dart';
import '../../data/repository/dashboard/dashboard_data_loader.dart';
import '../model/dashboard_data.dart';
import '../model/dashboard_query.dart';

/// Port of the Kotlin `LoadDashboardDayUseCase`.
class LoadDashboardDayUseCase {
  const LoadDashboardDayUseCase(this._dashboardDataLoader);

  final DashboardDataLoader _dashboardDataLoader;

  Future<Result<DashboardData>> call(DashboardQuery query) =>
      _dashboardDataLoader.loadDashboard(query);
}
