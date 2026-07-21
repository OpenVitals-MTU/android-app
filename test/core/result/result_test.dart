import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';

void main() {
  const failure = UnexpectedFailure('boom');

  test('map transforms an Ok value', () {
    const Result<int> result = Result.ok(2);

    final mapped = result.map((value) => value * 10);

    expect(mapped, isA<Ok<int>>());
    expect((mapped as Ok<int>).value, 20);
  });

  test('map carries a failure through untouched', () {
    const Result<int> result = Result.error(failure);

    final mapped = result.map((value) => value * 10);

    expect(mapped, isA<Err<int>>());
    expect((mapped as Err<int>).failure, same(failure));
  });

  test('flatMap chains on Ok', () async {
    const Result<int> result = Result.ok(2);

    final chained = await result.flatMap((value) async => Result.ok('$value!'));

    expect((chained as Ok<String>).value, '2!');
  });

  test('flatMap short-circuits on Err without invoking next', () async {
    const Result<int> result = Result.error(failure);
    var invoked = false;

    final chained = await result.flatMap((value) async {
      invoked = true;
      return const Result.ok('unreachable');
    });

    expect(invoked, isFalse);
    expect((chained as Err<String>).failure, same(failure));
  });

  test('getOrNull returns the value on Ok and null on Err', () {
    expect(const Result.ok(7).getOrNull(), 7);
    expect(const Result<int>.error(failure).getOrNull(), isNull);
  });

  test('orThrow unwraps an Ok value', () {
    expect(const Result.ok(7).orThrow(), 7);
  });

  test('orThrow rethrows the original cause with its original stack', () {
    final original = StateError('original');
    late final StackTrace originalStack;
    late final Result<int> result;
    try {
      throw original;
    } catch (error, stackTrace) {
      originalStack = stackTrace;
      result = Result.error(
        UnexpectedFailure('$error', cause: error, stackTrace: stackTrace),
      );
    }

    try {
      result.orThrow();
      fail('orThrow should have thrown');
    } catch (error, stackTrace) {
      expect(error, same(original));
      expect(stackTrace.toString(), originalStack.toString());
    }
  });

  test('orThrow on a cause-less failure throws a StateError naming it', () {
    const Result<int> result = Result.error(NotFoundFailure());

    expect(
      result.orThrow,
      throwsA(
        isA<StateError>().having(
          (error) => error.message,
          'message',
          contains('NotFoundFailure'),
        ),
      ),
    );
  });

  test('Ok has value equality', () {
    expect(const Ok(2), const Ok(2));
    expect(const Ok(2).hashCode, const Ok(2).hashCode);
    expect(const Ok(2), isNot(const Ok(3)));
    // Distinguishes the case as well as the value.
    expect(const Ok<int>(2), isNot(const Err<int>(failure)));
  });

  test('Err equals another Err over the same failure', () {
    expect(const Err<int>(failure), const Err<int>(failure));
    expect(const Err<int>(failure).hashCode, const Err<int>(failure).hashCode);
  });
}
