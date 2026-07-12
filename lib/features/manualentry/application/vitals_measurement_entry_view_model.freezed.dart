// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'vitals_measurement_entry_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$VitalsMeasurementEntryState {

 VitalsMeasurementType get type; String get inputText; String get secondaryInputText; Set<String> get writePermissions; bool get canWrite; bool get isCheckingPermission; bool get isSavingEntry; String? get editRecordId; DateTime? get editTime; bool get saveCompleted; VitalsMeasurementEntryError? get entryError; ScreenError? get writeError;
/// Create a copy of VitalsMeasurementEntryState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$VitalsMeasurementEntryStateCopyWith<VitalsMeasurementEntryState> get copyWith => _$VitalsMeasurementEntryStateCopyWithImpl<VitalsMeasurementEntryState>(this as VitalsMeasurementEntryState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is VitalsMeasurementEntryState&&(identical(other.type, type) || other.type == type)&&(identical(other.inputText, inputText) || other.inputText == inputText)&&(identical(other.secondaryInputText, secondaryInputText) || other.secondaryInputText == secondaryInputText)&&const DeepCollectionEquality().equals(other.writePermissions, writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.isSavingEntry, isSavingEntry) || other.isSavingEntry == isSavingEntry)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.editTime, editTime) || other.editTime == editTime)&&(identical(other.saveCompleted, saveCompleted) || other.saveCompleted == saveCompleted)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.writeError, writeError) || other.writeError == writeError));
}


@override
int get hashCode => Object.hash(runtimeType,type,inputText,secondaryInputText,const DeepCollectionEquality().hash(writePermissions),canWrite,isCheckingPermission,isSavingEntry,editRecordId,editTime,saveCompleted,entryError,writeError);

@override
String toString() {
  return 'VitalsMeasurementEntryState(type: $type, inputText: $inputText, secondaryInputText: $secondaryInputText, writePermissions: $writePermissions, canWrite: $canWrite, isCheckingPermission: $isCheckingPermission, isSavingEntry: $isSavingEntry, editRecordId: $editRecordId, editTime: $editTime, saveCompleted: $saveCompleted, entryError: $entryError, writeError: $writeError)';
}


}

/// @nodoc
abstract mixin class $VitalsMeasurementEntryStateCopyWith<$Res>  {
  factory $VitalsMeasurementEntryStateCopyWith(VitalsMeasurementEntryState value, $Res Function(VitalsMeasurementEntryState) _then) = _$VitalsMeasurementEntryStateCopyWithImpl;
@useResult
$Res call({
 VitalsMeasurementType type, String inputText, String secondaryInputText, Set<String> writePermissions, bool canWrite, bool isCheckingPermission, bool isSavingEntry, String? editRecordId, DateTime? editTime, bool saveCompleted, VitalsMeasurementEntryError? entryError, ScreenError? writeError
});




}
/// @nodoc
class _$VitalsMeasurementEntryStateCopyWithImpl<$Res>
    implements $VitalsMeasurementEntryStateCopyWith<$Res> {
  _$VitalsMeasurementEntryStateCopyWithImpl(this._self, this._then);

  final VitalsMeasurementEntryState _self;
  final $Res Function(VitalsMeasurementEntryState) _then;

/// Create a copy of VitalsMeasurementEntryState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? type = null,Object? inputText = null,Object? secondaryInputText = null,Object? writePermissions = null,Object? canWrite = null,Object? isCheckingPermission = null,Object? isSavingEntry = null,Object? editRecordId = freezed,Object? editTime = freezed,Object? saveCompleted = null,Object? entryError = freezed,Object? writeError = freezed,}) {
  return _then(_self.copyWith(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as VitalsMeasurementType,inputText: null == inputText ? _self.inputText : inputText // ignore: cast_nullable_to_non_nullable
as String,secondaryInputText: null == secondaryInputText ? _self.secondaryInputText : secondaryInputText // ignore: cast_nullable_to_non_nullable
as String,writePermissions: null == writePermissions ? _self.writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,isSavingEntry: null == isSavingEntry ? _self.isSavingEntry : isSavingEntry // ignore: cast_nullable_to_non_nullable
as bool,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,editTime: freezed == editTime ? _self.editTime : editTime // ignore: cast_nullable_to_non_nullable
as DateTime?,saveCompleted: null == saveCompleted ? _self.saveCompleted : saveCompleted // ignore: cast_nullable_to_non_nullable
as bool,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as VitalsMeasurementEntryError?,writeError: freezed == writeError ? _self.writeError : writeError // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}

}


/// Adds pattern-matching-related methods to [VitalsMeasurementEntryState].
extension VitalsMeasurementEntryStatePatterns on VitalsMeasurementEntryState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _VitalsMeasurementEntryState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _VitalsMeasurementEntryState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _VitalsMeasurementEntryState value)  $default,){
final _that = this;
switch (_that) {
case _VitalsMeasurementEntryState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _VitalsMeasurementEntryState value)?  $default,){
final _that = this;
switch (_that) {
case _VitalsMeasurementEntryState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( VitalsMeasurementType type,  String inputText,  String secondaryInputText,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  bool isSavingEntry,  String? editRecordId,  DateTime? editTime,  bool saveCompleted,  VitalsMeasurementEntryError? entryError,  ScreenError? writeError)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _VitalsMeasurementEntryState() when $default != null:
return $default(_that.type,_that.inputText,_that.secondaryInputText,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.isSavingEntry,_that.editRecordId,_that.editTime,_that.saveCompleted,_that.entryError,_that.writeError);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( VitalsMeasurementType type,  String inputText,  String secondaryInputText,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  bool isSavingEntry,  String? editRecordId,  DateTime? editTime,  bool saveCompleted,  VitalsMeasurementEntryError? entryError,  ScreenError? writeError)  $default,) {final _that = this;
switch (_that) {
case _VitalsMeasurementEntryState():
return $default(_that.type,_that.inputText,_that.secondaryInputText,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.isSavingEntry,_that.editRecordId,_that.editTime,_that.saveCompleted,_that.entryError,_that.writeError);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( VitalsMeasurementType type,  String inputText,  String secondaryInputText,  Set<String> writePermissions,  bool canWrite,  bool isCheckingPermission,  bool isSavingEntry,  String? editRecordId,  DateTime? editTime,  bool saveCompleted,  VitalsMeasurementEntryError? entryError,  ScreenError? writeError)?  $default,) {final _that = this;
switch (_that) {
case _VitalsMeasurementEntryState() when $default != null:
return $default(_that.type,_that.inputText,_that.secondaryInputText,_that.writePermissions,_that.canWrite,_that.isCheckingPermission,_that.isSavingEntry,_that.editRecordId,_that.editTime,_that.saveCompleted,_that.entryError,_that.writeError);case _:
  return null;

}
}

}

/// @nodoc


class _VitalsMeasurementEntryState extends VitalsMeasurementEntryState {
  const _VitalsMeasurementEntryState({required this.type, this.inputText = '', this.secondaryInputText = '', final  Set<String> writePermissions = const <String>{}, this.canWrite = false, this.isCheckingPermission = true, this.isSavingEntry = false, this.editRecordId, this.editTime, this.saveCompleted = false, this.entryError, this.writeError}): _writePermissions = writePermissions,super._();
  

@override final  VitalsMeasurementType type;
@override@JsonKey() final  String inputText;
@override@JsonKey() final  String secondaryInputText;
 final  Set<String> _writePermissions;
@override@JsonKey() Set<String> get writePermissions {
  if (_writePermissions is EqualUnmodifiableSetView) return _writePermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_writePermissions);
}

@override@JsonKey() final  bool canWrite;
@override@JsonKey() final  bool isCheckingPermission;
@override@JsonKey() final  bool isSavingEntry;
@override final  String? editRecordId;
@override final  DateTime? editTime;
@override@JsonKey() final  bool saveCompleted;
@override final  VitalsMeasurementEntryError? entryError;
@override final  ScreenError? writeError;

/// Create a copy of VitalsMeasurementEntryState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$VitalsMeasurementEntryStateCopyWith<_VitalsMeasurementEntryState> get copyWith => __$VitalsMeasurementEntryStateCopyWithImpl<_VitalsMeasurementEntryState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _VitalsMeasurementEntryState&&(identical(other.type, type) || other.type == type)&&(identical(other.inputText, inputText) || other.inputText == inputText)&&(identical(other.secondaryInputText, secondaryInputText) || other.secondaryInputText == secondaryInputText)&&const DeepCollectionEquality().equals(other._writePermissions, _writePermissions)&&(identical(other.canWrite, canWrite) || other.canWrite == canWrite)&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&(identical(other.isSavingEntry, isSavingEntry) || other.isSavingEntry == isSavingEntry)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.editTime, editTime) || other.editTime == editTime)&&(identical(other.saveCompleted, saveCompleted) || other.saveCompleted == saveCompleted)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.writeError, writeError) || other.writeError == writeError));
}


@override
int get hashCode => Object.hash(runtimeType,type,inputText,secondaryInputText,const DeepCollectionEquality().hash(_writePermissions),canWrite,isCheckingPermission,isSavingEntry,editRecordId,editTime,saveCompleted,entryError,writeError);

@override
String toString() {
  return 'VitalsMeasurementEntryState(type: $type, inputText: $inputText, secondaryInputText: $secondaryInputText, writePermissions: $writePermissions, canWrite: $canWrite, isCheckingPermission: $isCheckingPermission, isSavingEntry: $isSavingEntry, editRecordId: $editRecordId, editTime: $editTime, saveCompleted: $saveCompleted, entryError: $entryError, writeError: $writeError)';
}


}

/// @nodoc
abstract mixin class _$VitalsMeasurementEntryStateCopyWith<$Res> implements $VitalsMeasurementEntryStateCopyWith<$Res> {
  factory _$VitalsMeasurementEntryStateCopyWith(_VitalsMeasurementEntryState value, $Res Function(_VitalsMeasurementEntryState) _then) = __$VitalsMeasurementEntryStateCopyWithImpl;
@override @useResult
$Res call({
 VitalsMeasurementType type, String inputText, String secondaryInputText, Set<String> writePermissions, bool canWrite, bool isCheckingPermission, bool isSavingEntry, String? editRecordId, DateTime? editTime, bool saveCompleted, VitalsMeasurementEntryError? entryError, ScreenError? writeError
});




}
/// @nodoc
class __$VitalsMeasurementEntryStateCopyWithImpl<$Res>
    implements _$VitalsMeasurementEntryStateCopyWith<$Res> {
  __$VitalsMeasurementEntryStateCopyWithImpl(this._self, this._then);

  final _VitalsMeasurementEntryState _self;
  final $Res Function(_VitalsMeasurementEntryState) _then;

/// Create a copy of VitalsMeasurementEntryState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? type = null,Object? inputText = null,Object? secondaryInputText = null,Object? writePermissions = null,Object? canWrite = null,Object? isCheckingPermission = null,Object? isSavingEntry = null,Object? editRecordId = freezed,Object? editTime = freezed,Object? saveCompleted = null,Object? entryError = freezed,Object? writeError = freezed,}) {
  return _then(_VitalsMeasurementEntryState(
type: null == type ? _self.type : type // ignore: cast_nullable_to_non_nullable
as VitalsMeasurementType,inputText: null == inputText ? _self.inputText : inputText // ignore: cast_nullable_to_non_nullable
as String,secondaryInputText: null == secondaryInputText ? _self.secondaryInputText : secondaryInputText // ignore: cast_nullable_to_non_nullable
as String,writePermissions: null == writePermissions ? _self._writePermissions : writePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWrite: null == canWrite ? _self.canWrite : canWrite // ignore: cast_nullable_to_non_nullable
as bool,isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,isSavingEntry: null == isSavingEntry ? _self.isSavingEntry : isSavingEntry // ignore: cast_nullable_to_non_nullable
as bool,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,editTime: freezed == editTime ? _self.editTime : editTime // ignore: cast_nullable_to_non_nullable
as DateTime?,saveCompleted: null == saveCompleted ? _self.saveCompleted : saveCompleted // ignore: cast_nullable_to_non_nullable
as bool,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as VitalsMeasurementEntryError?,writeError: freezed == writeError ? _self.writeError : writeError // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}


}

// dart format on
