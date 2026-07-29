/// Handing a staged export to the system share sheet —
/// `Intent.createChooser(ACTION_SEND)`.
///
/// The one place `ShareParams` is built. Route exports, the diagnostics log and
/// the import reports had each grown their own identical construction of it;
/// they now differ only in the file, the MIME type and the strings they pass.
///
/// Distinct from opening a file (ACTION_VIEW, `open_filex`), which offers apps
/// that can *display* the type — map apps for a GPX. This offers the apps that
/// can *send* it: Signal, WhatsApp, Telegram, mail. Neither substitutes for the
/// other, which is why both plugins earn their place.
library;

import 'dart:io';

import 'package:share_plus/share_plus.dart';

/// Raises the share sheet. Injected as a seam so a test never raises a real one.
typedef ShareSheet = Future<void> Function(ShareParams params);

/// Hands [file] to the share sheet as a [mimeType] attachment.
///
/// [subject] fills the email fallback's subject line; callers pass the export's
/// file name, which is what tells one attachment from another in an inbox.
///
/// Throws when no target accepts it. A user who dismisses the sheet without
/// picking anything is NOT a failure and returns normally — the sheet is its own
/// feedback, which is also why a successful share shows no confirmation.
Future<void> shareStagedFile({
  required File file,
  required String mimeType,
  required String chooserTitle,
  String? subject,
  ShareSheet? share,
}) =>
    (share ?? _defaultShare)(
      ShareParams(
        files: [XFile(file.path, mimeType: mimeType)],
        // Android maps this onto Intent.createChooser's title.
        title: chooserTitle,
        subject: subject,
      ),
    );

Future<void> _defaultShare(ShareParams params) =>
    SharePlus.instance.share(params);
