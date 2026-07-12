import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/presentation/reorder.dart';
import '../../di/providers.dart';
import '../../domain/model/body_models.dart';
import '../../domain/model/vitals_models.dart';
import '../../data/source/health/health_permissions.dart';
import '../../navigation/app_routes.dart';
import '../../ui/components/health_connect_gate.dart';
import '../../ui/components/loading_state.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/components/widget_edit_controls.dart';
import '../../ui/theme/app_colors.dart';

/// The add-entry hub rendered inside the adaptive scaffold's "Add entry" branch.
///
/// Riverpod/Flutter port of the Kotlin `ManualEntryScreen` + `ManualEntryWidgets`:
/// a grid of entry-type tiles (in the user's saved order) that route to each
/// manual-entry form, plus the same edit mode as the dashboard carousel —
/// long-press to drag-reorder, ✕ to remove, and an "Add widgets" tray to restore.
///
/// Only entry types the installed Health Connect provider can accept writes for
/// are shown; the rest can never be granted, so a tile for them would be a dead
/// end. Per-tile write-permission prompts stay in the forms themselves, which
/// each guard writes behind a Health Connect gate.
class ManualEntryScreen extends ConsumerWidget {
  const ManualEntryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Resolves the provider's feature flags + supported-permission set, which
    // `supportedManualEntryWidgets` reads. Without it the taxonomy would sit at
    // its defaults and misreport what the device can do.
    final availability = ref.watch(healthConnectAvailabilityProvider).value;
    if (availability == null) return const FullScreenLoading();

    final state = ref.watch(manualEntryWidgetsProvider);
    final notifier = ref.read(manualEntryWidgetsProvider.notifier);
    final supported = supportedManualEntryWidgets(
      ref.watch(healthRepositoryProvider).managedPermissions,
      mindfulnessAvailable:
          ref.watch(healthRepositoryProvider).isMindfulnessAvailable(),
    );

    final visible = [
      for (final id in state.visible)
        if (supported.contains(id)) id,
    ];
    final hidden = [
      for (final id in ManualEntryWidgetId.values)
        if (supported.contains(id) && !visible.contains(id)) id,
    ];

    return ListView(
      padding: const EdgeInsets.fromLTRB(12, 4, 12, 24),
      children: [
        _EditModeBar(
          editing: state.editing,
          onToggleEdit: notifier.toggleEditing,
        ),
        if (state.editing)
          const Padding(
            padding: EdgeInsets.fromLTRB(4, 0, 4, 10),
            child: EditModeHint(),
          ),
        GridView.count(
          crossAxisCount: 3,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 8,
          crossAxisSpacing: 8,
          children: [
            for (var i = 0; i < visible.length; i++)
              _ManualEntryCell(
                spec: _specsById[visible[i]]!,
                index: i,
                editing: state.editing,
                onTap: () => context.push(_specsById[visible[i]]!.location),
                onRemove: () => notifier.remove(visible[i]),
                onReorder: (from, to) =>
                    notifier.setOrder(reorderOntoDropTarget(visible, from, to)),
              ),
          ],
        ),
        if (state.editing)
          HiddenWidgetsSection(
            padding: const EdgeInsets.fromLTRB(4, 20, 4, 0),
            titles: [for (final id in hidden) _specsById[id]!.title],
            onAdd: (title) => notifier.add(
              hidden.firstWhere((id) => _specsById[id]!.title == title),
            ),
          ),
      ],
    );
  }
}

/// The Edit / Done toggle, mirroring the dashboard's quick-action pencil.
class _EditModeBar extends StatelessWidget {
  const _EditModeBar({required this.editing, required this.onToggleEdit});

  final bool editing;
  final VoidCallback onToggleEdit;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Align(
      alignment: Alignment.centerRight,
      child: IconButton(
        onPressed: onToggleEdit,
        tooltip: editing ? 'Done' : 'Edit entries',
        isSelected: editing,
        icon: Icon(
          editing ? Icons.check : Icons.edit_outlined,
          color: editing ? scheme.primary : scheme.onSurfaceVariant,
        ),
      ),
    );
  }
}

/// One grid cell: a plain tappable tile, or — while [editing] — a long-press
/// draggable reorder source/target carrying a remove button.
class _ManualEntryCell extends StatelessWidget {
  const _ManualEntryCell({
    required this.spec,
    required this.index,
    required this.editing,
    required this.onTap,
    required this.onRemove,
    required this.onReorder,
  });

  final _ManualEntryWidgetSpec spec;
  final int index;
  final bool editing;
  final VoidCallback onTap;
  final VoidCallback onRemove;
  final void Function(int from, int to) onReorder;

  @override
  Widget build(BuildContext context) {
    if (!editing) return _ManualEntryTile(spec: spec, onTap: onTap);

    final card = Stack(
      children: [
        Positioned.fill(child: _ManualEntryTile(spec: spec)),
        Positioned(
          top: 0,
          right: 0,
          child: RemoveWidgetButton(onPressed: onRemove),
        ),
      ],
    );

    return ReorderableEditTile(
      index: index,
      onReorder: onReorder,
      feedbackSize: const Size(104, 104),
      feedbackBorderRadius: const BorderRadius.all(Radius.circular(16)),
      child: card,
    );
  }
}


/// The manual-entry widgets the installed provider can accept writes for. A
/// widget needs *all* of its write permissions defined on the device; anything
/// else can never be granted.
Set<ManualEntryWidgetId> supportedManualEntryWidgets(
  Set<String> managedPermissions, {
  required bool mindfulnessAvailable,
}) =>
    {
      for (final id in ManualEntryWidgetId.values)
        if (_writePermissionsFor(id).every(managedPermissions.contains) &&
            (id != ManualEntryWidgetId.mindfulness || mindfulnessAvailable))
          id,
    };

Set<String> _writePermissionsFor(ManualEntryWidgetId id) => switch (id) {
      ManualEntryWidgetId.hydration => {HcPermissions.writeHydration},
      ManualEntryWidgetId.carbs => {HcPermissions.writeNutrition},
      ManualEntryWidgetId.activity => {HcPermissions.writeExercise},
      ManualEntryWidgetId.mindfulness => {HcPermissions.writeMindfulness},
      ManualEntryWidgetId.weight => {HcPermissions.writeWeight},
      ManualEntryWidgetId.height => {HcPermissions.writeHeight},
      ManualEntryWidgetId.bodyFat => {HcPermissions.writeBodyFat},
      ManualEntryWidgetId.bloodPressure => {HcPermissions.writeBloodPressure},
      ManualEntryWidgetId.spo2 => {HcPermissions.writeSpO2},
      ManualEntryWidgetId.respiratoryRate =>
        {HcPermissions.writeRespiratoryRate},
      ManualEntryWidgetId.bodyTemperature =>
        {HcPermissions.writeBodyTemperature},
    };

/// Edit state + the visible widget order for the add-entry hub. As in the Kotlin
/// `ManualEntryViewModel`, the persisted order *is* the visible set: removing a
/// widget drops it from the list, adding appends it.
class ManualEntryWidgetsState {
  const ManualEntryWidgetsState({required this.visible, this.editing = false});

  final List<ManualEntryWidgetId> visible;
  final bool editing;
}

class ManualEntryWidgetsNotifier extends Notifier<ManualEntryWidgetsState> {
  @override
  ManualEntryWidgetsState build() => ManualEntryWidgetsState(
        visible: _widgetIdsFromStored(
          ref.read(preferencesRepositoryProvider).manualEntryWidgetOrder(),
        ),
      );

  void toggleEditing() =>
      state = ManualEntryWidgetsState(
        visible: state.visible,
        editing: !state.editing,
      );

  void remove(ManualEntryWidgetId id) =>
      setOrder([for (final it in state.visible) if (it != id) it]);

  void add(ManualEntryWidgetId id) {
    if (state.visible.contains(id)) return;
    setOrder([...state.visible, id]);
  }

  void setOrder(List<ManualEntryWidgetId> visible) {
    ref
        .read(preferencesRepositoryProvider)
        .setManualEntryWidgetOrder([for (final id in visible) id.storageName]);
    state = ManualEntryWidgetsState(visible: visible, editing: state.editing);
  }
}

final manualEntryWidgetsProvider =
    NotifierProvider<ManualEntryWidgetsNotifier, ManualEntryWidgetsState>(
  ManualEntryWidgetsNotifier.new,
);

class _ManualEntryTile extends StatelessWidget {
  const _ManualEntryTile({required this.spec, this.onTap});

  final _ManualEntryWidgetSpec spec;

  /// Null while editing — the tile must not navigate under a drag.
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Icon(spec.icon, color: spec.accentColor, size: 30),
            Text(
              spec.title,
              style: theme.textTheme.labelMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}

/// Port of the Kotlin `ManualEntryWidgetId` enum. The `storageName` matches the
/// Kotlin enum-constant name persisted in the widget-order preference.
enum ManualEntryWidgetId {
  hydration('HYDRATION'),
  carbs('CARBS'),
  activity('ACTIVITY'),
  mindfulness('MINDFULNESS'),
  weight('WEIGHT'),
  height('HEIGHT'),
  bodyFat('BODY_FAT'),
  bloodPressure('BLOOD_PRESSURE'),
  spo2('SPO2'),
  respiratoryRate('RESPIRATORY_RATE'),
  bodyTemperature('BODY_TEMPERATURE');

  const ManualEntryWidgetId(this.storageName);

  final String storageName;

  static ManualEntryWidgetId? fromStorage(String value) {
    for (final id in values) {
      if (id.storageName == value) return id;
    }
    return null;
  }
}

/// The default order (Kotlin `DefaultManualEntryWidgetIds`).
const List<ManualEntryWidgetId> _defaultWidgetIds = ManualEntryWidgetId.values;

/// Resolves the stored order into widget ids, falling back to the default set.
/// Port of the Kotlin `manualEntryWidgetIdsFromStored`.
List<ManualEntryWidgetId> _widgetIdsFromStored(List<String>? stored) {
  if (stored == null) return _defaultWidgetIds;
  if (stored.isEmpty) return const <ManualEntryWidgetId>[];
  final parsed = <ManualEntryWidgetId>[];
  final seen = <ManualEntryWidgetId>{};
  for (final raw in stored) {
    final id = ManualEntryWidgetId.fromStorage(raw);
    if (id != null && seen.add(id)) parsed.add(id);
  }
  return parsed.isEmpty ? _defaultWidgetIds : parsed;
}

class _ManualEntryWidgetSpec {
  const _ManualEntryWidgetSpec({
    required this.title,
    required this.icon,
    required this.accentColor,
    required this.location,
  });

  final String title;
  final IconData icon;
  final Color accentColor;
  final String location;
}

const Color _oxygenColor = Color(0xFF00897B);
const Color _respiratoryColor = Color(0xFF5E97F6);
const Color _temperatureColor = Color(0xFFFF7043);

final Map<ManualEntryWidgetId, _ManualEntryWidgetSpec> _specsById = {
  ManualEntryWidgetId.hydration: const _ManualEntryWidgetSpec(
    title: 'Hydration',
    icon: Icons.local_drink_outlined,
    accentColor: AppColors.hydration,
    location: AppRoutes.hydrationEntry,
  ),
  ManualEntryWidgetId.carbs: const _ManualEntryWidgetSpec(
    title: 'Carbs',
    icon: Icons.restaurant_outlined,
    accentColor: AppColors.nutrition,
    location: AppRoutes.carbsEntry,
  ),
  ManualEntryWidgetId.activity: _ManualEntryWidgetSpec(
    title: 'Activity',
    icon: Icons.directions_run_outlined,
    accentColor: AppColors.workout,
    location: AppRoutes.activityEntryLocation(),
  ),
  ManualEntryWidgetId.mindfulness: const _ManualEntryWidgetSpec(
    title: 'Mindfulness',
    icon: Icons.self_improvement,
    accentColor: AppColors.mindfulness,
    location: AppRoutes.mindfulnessEntry,
  ),
  ManualEntryWidgetId.weight: _ManualEntryWidgetSpec(
    title: 'Weight',
    icon: Icons.monitor_weight_outlined,
    accentColor: AppColors.weight,
    location: AppRoutes.bodyMeasurementEntryLocation(
      BodyMeasurementType.weight.storageName,
    ),
  ),
  ManualEntryWidgetId.height: _ManualEntryWidgetSpec(
    title: 'Height',
    icon: Icons.straighten_outlined,
    accentColor: AppColors.weight,
    location: AppRoutes.bodyMeasurementEntryLocation(
      BodyMeasurementType.height.storageName,
    ),
  ),
  ManualEntryWidgetId.bodyFat: _ManualEntryWidgetSpec(
    title: 'Body fat',
    icon: Icons.monitor_weight_outlined,
    accentColor: AppColors.bodyFat,
    location: AppRoutes.bodyMeasurementEntryLocation(
      BodyMeasurementType.bodyFat.storageName,
    ),
  ),
  ManualEntryWidgetId.bloodPressure: _ManualEntryWidgetSpec(
    title: 'Blood pressure',
    icon: Icons.favorite,
    accentColor: AppColors.vitals,
    location: AppRoutes.vitalsMeasurementEntryLocation(
      VitalsMeasurementType.bloodPressure.storageName,
    ),
  ),
  ManualEntryWidgetId.spo2: _ManualEntryWidgetSpec(
    title: 'Blood oxygen',
    icon: Icons.favorite_border,
    accentColor: _oxygenColor,
    location: AppRoutes.vitalsMeasurementEntryLocation(
      VitalsMeasurementType.spo2.storageName,
    ),
  ),
  ManualEntryWidgetId.respiratoryRate: _ManualEntryWidgetSpec(
    title: 'Respiratory rate',
    icon: Icons.air,
    accentColor: _respiratoryColor,
    location: AppRoutes.vitalsMeasurementEntryLocation(
      VitalsMeasurementType.respiratoryRate.storageName,
    ),
  ),
  ManualEntryWidgetId.bodyTemperature: _ManualEntryWidgetSpec(
    title: 'Body temperature',
    icon: Icons.device_thermostat,
    accentColor: _temperatureColor,
    location: AppRoutes.vitalsMeasurementEntryLocation(
      VitalsMeasurementType.bodyTemperature.storageName,
    ),
  ),
};
