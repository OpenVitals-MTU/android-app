import 'package:freezed_annotation/freezed_annotation.dart';

part 'mindfulness_models.freezed.dart';

@freezed
abstract class MindfulnessSession with _$MindfulnessSession {
  const MindfulnessSession._();

  const factory MindfulnessSession({
    required String id,
    required String? title,
    required DateTime startTime,
    required DateTime endTime,
    required int durationMs,
    required String source,
    @Default(false) bool isOpenVitalsEntry,
  }) = _MindfulnessSession;

  int get durationMinutes => durationMs ~/ 60000;
}

enum MindfulnessBellSound {
  struck('STRUCK'),
  rubbed('RUBBED'),
  bright('BRIGHT'),
  temple('TEMPLE'),
  harmony('HARMONY');

  const MindfulnessBellSound(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static MindfulnessBellSound? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum MindfulnessBackgroundSound {
  none('NONE'),
  bowl('BOWL'),
  meditation('MEDITATION'),
  chimes('CHIMES'),
  dreamscape('DREAMSCAPE');

  const MindfulnessBackgroundSound(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static MindfulnessBackgroundSound? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class MindfulnessTimerConfig with _$MindfulnessTimerConfig {
  const factory MindfulnessTimerConfig({
    required int durationMinutes,
    required int? intervalMinutes,
    required MindfulnessBellSound bellSound,
    @Default(MindfulnessBackgroundSound.none)
    MindfulnessBackgroundSound backgroundSound,
  }) = _MindfulnessTimerConfig;
}

@freezed
abstract class MindfulnessSessionWriteRequest
    with _$MindfulnessSessionWriteRequest {
  const factory MindfulnessSessionWriteRequest({
    required String title,
    required DateTime startTime,
    required DateTime endTime,
  }) = _MindfulnessSessionWriteRequest;
}
