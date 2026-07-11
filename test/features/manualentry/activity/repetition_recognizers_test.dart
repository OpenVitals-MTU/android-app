import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/manualentry/activity/repetition_recognizers.dart';

/// Port of the Kotlin `RepetitionRecognizersTest`.
void main() {
  test('push-up recognizer counts only close transitions', () {
    final recognizer = PushUpProximityRecognizer();

    expect(recognizer.onProximity(5, 0), isNull);
    expect(recognizer.onProximity(1, 100), isNotNull);
    expect(recognizer.onProximity(1, 200), isNull);
    expect(recognizer.onProximity(5, 300), isNull);
    expect(recognizer.onProximity(1, 400), isNotNull);
  });

  test('step recognizer counts each step detector event', () {
    final recognizer = StepDetectorRepetitionRecognizer();

    expect(recognizer.onStep(100), isNotNull);
    expect(recognizer.onStep(200), isNotNull);
  });

  test('jump recognizer counts jumping to falling transition', () {
    final recognizer = JumpRepetitionRecognizer(maxJumpDurationMillis: 1250);

    expect(recognizer.onAcceleration(0, 0, 1, 0), isNull);
    expect(recognizer.onAcceleration(0, 0, 22, 100), isNull);
    expect(recognizer.onAcceleration(0, 0, 1, 200), isNotNull);
  });

  test('pull-up recognizer counts pull and relax sequence', () {
    final recognizer = PullUpRepetitionRecognizer(smoothing: 1.0);

    expect(recognizer.onAcceleration(0, 0, 11, 0), isNull);
    expect(recognizer.onAcceleration(0, 0, 11, 600), isNull);
    expect(recognizer.onAcceleration(0, 0, 9, 700), isNull);
    expect(recognizer.onAcceleration(0, 0, 9, 1200), isNotNull);
  });
}
