import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/local/beverage/beverage_entity.dart';
import 'package:openvitals/domain/model/caffeine_models.dart';

void main() {
  group('BeverageEntity', () {
    test('preloaded defaults include water category drinks', () {
      final waters = BeverageEntity.preloadedDefaults()
          .take(2)
          .map((entity) => entity.toDomain())
          .toList();

      expect(
        waters.map((water) => water.id).toList(),
        ['openvitals-still-water', 'openvitals-gasified-water'],
      );
      expect(
        waters.map((water) => water.name).toList(),
        ['Still water', 'Gasified water'],
      );
      for (final water in waters) {
        expect(water.volumeMilliliters, closeTo(100.0, 0.001));
        expect(water.hydrationMultiplier, closeTo(1.0, 0.001));
        expect(water.category, CaffeineSourceCategory.water);
        expect(water.isPreloaded, isTrue);
        expect(water.nutrientValues, isEmpty);
      }
    });

    test('preloaded defaults seed the caffeine catalog after the waters', () {
      final defaults = BeverageEntity.preloadedDefaults();

      // Sort orders are contiguous starting at 0 (waters), then the catalog.
      expect(defaults[0].sortOrder, 0);
      expect(defaults[1].sortOrder, 1);
      expect(defaults[2].sortOrder, 2);
      expect(defaults.length, greaterThan(2));
      // Catalog presets carry the caffeinehealth- id prefix and are preloaded.
      final firstPreset = defaults[2];
      expect(firstPreset.id, startsWith('caffeinehealth-'));
      expect(firstPreset.isPreloaded, isTrue);
      expect(firstPreset.isDeleted, isFalse);
    });
  });
}
