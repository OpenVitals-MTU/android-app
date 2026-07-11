import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../l10n/app_localizations.dart';

// Outbound URLs. These deliberately do NOT live in the ARB catalog: a URL is not
// localizable content, and ARB has no `translatable="false"` marker to keep a
// translator from "translating" one. They were `translatable="false"` strings in
// the Kotlin `strings.xml`; here they are ordinary constants.

/// Issue tracker, linked from the Settings support section.
const String supportIssuesUrl = 'https://codeberg.org/mmarca-tech/OpenVitals/issues';

/// Community chat, linked from the Settings support section.
const String supportDiscussionUrl = 'http://openvitals.zulipchat.com/';

/// Donation page, linked from the Settings support section.
const String supportUrl = 'https://liberapay.com/manuel.mmarca.tech/donate';

/// "How to" guide for offline maps, linked from the offline-maps card.
const String offlineMapsHelpUrl =
    'https://openvitals.codeberg.page/website/how-to/offline-maps/';

/// Opens [url] in the system browser, the Flutter equivalent of the Kotlin
/// app's `LocalUriHandler.openUri`. Best-effort: on a malformed URL or a
/// platform that cannot launch it, a SnackBar reports the failure rather than
/// throwing.
Future<void> openExternalUrl(BuildContext context, String url) async {
  final messenger = ScaffoldMessenger.maybeOf(context);
  final l10n = AppLocalizations.of(context);
  final uri = Uri.tryParse(url);
  var launched = false;
  if (uri != null) {
    try {
      launched = await launchUrl(uri, mode: LaunchMode.externalApplication);
    } catch (_) {
      launched = false;
    }
  }
  if (!launched) {
    messenger?.showSnackBar(
      SnackBar(content: Text(l10n.linkCouldNotOpen)),
    );
  }
}
