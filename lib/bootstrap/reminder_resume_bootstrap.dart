import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../di/providers.dart';

/// Re-plans both reminder batches whenever the app returns to the foreground.
///
/// Reminders are pre-scheduled notifications, so the batch cannot react on its
/// own to anything that happens while the app is away: the daily goal being met
/// via another app's Health Connect write, a drink logged from a home-screen
/// widget, or the day simply rolling over. Re-planning on resume catches all of
/// those up. [bootstrapReminders] does the same on cold start; this covers warm
/// resumes, and is mounted above the router like the other launch bootstraps.
class ReminderResumeBootstrap extends ConsumerStatefulWidget {
  const ReminderResumeBootstrap({super.key, required this.child});

  final Widget child;

  @override
  ConsumerState<ReminderResumeBootstrap> createState() =>
      _ReminderResumeBootstrapState();
}

class _ReminderResumeBootstrapState
    extends ConsumerState<ReminderResumeBootstrap> {
  late final AppLifecycleListener _listener;

  @override
  void initState() {
    super.initState();
    _listener = AppLifecycleListener(onResume: _replan);
  }

  /// Best-effort: a failed re-plan must never escape a lifecycle callback —
  /// and each batch is caught on its own, so a hydration failure cannot starve
  /// the mindfulness re-plan (one try around both did exactly that).
  Future<void> _replan() async {
    try {
      await ref.read(hydrationReminderControllerProvider).restoreSchedule();
    } catch (error, stack) {
      debugPrint('Hydration reminder resume re-plan failed: $error\n$stack');
    }
    try {
      await ref.read(mindfulnessReminderControllerProvider).restoreSchedule();
    } catch (error, stack) {
      debugPrint('Mindfulness reminder resume re-plan failed: $error\n$stack');
    }
  }

  @override
  void dispose() {
    _listener.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
