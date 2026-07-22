import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/reorder.dart';
import 'package:openvitals/features/dashboard/application/dashboard_display.dart';

StatTileData _tile(String title) => StatTileData(
      // The layout keys on id; these fixtures use the same string for both so
      // the test reads as before.
      id: title,
      title: title,
      value: '1',
      icon: Icons.circle,
      accent: const Color(0xFF000000),
      location: '/x',
    );

List<String> _titles(List<StatTileData> tiles) =>
    [for (final t in tiles) t.title];

void main() {
  _migrationTests();

  final tiles = [for (final t in ['A', 'B', 'C', 'D']) _tile(t)];

  group('applyDashboardTileLayout', () {
    test('no order/hidden returns tiles in default order', () {
      expect(_titles(applyDashboardTileLayout(tiles)), ['A', 'B', 'C', 'D']);
    });

    test('reorders known tiles; unknown tiles keep default order at the end',
        () {
      // Only C and A are ordered; B and D are "new" and keep their relative
      // default order, appended after.
      final out = applyDashboardTileLayout(tiles, order: ['C', 'A']);
      expect(_titles(out), ['C', 'A', 'B', 'D']);
    });

    test('drops hidden tiles by default', () {
      final out = applyDashboardTileLayout(
        tiles,
        hidden: {'B', 'D'},
      );
      expect(_titles(out), ['A', 'C']);
    });

    test('includeHidden keeps hidden tiles but still applies order', () {
      final out = applyDashboardTileLayout(
        tiles,
        order: ['D', 'B'],
        hidden: {'B'},
        includeHidden: true,
      );
      expect(_titles(out), ['D', 'B', 'A', 'C']);
    });

    test('order + hidden together (hidden removed, rest ordered)', () {
      final out = applyDashboardTileLayout(
        tiles,
        order: ['C', 'A', 'B', 'D'],
        hidden: {'A'},
      );
      expect(_titles(out), ['C', 'B', 'D']);
    });
  });

  group('reorderOntoDropTarget', () {
    const ids = ['1', '2', '3', '4'];

    test('forward drag lands the moved card on the drop target', () {
      // Drag card 1 onto card 4: card 1 takes card 4's slot, and only the cards
      // between them shift left. Card 4 must not keep its position.
      expect(reorderOntoDropTarget(ids, 0, 3), ['2', '3', '4', '1']);
    });

    test('backward drag lands the moved card on the drop target', () {
      expect(reorderOntoDropTarget(ids, 3, 0), ['4', '1', '2', '3']);
    });

    test('adjacent drags swap neighbours', () {
      expect(reorderOntoDropTarget(ids, 1, 2), ['1', '3', '2', '4']);
      expect(reorderOntoDropTarget(ids, 2, 1), ['1', '3', '2', '4']);
    });

    test('dropping onto itself or out of range leaves the order untouched', () {
      expect(reorderOntoDropTarget(ids, 2, 2), ids);
      expect(reorderOntoDropTarget(ids, 0, 4), ids);
      expect(reorderOntoDropTarget(ids, -1, 2), ids);
    });
  });
}

void _migrationTests() {
  group('migrateDashboardLayoutKeys', () {
    // The order and hidden set were originally persisted as tile TITLES. Titles
    // are display text, so they change with wording and could never identify a
    // per-device tile whose name the user can edit.
    const idByTitle = {'Beverages': 'hydration', 'Distance': 'distance'};

    test('translates a legacy title to its id', () {
      expect(
        migrateDashboardLayoutKeys(['Beverages', 'Distance'], idByTitle),
        ['hydration', 'distance'],
      );
    });

    test('leaves ids alone, so migrating twice is a no-op', () {
      final once = migrateDashboardLayoutKeys(['Beverages'], idByTitle);
      expect(migrateDashboardLayoutKeys(once, idByTitle), once);
    });

    test('keeps a key it cannot resolve', () {
      // A layout saved on a device with more metrics must survive a device with
      // fewer — setTileOrder deliberately preserves what it does not recognise.
      expect(
        migrateDashboardLayoutKeys(['Distance', 'Something else'], idByTitle),
        ['distance', 'Something else'],
      );
    });

    test('collapses a title and its id to one entry', () {
      // Possible mid-migration: a write that mixed vocabularies would otherwise
      // leave the same tile listed twice and fighting itself for a position.
      expect(
        migrateDashboardLayoutKeys(['Distance', 'distance'], idByTitle),
        ['distance'],
      );
    });
  });
}
