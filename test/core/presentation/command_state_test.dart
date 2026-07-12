import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/presentation/command_state.dart';
import 'package:openvitals/core/presentation/screen_error.dart';

void main() {
  test('variants of the same shape and value are equal', () {
    expect(const CommandState<void>.idle(), const CommandState<void>.idle());
    expect(const CommandState<int>.success(3), const CommandState.success(3));
    expect(
      const CommandState<int>.failure(ScreenErrorMessage('nope')),
      const CommandState<int>.failure(ScreenErrorMessage('nope')),
    );
  });

  test('distinct variants are not equal', () {
    expect(
      const CommandState<int>.idle(),
      isNot(const CommandState<int>.running()),
    );
    expect(
      const CommandState<int>.success(3),
      isNot(const CommandState<int>.success(4)),
    );
  });

  test('switch exhaustively covers the command lifecycle', () {
    String describe(CommandState<int> state) => switch (state) {
          CommandIdle() => 'idle',
          CommandRunning() => 'running',
          CommandSuccess(:final value) => 'success:$value',
          CommandFailure(:final error) => 'failure:$error',
        };

    expect(describe(const CommandState.idle()), 'idle');
    expect(describe(const CommandState.running()), 'running');
    expect(describe(const CommandState.success(9)), 'success:9');
    expect(
      describe(const CommandState.failure(ScreenErrorNotFound())),
      startsWith('failure:'),
    );
  });
}
