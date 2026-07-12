import 'app_failure.dart';

/// The outcome of a data- or domain-layer operation: either [Ok] with a value
/// or [Err] with an [AppFailure]. Repositories and use-cases return `Result`
/// instead of throwing; view-models `switch` on it at the presentation
/// boundary (see the Flutter app-architecture "Result" pattern).
///
/// Exceptions become failures in exactly one place — the data layer's
/// `runCatching` — so everything above it deals in typed failures only.
sealed class Result<T> {
  const Result();

  const factory Result.ok(T value) = Ok<T>;

  const factory Result.error(AppFailure failure) = Err<T>;
}

final class Ok<T> extends Result<T> {
  const Ok(this.value);

  final T value;

  @override
  String toString() => 'Ok($value)';
}

final class Err<T> extends Result<T> {
  const Err(this.failure);

  final AppFailure failure;

  @override
  String toString() => 'Err($failure)';
}

extension ResultX<T> on Result<T> {
  /// Transforms the success value, carrying a failure through untouched.
  Result<R> map<R>(R Function(T value) transform) => switch (this) {
        Ok(:final value) => Ok(transform(value)),
        Err(:final failure) => Err(failure),
      };

  /// Chains a dependent async operation, short-circuiting on failure.
  Future<Result<R>> flatMap<R>(
    Future<Result<R>> Function(T value) next,
  ) async =>
      switch (this) {
        Ok(:final value) => await next(value),
        Err(:final failure) => Err<R>(failure),
      };

  /// The success value, or null on failure. This is how optional secondary
  /// reads stay neutral: a failed enrichment becomes "no data", not an error.
  T? getOrNull() => switch (this) {
        Ok(:final value) => value,
        Err() => null,
      };

  /// TEMPORARY migration bridge: unwraps the value or rethrows the original
  /// throwable with its original stack, so not-yet-migrated callers keep the
  /// exact throwing behavior they had before the Result migration.
  ///
  /// Every use must disappear by the end of the migration; the closeout phase
  /// deletes this member once `grep` finds no callers left in lib/.
  T orThrow() => switch (this) {
        Ok(:final value) => value,
        Err(:final failure) => Error.throwWithStackTrace(
            failure.cause ?? StateError(failure.toString()),
            failure.stackTrace ?? StackTrace.current,
          ),
      };
}
