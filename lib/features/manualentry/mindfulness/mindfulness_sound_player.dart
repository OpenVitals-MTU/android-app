import 'package:audioplayers/audioplayers.dart';

import '../../../domain/model/mindfulness_models.dart';

/// The ambient loop's volume during a session (Kotlin `setVolume(0.38f, 0.38f)`).
const double kMindfulnessBackgroundVolume = 0.38;

/// The asset backing each bell. Port of the Kotlin `MindfulnessBellSound.rawRes`.
String bellSoundAsset(MindfulnessBellSound sound) => switch (sound) {
      MindfulnessBellSound.struck => 'sounds/bowl_struck.ogg',
      MindfulnessBellSound.rubbed => 'sounds/bowl_rubbed.ogg',
      MindfulnessBellSound.bright => 'sounds/bowl_bright.ogg',
      MindfulnessBellSound.temple => 'sounds/bowl_temple.ogg',
      MindfulnessBellSound.harmony => 'sounds/bowl_harmony.ogg',
    };

/// The asset backing each ambient loop, or null for silence. Port of the Kotlin
/// `MindfulnessBackgroundSound.rawResOrNull` — note `bowl` reuses the rubbed
/// bowl bell rather than having an ambient track of its own.
String? backgroundSoundAsset(MindfulnessBackgroundSound sound) =>
    switch (sound) {
      MindfulnessBackgroundSound.none => null,
      MindfulnessBackgroundSound.bowl => 'sounds/bowl_rubbed.ogg',
      MindfulnessBackgroundSound.meditation => 'sounds/ambient_meditation.ogg',
      MindfulnessBackgroundSound.chimes => 'sounds/ambient_chimes.ogg',
      MindfulnessBackgroundSound.dreamscape => 'sounds/ambient_dreamscape.ogg',
    };

/// Plays the timer's bells and ambient loop.
///
/// An interface so the timer's state machine can be driven in tests with no
/// audio host, and so a platform without audio degrades to silence rather than
/// crashing. Mirrors the Kotlin `MindfulnessBellEffect` /
/// `MindfulnessBackgroundEffect` composables: the notifier emits sound *events*
/// and the screen plays them, so the notifier itself never touches audio.
abstract interface class MindfulnessSoundPlayer {
  /// Rings [sound] once. [previewMillis] cuts the clip short — used when the
  /// user taps a bell in the picker.
  Future<void> playBell(MindfulnessBellSound sound, {int? previewMillis});

  /// Plays a taste of [sound], for the background picker. Never loops.
  Future<void> previewBackground(
    MindfulnessBackgroundSound sound,
    int previewMillis,
  );

  /// Loops [sound] quietly for the duration of a session.
  Future<void> startBackgroundLoop(MindfulnessBackgroundSound sound);

  Future<void> stopBackground();

  Future<void> dispose();
}

/// Does nothing. The default in tests and on hosts with no audio.
class SilentMindfulnessSoundPlayer implements MindfulnessSoundPlayer {
  const SilentMindfulnessSoundPlayer();

  @override
  Future<void> playBell(MindfulnessBellSound sound, {int? previewMillis}) async {}

  @override
  Future<void> previewBackground(
    MindfulnessBackgroundSound sound,
    int previewMillis,
  ) async {}

  @override
  Future<void> startBackgroundLoop(MindfulnessBackgroundSound sound) async {}

  @override
  Future<void> stopBackground() async {}

  @override
  Future<void> dispose() async {}
}

/// `audioplayers`-backed player. Bells ring on their own player so a bell never
/// interrupts the ambient loop — the interval bell fires mid-session.
class AudioMindfulnessSoundPlayer implements MindfulnessSoundPlayer {
  AudioMindfulnessSoundPlayer({AudioPlayer? bell, AudioPlayer? background})
      : _bell = bell ?? AudioPlayer(),
        _background = background ?? AudioPlayer();

  final AudioPlayer _bell;
  final AudioPlayer _background;

  @override
  Future<void> playBell(
    MindfulnessBellSound sound, {
    int? previewMillis,
  }) async {
    await _guard(() async {
      await _bell.stop();
      await _bell.setReleaseMode(ReleaseMode.stop);
      await _bell.play(AssetSource(bellSoundAsset(sound)));
      if (previewMillis != null) {
        await Future<void>.delayed(Duration(milliseconds: previewMillis));
        await _bell.stop();
      }
    });
  }

  @override
  Future<void> previewBackground(
    MindfulnessBackgroundSound sound,
    int previewMillis,
  ) async {
    final asset = backgroundSoundAsset(sound);
    if (asset == null) return stopBackground();
    await _guard(() async {
      await _background.stop();
      await _background.setReleaseMode(ReleaseMode.stop);
      await _background.setVolume(1.0);
      await _background.play(AssetSource(asset));
      await Future<void>.delayed(Duration(milliseconds: previewMillis));
      await _background.stop();
    });
  }

  @override
  Future<void> startBackgroundLoop(MindfulnessBackgroundSound sound) async {
    final asset = backgroundSoundAsset(sound);
    if (asset == null) return stopBackground();
    await _guard(() async {
      await _background.stop();
      await _background.setReleaseMode(ReleaseMode.loop);
      await _background.setVolume(kMindfulnessBackgroundVolume);
      await _background.play(AssetSource(asset));
    });
  }

  @override
  Future<void> stopBackground() => _guard(_background.stop);

  @override
  Future<void> dispose() => _guard(() async {
        await _bell.dispose();
        await _background.dispose();
      });

  /// Audio is a nicety; a missing host must never break the timer.
  Future<void> _guard(Future<void> Function() block) async {
    try {
      await block();
    } catch (_) {}
  }
}
