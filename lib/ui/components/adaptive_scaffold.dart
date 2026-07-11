import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/dashboard/dashboard_sensor_status.dart';
import '../../l10n/app_localizations.dart';
import '../../navigation/app_routes.dart';

/// The OpenVitals home scaffold — a Material 3 top app bar over the dashboard.
///
/// Mirrors the Kotlin `OpenVitalsAdaptiveScaffold`: the app has **no bottom
/// navigation**. The dashboard is the home, and every other destination is
/// reached from the top-bar actions (Daily Readiness / sensor battery /
/// Achievements / Settings) or from in-screen actions (the Activities section,
/// the Log / Start buttons, metric cards). Those destinations are pushed onto
/// the root navigator and get their own back-enabled app bar.
class OpenVitalsHomeScaffold extends ConsumerWidget {
  const OpenVitalsHomeScaffold({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final sensorStatus = ref.watch(dashboardSensorStatusProvider);
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.appName),
        centerTitle: false,
        actions: [
          // Kotlin dashboard top bar: the SelfImprovement icon opens Daily
          // Readiness (AppNavigation.kt:415-425, cd_daily_readiness), not the
          // mindfulness entry form (which is reached from the Add-entry hub).
          IconButton(
            tooltip: l10n.screenDailyReadiness,
            icon: const Icon(Icons.self_improvement_outlined),
            onPressed: () => context.push(AppRoutes.dailyReadiness),
          ),
          // Only shown once at least one BLE sensor is paired (Kotlin
          // `dashboardDeviceActionVisible`).
          if (sensorStatus.hasDevices)
            IconButton(
              tooltip: l10n.cdSensorBatteryStatus,
              icon: const Icon(Icons.battery_charging_full_outlined),
              onPressed: () => context.push(AppRoutes.settingsSensors),
            ),
          IconButton(
            tooltip: l10n.screenAchievements,
            icon: const Icon(Icons.workspace_premium_outlined),
            onPressed: () => context.push(AppRoutes.achievements),
          ),
          IconButton(
            tooltip: l10n.screenSettings,
            icon: const Icon(Icons.settings_outlined),
            onPressed: () => context.push(AppRoutes.settings),
          ),
        ],
      ),
      body: child,
    );
  }
}
