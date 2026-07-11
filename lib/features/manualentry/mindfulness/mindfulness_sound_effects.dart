import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../di/providers.dart';
import '../../../domain/model/mindfulness_models.dart';
import '../mindfulness_entry_notifier.dart';
import 'mindfulness_sound_player.dart';

/// Plays the sounds the notifier asks for. Port of the Kotlin
/// `MindfulnessBellEffect` / `MindfulnessBackgroundPreviewEffect` /
/// `MindfulnessBackgroundEffect` composables.
///
/// The notifier never touches audio; it emits events whose `id` changes on every
/// emission, so re-picking the same bell still rings it. This widget watches
/// those ids and the running flag, and drives the player.
class MindfulnessSoundEffects extends ConsumerStatefulWidget {
  const MindfulnessSoundEffects({
    super.key,
    required this.provider,
    required this.child,
  });

  final NotifierProvider<MindfulnessEntryNotifier, MindfulnessEntryState>
      provider;
  final Widget child;

  @override
  ConsumerState<MindfulnessSoundEffects> createState() =>
      _MindfulnessSoundEffectsState();
}

class _MindfulnessSoundEffectsState
    extends ConsumerState<MindfulnessSoundEffects> {
  int? _lastBellEventId;
  int? _lastBackgroundEventId;
  bool _loopRunning = false;

  /// Cached, not read from `ref` in [dispose] — `ref` is unsafe once the widget
  /// is unmounted, and the loop must still be stopped on the way out.
  late final MindfulnessSoundPlayer _player =
      ref.read(mindfulnessSoundPlayerProvider);

  @override
  void initState() {
    super.initState();
    _player; // Resolve before the widget can be unmounted.
  }

  @override
  void dispose() {
    // Leaving the screen must not leave an ambient loop playing.
    _player.stopBackground();
    super.dispose();
  }

  void _onState(MindfulnessEntryState? previous, MindfulnessEntryState next) {
    final bell = next.bellEvent;
    if (bell != null && bell.id != _lastBellEventId) {
      _lastBellEventId = bell.id;
      _player.playBell(bell.sound, previewMillis: bell.previewMillis);
    }

    // A preview only makes sense while idle; during a session the loop owns the
    // background channel.
    final background = next.backgroundEvent;
    if (background != null &&
        background.id != _lastBackgroundEventId &&
        !next.isTimerRunning) {
      _lastBackgroundEventId = background.id;
      _player.previewBackground(background.sound, background.previewMillis);
    }

    final wantsLoop = next.isTimerRunning &&
        next.backgroundSound != MindfulnessBackgroundSound.none;
    if (wantsLoop && !_loopRunning) {
      _loopRunning = true;
      _player.startBackgroundLoop(next.backgroundSound);
    } else if (!wantsLoop && _loopRunning) {
      _loopRunning = false;
      _player.stopBackground();
    }
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(widget.provider, _onState);
    return widget.child;
  }
}
