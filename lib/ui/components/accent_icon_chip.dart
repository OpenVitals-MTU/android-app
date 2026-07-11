import 'package:flutter/material.dart';

/// A circular chip holding an icon, tinted with a wash of the icon's own colour.
///
/// The wash is deliberately faint — it is a backdrop for the icon, not a badge in
/// its own right. This existed twice, in the dashboard and in the activities
/// sections, with **different alphas** (0.14 and 0.16): a difference nobody chose
/// and nobody could have seen. It is one alpha now.
class AccentIconChip extends StatelessWidget {
  const AccentIconChip({
    required this.icon,
    required this.color,
    this.size = 40,
    this.iconSize,
    super.key,
  });

  final IconData icon;
  final Color color;
  final double size;

  /// Defaults to half the chip, which keeps the icon centred in its wash at any
  /// [size].
  final double? iconSize;

  @override
  Widget build(BuildContext context) => Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.14),
          shape: BoxShape.circle,
        ),
        child: Icon(icon, color: color, size: iconSize ?? size * 0.5),
      );
}
