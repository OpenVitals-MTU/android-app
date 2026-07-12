import 'package:flutter/material.dart';

import '../../heart/presentation/heart_metric.dart';
import '../../heart/presentation/heart_metric_screen.dart';

/// The route-facing vitals detail screens, ported from the Kotlin
/// `VitalsScreen.kt`. Each is a thin wrapper over the shared [HeartMetricScreen]
/// with a fixed [HeartMetric] (all vitals load through the same
/// `LoadHeartPeriodUseCase` vitals-only path).

class BloodPressureScreen extends StatelessWidget {
  const BloodPressureScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.bloodPressure);
}

class SpO2Screen extends StatelessWidget {
  const SpO2Screen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.spo2);
}

class RespiratoryRateScreen extends StatelessWidget {
  const RespiratoryRateScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.respiratoryRate);
}

class BodyTemperatureScreen extends StatelessWidget {
  const BodyTemperatureScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.bodyTemperature);
}

class Vo2MaxScreen extends StatelessWidget {
  const Vo2MaxScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.vo2Max);
}

class BloodGlucoseScreen extends StatelessWidget {
  const BloodGlucoseScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.bloodGlucose);
}

class SkinTemperatureScreen extends StatelessWidget {
  const SkinTemperatureScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const HeartMetricScreen(metric: HeartMetric.skinTemperature);
}
