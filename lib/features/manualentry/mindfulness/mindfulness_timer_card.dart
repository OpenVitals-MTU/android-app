import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../domain/model/mindfulness_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/mindfulness_entry_view_model.dart';

/// The guided meditation timer. Port of the Kotlin `MindfulnessTimerCard`.
///
/// Idle: duration, optional interval bell, bell + ambient pickers, and Start.
/// Running: the countdown, Stop. Paused: Resume, Save, Discard. Completed: Save,
/// Discard. The schedule fields freeze once the timer leaves the idle state, so
/// the session that gets saved is the one that was actually run.
class MindfulnessTimerCard extends ConsumerStatefulWidget {
  const MindfulnessTimerCard({super.key, required this.provider});

  final NotifierProvider<MindfulnessEntryViewModel, MindfulnessEntryState>
      provider;

  @override
  ConsumerState<MindfulnessTimerCard> createState() =>
      _MindfulnessTimerCardState();
}

class _MindfulnessTimerCardState extends ConsumerState<MindfulnessTimerCard> {
  late final TextEditingController _duration;
  late final TextEditingController _interval;

  @override
  void initState() {
    super.initState();
    final state = ref.read(widget.provider);
    _duration = TextEditingController(text: state.durationMinutesText);
    _interval = TextEditingController(text: state.intervalMinutesText);
  }

  @override
  void dispose() {
    _duration.dispose();
    _interval.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(widget.provider);
    final notifier = ref.read(widget.provider.notifier);
    final canEdit = state.canEditTimer && !state.isSavingEntry;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                const Icon(Icons.self_improvement,
                    color: AppColors.mindfulness, size: 22),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    l10n.mindfulnessEntryTimerTitle,
                    style: theme.textTheme.titleSmall,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Center(
              child: Text(
                formattedTimer(state.remainingSeconds),
                style: theme.textTheme.displaySmall?.copyWith(
                  fontFeatures: const [FontFeature.tabularFigures()],
                  color: state.isTimerRunning
                      ? AppColors.mindfulness
                      : theme.colorScheme.onSurface,
                ),
              ),
            ),
            if (state.timerCompleted)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  l10n.mindfulnessEntryCompleted,
                  textAlign: TextAlign.center,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ),
            const SizedBox(height: 16),
            if (canEdit) ...[
              TextField(
                controller: _duration,
                keyboardType: TextInputType.number,
                maxLines: 1,
                onChanged: notifier.updateDurationMinutes,
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  labelText: l10n.mindfulnessEntryMinutes,
                ),
              ),
              const SizedBox(height: 8),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(l10n.mindfulnessEntryIntervalBell),
                value: state.intervalEnabled,
                onChanged: notifier.updateIntervalEnabled,
              ),
              if (state.intervalEnabled)
                TextField(
                  controller: _interval,
                  keyboardType: TextInputType.number,
                  maxLines: 1,
                  onChanged: notifier.updateIntervalMinutes,
                  decoration: InputDecoration(
                    border: const OutlineInputBorder(),
                    labelText: l10n.mindfulnessEntryIntervalMinutes,
                  ),
                ),
              const SizedBox(height: 12),
              _SoundPicker<MindfulnessBellSound>(
                label: l10n.mindfulnessEntryBellSound,
                values: MindfulnessBellSound.values,
                selected: state.bellSound,
                labelFor: (sound) => bellSoundLabel(sound, l10n),
                onSelected: notifier.updateBellSound,
              ),
              const SizedBox(height: 8),
              _SoundPicker<MindfulnessBackgroundSound>(
                label: l10n.mindfulnessEntryBackgroundSound,
                values: MindfulnessBackgroundSound.values,
                selected: state.backgroundSound,
                labelFor: (sound) => backgroundSoundLabel(sound, l10n),
                onSelected: notifier.updateBackgroundSound,
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: notifier.startTimer,
                icon: const Icon(Icons.play_arrow, size: 18),
                label: Text(l10n.mindfulnessEntryStartTimer),
              ),
            ] else ...[
              if (state.isTimerRunning)
                FilledButton.icon(
                  onPressed: notifier.stopTimer,
                  icon: const Icon(Icons.pause, size: 18),
                  label: Text(l10n.mindfulnessEntryStopTimer),
                ),
              if (state.isTimerPaused) ...[
                FilledButton.icon(
                  onPressed: notifier.resumeTimer,
                  icon: const Icon(Icons.play_arrow, size: 18),
                  label: Text(l10n.mindfulnessEntryResumeTimer),
                ),
                const SizedBox(height: 8),
              ],
              if (state.isTimerPaused || state.timerCompleted) ...[
                FilledButton.icon(
                  onPressed:
                      state.isSavingEntry ? null : notifier.saveTimerSession,
                  icon: const Icon(Icons.check, size: 18),
                  label: Text(l10n.mindfulnessEntrySaveSession),
                ),
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  onPressed: notifier.discardTimer,
                  icon: const Icon(Icons.close, size: 18),
                  label: Text(l10n.mindfulnessEntryDiscardTimer),
                ),
              ],
            ],
          ],
        ),
      ),
    );
  }
}

/// A labelled row of choice chips. Selecting one previews its sound.
class _SoundPicker<T> extends StatelessWidget {
  const _SoundPicker({
    required this.label,
    required this.values,
    required this.selected,
    required this.labelFor,
    required this.onSelected,
  });

  final String label;
  final List<T> values;
  final T selected;
  final String Function(T value) labelFor;
  final void Function(T value) onSelected;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelMedium
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
        const SizedBox(height: 6),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final value in values)
              ChoiceChip(
                label: Text(labelFor(value)),
                selected: value == selected,
                // Re-tapping the selected chip re-previews it, as in Kotlin.
                onSelected: (_) => onSelected(value),
              ),
          ],
        ),
      ],
    );
  }
}

/// Port of the Kotlin `MindfulnessBellSound.labelRes`.
String bellSoundLabel(MindfulnessBellSound sound, AppLocalizations l10n) =>
    switch (sound) {
      MindfulnessBellSound.struck => l10n.mindfulnessBellStruck,
      MindfulnessBellSound.rubbed => l10n.mindfulnessBellRubbed,
      MindfulnessBellSound.bright => l10n.mindfulnessBellBright,
      MindfulnessBellSound.temple => l10n.mindfulnessBellTemple,
      MindfulnessBellSound.harmony => l10n.mindfulnessBellHarmony,
    };

/// Port of the Kotlin `MindfulnessBackgroundSound.labelRes`.
String backgroundSoundLabel(
  MindfulnessBackgroundSound sound,
  AppLocalizations l10n,
) =>
    switch (sound) {
      MindfulnessBackgroundSound.none => l10n.mindfulnessBackgroundNone,
      MindfulnessBackgroundSound.bowl => l10n.mindfulnessBackgroundBowl,
      MindfulnessBackgroundSound.meditation =>
        l10n.mindfulnessBackgroundMeditation,
      MindfulnessBackgroundSound.chimes => l10n.mindfulnessBackgroundChimes,
      MindfulnessBackgroundSound.dreamscape =>
        l10n.mindfulnessBackgroundDreamscape,
    };
