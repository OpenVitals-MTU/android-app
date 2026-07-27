import 'package:share_plus/share_plus.dart';

import '../../../domain/model/activity_models.dart';
import 'activity_route_export.dart';
import 'activity_route_export_cache.dart';

/// Hands a route export to the system share sheet, so it can leave the app as a
/// Signal/WhatsApp message, an email attachment, or anything else that accepts
/// a file — `Intent.createChooser(ACTION_SEND)`.
///
/// The sibling of [ActivityRouteExportCache]'s other consumer: "Open route in
/// map app" fires ACTION_VIEW and offers map apps, this fires ACTION_SEND and
/// offers messengers. They are different intents and neither substitutes for
/// the other, which is why open_filex and share_plus both earn their place.
///
/// The FileProvider half is absorbed by the plugin, exactly as it is for
/// [DebugLogSharing]: share_plus ships its own provider (authority
/// `${applicationId}.flutter.share_provider`) and copies whatever file it is
/// given into `cacheDir/share_plus/` before granting the URI, so the Android
/// host needs no `file_paths.xml` entry for the staged export.
class ActivityRouteSharing {
  const ActivityRouteSharing({
    this.cache = const ActivityRouteExportCache(),
    this.share,
  });

  final ActivityRouteExportCache cache;

  /// Test seam for raising the sheet; defaults to [SharePlus.instance], which a
  /// `const` constructor cannot name because it is a lazy static.
  final Future<void> Function(ShareParams params)? share;

  /// Stages [workout]'s route as [format] and raises the share sheet.
  ///
  /// Throws on any failure (empty route, write, or no share target); the caller
  /// catches and surfaces one message. A user who dismisses the sheet without
  /// picking anything is NOT a failure and reports nothing — the sheet is its
  /// own feedback, which is also why success shows no snackbar.
  Future<void> shareRoute({
    required ExerciseData workout,
    required ActivityRouteExportFormat format,
    required String chooserTitle,
  }) async {
    final file = await cache.write(workout, format);
    await (share ?? _defaultShare)(
      ShareParams(
        files: [XFile(file.path, mimeType: format.mimeType)],
        // Android maps this onto Intent.createChooser's title.
        title: chooserTitle,
        // The email fallback's subject line. The generated file name already
        // carries the activity title and its date, which is what a recipient
        // needs to tell one route from another.
        subject: activityRouteExportFileName(workout, format),
      ),
    );
  }

  static Future<void> _defaultShare(ShareParams params) =>
      SharePlus.instance.share(params);
}
