import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/presentation/command_state.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../core/result/result.dart';
import '../../../di/providers.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../../../ui/theme/app_colors.dart';
import 'hydration_catalog_widgets.dart';
import 'hydration_drink_dialogs.dart';
import '../application/hydration_entry_view_model.dart';
import 'manual_entry_form_scaffold.dart';
import 'manual_entry_timestamp_fields.dart';

/// Hydration manual-entry screen pushed over the shell. Backs three routes:
/// new entry, edit (carries [hydrationEntryId]) and log-drink (carries
/// [logDrinkId], which opens straight into that saved drink's entry dialog).
///
/// Riverpod/Flutter port of the Kotlin `HydrationEntryScreen` +
/// `HydrationTrackerCard`: today's counter, then either the drink catalog and a
/// "New drink" action, or — when editing an existing record — the entry-time
/// fields and Save.
///
/// The screen is deliberately *not* gated on the write permissions: a user
/// without nutrition-write can still log plain water, so the permission state is
/// reported in the card's subtitle with a Grant action, as in Kotlin.
class HydrationEntryScreen extends ConsumerStatefulWidget {
  const HydrationEntryScreen({
    super.key,
    this.hydrationEntryId,
    this.logDrinkId,
  });

  final String? hydrationEntryId;
  final String? logDrinkId;

  @override
  ConsumerState<HydrationEntryScreen> createState() =>
      _HydrationEntryScreenState();
}

class _HydrationEntryScreenState extends ConsumerState<HydrationEntryScreen>
    with RefreshPermissionOnResume {
  @override
  void refreshPermission() => ref.read(_provider.notifier).refreshPermission();

  late final NotifierProvider<HydrationEntryViewModel, HydrationEntryState>
      _provider = NotifierProvider.autoDispose<HydrationEntryViewModel,
          HydrationEntryState>(
    () => HydrationEntryViewModel(editRecordId: widget.hydrationEntryId),
  );

  /// Guards the deep link against re-firing, as the Kotlin
  /// `handledInitialLogDrinkId` does.
  bool _handledInitialLogDrink = false;

  /// Opens the "log this drink" deep link once its drink appears.
  ///
  /// The catalog loads asynchronously from the beverage store, so the drink is
  /// not there on the first frame. Kotlin keys its `LaunchedEffect` on
  /// `state.customDrinkOptions` for exactly this reason.
  void _maybeOpenInitialLogDrink(List<CustomHydrationDrink> drinks) {
    final drinkId = widget.logDrinkId;
    if (drinkId == null || _handledInitialLogDrink) return;
    final drink = drinks.where((it) => it.id == drinkId).firstOrNull;
    if (drink == null) return;
    _handledInitialLogDrink = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _openLogDrinkDialog(drink);
    });
  }

  Future<void> _openLogDrinkDialog(CustomHydrationDrink drink) async {
    final result = await showSavedDrinkEntryDialog(
      context,
      drink,
      ref.read(unitFormatterProvider),
    );
    if (result == null || !mounted) return;
    await ref.read(_provider.notifier).addSavedCustomDrinkEntry(
          drink,
          amountMilliliters: result.amountMilliliters,
          entryTime: result.entryTime,
        );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    ref.listen(_provider.select((s) => s.customDrinkOptions),
        (previous, next) => _maybeOpenInitialLogDrink(next));
    // The catalog may already be loaded when this screen rebuilds.
    _maybeOpenInitialLogDrink(ref.read(_provider).customDrinkOptions);
    ref.listen(_provider.select((s) => s.save), (previous, next) {
      // The success is consumed exactly once, then the command is put back to
      // rest — otherwise returning to this route would replay the toast.
      if (next is! CommandSuccess<void>) return;
      final notifier = ref.read(_provider.notifier);
      notifier.onSaveCompletedHandled();
      // Editing an existing record is a one-shot, so it returns. Logging a new
      // drink keeps the catalog open — the today counter updates in place and
      // the user can log another.
      final isEdit = ref.read(_provider).isEditMode;
      onManualEntrySaved(context, 'Hydration entry saved', pop: isEdit);
    });

    return Scaffold(
      appBar: AppBar(title: Text(l10n.screenHydrationEntry)),
      body: HealthConnectGate(
        // Availability only; the write permissions are handled in-card.
        child: _HydrationEntryForm(provider: _provider),
      ),
    );
  }
}

class _HydrationEntryForm extends ConsumerWidget {
  const _HydrationEntryForm({required this.provider});

  final NotifierProvider<HydrationEntryViewModel, HydrationEntryState> provider;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(provider);
    final notifier = ref.read(provider.notifier);
    final formatter = ref.watch(unitFormatterProvider);

    final canInteract = !state.isSavingEntry && !state.isCheckingPermission;
    final canLogHydrationEntry = state.canWriteHydration && canInteract;
    final needsPermission = !state.canWriteHydration || !state.canWriteNutrition;

    final subtitle = !state.canWriteHydration
        ? l10n.hydrationTrackerPermissionNeeded
        : (!state.canWriteNutrition
            ? l10n.hydrationNutritionPermissionNeeded
            : l10n.hydrationTrackerSubtitle);

    return ListView(
      padding: screenScrollPadding(context),
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: OpenVitalsCard(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          color: AppColors.hydration.withValues(alpha: 0.16),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.local_drink_outlined,
                            size: 20, color: AppColors.hydration),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(l10n.hydrationTrackerTitle,
                                style: theme.textTheme.titleMedium),
                            Text(
                              subtitle,
                              style: theme.textTheme.bodySmall?.copyWith(
                                color: theme.colorScheme.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                      if (needsPermission && !state.isCheckingPermission)
                        OutlinedButton(
                          onPressed: () => _requestWritePermissions(ref, state),
                          child: Text(l10n.actionGrant),
                        ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  HydrationTodayCounter(
                    liters: state.todayHydrationLiters,
                    dailyGoalLiters: state.dailyGoalLiters,
                    formatter: formatter,
                  ),
                  if (state.entryNotice != null) ...[
                    const SizedBox(height: 12),
                    HydrationEntryNoticeCallout(notice: state.entryNotice!),
                  ],
                  const SizedBox(height: 12),
                  if (state.isEditMode) ...[
                    // Editing an existing record only changes when it happened —
                    // its volume came from the record itself.
                    ManualEntryTimestampFields(
                      timestamp: state.editTime,
                      enabled: !state.isSavingEntry,
                      onChanged: notifier.updateEntryTime,
                    ),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      onPressed: canLogHydrationEntry
                          ? notifier.addSelectedHydrationEntry
                          : null,
                      icon: const Icon(Icons.check, size: 18),
                      label: Text(l10n.actionSave),
                    ),
                  ] else ...[
                    HydrationCatalogCarousel(
                      savedDrinks: state.customDrinkOptions,
                      frequentDrinks: state.frequentDrinkOptions,
                      formatter: formatter,
                      canEditSavedDrinks: canInteract,
                      canSelectDrink: (drink) => _canLogDrink(state, drink),
                      onSelectDrink: (drink) =>
                          _logDrink(context, notifier, formatter, drink),
                      onEditDrink: (drink) =>
                          _editDrink(context, notifier, formatter, drink),
                      onDeleteDrink: notifier.deleteCustomDrink,
                      onMoveDrinkToTarget: notifier.moveCustomDrinkToTarget,
                      onMoveDrinkToCategory: notifier.moveCustomDrinkToCategory,
                    ),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      onPressed: canInteract
                          ? () => _createDrink(context, notifier, formatter)
                          : null,
                      icon: const Icon(Icons.add, size: 18),
                      label: Text(l10n.hydrationNewDrinkAction),
                    ),
                  ],
                  if (_errorText(state, l10n) case final message?)
                    Padding(
                      padding: const EdgeInsets.only(top: 12),
                      child: Text(
                        message,
                        style: theme.textTheme.bodySmall
                            ?.copyWith(color: theme.colorScheme.error),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _requestWritePermissions(
    WidgetRef ref,
    HydrationEntryState state,
  ) async {
    (await ref
            .read(healthRepositoryProvider)
            .requestPermissions(state.writePermissions))
        .orThrow();
    await ref.read(provider.notifier).refreshPermission();
  }

  /// Port of the Kotlin `canLogDrink`: a drink that writes only nutrients needs
  /// the nutrition permission, not the hydration one.
  bool _canLogDrink(HydrationEntryState state, CustomHydrationDrink drink) {
    final canInteract = !state.isSavingEntry && !state.isCheckingPermission;
    final writesHydration = drink.volumeLiters * drink.hydrationMultiplier > 0.0;
    final writesNutrition =
        drink.nutrientValues.values.any((it) => it > 0.0 && it.isFinite);
    return canInteract &&
        (!writesHydration || state.canWriteHydration) &&
        (!writesNutrition || state.canWriteNutrition);
  }

  Future<void> _createDrink(
    BuildContext context,
    HydrationEntryViewModel notifier,
    UnitFormatter formatter,
  ) async {
    final input = await showCustomDrinkDialog(context, formatter);
    if (input != null) await notifier.saveCustomDrink(input);
  }

  Future<void> _editDrink(
    BuildContext context,
    HydrationEntryViewModel notifier,
    UnitFormatter formatter,
    CustomHydrationDrink drink,
  ) async {
    final input =
        await showCustomDrinkDialog(context, formatter, existing: drink);
    if (input != null) {
      await notifier.saveCustomDrink(input, existingDrinkId: drink.id);
    }
  }

  Future<void> _logDrink(
    BuildContext context,
    HydrationEntryViewModel notifier,
    UnitFormatter formatter,
    CustomHydrationDrink drink,
  ) async {
    final result = await showSavedDrinkEntryDialog(context, drink, formatter);
    if (result == null) return;
    await notifier.addSavedCustomDrinkEntry(
      drink,
      amountMilliliters: result.amountMilliliters,
      entryTime: result.entryTime,
    );
  }

  /// The one message the form shows: why the drink was refused, or — failing
  /// that — why the last attempt to log it (or to read the entry being edited)
  /// did not land.
  String? _errorText(HydrationEntryState state, AppLocalizations l10n) {
    final blocking = state.blockingError;
    final entryError = state.entryError;
    if (entryError != null) {
      return switch (entryError) {
        HydrationEntryError.invalidAmount => l10n.hydrationInvalidAmount,
        HydrationEntryError.invalidCustomDrink =>
          l10n.hydrationCustomDrinkInvalid,
        HydrationEntryError.missingWritePermission =>
          l10n.hydrationTrackerPermissionNeeded,
        HydrationEntryError.missingNutritionWritePermission =>
          l10n.hydrationNutritionPermissionNeeded,
        // This form never raises writeFailed any more — an attempted write that
        // failed is a CommandFailure on `save`. The member survives because the
        // enum is the use case's, and the quick-beverage widget still maps it.
        HydrationEntryError.writeFailed =>
          l10n.hydrationWriteFailed(screenErrorText(blocking, l10n)),
      };
    }
    if (blocking == null) return null;
    return l10n.hydrationWriteFailed(screenErrorText(blocking, l10n));
  }
}
