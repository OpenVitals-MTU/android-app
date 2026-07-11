import 'package:flutter/material.dart';

/// A centred full-screen progress indicator. Port of Kotlin `FullScreenLoading`.
class FullScreenLoading extends StatelessWidget {
  const FullScreenLoading({super.key});

  @override
  Widget build(BuildContext context) => const Center(
        child: CircularProgressIndicator(),
      );
}

/// A progress indicator sized to stand in for **one section** of a scrolling
/// screen, rather than the whole screen.
///
/// The vertical padding is what reserves roughly a section's worth of height, so
/// the rest of the page does not jump when the section resolves. This was
/// written out as a private `_LoadingBlock` in four separate screens (body,
/// sleep, heart and vitals), byte for byte.
class SectionLoading extends StatelessWidget {
  const SectionLoading({super.key});

  @override
  Widget build(BuildContext context) => const Padding(
        padding: EdgeInsets.symmetric(vertical: 48),
        child: Center(child: CircularProgressIndicator()),
      );
}

/// A centred inline error message. Port of Kotlin `ErrorMessage`.
class ErrorMessage extends StatelessWidget {
  const ErrorMessage(this.message, {super.key});

  final String message;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Center(
        child: Text(
          message,
          style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.error),
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}
