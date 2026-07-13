import 'package:flutter/material.dart';

/// How long a chart takes to arrive.
const Duration kChartEntryDuration = Duration(milliseconds: 550);

/// Draws a chart in, once, when it first appears.
///
/// A line draws itself from left to right; bars grow up out of the axis; the ring
/// sweeps round. It is not decoration — it is the chart telling you which way to
/// read it, and it is the difference between a picture that was already there when
/// you arrived and one that was drawn for you.
///
/// ## It honours reduce-motion, and that is not a nicety
///
/// `MediaQuery.disableAnimations` is the accessibility contract: a user who has
/// asked their phone to stop moving things has asked THIS to stop moving too, and
/// vestibular disorders are the reason that switch exists.
///
/// It is also the only thing standing between this widget and a two-thousand-test
/// suite. A chart that animates on mount makes every single-`pump()` assertion see
/// the first frame — an empty chart — and any test that calls `pumpAndSettle`
/// waits for the animation to finish before it can proceed. Wire the switch before
/// the animation, or spend a day finding out why the suite hangs.
class ChartReveal extends StatefulWidget {
  const ChartReveal({
    super.key,
    required this.builder,
    this.duration = kChartEntryDuration,
    this.curve = Curves.easeOutCubic,
  });

  /// `t` runs 0 → 1. Paint the chart at that fraction of drawn.
  final Widget Function(BuildContext context, double t) builder;

  final Duration duration;
  final Curve curve;

  @override
  State<ChartReveal> createState() => _ChartRevealState();
}

class _ChartRevealState extends State<ChartReveal>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: widget.duration,
  );
  late final Animation<double> _t =
      CurvedAnimation(parent: _controller, curve: widget.curve);

  bool _started = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_started) return;
    _started = true;
    // Asked once, here rather than in initState, because MediaQuery is not
    // available until dependencies resolve.
    if (MediaQuery.maybeDisableAnimationsOf(context) ?? false) {
      _controller.value = 1.0;
    } else {
      _controller.forward();
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AnimatedBuilder(
        animation: _t,
        builder: (context, _) => widget.builder(context, _t.value),
      );
}
