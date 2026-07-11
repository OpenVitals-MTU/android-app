/// Expansion of the shared Protomaps base style for multi-pack PMTiles
/// rendering, ported 1:1 from the Kotlin `OfflineRouteMap.kt`
/// (`offlineMapStyleJson` / `toPmtilesSources` / `toExpandedPmtilesLayers`).
///
/// The bundled style (`assets/offline_maps/protomaps_base_style.json`) declares
/// a single template source `openvitals_pmtiles` and layers referencing it. At
/// runtime the style is regenerated for the active packs: one vector source per
/// pack (`openvitals_pmtiles_<index>`), with every template layer duplicated
/// once per pack (layer id suffixed `-<index>`). Layers that do not reference
/// the template source (the `background` layer) pass through unchanged.
///
/// Pure Dart (no Flutter imports) so it is unit-testable.
library;

/// Asset path of the shared Protomaps base style (copied verbatim from the
/// Kotlin app's `assets/offline_maps/protomaps_base_style.json`).
const String offlineMapStyleAsset = 'assets/offline_maps/protomaps_base_style.json';

/// Kotlin `TemplatePmtilesSourceId`: the placeholder source id in the asset.
const String templatePmtilesSourceId = 'openvitals_pmtiles';

/// Kotlin `PmtilesSourceIdPrefix`.
const String _pmtilesSourceIdPrefix = 'openvitals_pmtiles_';

/// Kotlin attribution string attached to every generated source.
const String pmtilesAttribution = '© OpenStreetMap contributors, Protomaps';

/// Kotlin `sourceIds = mapPacks.mapIndexed { index, _ -> "$PmtilesSourceIdPrefix$index" }`.
List<String> pmtilesSourceIds(int packCount) =>
    List.generate(packCount, (index) => '$_pmtilesSourceIdPrefix$index');

/// Kotlin `Context.offlineMapStyleJson`: regenerates the style's `sources` and
/// `layers` for the given pack file paths; all other root keys pass through.
Map<String, dynamic> expandPmtilesStyle(
  Map<String, dynamic> style,
  List<String> packPaths,
) {
  final sourceIds = pmtilesSourceIds(packPaths.length);
  return <String, dynamic>{
    for (final entry in style.entries)
      entry.key: switch (entry.key) {
        'sources' => _pmtilesSources(packPaths, sourceIds),
        'layers' => _expandedPmtilesLayers(
            (entry.value as List<dynamic>? ?? const <dynamic>[]),
            sourceIds,
          ),
        _ => entry.value,
      },
  };
}

/// Kotlin `List<OfflineMapPack>.toPmtilesSources`: one vector source per pack.
Map<String, dynamic> _pmtilesSources(
  List<String> packPaths,
  List<String> sourceIds,
) =>
    <String, dynamic>{
      for (var index = 0; index < packPaths.length; index++)
        sourceIds[index]: <String, dynamic>{
          'type': 'vector',
          'url': 'pmtiles://${Uri.file(packPaths[index])}',
          'attribution': pmtilesAttribution,
        },
    };

/// Kotlin `JsonArray.toExpandedPmtilesLayers`: template layers are duplicated
/// once per pack; all other layers pass through unchanged.
List<dynamic> _expandedPmtilesLayers(
  List<dynamic> layers,
  List<String> sourceIds,
) {
  final expanded = <dynamic>[];
  for (final element in layers) {
    final layer = element as Map<String, dynamic>;
    if (layer['source'] == templatePmtilesSourceId) {
      for (var index = 0; index < sourceIds.length; index++) {
        expanded.add(_layerWithSource(layer, sourceIds[index], '-$index'));
      }
    } else {
      expanded.add(layer);
    }
  }
  return expanded;
}

/// Kotlin `JsonObject.withSource`: rewrites `id` (suffix) and `source`.
Map<String, dynamic> _layerWithSource(
  Map<String, dynamic> layer,
  String sourceId,
  String suffix,
) =>
    <String, dynamic>{
      for (final entry in layer.entries)
        entry.key: switch (entry.key) {
          'id' => '${entry.value ?? ''}$suffix',
          'source' => sourceId,
          _ => entry.value,
        },
    };
