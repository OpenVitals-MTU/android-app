// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'carbs_entry_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CarbsEntryState {

 String get inputText; Set<String> get writePermissions; bool get canWrite; bool get isCheckingPermission; CommandState<void> get save; CarbsEntryError? get entryError;
/// Create a copy of CarbsEntryState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CarbsEntryStateCopyWith<CarbsEntryState> get copyWith => _$CarbsEntryStateCopyWithImpl<CarbsEntryState>(this as CarbsEntryState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CarbsEntryState&&(identical(other.inputText, inputText) || other.inputText == inputText)&&const DeepCollectionEquality().equals(other.writePermissions, writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.save, save) || other.save == save)&&(identical(other.entryError, entryError) || other.entryError == entryError));
}


@override
int get hashCode => Object.hash(runtimeType,inputText,const DeepCollectionEquality().hash(writePermissions),canWrite,isCheckingPermission,save,entryError);

@override
String toString() {
  return 'CarbsEntryState(inputText: $inputText, writePermissions: $writePermissions, canWrite: $canWrite, isCheckingPermission: $isCheckingPermission, save: $save, entryError: $entryError)';
}


}

/// @nodoc
abstract mixin class $CarbsEntryStateCopyWith<$Res>  {
  factory $CarbsEntryStateCopyWith(CarbsEntryState value, $Res Function(CarbsEntryState) _then) = _$CarbsEntryStateCopyWithImpl;
@useResult
$Res call({
 String inputText, Set<String> writePermissions, bool canWrite, bool isCheckingPermission, CommandState<void> save, CarbsEntryError? entryError
});


$CommandStateCopyWith<void, $Res> get save;

}
/// @nodoc
class _$CarbsEntryStateCopyWithImpl<$Res>
    implements $CarbsEntryStateCopyWith<$Res> {
  _$CarbsEntryStateCopyWithImpl(this._self, this._then);

  final CarbsEntryState _self;
  final $Res Function(CarbsEntryState) _then;

/// Create a copy of CarbsEntryState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? inputText = null,Object? writePermissions = null,Object? canWrite = null,Object? isCheckingPermission = null,Object? save = null,Object? entryError = freezed,}) {
  return _then(_self.copyWith(
inputText: null == inputText ? _self.inputText : inputText // ignore: cast_nullable_to_non_nullable
as String,writePermissions: null == writePermissions ? _self.writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,save: null == save ? _self.save : save // ignore: cast_nullable_to_non_nullable
as CommandState<void>,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as CarbsEntryError?,
  ));
}
/// Create a copy of CarbsEntryState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get save {
  
  return $CommandStateCopyWith<void, $Res>(_self.save, (value) {
    return _then(_self.copyWith(save: value));
  });
}
}


/// Adds pattern-matching-related methods to [CarbsEntryState].
extension CarbsEntryStatePatterns on CarbsEntryState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CarbsEntryState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CarbsEntryState() when $default != null:
return $default(_that);case _:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CarbsEntryState value)  $default,){
final _that = this;
switch (_that) {
case _CarbsEntryState():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CarbsEntryState value)?  $default,){
final _that = this;
switch (_that) {
case _CarbsEntryState() when $default != null:
return $default(_that);case _:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String inputText,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  CommandState<void> save,  CarbsEntryError? entryError)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CarbsEntryState() when $default != null:
return $default(_that.inputText,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.save,_that.entryError);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String inputText,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  CommandState<void> save,  CarbsEntryError? entryError)  $default,) {final _that = this;
switch (_that) {
case _CarbsEntryState():
return $default(_that.inputText,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.save,_that.entryError);case _:
  throw StateError('Unexpected subclass');

}
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String inputText,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  CommandState<void> save,  CarbsEntryError? entryError)?  $default,) {final _that = this;
switch (_that) {
case _CarbsEntryState() when $default != null:
return $default(_that.inputText,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.save,_that.entryError);case _:
  return null;

}
}

}

/// @nodoc


class _CarbsEntryState extends CarbsEntryState {
  const _CarbsEntryState({this.inputText = '', final  Set<String> writePermissions = const <String>{}, this.canWrite = false, this.isCheckingPermission = true, this.save = const CommandState<void>.idle(), this.entryError}): _writePermissions = writePermissions,super._();
  

@override@JsonKey() final  String inputText;
 final  Set<String> _writePermissions;
@override@JsonKey() Set<String> get writePermissions {
  if (_writePermissions is EqualUnmodifiableSetView) return _writePermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_writePermissions);
}

@override@JsonKey() final  bool canWrite;
@override@JsonKey() final  bool isCheckingPermission;
@override@JsonKey() final  CommandState<void> save;
@override final  CarbsEntryError? entryError;

/// Create a copy of CarbsEntryState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CarbsEntryStateCopyWith<_CarbsEntryState> get copyWith => __$CarbsEntryStateCopyWithImpl<_CarbsEntryState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CarbsEntryState&&(identical(other.inputText, inputText) || other.inputText == inputText)&&const DeepCollectionEquality().equals(other._writePermissions, _writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.save, save) || other.save == save)&&(identical(other.entryError, entryError) || other.entryError == entryError));
}


@override
int get hashCode => Object.hash(runtimeType,inputText,const DeepCollectionEquality().hash(_writePermissions),canWrite,isCheckingPermission,save,entryError);

@override
String toString() {
  return 'CarbsEntryState(inputText: $inputText, writePermissions: $writePermissions, canWrite: $canWrite, isCheckingPermission: $isCheckingPermission, save: $save, entryError: $entryError)';
}


}

/// @nodoc
abstract mixin class _$CarbsEntryStateCopyWith<$Res> implements $CarbsEntryStateCopyWith<$Res> {
  factory _$CarbsEntryStateCopyWith(_CarbsEntryState value, $Res Function(_CarbsEntryState) _then) = __$CarbsEntryStateCopyWithImpl;
@override @useResult
$Res call({
 String inputText, Set<String> writePermissions, bool canWrite, bool isCheckingPermission, CommandState<void> save, CarbsEntryError? entryError
});


@override $CommandStateCopyWith<void, $Res> get save;

}
/// @nodoc
class __$CarbsEntryStateCopyWithImpl<$Res>
    implements _$CarbsEntryStateCopyWith<$Res> {
  __$CarbsEntryStateCopyWithImpl(this._self, this._then);

  final _CarbsEntryState _self;
  final $Res Function(_CarbsEntryState) _then;

/// Create a copy of CarbsEntryState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? inputText = null,Object? writePermissions = null,Object? canWrite = null,Object? isCheckingPermission = null,Object? save = null,Object? entryError = freezed,}) {
  return _then(_CarbsEntryState(
inputText: null == inputText ? _self.inputText : inputText // ignore: cast_nullable_to_non_nullable
as String,writePermissions: null == writePermissions ? _self._writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,save: null == save ? _self.save : save // ignore: cast_nullable_to_non_nullable
as CommandState<void>,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as CarbsEntryError?,
  ));
}

/// Create a copy of CarbsEntryState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get save {
  
  return $CommandStateCopyWith<void, $Res>(_self.save, (value) {
    return _then(_self.copyWith(save: value));
  });
}
}

// dart format on
