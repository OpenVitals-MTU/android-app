import 'dart:math' as math;

/// Port of the Kotlin `RepetitionRecognizers.kt` — pure repetition detectors fed
/// raw sensor values. No plugins (the recording controller wires
/// `sensors_plus`/proximity events into these).

class RecognizedRepetition {
  const RecognizedRepetition(this.timeMillis, [this.intensity = 0.0]);

  final int timeMillis;
  final double intensity;
}

class PushUpProximityRecognizer {
  PushUpProximityRecognizer({this.thresholdCentimeters = 2.0});

  final double thresholdCentimeters;
  bool _wasClose = false;

  RecognizedRepetition? onProximity(double valueCentimeters, int nowMillis) {
    final isClose = valueCentimeters < thresholdCentimeters;
    final recognized = isClose && !_wasClose;
    _wasClose = isClose;
    return recognized ? RecognizedRepetition(nowMillis) : null;
  }
}

class StepDetectorRepetitionRecognizer {
  RecognizedRepetition onStep(int nowMillis) => RecognizedRepetition(nowMillis);
}

enum _MotionState { relaxing, prepare, falling, jumping }

class JumpRepetitionRecognizer {
  JumpRepetitionRecognizer({
    required this.maxJumpDurationMillis,
    this.fallingThreshold = 2.5,
    this.jumpingThreshold = 20.0,
  });

  final int maxJumpDurationMillis;
  final double fallingThreshold;
  final double jumpingThreshold;

  _MotionState _state = _MotionState.relaxing;
  int _lastJumpDetectedMillis = 0;
  double _currentJumpMaxAcceleration = 0.0;
  double _lastJumpMaxAcceleration = 0.0;

  RecognizedRepetition? onAcceleration(
    double x,
    double y,
    double z,
    int nowMillis,
  ) {
    final acceleration = _absoluteAcceleration(x, y, z);
    RecognizedRepetition? recognized;
    if (_state == _MotionState.relaxing && acceleration < fallingThreshold) {
      _state = _MotionState.falling;
      _lastJumpDetectedMillis = nowMillis;
      recognized = null;
    } else if ((_state == _MotionState.prepare ||
            _state == _MotionState.falling) &&
        acceleration > jumpingThreshold &&
        acceleration > _lastJumpMaxAcceleration * 0.6) {
      _state = _MotionState.jumping;
      _lastJumpDetectedMillis = nowMillis;
      _currentJumpMaxAcceleration = acceleration;
      recognized = null;
    } else if (_state == _MotionState.jumping &&
        acceleration < fallingThreshold) {
      _state = _MotionState.falling;
      final intensity = _currentJumpMaxAcceleration;
      _lastJumpMaxAcceleration = _currentJumpMaxAcceleration;
      _currentJumpMaxAcceleration = 0.0;
      recognized = RecognizedRepetition(_lastJumpDetectedMillis, intensity);
    } else if (_state != _MotionState.relaxing &&
        nowMillis - _lastJumpDetectedMillis > maxJumpDurationMillis) {
      _state = _MotionState.relaxing;
      _lastJumpMaxAcceleration = 0.0;
      _currentJumpMaxAcceleration = 0.0;
      recognized = null;
    } else {
      recognized = null;
    }
    if (_state == _MotionState.jumping) {
      _currentJumpMaxAcceleration =
          math.max(_currentJumpMaxAcceleration, acceleration);
    }
    return recognized;
  }
}

enum _PullState { relaxing, pulling, returning }

class PullUpRepetitionRecognizer {
  PullUpRepetitionRecognizer({
    this.smoothing = 0.02,
    this.pullThreshold = 10.2,
    this.relaxThreshold = 9.65,
    this.minimumPullMillis = 500,
    this.minimumRelaxMillis = 400,
    this.maximumRelaxMillis = 2000,
  });

  final double smoothing;
  final double pullThreshold;
  final double relaxThreshold;
  final int minimumPullMillis;
  final int minimumRelaxMillis;
  final int maximumRelaxMillis;

  _PullState _state = _PullState.relaxing;
  double _smoothedAcceleration = 9.81;
  int _pullStartMillis = 0;
  int _relaxStartMillis = 0;
  double _maxAcceleration = 9.81;

  RecognizedRepetition? onAcceleration(
    double x,
    double y,
    double z,
    int nowMillis,
  ) {
    final acceleration = _absoluteAcceleration(x, y, z);
    _smoothedAcceleration =
        _smoothedAcceleration * (1 - smoothing) + acceleration * smoothing;
    switch (_state) {
      case _PullState.relaxing:
        if (_smoothedAcceleration > pullThreshold) {
          _state = _PullState.pulling;
          _pullStartMillis = nowMillis;
          _maxAcceleration = _smoothedAcceleration;
        }
        return null;
      case _PullState.pulling:
        _maxAcceleration = math.max(_maxAcceleration, _smoothedAcceleration);
        if (_smoothedAcceleration < relaxThreshold) {
          _state = _PullState.returning;
          _relaxStartMillis = nowMillis;
        }
        return null;
      case _PullState.returning:
        final pullDuration = _relaxStartMillis - _pullStartMillis;
        final relaxDuration = nowMillis - _relaxStartMillis;
        if (_smoothedAcceleration > pullThreshold) {
          _state = _PullState.pulling;
          _pullStartMillis = nowMillis;
          _maxAcceleration = _smoothedAcceleration;
          return null;
        } else if (relaxDuration > maximumRelaxMillis) {
          _state = _PullState.relaxing;
          return null;
        } else if (pullDuration >= minimumPullMillis &&
            relaxDuration >= minimumRelaxMillis) {
          _state = _PullState.relaxing;
          return RecognizedRepetition(nowMillis, (_maxAcceleration - 9.81) * 10);
        } else {
          return null;
        }
    }
  }
}

double _absoluteAcceleration(double x, double y, double z) =>
    math.sqrt(x * x + y * y + z * z);
