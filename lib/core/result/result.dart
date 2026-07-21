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

  // Value equality so a state object embedding a Result short-circuits Riverpod's
  // identical-state check, and test matchers can compare directly.
  @override
  bool operator ==(Object other) =>
      identical(this, other) || (other is Ok<T> && other.value == value);

  @override
  int get hashCode => Object.hash(Ok<T>, value);
}

final class Err<T> extends Result<T> {
  const Err(this.failure);

  final AppFailure failure;

  @override
  String toString() => 'Err($failure)';

  @override
  bool operator ==(Object other) =>
      identical(this, other) || (other is Err<T> && other.failure == failure);

  @override
  int get hashCode => Object.hash(Err<T>, failure);
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

  /// Unwraps the value, or rethrows the original throwable with its original
  /// stack.
  ///
  /// This is the adapter to the parts of Dart that signal failure by throwing,
  /// and it is *not* a shortcut for a view-model that should be switching on
  /// the `Result`. The legitimate callers are:
  ///
  /// - a `FutureProvider` body, whose error channel (`AsyncError`) already IS
  ///   Riverpod's version of this type — see `health_connect_gate.dart`;
  /// - a background-isolate entrypoint, which has no screen to render a
  ///   `ScreenError` onto and must fail loudly instead of silently;
  /// - a repository composing another repository *inside* its own
  ///   `runCatching`, where the throw is caught two lines later and re-typed.
  ///
  /// Anywhere else, switch on the `Result`.
  T orThrow() => switch (this) {
        Ok(:final value) => value,
        Err(:final failure) => Error.throwWithStackTrace(
            failure.cause ?? StateError(failure.toString()),
            failure.stackTrace ?? StackTrace.current,
          ),
      };
}
