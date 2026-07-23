import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/bootstrap/reminder_resume_bootstrap.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/hydration/reminders/hydration_reminder_controller.dart';
import 'package:openvitals/features/mindfulness/reminders/mindfulness_reminder_controller.dart';

class _FakeHydrationController implements HydrationReminderController {
  int restores = 0;
  bool throwOnRestore = false;

  @override
  Future<void> restoreSchedule() async {
    restores++;
    if (throwOnRestore) throw StateError('hydration re-plan failed');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeMindfulnessController implements MindfulnessReminderController {
  int restores = 0;

  @override
  Future<void> restoreSchedule() async => restores++;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late _FakeHydrationController hydration;
  late _FakeMindfulnessController mindfulness;

  Future<void> pump(WidgetTester tester) async {
    hydration = _FakeHydrationController();
    mindfulness = _FakeMindfulnessController();
    final container = ProviderContainer(overrides: [
      hydrationReminderControllerProvider.overrideWithValue(hydration),
      mindfulnessReminderControllerProvider.overrideWithValue(mindfulness),
    ]);
    addTearDown(container.dispose);
    await tester.pumpWidget(UncontrolledProviderScope(
      container: container,
      child: const ReminderResumeBootstrap(child: SizedBox()),
    ));
  }

  /// Backgrounds and foregrounds the app through the legal state walk —
  /// AppLifecycleListener asserts on skipped transitions.
  Future<void> resume(WidgetTester tester) async {
    final binding = tester.binding;
    binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
  }

  testWidgets('returning to the foreground re-plans both reminder batches',
      (tester) async {
    await pump(tester);
    // Mounting alone must not re-plan — cold start is bootstrapReminders' job.
    expect(hydration.restores, 0);
    expect(mindfulness.restores, 0);

    await resume(tester);

    expect(hydration.restores, 1);
    expect(mindfulness.restores, 1);
  });

  testWidgets('a failed hydration re-plan neither escapes the lifecycle '
      'callback nor starves the mindfulness one', (tester) async {
    await pump(tester);
    hydration.throwOnRestore = true;

    await resume(tester);

    expect(tester.takeException(), isNull);
    expect(mindfulness.restores, 1,
        reason: 'each batch is caught on its own; one failing re-plan must '
            'not skip the other');
  });
}
