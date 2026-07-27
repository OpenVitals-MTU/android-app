import 'package:flutter_test/flutter_test.dart';
import 'package:mapsforge_flutter_core/model.dart';
import 'package:mapsforge_flutter_renderer/offline_renderer.dart';

/// The merge policy the offline base map builds its `MultimapDatastore` with.
///
/// This is a contract test over a package class rather than over our own code,
/// and it earns its place: the policy is a one-word choice in
/// `offline_base_map_layer.dart` whose consequence is invisible until two packs
/// meet, and getting it wrong shipped a blank wedge across the seam between an
/// imported 400MB pack and a 200MB one.
///
/// The trap is that `supportsTile` is only a zoom-range and bounding-box check.
/// A pack's bounding box is a RECTANGLE around a region-shaped extract, so two
/// adjacent regions have overlapping boxes even where their data does not
/// overlap at all. Under RETURN_FIRST the first pack claims every tile in that
/// rectangle — including the ones where it holds nothing — and answers with an
/// empty bundle, and the pack that actually covers the ground is never asked.
class _FakeDatastore extends Datastore {
  _FakeDatastore({required this.boundingBox, required this.ways});

  final BoundingBox boundingBox;

  /// What this pack actually holds. Empty models a pack whose box covers the
  /// tile but whose data stops short of it.
  final List<Way> ways;

  int reads = 0;

  @override
  Future<BoundingBox> getBoundingBox() async => boundingBox;

  /// Exactly what `Mapfile.supportsTile` does: the box and the zoom range, with
  /// no idea whether there is data inside.
  @override
  Future<bool> supportsTile(Tile tile) async =>
      boundingBox.intersects(tile.getBoundingBox());

  @override
  Future<DatastoreBundle?> readMapDataSingle(Tile tile) async {
    reads++;
    return DatastoreBundle(pointOfInterests: [], ways: List.of(ways));
  }

  @override
  Future<DatastoreBundle?> readLabelsSingle(Tile tile) async => null;

  @override
  Future<DatastoreBundle?> readLabels(Tile upperLeft, Tile lowerRight) async =>
      null;

  @override
  Future<DatastoreBundle> readMapData(Tile upperLeft, Tile lowerRight) async =>
      DatastoreBundle(pointOfInterests: [], ways: []);

  @override
  Future<DatastoreBundle?> readPoiDataSingle(Tile tile) async => null;

  @override
  Future<DatastoreBundle?> readPoiData(Tile upperLeft, Tile lowerRight) async =>
      null;

  @override
  void dispose() {}
}

Way _way(double latitude, double longitude) => Way.simple(
      [LatLong(latitude, longitude), LatLong(latitude + 0.001, longitude)],
    );

void main() {
  /// A tile inside BOTH packs' bounding boxes — the seam.
  final seamTile = Tile(8710, 5620, 14, 0);

  late _FakeDatastore emptyHere;
  late _FakeDatastore holdsTheData;

  setUp(() {
    final box = seamTile.getBoundingBox();
    // Two packs whose boxes both swallow the tile, as two adjacent regional
    // extracts do. Only the second has anything to draw there.
    emptyHere = _FakeDatastore(
      boundingBox: BoundingBox(
        box.minLatitude - 1,
        box.minLongitude - 1,
        box.maxLatitude + 1,
        box.maxLongitude + 1,
      ),
      ways: const [],
    );
    holdsTheData = _FakeDatastore(
      boundingBox: BoundingBox(
        box.minLatitude - 1,
        box.minLongitude - 1,
        box.maxLatitude + 1,
        box.maxLongitude + 1,
      ),
      ways: [_way(box.minLatitude, box.minLongitude)],
    );
  });

  test('a tile at the seam draws data from whichever pack actually holds it',
      () async {
    final datastore = MultimapDatastore(DataPolicy.RETURN_ALL);
    await datastore.addDatastore(emptyHere);
    await datastore.addDatastore(holdsTheData);

    final bundle = await datastore.readMapDataSingle(seamTile);

    expect(bundle!.ways, hasLength(1),
        reason: 'the pack that covers the ground must be read even when an '
            'earlier pack claims the tile through its bounding box');
    expect(holdsTheData.reads, 1);
  });

  test('every pack whose bounding box covers a tile is read, not just the first',
      () async {
    // The direct statement of the bug: RETURN_FIRST stops at the first pack.
    final datastore = MultimapDatastore(DataPolicy.RETURN_ALL);
    await datastore.addDatastore(emptyHere);
    await datastore.addDatastore(holdsTheData);

    await datastore.readMapDataSingle(seamTile);

    expect(emptyHere.reads, 1);
    expect(holdsTheData.reads, 1);
  });

  test('a tile only one pack covers reads only that pack', () async {
    // The merge must not cost anything away from the seam: a tile deep inside
    // one region never touches the other, because the box does not intersect.
    final datastore = MultimapDatastore(DataPolicy.RETURN_ALL);
    final farAway = _FakeDatastore(
      boundingBox: BoundingBox(-40, -40, -30, -30),
      ways: const [],
    );
    await datastore.addDatastore(farAway);
    await datastore.addDatastore(holdsTheData);

    final bundle = await datastore.readMapDataSingle(seamTile);

    expect(bundle!.ways, hasLength(1));
    expect(farAway.reads, 0);
  });

  test('a tile no pack covers stays empty rather than drawing a blank', () async {
    final datastore = MultimapDatastore(DataPolicy.RETURN_ALL);
    await datastore.addDatastore(_FakeDatastore(
      boundingBox: BoundingBox(-40, -40, -30, -30),
      ways: const [],
    ));

    expect(await datastore.readMapDataSingle(seamTile), isNull);
  });
}
