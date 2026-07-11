import 'package:flutter/material.dart';

// TODO(phase5): every screen that renders [PlaceholderScreen]/[PlaceholderBody]
// is a Phase-3 app-shell stub. Replace the body with the real feature UI in
// Phase 5; the routes and navigation wiring are already in place.

/// Centered "coming soon" body shared by every placeholder screen.
class PlaceholderBody extends StatelessWidget {
  const PlaceholderBody({super.key, required this.title, this.subtitle});

  final String title;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.construction_outlined,
              size: 48,
              color: theme.colorScheme.primary,
            ),
            const SizedBox(height: 16),
            Text(
              title,
              style: theme.textTheme.titleLarge,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              subtitle ?? 'Coming soon',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

/// Full-screen placeholder for routes pushed over the adaptive shell: its own
/// [Scaffold] + [AppBar] (with an automatic back button) wrapping a
/// [PlaceholderBody]. Optional [floatingActionButton] carries the contextual
/// Add action where a detail route offers one.
class PlaceholderScreen extends StatelessWidget {
  const PlaceholderScreen({
    super.key,
    required this.title,
    this.subtitle,
    this.floatingActionButton,
  });

  final String title;
  final String? subtitle;
  final Widget? floatingActionButton;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      floatingActionButton: floatingActionButton,
      body: PlaceholderBody(title: title, subtitle: subtitle),
    );
  }
}
