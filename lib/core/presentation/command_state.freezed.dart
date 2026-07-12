// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'command_state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CommandState<T> {





@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CommandState<T>);
}


@override
int get hashCode => runtimeType.hashCode;

@override
String toString() {
  return 'CommandState<$T>()';
}


}

/// @nodoc
class $CommandStateCopyWith<T,$Res>  {
$CommandStateCopyWith(CommandState<T> _, $Res Function(CommandState<T>) __);
}


/// Adds pattern-matching-related methods to [CommandState].
extension CommandStatePatterns<T> on CommandState<T> {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>({TResult Function( CommandIdle<T> value)?  idle,TResult Function( CommandRunning<T> value)?  running,TResult Function( CommandSuccess<T> value)?  success,TResult Function( CommandFailure<T> value)?  failure,required TResult orElse(),}){
final _that = this;
switch (_that) {
case CommandIdle() when idle != null:
return idle(_that);case CommandRunning() when running != null:
return running(_that);case CommandSuccess() when success != null:
return success(_that);case CommandFailure() when failure != null:
return failure(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>({required TResult Function( CommandIdle<T> value)  idle,required TResult Function( CommandRunning<T> value)  running,required TResult Function( CommandSuccess<T> value)  success,required TResult Function( CommandFailure<T> value)  failure,}){
final _that = this;
switch (_that) {
case CommandIdle():
return idle(_that);case CommandRunning():
return running(_that);case CommandSuccess():
return success(_that);case CommandFailure():
return failure(_that);}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>({TResult? Function( CommandIdle<T> value)?  idle,TResult? Function( CommandRunning<T> value)?  running,TResult? Function( CommandSuccess<T> value)?  success,TResult? Function( CommandFailure<T> value)?  failure,}){
final _that = this;
switch (_that) {
case CommandIdle() when idle != null:
return idle(_that);case CommandRunning() when running != null:
return running(_that);case CommandSuccess() when success != null:
return success(_that);case CommandFailure() when failure != null:
return failure(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>({TResult Function()?  idle,TResult Function()?  running,TResult Function( T value)?  success,TResult Function( ScreenError error)?  failure,required TResult orElse(),}) {final _that = this;
switch (_that) {
case CommandIdle() when idle != null:
return idle();case CommandRunning() when running != null:
return running();case CommandSuccess() when success != null:
return success(_that.value);case CommandFailure() when failure != null:
return failure(_that.error);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>({required TResult Function()  idle,required TResult Function()  running,required TResult Function( T value)  success,required TResult Function( ScreenError error)  failure,}) {final _that = this;
switch (_that) {
case CommandIdle():
return idle();case CommandRunning():
return running();case CommandSuccess():
return success(_that.value);case CommandFailure():
return failure(_that.error);}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>({TResult? Function()?  idle,TResult? Function()?  running,TResult? Function( T value)?  success,TResult? Function( ScreenError error)?  failure,}) {final _that = this;
switch (_that) {
case CommandIdle() when idle != null:
return idle();case CommandRunning() when running != null:
return running();case CommandSuccess() when success != null:
return success(_that.value);case CommandFailure() when failure != null:
return failure(_that.error);case _:
  return null;

}
}

}

/// @nodoc


class CommandIdle<T> implements CommandState<T> {
  const CommandIdle();
  






@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CommandIdle<T>);
}


@override
int get hashCode => runtimeType.hashCode;

@override
String toString() {
  return 'CommandState<$T>.idle()';
}


}




/// @nodoc


class CommandRunning<T> implements CommandState<T> {
  const CommandRunning();
  






@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CommandRunning<T>);
}


@override
int get hashCode => runtimeType.hashCode;

@override
String toString() {
  return 'CommandState<$T>.running()';
}


}




/// @nodoc


class CommandSuccess<T> implements CommandState<T> {
  const CommandSuccess(this.value);
  

 final  T value;

/// Create a copy of CommandState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CommandSuccessCopyWith<T, CommandSuccess<T>> get copyWith => _$CommandSuccessCopyWithImpl<T, CommandSuccess<T>>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CommandSuccess<T>&&const DeepCollectionEquality().equals(other.value, value));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(value));

@override
String toString() {
  return 'CommandState<$T>.success(value: $value)';
}


}

/// @nodoc
abstract mixin class $CommandSuccessCopyWith<T,$Res> implements $CommandStateCopyWith<T, $Res> {
  factory $CommandSuccessCopyWith(CommandSuccess<T> value, $Res Function(CommandSuccess<T>) _then) = _$CommandSuccessCopyWithImpl;
@useResult
$Res call({
 T value
});




}
/// @nodoc
class _$CommandSuccessCopyWithImpl<T,$Res>
    implements $CommandSuccessCopyWith<T, $Res> {
  _$CommandSuccessCopyWithImpl(this._self, this._then);

  final CommandSuccess<T> _self;
  final $Res Function(CommandSuccess<T>) _then;

/// Create a copy of CommandState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') $Res call({Object? value = freezed,}) {
  return _then(CommandSuccess<T>(
freezed == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as T,
  ));
}


}

/// @nodoc


class CommandFailure<T> implements CommandState<T> {
  const CommandFailure(this.error);
  

 final  ScreenError error;

/// Create a copy of CommandState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CommandFailureCopyWith<T, CommandFailure<T>> get copyWith => _$CommandFailureCopyWithImpl<T, CommandFailure<T>>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CommandFailure<T>&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,error);

@override
String toString() {
  return 'CommandState<$T>.failure(error: $error)';
}


}

/// @nodoc
abstract mixin class $CommandFailureCopyWith<T,$Res> implements $CommandStateCopyWith<T, $Res> {
  factory $CommandFailureCopyWith(CommandFailure<T> value, $Res Function(CommandFailure<T>) _then) = _$CommandFailureCopyWithImpl;
@useResult
$Res call({
 ScreenError error
});




}
/// @nodoc
class _$CommandFailureCopyWithImpl<T,$Res>
    implements $CommandFailureCopyWith<T, $Res> {
  _$CommandFailureCopyWithImpl(this._self, this._then);

  final CommandFailure<T> _self;
  final $Res Function(CommandFailure<T>) _then;

/// Create a copy of CommandState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') $Res call({Object? error = null,}) {
  return _then(CommandFailure<T>(
null == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError,
  ));
}


}

// dart format on
