// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'body_measurement_entry_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BodyMeasurementEntryState {

 BodyMeasurementType get type; String get inputText; Set<String> get writePermissions; bool get canWrite; bool get isCheckingPermission; CommandState<void> get save; String? get editRecordId; DateTime? get editTime; BodyMeasurementEntryError? get entryError;/// The edit prefill could not be read — a different thing from a save that
/// failed, and it blocks the form before the user has done anything.
 ScreenError? get prefillError;
/// Create a copy of BodyMeasurementEntryState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BodyMeasurementEntryStateCopyWith<BodyMeasurementEntryState> get copyWith => _$BodyMeasurementEntryStateCopyWithImpl<BodyMeasurementEntryState>(this as BodyMeasurementEntryState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BodyMeasurementEntryState&&(identical(other.type, type) || other.type == type)&&(identical(other.inputText, inputText) || other.inputText == inputText)&&const DeepCollectionEquality().equals(other.writePermissions, writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.save, save) || other.save == save)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.editTime, editTime) || other.editTime == editTime)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.prefillError, prefillError) || other.prefillError == prefillError));
}


@override
int get hashCode => Object.hash(runtimeType,type,inputText,const DeepCollectionEquality().hash(writePermissions),canWrite,isCheckingPermission,save,editRecordId,editTime,entryError,prefillError);

@override
String toString() {
  return 'BodyMeasurementEntryState(type: $type, inputText: $inputText, writePermissions: $writePermissions, canWrite: $canWrite, isCheckingPermission: $isCheckingPermission, save: $save, editRecordId: $editRecordId, editTime: $editTime, entryError: $entryError, prefillError: $prefillError)';
}


}

/// @nodoc
abstract mixin class $BodyMeasurementEntryStateCopyWith<$Res>  {
  factory $BodyMeasurementEntryStateCopyWith(BodyMeasurementEntryState value, $Res Function(BodyMeasurementEntryState) _then) = _$BodyMeasurementEntryStateCopyWithImpl;
@useResult
$Res call({
 BodyMeasurementType type, String inputText, Set<String> writePermissions, bool canWrite, bool isCheckingPermission, CommandState<void> save, String? editRecordId, DateTime? editTime, BodyMeasurementEntryError? entryError, ScreenError? prefillError
});


$CommandStateCopyWith<void, $Res> get save;

}
/// @nodoc
class _$BodyMeasurementEntryStateCopyWithImpl<$Res>
    implements $BodyMeasurementEntryStateCopyWith<$Res> {
  _$BodyMeasurementEntryStateCopyWithImpl(this._self, this._then);

  final BodyMeasurementEntryState _self;
  final $Res Function(BodyMeasurementEntryState) _then;

/// Create a copy of BodyMeasurementEntryState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? type = null,Object? inputText = null,Object? writePermissions = null,Object? canWrite = null,Object? isCheckingPermission = null,Object? save = null,Object? editRecordId = freezed,Object? editTime = freezed,Object? entryError = freezed,Object? prefillError = freezed,}) {
  return _then(_self.copyWith(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as BodyMeasurementType,inputText: null == inputText ? _self.inputText : inputText // ignore: cast_nullable_to_non_nullable
as String,writePermissions: null == writePermissions ? _self.writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,save: null == save ? _self.save : save // ignore: cast_nullable_to_non_nullable
as CommandState<void>,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,editTime: freezed == editTime ? _self.editTime : editTime // ignore: cast_nullable_to_non_nullable
as DateTime?,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as BodyMeasurementEntryError?,prefillError: freezed == prefillError ? _self.prefillError : prefillError // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}
/// Create a copy of BodyMeasurementEntryState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get save {
  
  return $CommandStateCopyWith<void, $Res>(_self.save, (value) {
    return _then(_self.copyWith(save: value));
  });
}
}


/// Adds pattern-matching-related methods to [BodyMeasurementEntryState].
extension BodyMeasurementEntryStatePatterns on BodyMeasurementEntryState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BodyMeasurementEntryState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BodyMeasurementEntryState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BodyMeasurementEntryState value)  $default,){
final _that = this;
switch (_that) {
case _BodyMeasurementEntryState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BodyMeasurementEntryState value)?  $default,){
final _that = this;
switch (_that) {
case _BodyMeasurementEntryState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( BodyMeasurementType type,  String inputText,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  CommandState<void> save,  String? editRecordId,  DateTime? editTime,  BodyMeasurementEntryError? entryError,  ScreenError? prefillError)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BodyMeasurementEntryState() when $default != null:
return $default(_that.type,_that.inputText,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.save,_that.editRecordId,_that.editTime,_that.entryError,_that.prefillError);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( BodyMeasurementType type,  String inputText,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  CommandState<void> save,  String? editRecordId,  DateTime? editTime,  BodyMeasurementEntryError? entryError,  ScreenError? prefillError)  $default,) {final _that = this;
switch (_that) {
case _BodyMeasurementEntryState():
return $default(_that.type,_that.inputText,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.save,_that.editRecordId,_that.editTime,_that.entryError,_that.prefillError);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( BodyMeasurementType type,  String inputText,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  CommandState<void> save,  String? editRecordId,  DateTime? editTime,  BodyMeasurementEntryError? entryError,  ScreenError? prefillError)?  $default,) {final _that = this;
switch (_that) {
case _BodyMeasurementEntryState() when $default != null:
return $default(_that.type,_that.inputText,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.save,_that.editRecordId,_that.editTime,_that.entryError,_that.prefillError);case _:
  return null;

}
}

}

/// @nodoc


class _BodyMeasurementEntryState extends BodyMeasurementEntryState {
  const _BodyMeasurementEntryState({required this.type, this.inputText = '', final  Set<String> writePermissions = const <String>{}, this.canWrite = false, this.isCheckingPermission = true, this.save = const CommandState<void>.idle(), this.editRecordId, this.editTime, this.entryError, this.prefillError}): _writePermissions = writePermissions,super._();
  

@override final  BodyMeasurementType type;
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
@override final  String? editRecordId;
@override final  DateTime? editTime;
@override final  BodyMeasurementEntryError? entryError;
/// The edit prefill could not be read — a different thing from a save that
/// failed, and it blocks the form before the user has done anything.
@override final  ScreenError? prefillError;

/// Create a copy of BodyMeasurementEntryState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BodyMeasurementEntryStateCopyWith<_BodyMeasurementEntryState> get copyWith => __$BodyMeasurementEntryStateCopyWithImpl<_BodyMeasurementEntryState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BodyMeasurementEntryState&&(identical(other.type, type) || other.type == type)&&(identical(other.inputText, inputText) || other.inputText == inputText)&&const DeepCollectionEquality().equals(other._writePermissions, _writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.save, save) || other.save == save)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.editTime, editTime) || other.editTime == editTime)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.prefillError, prefillError) || other.prefillError == prefillError));
}


@override
int get hashCode => Object.hash(runtimeType,type,inputText,const DeepCollectionEquality().hash(_writePermissions),canWrite,isCheckingPermission,save,editRecordId,editTime,entryError,prefillError);

@override
String toString() {
  return 'BodyMeasurementEntryState(type: $type, inputText: $inputText, writePermissions: $writePermissions, canWrite: $canWrite, isCheckingPermission: $isCheckingPermission, save: $save, editRecordId: $editRecordId, editTime: $editTime, entryError: $entryError, prefillError: $prefillError)';
}


}

/// @nodoc
abstract mixin class _$BodyMeasurementEntryStateCopyWith<$Res> implements $BodyMeasurementEntryStateCopyWith<$Res> {
  factory _$BodyMeasurementEntryStateCopyWith(_BodyMeasurementEntryState value, $Res Function(_BodyMeasurementEntryState) _then) = __$BodyMeasurementEntryStateCopyWithImpl;
@override @useResult
$Res call({
 BodyMeasurementType type, String inputText, Set<String> writePermissions, bool canWrite, bool isCheckingPermission, CommandState<void> save, String? editRecordId, DateTime? editTime, BodyMeasurementEntryError? entryError, ScreenError? prefillError
});


@override $CommandStateCopyWith<void, $Res> get save;

}
/// @nodoc
class __$BodyMeasurementEntryStateCopyWithImpl<$Res>
    implements _$BodyMeasurementEntryStateCopyWith<$Res> {
  __$BodyMeasurementEntryStateCopyWithImpl(this._self, this._then);

  final _BodyMeasurementEntryState _self;
  final $Res Function(_BodyMeasurementEntryState) _then;

/// Create a copy of BodyMeasurementEntryState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? type = null,Object? inputText = null,Object? writePermissions = null,Object? canWrite = null,Object? isCheckingPermission = null,Object? save = null,Object? editRecordId = freezed,Object? editTime = freezed,Object? entryError = freezed,Object? prefillError = freezed,}) {
  return _then(_BodyMeasurementEntryState(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as BodyMeasurementType,inputText: null == inputText ? _self.inputText : inputText // ignore: cast_nullable_to_non_nullable
as String,writePermissions: null == writePermissions ? _self._writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,save: null == save ? _self.save : save // ignore: cast_nullable_to_non_nullable
as CommandState<void>,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,editTime: freezed == editTime ? _self.editTime : editTime // ignore: cast_nullable_to_non_nullable
as DateTime?,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as BodyMeasurementEntryError?,prefillError: freezed == prefillError ? _self.prefillError : prefillError // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}

/// Create a copy of BodyMeasurementEntryState
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
