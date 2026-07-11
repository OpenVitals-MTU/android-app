import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

/// Date + time picker row used by the edit paths of the body/vitals/mindfulness
/// entry forms. Port of the Kotlin `ManualEntryTimestampFields`: two outlined
/// buttons that open a date picker (capped at today) and a time picker, then
/// emit the combined [DateTime] coerced at now.
class ManualEntryTimestampFields extends StatelessWidget {
  const ManualEntryTimestampFields({
    super.key,
    required this.timestamp,
    required this.enabled,
    required this.onChanged,
  });

  final DateTime? timestamp;
  final bool enabled;
  final ValueChanged<DateTime> onChanged;

  static final DateFormat _dateFormat = DateFormat.yMMMd();
  static final DateFormat _timeFormat = DateFormat.jm();

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final current = (timestamp ?? now).isAfter(now) ? now : (timestamp ?? now);

    return Row(
      children: [
        Expanded(
          child: _PickerButton(
            label: 'Date',
            value: _dateFormat.format(current),
            icon: Icons.calendar_month_outlined,
            enabled: enabled,
            onPressed: () => _pickDate(context, current),
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: _PickerButton(
            label: 'Time',
            value: _timeFormat.format(current),
            icon: Icons.schedule_outlined,
            enabled: enabled,
            onPressed: () => _pickTime(context, current),
          ),
        ),
      ],
    );
  }

  Future<void> _pickDate(BuildContext context, DateTime current) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: current,
      firstDate: DateTime(now.year - 10),
      lastDate: now,
    );
    if (picked == null) return;
    final combined = DateTime(
      picked.year,
      picked.month,
      picked.day,
      current.hour,
      current.minute,
    );
    onChanged(combined.isAfter(now) ? now : combined);
  }

  Future<void> _pickTime(BuildContext context, DateTime current) async {
    final now = DateTime.now();
    final picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(current),
    );
    if (picked == null) return;
    final combined = DateTime(
      current.year,
      current.month,
      current.day,
      picked.hour,
      picked.minute,
    );
    onChanged(combined.isAfter(now) ? now : combined);
  }
}

class _PickerButton extends StatelessWidget {
  const _PickerButton({
    required this.label,
    required this.value,
    required this.icon,
    required this.enabled,
    required this.onPressed,
  });

  final String label;
  final String value;
  final IconData icon;
  final bool enabled;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OutlinedButton(
      onPressed: enabled ? onPressed : null,
      child: Row(
        children: [
          Icon(icon, size: 18),
          const SizedBox(width: 6),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label,
                    style: theme.textTheme.labelSmall, maxLines: 1),
                Text(
                  value,
                  style: theme.textTheme.labelLarge,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
