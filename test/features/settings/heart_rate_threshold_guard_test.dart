import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/di/providers.dart';
import 'package:openvitals/features/heart/presentation/heart_metric_cards.dart';
import 'package:openvitals/features/settings/application/settings_view_model.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<ProviderContainer> container() async {
    SharedPreferences.setMockInitialValues(const {});
    final prefs = await SharedPreferences.getInstance();
    final c = ProviderContainer(
      overrides: [sharedPreferencesProvider.overrideWithValue(prefs)],
    );
    addTearDown(c.dispose);
    return c;
  }

  // The heart screen enforced a minimum gap between the two thresholds; the
  // settings steppers did not, so stepping from here could push "high" below
  // "low" and leave every sample flagged as both at once.

  test('the high threshold cannot be stepped below the low one', () async {
    final c = await container();
    final notifier = c.read(settingsProvider.notifier);
    notifier.setLowHeartRateThresholdBpm(90);

    notifier.setHighHeartRateThresholdBpm(60);

    final state = c.read(settingsProvider);
    expect(
      state.highHeartRateThresholdBpm,
      state.lowHeartRateThresholdBpm + heartRateThresholdMinimumGapBpm,
    );
    expect(state.highHeartRateThresholdBpm,
        greaterThan(state.lowHeartRateThresholdBpm));
  });

  test('the low threshold cannot be stepped above the high one', () async {
    final c = await container();
    final notifier = c.read(settingsProvider.notifier);
    notifier.setHighHeartRateThresholdBpm(100);

    notifier.setLowHeartRateThresholdBpm(140);

    final state = c.read(settingsProvider);
    expect(
      state.lowHeartRateThresholdBpm,
      state.highHeartRateThresholdBpm - heartRateThresholdMinimumGapBpm,
    );
  });

  test('a legitimate change still lands unchanged', () async {
    final c = await container();
    final notifier = c.read(settingsProvider.notifier);

    notifier.setHighHeartRateThresholdBpm(150);
    notifier.setLowHeartRateThresholdBpm(45);

    final state = c.read(settingsProvider);
    expect(state.highHeartRateThresholdBpm, 150);
    expect(state.lowHeartRateThresholdBpm, 45);
  });
}
