import 'dart:typed_data';

import 'package:pmtiles/pmtiles.dart';
import 'package:vector_map_tiles/vector_map_tiles.dart';

/// A [VectorTileProvider] backed by a local PMTiles archive.
///
/// Replaces the Kotlin app's MapLibre `pmtiles://` protocol handler: each
/// imported `.pmtiles` pack is opened as a [PmTilesArchive] and its MVT tiles
/// are served straight from disk — never from the network (the app ships
/// without the INTERNET permission).
///
/// The published `vector_map_tiles_pmtiles` bridge does not support
/// vector_map_tiles 10.x, hence this small adapter.
class PmtilesVectorTileProvider extends VectorTileProvider {
  PmtilesVectorTileProvider(this.archive);

  final PmTilesArchive archive;

  @override
  TileProviderType get type => TileProviderType.vector;

  @override
  TileOffset get tileOffset => TileOffset.DEFAULT;

  @override
  int get minimumZoom => archive.minZoom;

  @override
  int get maximumZoom => archive.maxZoom;

  @override
  Future<Uint8List> provide(TileIdentity tile) async {
    final Tile data;
    try {
      data = await archive.tile(ZXY(tile.z, tile.x, tile.y).toTileId());
    } catch (error) {
      throw ProviderException(
        message: 'Failed to look up PMTiles tile $tile: $error',
        retryable: Retryable.none,
        statusCode: 500,
      );
    }
    try {
      // `bytes()` applies the archive's tile compression (gzip) and returns
      // uncompressed MVT bytes, which is what vector_tile_renderer expects.
      return Uint8List.fromList(data.bytes());
    } on TileNotFoundException {
      // Mapped to an empty tile by vector_map_tiles (statusCode 404).
      throw ProviderException(
        message: 'PMTiles tile not found: $tile',
        retryable: Retryable.none,
        statusCode: 404,
      );
    } catch (error) {
      throw ProviderException(
        message: 'Failed to read PMTiles tile $tile: $error',
        retryable: Retryable.none,
        statusCode: 500,
      );
    }
  }
}
