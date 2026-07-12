import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/screen_error.dart';
import 'package:openvitals/core/result/app_failure.dart';

void main() {
  test('PermissionFailure maps to ScreenErrorPermissionDenied', () {
    expect(
      const PermissionFailure('steps write').toScreenError(),
      const ScreenErrorPermissionDenied(),
    );
  });

  test('HealthConnectUnavailableFailure maps to its ScreenError', () {
    expect(
      const HealthConnectUnavailableFailure().toScreenError(),
      const ScreenErrorHealthConnectUnavailable(),
    );
  });

  test('NotFoundFailure maps to ScreenErrorNotFound', () {
    expect(
      const NotFoundFailure().toScreenError(),
      const ScreenErrorNotFound(),
    );
  });

  test('UnexpectedFailure maps to a trimmed ScreenErrorMessage', () {
    expect(
      const UnexpectedFailure(' something broke ').toScreenError(),
      const ScreenErrorMessage('something broke'),
    );
  });

  test('blank or null-ish UnexpectedFailure falls back', () {
    expect(
      const UnexpectedFailure('  ').toScreenError(fallback: 'Fallback.'),
      const ScreenErrorMessage('Fallback.'),
    );
    expect(
      const UnexpectedFailure('null').toScreenError(fallback: 'Fallback.'),
      const ScreenErrorMessage('Fallback.'),
    );
  });
}
