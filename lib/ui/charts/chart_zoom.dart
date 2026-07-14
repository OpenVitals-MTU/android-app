import 'dart:math' as math;

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
/// So the recognizer here refuses to engage until a SECOND pointer lands
/// ([_TwoFingerScaleGestureRecognizer]). One finger behaves exactly as it always has —
/// scrub horizontally, scroll the page vertically. Two fingers pinch to zoom and drag to
/// move along the axis. Nothing that worked before this widget existed behaves any
/// differently.
///
/// Double-tap resets, because a chart you have zoomed into and cannot get out of is worse
/// than one that never zoomed.
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

  /// The state the pinch started from. Every move is applied to THIS rather than to the
  /// last frame's result: compounding frame by frame accumulates the rounding, and a slow
  /// pinch would visibly drift.
  ChartViewport? _pinchStartViewport;
  double _pinchStartSeparation = 0.0;
  double _pinchStartFocus = 0.0;

  void _onPointerDown(PointerDownEvent event) {
    _pointers[event.pointer] = event.localPosition;
    _restartPinch();
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

    if (next != _viewport) setState(() => _viewport = next);
  }

  void _onPointerEnd(PointerEvent event) {
    _pointers.remove(event.pointer);
    _restartPinch();
  }

  /// Rebaselines whenever the number of fingers changes, so lifting one of three, or
  /// adding a second, does not make the chart leap.
  void _restartPinch() {
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
        // A Listener, NOT a gesture recognizer. It claims nothing in the gesture arena,
        // so it cannot take the scrubber's drag or the page's scroll away from them —
        // they carry on competing for the single finger exactly as they did before this
        // widget existed. The pinch is worked out from the raw pointers instead, and it
        // only does anything at all once a second finger is down.
        return Listener(
          behavior: HitTestBehavior.translucent,
          onPointerDown: _onPointerDown,
          onPointerMove: (event) => _onPointerMove(event, width),
          onPointerUp: _onPointerEnd,
          onPointerCancel: _onPointerEnd,
          child: GestureDetector(
            behavior: HitTestBehavior.translucent,
            // A tap recognizer, so it competes only with other taps. A chart you can zoom
            // into and cannot get out of is worse than one that never zoomed.
            onDoubleTap: _reset,
            child: widget.builder(context, _viewport),
          ),
        );
      },
    );
  }
}
