import '../../../core/presentation/screen_error.dart';

/// Resolves a [ScreenError] into the message a settings card shows when one of
/// its commands fails. Same shape as the onboarding screen's resolver: the
/// settings cards' failure copy is untranslated in Kotlin too.
String settingsErrorText(ScreenError error) => switch (error) {
      ScreenErrorMessage(:final text) => text,
      ScreenErrorNotFound() => 'Not found.',
      ScreenErrorMissingArgument() => 'Something went wrong.',
      ScreenErrorPermissionDenied() => 'Permission denied.',
      ScreenErrorHealthConnectUnavailable() => 'Health Connect is unavailable.',
    };
