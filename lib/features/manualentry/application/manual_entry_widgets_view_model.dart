import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../di/providers.dart';

part 'manual_entry_widgets_view_model.freezed.dart';

/// The entry tiles the add-entry hub offers, in the user's order.
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
List<ManualEntryWidgetId> manualEntryWidgetIdsFromStored(List<String>? stored) {
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

/// Edit state + the visible widget order for the add-entry hub. As in the Kotlin
/// `ManualEntryViewModel`, the persisted order *is* the visible set: removing a
/// widget drops it from the list, adding appends it.
@freezed
abstract class ManualEntryWidgetsState with _$ManualEntryWidgetsState {
  const factory ManualEntryWidgetsState({
    required List<ManualEntryWidgetId> visible,
    @Default(false) bool editing,
  }) = _ManualEntryWidgetsState;
}

class ManualEntryWidgetsViewModel extends Notifier<ManualEntryWidgetsState> {
  @override
  ManualEntryWidgetsState build() => ManualEntryWidgetsState(
        visible: manualEntryWidgetIdsFromStored(
          ref.read(preferencesRepositoryProvider).manualEntryWidgetOrder(),
        ),
      );

  void toggleEditing() => state = state.copyWith(editing: !state.editing);

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
    state = state.copyWith(visible: visible);
  }
}

final manualEntryWidgetsProvider =
    NotifierProvider<ManualEntryWidgetsViewModel, ManualEntryWidgetsState>(
  ManualEntryWidgetsViewModel.new,
);
