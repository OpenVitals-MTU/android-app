/// The "Sync with another phone" wizard screen. Renders each step of
/// [DeviceSyncViewModel]'s state machine, mirroring the design-system mockups.
library;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/source/sync/sync_report.dart';
import '../../../l10n/app_localizations.dart';
import '../application/device_sync_view_model.dart';

/// Groups the syncable record types by category for the picker.
const Map<String, List<String>> _categories = {
  'activity': [
    'StepsRecord',
    'DistanceRecord',
    'ActiveCaloriesBurnedRecord',
    'TotalCaloriesBurnedRecord',
    'FloorsClimbedRecord',
    'ElevationGainedRecord',
    'WheelchairPushesRecord',
    'SpeedRecord',
    'StepsCadenceRecord',
    'CyclingPedalingCadenceRecord',
    'PowerRecord',
  ],
  'workouts': ['ExerciseSessionRecord', 'PlannedExerciseSessionRecord'],
  'heart': [
    'HeartRateRecord',
    'RestingHeartRateRecord',
    'HeartRateVariabilityRmssdRecord',
  ],
  'sleep': ['SleepSessionRecord'],
  'body': [
    'WeightRecord',
    'HeightRecord',
    'BodyFatRecord',
    'LeanBodyMassRecord',
    'BasalMetabolicRateRecord',
    'BoneMassRecord',
    'BodyWaterMassRecord',
  ],
  'vitals': [
    'BloodPressureRecord',
    'OxygenSaturationRecord',
    'RespiratoryRateRecord',
    'BodyTemperatureRecord',
    'Vo2MaxRecord',
    'BloodGlucoseRecord',
    'BasalBodyTemperatureRecord',
    'SkinTemperatureRecord',
  ],
  'nutrition': ['NutritionRecord'],
  'hydration': ['HydrationRecord'],
  'mindfulness': ['MindfulnessSessionRecord'],
  'cycle': [
    'MenstruationFlowRecord',
    'MenstruationPeriodRecord',
    'OvulationTestRecord',
    'CervicalMucusRecord',
    'IntermenstrualBleedingRecord',
    'SexualActivityRecord',
  ],
};

class DeviceSyncScreen extends ConsumerWidget {
  const DeviceSyncScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(deviceSyncProvider);
    final vm = ref.read(deviceSyncProvider.notifier);
    final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.deviceSyncTitle)),
      body: SafeArea(
        child: switch (state.step) {
          DeviceSyncStep.role => _RoleStep(state: state, vm: vm, l10n: l10n),
          DeviceSyncStep.hostWaiting => _HostStep(state: state, l10n: l10n),
          DeviceSyncStep.guestScanning =>
            _ScanStep(state: state, vm: vm, l10n: l10n),
          DeviceSyncStep.guestCode => _CodeStep(state: state, vm: vm, l10n: l10n),
          DeviceSyncStep.range => _RangeStep(state: state, vm: vm, l10n: l10n),
          DeviceSyncStep.types => _TypesStep(state: state, vm: vm, l10n: l10n),
          DeviceSyncStep.syncing =>
            _ProgressStep(state: state, vm: vm, l10n: l10n),
          DeviceSyncStep.report => _ReportStep(state: state, vm: vm, l10n: l10n),
        },
      ),
    );
  }
}

Widget _hero(BuildContext context, IconData icon, String title, String body) {
  final theme = Theme.of(context);
  return Padding(
    padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        CircleAvatar(
          backgroundColor: theme.colorScheme.primaryContainer,
          child: Icon(icon, color: theme.colorScheme.onPrimaryContainer),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: theme.textTheme.titleLarge),
              const SizedBox(height: 2),
              Text(body, style: theme.textTheme.bodyMedium),
            ],
          ),
        ),
      ],
    ),
  );
}

class _RoleStep extends StatelessWidget {
  const _RoleStep({required this.state, required this.vm, required this.l10n});
  final DeviceSyncState state;
  final DeviceSyncViewModel vm;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        _hero(context, Icons.devices_other, l10n.deviceSyncRoleHeading,
            l10n.deviceSyncRoleBody),
        if (state.bluetoothUnavailable)
          _banner(context, l10n.deviceSyncBluetoothOff),
        Card(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          child: ListTile(
            leading: const Icon(Icons.wifi_tethering),
            title: Text(l10n.deviceSyncHostOption),
            subtitle: Text(l10n.deviceSyncHostOptionBody),
            trailing: const Icon(Icons.chevron_right),
            onTap: vm.chooseHost,
          ),
        ),
        Card(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
          child: ListTile(
            leading: const Icon(Icons.phonelink_ring),
            title: Text(l10n.deviceSyncGuestOption),
            subtitle: Text(l10n.deviceSyncGuestOptionBody),
            trailing: const Icon(Icons.chevron_right),
            onTap: vm.chooseGuest,
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              const Icon(Icons.lock_outline, size: 20),
              const SizedBox(width: 10),
              Expanded(
                child: Text(l10n.deviceSyncPrivacyNote,
                    style: Theme.of(context).textTheme.bodySmall),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _HostStep extends StatelessWidget {
  const _HostStep({required this.state, required this.l10n});
  final DeviceSyncState state;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        _hero(context, Icons.wifi_tethering, l10n.deviceSyncHostHeading,
            l10n.deviceSyncHostBody),
        Card(
          margin: const EdgeInsets.all(16),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              children: [
                Text(l10n.deviceSyncCodeLabel,
                    style: Theme.of(context).textTheme.labelLarge),
                const SizedBox(height: 12),
                Text(
                  state.code,
                  style: Theme.of(context).textTheme.displaySmall?.copyWith(
                        letterSpacing: 8,
                        fontFeatures: const [FontFeature.tabularFigures()],
                      ),
                ),
                const SizedBox(height: 16),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2)),
                    const SizedBox(width: 8),
                    Text(l10n.deviceSyncWaiting),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _ScanStep extends StatelessWidget {
  const _ScanStep({required this.state, required this.vm, required this.l10n});
  final DeviceSyncState state;
  final DeviceSyncViewModel vm;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _hero(context, Icons.bluetooth_searching, l10n.deviceSyncScanHeading,
            l10n.deviceSyncScanBody),
        Expanded(
          child: ListView(
            children: [
              for (final device in state.devices)
                Card(
                  margin:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                  child: ListTile(
                    leading: const Icon(Icons.smartphone),
                    title: Text(device.name ?? device.address),
                    subtitle: device.bonded ? Text(l10n.deviceSyncPaired) : null,
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => vm.selectDevice(device),
                  ),
                ),
              const Padding(
                padding: EdgeInsets.all(24),
                child: Center(
                  child: SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2)),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _CodeStep extends StatelessWidget {
  const _CodeStep({required this.state, required this.vm, required this.l10n});
  final DeviceSyncState state;
  final DeviceSyncViewModel vm;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final name = state.selectedDevice?.name ??
        state.selectedDevice?.address ??
        '';
    return ListView(
      children: [
        _hero(context, Icons.password, l10n.deviceSyncCodeHeading(name),
            l10n.deviceSyncCodeBody),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              for (var i = 0; i < 6; i++)
                Container(
                  width: 40,
                  height: 52,
                  margin: const EdgeInsets.all(4),
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: theme.colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(
                      color: i == state.codeEntry.length
                          ? theme.colorScheme.primary
                          : Colors.transparent,
                      width: 2,
                    ),
                  ),
                  child: Text(
                    i < state.codeEntry.length ? state.codeEntry[i] : '',
                    style: theme.textTheme.headlineSmall,
                  ),
                ),
            ],
          ),
        ),
        if (state.codeError)
          _banner(context, l10n.deviceSyncWrongCode, isError: true),
        Padding(
          padding: const EdgeInsets.all(16),
          child: _Keypad(
            onDigit: vm.enterDigit,
            onDelete: vm.deleteDigit,
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: FilledButton(
            onPressed:
                state.codeEntry.length == 6 ? vm.submitCode : null,
            child: Text(l10n.deviceSyncConnect),
          ),
        ),
      ],
    );
  }
}

class _Keypad extends StatelessWidget {
  const _Keypad({required this.onDigit, required this.onDelete});
  final void Function(String) onDigit;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) {
    final keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '', '0', 'del'];
    return GridView.count(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisCount: 3,
      childAspectRatio: 2,
      mainAxisSpacing: 8,
      crossAxisSpacing: 8,
      children: [
        for (final key in keys)
          if (key.isEmpty)
            const SizedBox.shrink()
          else
            OutlinedButton(
              onPressed: () => key == 'del' ? onDelete() : onDigit(key),
              child: key == 'del'
                  ? const Icon(Icons.backspace_outlined)
                  : Text(key, style: Theme.of(context).textTheme.titleLarge),
            ),
      ],
    );
  }
}

class _RangeStep extends StatelessWidget {
  const _RangeStep({required this.state, required this.vm, required this.l10n});
  final DeviceSyncState state;
  final DeviceSyncViewModel vm;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final options = <(SyncRange, String)>[
      (SyncRange.days30, l10n.deviceSyncRange30),
      (SyncRange.months6, l10n.deviceSyncRange6mo),
      (SyncRange.year1, l10n.deviceSyncRange1y),
      (SyncRange.all, l10n.deviceSyncRangeAll),
    ];
    return Column(
      children: [
        _hero(context, Icons.history, l10n.deviceSyncRangeHeading,
            l10n.deviceSyncRangeBody),
        Expanded(
          child: ListView(
            children: [
              for (final (range, label) in options)
                ListTile(
                  leading: Icon(
                    state.range == range
                        ? Icons.radio_button_checked
                        : Icons.radio_button_unchecked,
                    color: state.range == range
                        ? Theme.of(context).colorScheme.primary
                        : null,
                  ),
                  title: Text(label),
                  onTap: () => vm.setRange(range),
                ),
            ],
          ),
        ),
        _bottomButton(context, l10n.deviceSyncNext, vm.goToTypes),
      ],
    );
  }
}

class _TypesStep extends StatelessWidget {
  const _TypesStep({required this.state, required this.vm, required this.l10n});
  final DeviceSyncState state;
  final DeviceSyncViewModel vm;
  final AppLocalizations l10n;

  String _categoryLabel(String key) => switch (key) {
        'activity' => l10n.deviceSyncCategoryActivity,
        'workouts' => l10n.deviceSyncCategoryWorkouts,
        'heart' => l10n.deviceSyncCategoryHeart,
        'sleep' => l10n.deviceSyncCategorySleep,
        'body' => l10n.deviceSyncCategoryBody,
        'vitals' => l10n.deviceSyncCategoryVitals,
        'nutrition' => l10n.deviceSyncCategoryNutrition,
        'hydration' => l10n.deviceSyncCategoryHydration,
        'cycle' => l10n.deviceSyncCategoryCycle,
        _ => l10n.deviceSyncCategoryMindfulness,
      };

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _hero(context, Icons.checklist, l10n.deviceSyncTypesHeading,
            l10n.deviceSyncTypesBody),
        Expanded(
          child: ListView(
            children: [
              for (final entry in _categories.entries)
                // Only the types this device's Health Connect provider supports.
                if (entry.value.any(state.availableTypes.contains))
                  Builder(builder: (context) {
                    final types = [
                      for (final t in entry.value)
                        if (state.availableTypes.contains(t)) t,
                    ];
                    return CheckboxListTile(
                      value: types.every(state.selectedTypes.contains),
                      onChanged: (_) {
                        final allOn =
                            types.every(state.selectedTypes.contains);
                        for (final type in types) {
                          if (allOn == state.selectedTypes.contains(type)) {
                            vm.toggleType(type);
                          }
                        }
                      },
                      title: Text(_categoryLabel(entry.key)),
                    );
                  }),
            ],
          ),
        ),
        _bottomButton(
          context,
          l10n.deviceSyncStartSync(state.selectedTypes.length),
          state.selectedTypes.isEmpty ? null : vm.startSync,
        ),
      ],
    );
  }
}

class _ProgressStep extends StatelessWidget {
  const _ProgressStep(
      {required this.state, required this.vm, required this.l10n});
  final DeviceSyncState state;
  final DeviceSyncViewModel vm;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final progress = state.progress;
    final phase = switch (progress?.phase) {
      SyncPhase.writing => l10n.deviceSyncPhaseWriting,
      SyncPhase.complete => l10n.deviceSyncPhaseWriting,
      SyncPhase.exchanging => l10n.deviceSyncPhaseExchanging,
      _ => l10n.deviceSyncPhaseHandshake,
    };
    return ListView(
      children: [
        _hero(context, Icons.sync, l10n.deviceSyncProgressHeading, phase),
        const Padding(
          padding: EdgeInsets.all(24),
          child: LinearProgressIndicator(),
        ),
        _statRow(context, l10n.deviceSyncSent, progress?.itemsSent ?? 0),
        _statRow(context, l10n.deviceSyncReceived, progress?.itemsReceived ?? 0),
        _statRow(context, l10n.deviceSyncWritten, progress?.itemsWritten ?? 0),
      ],
    );
  }
}

class _ReportStep extends StatelessWidget {
  const _ReportStep(
      {required this.state, required this.vm, required this.l10n});
  final DeviceSyncState state;
  final DeviceSyncViewModel vm;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final report = state.report;
    final imported = report?.imported ?? 0;
    return ListView(
      children: [
        Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              Icon(Icons.task_alt,
                  size: 56, color: Theme.of(context).colorScheme.primary),
              const SizedBox(height: 12),
              Text(l10n.deviceSyncReportHeading(imported),
                  style: Theme.of(context).textTheme.headlineSmall),
            ],
          ),
        ),
        _statRow(context, l10n.deviceSyncImported, imported),
        _statRow(context, l10n.deviceSyncDuplicates,
            report?.duplicateSkipped ?? 0),
        for (final summary in report?.typeSummaries ?? const <SyncTypeSummary>[])
          ListTile(
            dense: true,
            title: Text(summary.recordType),
            trailing: Text('+${summary.imported}'),
          ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: FilledButton(
            onPressed: () {
              vm.reset();
              Navigator.of(context).maybePop();
            },
            child: Text(l10n.deviceSyncDone),
          ),
        ),
      ],
    );
  }
}

Widget _statRow(BuildContext context, String label, int value) => ListTile(
      title: Text(label),
      trailing: Text('$value',
          style: Theme.of(context).textTheme.titleMedium),
    );

Widget _bottomButton(BuildContext context, String label, VoidCallback? onTap) =>
    Padding(
      padding: const EdgeInsets.all(16),
      child: SizedBox(
        width: double.infinity,
        child: FilledButton(onPressed: onTap, child: Text(label)),
      ),
    );

Widget _banner(BuildContext context, String message, {bool isError = false}) {
  final scheme = Theme.of(context).colorScheme;
  return Container(
    margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
    padding: const EdgeInsets.all(14),
    decoration: BoxDecoration(
      color: isError ? scheme.errorContainer : scheme.surfaceContainerHighest,
      borderRadius: BorderRadius.circular(12),
    ),
    child: Row(
      children: [
        Icon(isError ? Icons.error_outline : Icons.info_outline,
            color: isError ? scheme.onErrorContainer : scheme.onSurface),
        const SizedBox(width: 10),
        Expanded(child: Text(message)),
      ],
    ),
  );
}
