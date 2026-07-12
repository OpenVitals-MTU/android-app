import 'package:flutter/material.dart';

import '../../../../ui/components/ov_card.dart';

/// Small, self-contained settings controls shared by the activity settings
/// cards. These mirror the visual idiom of the private `_SwitchCard` /
/// `_ChoiceRow` widgets in `settings_section_screen.dart` (which cannot be
/// imported), and the Kotlin `SettingsSwitchRow` /
/// `ActivityRecordingSegmentedChoice` helpers in `SettingsCards.kt`.

/// A titled [OpenVitalsCard] wrapping a vertical stack of controls. Port of the
/// Kotlin settings card chrome (title + body + spaced children).
class SettingsCardShell extends StatelessWidget {
  const SettingsCardShell({
    super.key,
    required this.title,
    required this.body,
    required this.children,
  });

  final String title;
  final String body;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: theme.textTheme.titleSmall),
              const SizedBox(height: 4),
              Text(
                body,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
              const SizedBox(height: 14),
              for (var i = 0; i < children.length; i++) ...[
                if (i > 0) const SizedBox(height: 14),
                children[i],
              ],
            ],
          ),
        ),
      ),
    );
  }
}

/// An inline title + body + trailing [Switch] row. Port of the Kotlin
/// `SettingsSwitchRow`.
class SettingsSwitchRow extends StatelessWidget {
  const SettingsSwitchRow({
    super.key,
    required this.title,
    required this.body,
    required this.value,
    required this.onChanged,
  });

  final String title;
  final String body;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: theme.textTheme.bodyLarge),
              const SizedBox(height: 2),
              Text(
                body,
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ),
        ),
        const SizedBox(width: 12),
        Switch(value: value, onChanged: onChanged),
      ],
    );
  }
}

/// A titled single-choice segmented row. Port of the Kotlin
/// `ActivityRecordingSegmentedChoice`: a label above a row of choice chips, one
/// per option, dimmed and non-interactive when [enabled] is false. The generic
/// [T] lets callers pass nullable option lists (e.g. an "Off"/null entry).
class SettingsSegmentedChoice<T> extends StatelessWidget {
  const SettingsSegmentedChoice({
    super.key,
    required this.title,
    required this.options,
    required this.selected,
    required this.labelFor,
    required this.onSelect,
    this.enabled = true,
  });

  final String title;
  final List<T> options;
  final T selected;
  final String Function(T) labelFor;
  final ValueChanged<T> onSelect;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final titleColor = enabled
        ? theme.colorScheme.onSurface
        : theme.colorScheme.onSurfaceVariant;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: theme.textTheme.bodyMedium?.copyWith(color: titleColor),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final option in options)
              ChoiceChip(
                label: Text(labelFor(option)),
                selected: option == selected,
                onSelected: enabled ? (_) => onSelect(option) : null,
              ),
          ],
        ),
      ],
    );
  }
}
