import 'package:flutter/material.dart';

import '../../../data/repository/contract/heart_repository.dart';
import '../../../data/repository/contract/vitals_repository.dart';
import '../../../domain/usecase/load_heart_period_use_case.dart';
import '../../../data/source/health/health_permissions.dart';
import '../../../ui/theme/app_colors.dart';

// Vitals accent colours, ported from the Kotlin `HeartVitalsPresentation.kt`.
const Color _oxygenColor = Color(0xFF00897B);
const Color _respiratoryColor = Color(0xFF5E97F6);
const Color _temperatureColor = Color(0xFFFF7043);
const Color _vo2Color = Color(0xFF7E57C2);
const Color _glucoseColor = Color(0xFF8E5D42);

/// The heart + vitals metric family surfaced by the shared period-detail screen.
///
/// Port of the Kotlin `HeartMetric` enum (`HeartScreen.kt`). Each constant knows
/// its route id (the `DashboardMetricId.storageName` used by `/metric/:metricId`),
/// chrome (title/icon/accent), the Health Connect read permission the gate
/// requires, and the [HeartPeriodLoadRequest] its screen loads through
/// [LoadHeartPeriodUseCase] (heart-only vs vitals-only).
enum HeartMetric {
  averageHeartRate('AVG_HEART_RATE', 'Heart rate', Icons.favorite),
  restingHeartRate('RESTING_HEART_RATE', 'Resting heart rate',
      Icons.favorite_border),
  hrv('HRV', 'Heart rate variability', Icons.favorite_border),
  bloodPressure('BLOOD_PRESSURE', 'Blood pressure', Icons.favorite),
  spo2('SPO2', 'Blood oxygen', Icons.favorite_border),
  vo2Max('VO2_MAX', 'VO2 max', Icons.speed),
  respiratoryRate('RESPIRATORY_RATE', 'Respiratory rate', Icons.air),
  bodyTemperature('BODY_TEMPERATURE', 'Body temperature',
      Icons.device_thermostat),
  bloodGlucose('BLOOD_GLUCOSE', 'Blood glucose', Icons.favorite),
  skinTemperature('SKIN_TEMPERATURE', 'Skin temperature',
      Icons.device_thermostat);

  const HeartMetric(this.routeName, this.title, this.icon);

  /// The `DashboardMetricId.storageName` this metric is reached through on the
  /// `/metric/:metricId` route.
  final String routeName;
  final String title;
  final IconData icon;

  Color get accentColor {
    switch (this) {
      case HeartMetric.averageHeartRate:
      case HeartMetric.restingHeartRate:
      case HeartMetric.hrv:
        return AppColors.heart;
      case HeartMetric.bloodPressure:
        return AppColors.vitals;
      case HeartMetric.spo2:
        return _oxygenColor;
      case HeartMetric.vo2Max:
        return _vo2Color;
      case HeartMetric.respiratoryRate:
        return _respiratoryColor;
      case HeartMetric.bodyTemperature:
      case HeartMetric.skinTemperature:
        return _temperatureColor;
      case HeartMetric.bloodGlucose:
        return _glucoseColor;
    }
  }

  /// The Health Connect read permission the [HealthConnectGate] requires.
  String get readPermission {
    switch (this) {
      case HeartMetric.averageHeartRate:
        return HcPermissions.readHeartRate;
      case HeartMetric.restingHeartRate:
        return HcPermissions.readRestingHeartRate;
      case HeartMetric.hrv:
        return HcPermissions.readHrv;
      case HeartMetric.bloodPressure:
        return HcPermissions.readBloodPressure;
      case HeartMetric.spo2:
        return HcPermissions.readSpO2;
      case HeartMetric.vo2Max:
        return HcPermissions.readVo2Max;
      case HeartMetric.respiratoryRate:
        return HcPermissions.readRespiratoryRate;
      case HeartMetric.bodyTemperature:
        return HcPermissions.readBodyTemperature;
      case HeartMetric.bloodGlucose:
        return HcPermissions.readBloodGlucose;
      case HeartMetric.skinTemperature:
        return HcPermissions.readSkinTemperature;
    }
  }

  /// The load request this metric issues (Kotlin `HeartMetric.toLoadRequest`).
  HeartPeriodLoadRequest get loadRequest {
    switch (this) {
      case HeartMetric.averageHeartRate:
        return const HeartPeriodLoadHeartOnly(
            HeartPeriodMetric.averageHeartRate);
      case HeartMetric.restingHeartRate:
        return const HeartPeriodLoadHeartOnly(
            HeartPeriodMetric.restingHeartRate);
      case HeartMetric.hrv:
        return const HeartPeriodLoadHeartOnly(HeartPeriodMetric.hrv);
      case HeartMetric.bloodPressure:
        return const HeartPeriodLoadVitalsOnly(
            VitalsPeriodMetric.bloodPressure);
      case HeartMetric.spo2:
        return const HeartPeriodLoadVitalsOnly(VitalsPeriodMetric.spo2);
      case HeartMetric.vo2Max:
        return const HeartPeriodLoadVitalsOnly(VitalsPeriodMetric.vo2Max);
      case HeartMetric.respiratoryRate:
        return const HeartPeriodLoadVitalsOnly(
            VitalsPeriodMetric.respiratoryRate);
      case HeartMetric.bodyTemperature:
        return const HeartPeriodLoadVitalsOnly(
            VitalsPeriodMetric.bodyTemperature);
      case HeartMetric.bloodGlucose:
        return const HeartPeriodLoadVitalsOnly(VitalsPeriodMetric.bloodGlucose);
      case HeartMetric.skinTemperature:
        return const HeartPeriodLoadVitalsOnly(
            VitalsPeriodMetric.skinTemperature);
    }
  }

  /// Message shown when the selected period has no readings.
  String get emptyMessage {
    switch (this) {
      case HeartMetric.averageHeartRate:
        return 'No heart rate readings for this period.';
      case HeartMetric.restingHeartRate:
        return 'No resting heart rate readings for this period.';
      case HeartMetric.hrv:
        return 'No HRV readings for this period.';
      case HeartMetric.bloodPressure:
        return 'No blood pressure readings for this period.';
      case HeartMetric.spo2:
        return 'No blood oxygen readings for this period.';
      case HeartMetric.vo2Max:
        return 'No VO2 max readings for this period.';
      case HeartMetric.respiratoryRate:
        return 'No respiratory rate readings for this period.';
      case HeartMetric.bodyTemperature:
        return 'No body temperature readings for this period.';
      case HeartMetric.bloodGlucose:
        return 'No blood glucose readings for this period.';
      case HeartMetric.skinTemperature:
        return 'No skin temperature readings for this period.';
    }
  }

  /// Resolves the heart/vitals metric a `/metric/:metricId` argument maps to, or
  /// null when it belongs to another family. Mirrors the Kotlin
  /// `heartMetricFromRoute` (which also accepts the legacy `AVERAGE_HEART_RATE`).
  static HeartMetric? fromRouteName(String? routeName) {
    if (routeName == null) return null;
    if (routeName == 'AVERAGE_HEART_RATE') return HeartMetric.averageHeartRate;
    for (final metric in values) {
      if (metric.routeName == routeName) return metric;
    }
    return null;
  }
}
