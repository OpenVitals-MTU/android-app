import 'dart:math' as math;

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

import 'chart_viewport.dart';

/// Pinch a chart to look closer at part of it.
///
/// **Two fingers, and only two.** This is not a style choice, it is what keeps the rest
/// of the chart working. A chart sits inside a scrolling page and already claims the
/// single-finger horizontal drag for the scrubber ([ChartScrubber]) while leaving the
/// vertical one to the page. A zoom gesture that accepted one finger would have to fight
/// both: it would either eat the scrub, or freeze the page under the user's thumb.
///
/// So this widget uses a raw [Listener] and CLAIMS NOTHING in the gesture arena. It reads
/// the pointers as they go past and works the pinch out itself, doing nothing at all until
/// a second finger lands. One finger behaves exactly as it always has — scrub
/// horizontally, scroll the page vertically, tap a bar to select its day. Nothing that
/// worked before this widget existed behaves any differently.
///
/// That is not belt-and-braces. The first version of this used real recognizers, and each
/// one took something away: a scale recognizer treats a single finger as a pan and ate the
/// scrub, and a DoubleTapGestureRecognizer held the arena for its 300ms and swallowed the
/// bar chart's day-selecting tap. Both were caught by tests, and both are the same lesson.
///
/// Double tap resets — spotted from the raw pointers, for the reason above. A chart you
/// have zoomed into and cannot get out of is worse than one that never zoomed.
class ChartZoom extends StatefulWidget {
  const ChartZoom({
    super.key,
    required this.builder,
    this.enabled = true,
  });

  /// Draws the chart for the visible slice of the axis.
  final Widget Function(BuildContext context, ChartViewport viewport) builder;

  /// Off for a chart with nothing to zoom into — an empty state, a skeleton, or a
  /// heatmap, whose cells are a grid rather than an axis.
  final bool enabled;

  @override
  State<ChartZoom> createState() => _ChartZoomState();
}

class _ChartZoomState extends State<ChartZoom> {
  ChartViewport _viewport = ChartViewport.full;

  /// Where each finger currently is, by pointer id.
  final Map<int, Offset> _pointers = <int, Offset>{};

  /// True while a second finger is down — a pinch, not a scrub. Published to
  /// descendants through [ChartZoomScope] so the [ChartScrubber] stands down: the
  /// pointer that started a one-finger scrub is already routed to it and cannot be
  /// taken back, so the scrubber has to hide itself rather than be hit-tested away.
  bool _multiTouch = false;

  /// The state the pinch started from. Every move is applied to THIS rather than to the
  /// last frame's result: compounding frame by frame accumulates the rounding, and a slow
  /// pinch would visibly drift.
  ChartViewport? _pinchStartViewport;
  double _pinchStartSeparation = 0.0;
  double _pinchStartFocus = 0.0;

  /// When and where the last finger lifted, for spotting a double tap ourselves.
  DateTime? _lastTapAt;
  Offset? _lastTapPosition;
  Offset? _downPosition;

  static const Duration _doubleTapWindow = Duration(milliseconds: 300);
  static const double _tapSlop = 18.0;

  void _onPointerDown(PointerDownEvent event) {
    _pointers[event.pointer] = event.localPosition;
    _downPosition = event.localPosition;
    _restartPinch();
    _syncMultiTouch();
  }

  /// Rebuilds only when the pinch/scrub distinction actually flips, so publishing
  /// the flag to [ChartZoomScope] costs nothing on an ordinary one-finger touch.
  void _syncMultiTouch() {
    final multiTouch = _pointers.length >= 2;
    if (multiTouch != _multiTouch) {
      setState(() => _multiTouch = multiTouch);
    }
  }

  /// A double tap, worked out from the raw pointers rather than asked of a
  /// [GestureDetector].
  ///
  /// A DoubleTapGestureRecognizer here would enter the arena, and the bar chart underneath
  /// resolves a SINGLE tap to select a day — the recognizer held the arena for its 300ms
  /// and the day never got selected. Which is the whole point of this widget: it claims
  /// nothing, so it can take nothing away. Recognising the second tap by hand costs a
  /// dozen lines and leaves every gesture below it exactly as it was.
  void _maybeDoubleTap(PointerUpEvent event) {
    final down = _downPosition;
    if (down == null) return;
    if ((event.localPosition - down).distance > _tapSlop) return;

    final now = DateTime.now();
    final last = _lastTapAt;
    final lastPosition = _lastTapPosition;
    if (last != null &&
        lastPosition != null &&
        now.difference(last) < _doubleTapWindow &&
        (event.localPosition - lastPosition).distance <= _tapSlop) {
      _lastTapAt = null;
      _lastTapPosition = null;
      _reset();
      return;
    }
    _lastTapAt = now;
    _lastTapPosition = event.localPosition;
  }

  void _onPointerMove(PointerMoveEvent event, double width) {
    if (!_pointers.containsKey(event.pointer)) return;
    _pointers[event.pointer] = event.localPosition;

    final startViewport = _pinchStartViewport;
    if (startViewport == null || _pointers.length < 2 || width <= 0) return;

    final separation = _separation();
    if (separation <= 0 || _pinchStartSeparation <= 0) return;

    // Horizontal only, as asked: the fingers' HORIZONTAL separation is the zoom. A pinch
    // that is mostly vertical does almost nothing, which is right — the y axis of these
    // charts is a fixed scale of the thing being measured, and stretching it would only
    // misrepresent it.
    final scale = separation / _pinchStartSeparation;
    final focus = _focusX() / width;

    // Zoom about the point BETWEEN the fingers so the chart stretches under them rather
    // than jumping, then slide by however far that point has travelled — which is what
    // turns a two-finger drag into a pan.
    final next = startViewport
        .zoomed(scale, _pinchStartFocus)
        .panned(focus - _pinchStartFocus);

    if (next != _viewport) {
      _isPinching = true;
      setState(() => _viewport = next);
    }
  }

  void _onPointerEnd(PointerEvent event) {
    // Only a lift that ended a single-finger, still gesture counts as a tap: a finger
    // coming off a pinch is not a tap, and must not reset the zoom the user just set.
    if (event is PointerUpEvent && _pointers.length == 1 && !_isPinching) {
      _maybeDoubleTap(event);
    }
    _pointers.remove(event.pointer);
    _restartPinch();
    _syncMultiTouch();
  }

  /// Whether a pinch has actually happened during this touch, as opposed to two fingers
  /// merely resting.
  bool _isPinching = false;

  /// Rebaselines whenever the number of fingers changes, so lifting one of three, or
  /// adding a second, does not make the chart leap.
  void _restartPinch() {
    if (_pointers.isEmpty) _isPinching = false;
    if (_pointers.length < 2) {
      _pinchStartViewport = null;
      return;
    }
    final width = context.size?.width ?? 0;
    _pinchStartViewport = _viewport;
    _pinchStartSeparation = _separation();
    _pinchStartFocus = width <= 0 ? 0.5 : (_focusX() / width).clamp(0.0, 1.0);
  }

  /// The horizontal distance between the two outermost fingers.
  double _separation() {
    final xs = _pointers.values.map((offset) => offset.dx);
    return xs.reduce(math.max) - xs.reduce(math.min);
  }

  double _focusX() {
    final xs = _pointers.values.map((offset) => offset.dx).toList();
    return xs.reduce((a, b) => a + b) / xs.length;
  }

  void _reset() {
    if (_viewport.isZoomed) setState(() => _viewport = ChartViewport.full);
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.enabled) {
      return widget.builder(context, ChartViewport.full);
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        // The pinch itself is still worked out from the raw pointers below — a
        // [Listener] claims nothing in the arena, so it never takes the scrubber's
        // drag or the page's scroll away from a single finger. But a passive
        // Listener cannot HOLD a two-finger gesture either: inside a scrolling
        // page the parent Scrollable claims the pointers and the Listener never
        // sees the second finger. So a [ScaleGestureRecognizer] rides alongside,
        // purely to win the two-finger gesture in the arena — it computes nothing
        // itself; with the pointers no longer stolen, the Listener's own math
        // runs. A single finger never forms a scale, so scroll/scrub/tap are left
        // exactly as they were.
        return Listener(
          behavior: HitTestBehavior.translucent,
          onPointerDown: _onPointerDown,
          onPointerMove: (event) => _onPointerMove(event, width),
          onPointerUp: _onPointerEnd,
          onPointerCancel: _onPointerEnd,
          child: RawGestureDetector(
            behavior: HitTestBehavior.translucent,
            gestures: {
              ScaleGestureRecognizer:
                  GestureRecognizerFactoryWithHandlers<ScaleGestureRecognizer>(
                ScaleGestureRecognizer.new,
                (instance) {
                  // Non-null handlers keep the recognizer live in the arena; the
                  // zoom is driven by the Listener above, not from here.
                  instance
                    ..onStart = _noopScaleStart
                    ..onUpdate = _noopScaleUpdate;
                },
              ),
            },
            child: ChartZoomScope(
              multiTouch: _multiTouch,
              child: widget.builder(context, _viewport),
            ),
          ),
        );
      },
    );
  }

  static void _noopScaleStart(ScaleStartDetails _) {}
  static void _noopScaleUpdate(ScaleUpdateDetails _) {}
}

/// Publishes whether a pinch (two or more fingers) is in progress on the chart,
/// so a descendant [ChartScrubber] can stand down while it is: the finger that
/// began a one-finger scrub is already routed to the scrubber and cannot be
/// hit-tested away, so the scrubber hides itself off this flag instead.
class ChartZoomScope extends InheritedWidget {
  const ChartZoomScope({
    super.key,
    required this.multiTouch,
    required super.child,
  });

  final bool multiTouch;

  static bool of(BuildContext context) =>
      context
          .dependOnInheritedWidgetOfExactType<ChartZoomScope>()
          ?.multiTouch ??
      false;

  @override
  bool updateShouldNotify(ChartZoomScope oldWidget) =>
      oldWidget.multiTouch != multiTouch;
}
