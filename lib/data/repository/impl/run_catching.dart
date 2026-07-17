import 'package:flutter/services.dart';

import '../../../core/result/app_failure.dart';
import '../../../core/result/result.dart';
import '../contract/repository_exceptions.dart';

/// Upper bound on a composed period load. A single native Health Connect read
/// that never returns (seen on very large Year windows) would otherwise leave
/// the metric screen pinned on its "Syncing with Health Connect…" banner
/// forever, since that banner is just the view-model's `isLoading`. Wrapping the
/// composed body in `.timeout(healthReadBudget)` inside [runCatching] turns a
/// stuck read into a retryable [UnexpectedFailure] → `ScreenError`. Generous on
/// purpose: it must not fire on a legitimately slow-but-valid cold Year load.
const Duration healthReadBudget = Duration(seconds: 30);

/// Per-metric budget for the non-day vitals overview. Metrics with no Health
/// Connect aggregate (respiratory rate, SpO2, …) must be read as raw records; a
/// densely-sampled one (e.g. a year of wearable respiratory rate) can take 40s+
/// and would otherwise sink the whole combined load. Budgeting each metric on
/// its own lets a too-large one degrade to "unavailable for this range" while
/// every other card still renders. Well clear of the sparse metrics (<1s) and of
/// [healthReadBudget], so it only ever fires on a genuinely oversized read.
const Duration vitalsMetricBudget = Duration(seconds: 6);

/// The single place where thrown errors become [AppFailure]s. Repository
/// implementations wrap each operation's body in [runCatching]; nothing above
/// the data layer converts exceptions.
///
/// Deliberately catches *everything* (including `Error`s): the pre-Result
/// view-models did a bare `catch (error)` around repository calls, and the
/// migration must not turn errors that used to render as a `ScreenError`
/// into crashes. The original throwable and stack are preserved on the
/// failure for logging and the temporary `orThrow` bridge.
Future<Result<T>> runCatching<T>(Future<T> Function() body) async {
  try {
    return Ok(await body());
  } on MissingHealthPermissionException catch (error, stackTrace) {
    return Err(
      PermissionFailure(error.message, cause: error, stackTrace: stackTrace),
    );
  } on PlatformException catch (error, stackTrace) {
    // Kept out of UnexpectedFailure on purpose: a spent Health Connect quota is not
    // a broken record, and a bulk import has to STOP on it rather than shrug and
    // fail every remaining file for the same reason.
    if (error.code == healthConnectRateLimitedCode) {
      return Err(
        RateLimitFailure(
          error.message ?? 'Health Connect API call quota exceeded.',
          cause: error,
          stackTrace: stackTrace,
        ),
      );
    }
    return Err(
      UnexpectedFailure(error.toString(), cause: error, stackTrace: stackTrace),
    );
  } catch (error, stackTrace) {
    return Err(
      UnexpectedFailure(
        error.toString(),
        cause: error,
        stackTrace: stackTrace,
      ),
    );
  }
}
