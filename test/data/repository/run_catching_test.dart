import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/result/app_failure.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/data/repository/contract/repository_exceptions.dart';
import 'package:openvitals/data/repository/impl/run_catching.dart';

void main() {
  test('wraps a successful body in Ok', () async {
    final result = await runCatching(() async => 42);

    expect((result as Ok<int>).value, 42);
  });

  test('maps MissingHealthPermissionException to PermissionFailure', () async {
    const exception = MissingHealthPermissionException('steps write');

    final result = await runCatching<int>(() async => throw exception);

    final failure = (result as Err<int>).failure;
    expect(failure, isA<PermissionFailure>());
    expect((failure as PermissionFailure).message, 'steps write');
    expect(failure.cause, same(exception));
    expect(failure.stackTrace, isNotNull);
  });

  test('maps any other exception to UnexpectedFailure', () async {
    final exception = Exception('network gone');

    final result = await runCatching<int>(() async => throw exception);

    final failure = (result as Err<int>).failure;
    expect(failure, isA<UnexpectedFailure>());
    expect((failure as UnexpectedFailure).message, contains('network gone'));
    expect(failure.cause, same(exception));
  });

  test('catches Errors too, matching the pre-Result bare catch in notifiers',
      () async {
    final result = await runCatching<int>(() async => throw StateError('bug'));

    final failure = (result as Err<int>).failure;
    expect(failure, isA<UnexpectedFailure>());
    expect(failure.cause, isA<StateError>());
  });
}
