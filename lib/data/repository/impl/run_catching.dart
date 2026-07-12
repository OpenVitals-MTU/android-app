import '../../../core/result/app_failure.dart';
import '../../../core/result/result.dart';
import '../contract/repository_exceptions.dart';

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
