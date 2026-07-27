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
    this.embedded = false,
  });

  final String title;
  final String body;
  final List<Widget> children;

  /// Drop the card chrome — the padding, the surface and the corners — and
  /// return the contents alone, for a card that is a SECTION of another one.
  ///
  /// A nested card reads as a mistake: two surfaces, two sets of corners, and
  /// an inner one that looks clickable because everything else shaped like it
  /// is. What separates sections inside one card is a rule, not a second card.
  final bool embedded;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final content = Column(
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
    );
    if (embedded) return content;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: OpenVitalsCard(
        child: Padding(padding: const EdgeInsets.all(16), child: content),
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
    this.body,
    required this.value,
    required this.onChanged,
  });

  final String title;

  /// Optional: a bare label + switch when omitted, which is what a list of
  /// self-explanatory physiological flags wants.
  final String? body;

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
              if (body case final body?) ...[
                const SizedBox(height: 2),
                Text(
                  body,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
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

/// A labelled dropdown over an enum's values.
///
/// Moved here from the caffeine card when its physiological half was promoted
/// into the Body profile section — two cards now render the same control, and a
/// second private copy would drift.
class SettingsEnumDropdown<T> extends StatelessWidget {
  const SettingsEnumDropdown({
    super.key,
    required this.label,
    required this.selected,
    required this.values,
    required this.labelFor,
    required this.onSelect,
  });

  final String label;
  final T selected;
  final List<T> values;
  final String Function(T) labelFor;
  final ValueChanged<T> onSelect;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: DropdownButtonFormField<T>(
        initialValue: selected,
        isExpanded: true,
        decoration: InputDecoration(
          border: const OutlineInputBorder(),
          labelText: label,
        ),
        items: [
          for (final value in values)
            DropdownMenuItem(value: value, child: Text(labelFor(value))),
        ],
        onChanged: (value) {
          if (value != null) onSelect(value);
        },
      ),
    );
  }
}
