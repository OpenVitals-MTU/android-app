import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/features/activity/maps/offline_map_style.dart';

void main() {
  Map<String, dynamic> baseStyle() => <String, dynamic>{
        'version': 8,
        'name': 'OpenVitals Offline Base',
        'sources': <String, dynamic>{
          templatePmtilesSourceId: <String, dynamic>{
            'type': 'vector',
            'url': '__OPENVITALS_PMTILES_URL__',
          },
        },
        'layers': <dynamic>[
          <String, dynamic>{'id': 'background', 'type': 'background'},
          <String, dynamic>{
            'id': 'water',
            'type': 'fill',
            'source': templatePmtilesSourceId,
            'source-layer': 'water',
          },
        ],
      };

  test('expands one source and one templated-layer copy per pack', () {
    final expanded = expandPmtilesStyle(
      baseStyle(),
      ['/packs/city-a.pmtiles', '/packs/city-b.pmtiles'],
    );

    final sources = expanded['sources'] as Map<String, dynamic>;
    expect(sources.keys, ['openvitals_pmtiles_0', 'openvitals_pmtiles_1']);
    final source0 = sources['openvitals_pmtiles_0'] as Map<String, dynamic>;
    expect(source0['type'], 'vector');
    expect(source0['url'], 'pmtiles://${Uri.file('/packs/city-a.pmtiles')}');
    expect(source0['attribution'], pmtilesAttribution);

    final layers = expanded['layers'] as List<dynamic>;
    // background passes through unchanged; water duplicated per pack.
    expect(
      layers.map((dynamic l) => (l as Map<String, dynamic>)['id']),
      ['background', 'water-0', 'water-1'],
    );
    expect((layers[1] as Map<String, dynamic>)['source'], 'openvitals_pmtiles_0');
    expect((layers[2] as Map<String, dynamic>)['source'], 'openvitals_pmtiles_1');
    // Non-source keys of the template layer are preserved on every copy.
    expect((layers[2] as Map<String, dynamic>)['source-layer'], 'water');
    // Root keys other than sources/layers pass through.
    expect(expanded['version'], 8);
    expect(expanded['name'], 'OpenVitals Offline Base');
  });

  test('bundled style asset parses and only references the template source',
      () {
    final styleText =
        File('assets/offline_maps/protomaps_base_style.json').readAsStringSync();
    final style = jsonDecode(styleText) as Map<String, dynamic>;
    final layers = style['layers'] as List<dynamic>;
    expect(layers, isNotEmpty);
    for (final dynamic layer in layers) {
      final map = layer as Map<String, dynamic>;
      final source = map['source'];
      // Every layer is either source-less (background) or templated: the
      // runtime expansion in expandPmtilesStyle rebinds all of them.
      expect(source == null || source == templatePmtilesSourceId, isTrue,
          reason: 'layer ${map['id']} references unexpected source $source');
      // The style must stay glyph/sprite-free: text/symbol layers would need
      // font assets the app does not bundle.
      expect(map['type'], isNot('symbol'),
          reason: 'layer ${map['id']} needs glyphs');
    }
    expect(style.containsKey('glyphs'), isFalse);
    expect(style.containsKey('sprite'), isFalse);
  });
}
