// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'hydration_entry_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HydrationContainerOption {

 String get id; double get volumeMilliliters;
/// Create a copy of HydrationContainerOption
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationContainerOptionCopyWith<HydrationContainerOption> get copyWith => _$HydrationContainerOptionCopyWithImpl<HydrationContainerOption>(this as HydrationContainerOption, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationContainerOption&&(identical(other.id, id) || other.id == id)&&(identical(other.volumeMilliliters, volumeMilliliters) || other.volumeMilliliters == volumeMilliliters));
}


@override
int get hashCode => Object.hash(runtimeType,id,volumeMilliliters);

@override
String toString() {
  return 'HydrationContainerOption(id: $id, volumeMilliliters: $volumeMilliliters)';
}


}

/// @nodoc
abstract mixin class $HydrationContainerOptionCopyWith<$Res>  {
  factory $HydrationContainerOptionCopyWith(HydrationContainerOption value, $Res Function(HydrationContainerOption) _then) = _$HydrationContainerOptionCopyWithImpl;
@useResult
$Res call({
 String id, double volumeMilliliters
});




}
/// @nodoc
class _$HydrationContainerOptionCopyWithImpl<$Res>
    implements $HydrationContainerOptionCopyWith<$Res> {
  _$HydrationContainerOptionCopyWithImpl(this._self, this._then);

  final HydrationContainerOption _self;
  final $Res Function(HydrationContainerOption) _then;

/// Create a copy of HydrationContainerOption
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? volumeMilliliters = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,volumeMilliliters: null == volumeMilliliters ? _self.volumeMilliliters : volumeMilliliters // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [HydrationContainerOption].
extension HydrationContainerOptionPatterns on HydrationContainerOption {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationContainerOption value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationContainerOption() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationContainerOption value)  $default,){
final _that = this;
switch (_that) {
case _HydrationContainerOption():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationContainerOption value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationContainerOption() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  double volumeMilliliters)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationContainerOption() when $default != null:
return $default(_that.id,_that.volumeMilliliters);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  double volumeMilliliters)  $default,) {final _that = this;
switch (_that) {
case _HydrationContainerOption():
return $default(_that.id,_that.volumeMilliliters);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  double volumeMilliliters)?  $default,) {final _that = this;
switch (_that) {
case _HydrationContainerOption() when $default != null:
return $default(_that.id,_that.volumeMilliliters);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationContainerOption extends HydrationContainerOption {
  const _HydrationContainerOption({required this.id, required this.volumeMilliliters}): super._();
  

@override final  String id;
@override final  double volumeMilliliters;

/// Create a copy of HydrationContainerOption
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationContainerOptionCopyWith<_HydrationContainerOption> get copyWith => __$HydrationContainerOptionCopyWithImpl<_HydrationContainerOption>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationContainerOption&&(identical(other.id, id) || other.id == id)&&(identical(other.volumeMilliliters, volumeMilliliters) || other.volumeMilliliters == volumeMilliliters));
}


@override
int get hashCode => Object.hash(runtimeType,id,volumeMilliliters);

@override
String toString() {
  return 'HydrationContainerOption(id: $id, volumeMilliliters: $volumeMilliliters)';
}


}

/// @nodoc
abstract mixin class _$HydrationContainerOptionCopyWith<$Res> implements $HydrationContainerOptionCopyWith<$Res> {
  factory _$HydrationContainerOptionCopyWith(_HydrationContainerOption value, $Res Function(_HydrationContainerOption) _then) = __$HydrationContainerOptionCopyWithImpl;
@override @useResult
$Res call({
 String id, double volumeMilliliters
});




}
/// @nodoc
class __$HydrationContainerOptionCopyWithImpl<$Res>
    implements _$HydrationContainerOptionCopyWith<$Res> {
  __$HydrationContainerOptionCopyWithImpl(this._self, this._then);

  final _HydrationContainerOption _self;
  final $Res Function(_HydrationContainerOption) _then;

/// Create a copy of HydrationContainerOption
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? volumeMilliliters = null,}) {
  return _then(_HydrationContainerOption(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,volumeMilliliters: null == volumeMilliliters ? _self.volumeMilliliters : volumeMilliliters // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$HydrationEntryState {

 bool get isCheckingPermission; Set<String> get hydrationWritePermissions; Set<String> get nutritionWritePermissions; bool get canWriteHydration; bool get canWriteNutrition; double get todayHydrationLiters; double get dailyGoalLiters; bool get isSavingEntry; List<HydrationContainerOption> get containerOptions; HydrationContainerOption get selectedContainer; double? get lastCustomAmountMilliliters; List<CustomHydrationDrink> get customDrinkOptions;/// The most-logged saved drinks, derived from Health Connect entries.
 List<CustomHydrationDrink> get frequentDrinkOptions; String? get editRecordId; DateTime? get editTime; bool get saveCompleted; HydrationEntryNotice? get entryNotice; HydrationEntryError? get entryError; ScreenError? get writeError;
/// Create a copy of HydrationEntryState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationEntryStateCopyWith<HydrationEntryState> get copyWith => _$HydrationEntryStateCopyWithImpl<HydrationEntryState>(this as HydrationEntryState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationEntryState&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&const DeepCollectionEquality().equals(other.hydrationWritePermissions, hydrationWritePermissions)&&const DeepCollectionEquality().equals(other.nutritionWritePermissions, nutritionWritePermissions)&&(identical(other.canWriteHydration, canWriteHydration) || other.canWriteHydration == canWriteHydration)&&(identical(other.canWriteNutrition, canWriteNutrition) || other.canWriteNutrition == canWriteNutrition)&&(identical(other.todayHydrationLiters, todayHydrationLiters) || other.todayHydrationLiters == todayHydrationLiters)&&(identical(other.dailyGoalLiters, dailyGoalLiters) || other.dailyGoalLiters == dailyGoalLiters)&&(identical(other.isSavingEntry, isSavingEntry) || other.isSavingEntry == isSavingEntry)&&const DeepCollectionEquality().equals(other.containerOptions, containerOptions)&&(identical(other.selectedContainer, selectedContainer) || other.selectedContainer == selectedContainer)&&(identical(other.lastCustomAmountMilliliters, lastCustomAmountMilliliters) || other.lastCustomAmountMilliliters == lastCustomAmountMilliliters)&&const DeepCollectionEquality().equals(other.customDrinkOptions, customDrinkOptions)&&const DeepCollectionEquality().equals(other.frequentDrinkOptions, frequentDrinkOptions)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.editTime, editTime) || other.editTime == editTime)&&(identical(other.saveCompleted, saveCompleted) || other.saveCompleted == saveCompleted)&&(identical(other.entryNotice, entryNotice) || other.entryNotice == entryNotice)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.writeError, writeError) || other.writeError == writeError));
}


@override
int get hashCode => Object.hashAll([runtimeType,isCheckingPermission,const DeepCollectionEquality().hash(hydrationWritePermissions),const DeepCollectionEquality().hash(nutritionWritePermissions),canWriteHydration,canWriteNutrition,todayHydrationLiters,dailyGoalLiters,isSavingEntry,const DeepCollectionEquality().hash(containerOptions),selectedContainer,lastCustomAmountMilliliters,const DeepCollectionEquality().hash(customDrinkOptions),const DeepCollectionEquality().hash(frequentDrinkOptions),editRecordId,editTime,saveCompleted,entryNotice,entryError,writeError]);

@override
String toString() {
  return 'HydrationEntryState(isCheckingPermission: $isCheckingPermission, hydrationWritePermissions: $hydrationWritePermissions, nutritionWritePermissions: $nutritionWritePermissions, canWriteHydration: $canWriteHydration, canWriteNutrition: $canWriteNutrition, todayHydrationLiters: $todayHydrationLiters, dailyGoalLiters: $dailyGoalLiters, isSavingEntry: $isSavingEntry, containerOptions: $containerOptions, selectedContainer: $selectedContainer, lastCustomAmountMilliliters: $lastCustomAmountMilliliters, customDrinkOptions: $customDrinkOptions, frequentDrinkOptions: $frequentDrinkOptions, editRecordId: $editRecordId, editTime: $editTime, saveCompleted: $saveCompleted, entryNotice: $entryNotice, entryError: $entryError, writeError: $writeError)';
}


}

/// @nodoc
abstract mixin class $HydrationEntryStateCopyWith<$Res>  {
  factory $HydrationEntryStateCopyWith(HydrationEntryState value, $Res Function(HydrationEntryState) _then) = _$HydrationEntryStateCopyWithImpl;
@useResult
$Res call({
 bool isCheckingPermission, Set<String> hydrationWritePermissions, Set<String> nutritionWritePermissions, bool canWriteHydration, bool canWriteNutrition, double todayHydrationLiters, double dailyGoalLiters, bool isSavingEntry, List<HydrationContainerOption> containerOptions, HydrationContainerOption selectedContainer, double? lastCustomAmountMilliliters, List<CustomHydrationDrink> customDrinkOptions, List<CustomHydrationDrink> frequentDrinkOptions, String? editRecordId, DateTime? editTime, bool saveCompleted, HydrationEntryNotice? entryNotice, HydrationEntryError? entryError, ScreenError? writeError
});


$HydrationContainerOptionCopyWith<$Res> get selectedContainer;

}
/// @nodoc
class _$HydrationEntryStateCopyWithImpl<$Res>
    implements $HydrationEntryStateCopyWith<$Res> {
  _$HydrationEntryStateCopyWithImpl(this._self, this._then);

  final HydrationEntryState _self;
  final $Res Function(HydrationEntryState) _then;

/// Create a copy of HydrationEntryState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? isCheckingPermission = null,Object? hydrationWritePermissions = null,Object? nutritionWritePermissions = null,Object? canWriteHydration = null,Object? canWriteNutrition = null,Object? todayHydrationLiters = null,Object? dailyGoalLiters = null,Object? isSavingEntry = null,Object? containerOptions = null,Object? selectedContainer = null,Object? lastCustomAmountMilliliters = freezed,Object? customDrinkOptions = null,Object? frequentDrinkOptions = null,Object? editRecordId = freezed,Object? editTime = freezed,Object? saveCompleted = null,Object? entryNotice = freezed,Object? entryError = freezed,Object? writeError = freezed,}) {
  return _then(_self.copyWith(
isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,hydrationWritePermissions: null == hydrationWritePermissions ? _self.hydrationWritePermissions : hydrationWritePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,nutritionWritePermissions: null == nutritionWritePermissions ? _self.nutritionWritePermissions : nutritionWritePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWriteHydration: null == canWriteHydration ? _self.canWriteHydration : canWriteHydration // ignore: cast_nullable_to_non_nullable
as bool,canWriteNutrition: null == canWriteNutrition ? _self.canWriteNutrition : canWriteNutrition // ignore: cast_nullable_to_non_nullable
as bool,todayHydrationLiters: null == todayHydrationLiters ? _self.todayHydrationLiters : todayHydrationLiters // ignore: cast_nullable_to_non_nullable
as double,dailyGoalLiters: null == dailyGoalLiters ? _self.dailyGoalLiters : dailyGoalLiters // ignore: cast_nullable_to_non_nullable
as double,isSavingEntry: null == isSavingEntry ? _self.isSavingEntry : isSavingEntry // ignore: cast_nullable_to_non_nullable
as bool,containerOptions: null == containerOptions ? _self.containerOptions : containerOptions // ignore: cast_nullable_to_non_nullable
as List<HydrationContainerOption>,selectedContainer: null == selectedContainer ? _self.selectedContainer : selectedContainer // ignore: cast_nullable_to_non_nullable
as HydrationContainerOption,lastCustomAmountMilliliters: freezed == lastCustomAmountMilliliters ? _self.lastCustomAmountMilliliters : lastCustomAmountMilliliters // ignore: cast_nullable_to_non_nullable
as double?,customDrinkOptions: null == customDrinkOptions ? _self.customDrinkOptions : customDrinkOptions // ignore: cast_nullable_to_non_nullable
as List<CustomHydrationDrink>,frequentDrinkOptions: null == frequentDrinkOptions ? _self.frequentDrinkOptions : frequentDrinkOptions // ignore: cast_nullable_to_non_nullable
as List<CustomHydrationDrink>,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,editTime: freezed == editTime ? _self.editTime : editTime // ignore: cast_nullable_to_non_nullable
as DateTime?,saveCompleted: null == saveCompleted ? _self.saveCompleted : saveCompleted // ignore: cast_nullable_to_non_nullable
as bool,entryNotice: freezed == entryNotice ? _self.entryNotice : entryNotice // ignore: cast_nullable_to_non_nullable
as HydrationEntryNotice?,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as HydrationEntryError?,writeError: freezed == writeError ? _self.writeError : writeError // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}
/// Create a copy of HydrationEntryState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HydrationContainerOptionCopyWith<$Res> get selectedContainer {
  
  return $HydrationContainerOptionCopyWith<$Res>(_self.selectedContainer, (value) {
    return _then(_self.copyWith(selectedContainer: value));
  });
}
}


/// Adds pattern-matching-related methods to [HydrationEntryState].
extension HydrationEntryStatePatterns on HydrationEntryState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationEntryState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationEntryState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationEntryState value)  $default,){
final _that = this;
switch (_that) {
case _HydrationEntryState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationEntryState value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationEntryState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool isCheckingPermission,  Set<String> hydrationWritePermissions,  Set<String> nutritionWritePermissions,  bool canWriteHydration,  bool canWriteNutrition,  double todayHydrationLiters,  double dailyGoalLiters,  bool isSavingEntry,  List<HydrationContainerOption> containerOptions,  HydrationContainerOption selectedContainer,  double? lastCustomAmountMilliliters,  List<CustomHydrationDrink> customDrinkOptions,  List<CustomHydrationDrink> frequentDrinkOptions,  String? editRecordId,  DateTime? editTime,  bool saveCompleted,  HydrationEntryNotice? entryNotice,  HydrationEntryError? entryError,  ScreenError? writeError)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationEntryState() when $default != null:
return $default(_that.isCheckingPermission,_that.hydrationWritePermissions,_that.nutritionWritePermissions,_that.canWriteHydration,_that.canWriteNutrition,_that.todayHydrationLiters,_that.dailyGoalLiters,_that.isSavingEntry,_that.containerOptions,_that.selectedContainer,_that.lastCustomAmountMilliliters,_that.customDrinkOptions,_that.frequentDrinkOptions,_that.editRecordId,_that.editTime,_that.saveCompleted,_that.entryNotice,_that.entryError,_that.writeError);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool isCheckingPermission,  Set<String> hydrationWritePermissions,  Set<String> nutritionWritePermissions,  bool canWriteHydration,  bool canWriteNutrition,  double todayHydrationLiters,  double dailyGoalLiters,  bool isSavingEntry,  List<HydrationContainerOption> containerOptions,  HydrationContainerOption selectedContainer,  double? lastCustomAmountMilliliters,  List<CustomHydrationDrink> customDrinkOptions,  List<CustomHydrationDrink> frequentDrinkOptions,  String? editRecordId,  DateTime? editTime,  bool saveCompleted,  HydrationEntryNotice? entryNotice,  HydrationEntryError? entryError,  ScreenError? writeError)  $default,) {final _that = this;
switch (_that) {
case _HydrationEntryState():
return $default(_that.isCheckingPermission,_that.hydrationWritePermissions,_that.nutritionWritePermissions,_that.canWriteHydration,_that.canWriteNutrition,_that.todayHydrationLiters,_that.dailyGoalLiters,_that.isSavingEntry,_that.containerOptions,_that.selectedContainer,_that.lastCustomAmountMilliliters,_that.customDrinkOptions,_that.frequentDrinkOptions,_that.editRecordId,_that.editTime,_that.saveCompleted,_that.entryNotice,_that.entryError,_that.writeError);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool isCheckingPermission,  Set<String> hydrationWritePermissions,  Set<String> nutritionWritePermissions,  bool canWriteHydration,  bool canWriteNutrition,  double todayHydrationLiters,  double dailyGoalLiters,  bool isSavingEntry,  List<HydrationContainerOption> containerOptions,  HydrationContainerOption selectedContainer,  double? lastCustomAmountMilliliters,  List<CustomHydrationDrink> customDrinkOptions,  List<CustomHydrationDrink> frequentDrinkOptions,  String? editRecordId,  DateTime? editTime,  bool saveCompleted,  HydrationEntryNotice? entryNotice,  HydrationEntryError? entryError,  ScreenError? writeError)?  $default,) {final _that = this;
switch (_that) {
case _HydrationEntryState() when $default != null:
return $default(_that.isCheckingPermission,_that.hydrationWritePermissions,_that.nutritionWritePermissions,_that.canWriteHydration,_that.canWriteNutrition,_that.todayHydrationLiters,_that.dailyGoalLiters,_that.isSavingEntry,_that.containerOptions,_that.selectedContainer,_that.lastCustomAmountMilliliters,_that.customDrinkOptions,_that.frequentDrinkOptions,_that.editRecordId,_that.editTime,_that.saveCompleted,_that.entryNotice,_that.entryError,_that.writeError);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationEntryState extends HydrationEntryState {
  const _HydrationEntryState({this.isCheckingPermission = true, final  Set<String> hydrationWritePermissions = const <String>{}, final  Set<String> nutritionWritePermissions = const <String>{}, this.canWriteHydration = false, this.canWriteNutrition = false, this.todayHydrationLiters = 0.0, this.dailyGoalLiters = 2.0, this.isSavingEntry = false, final  List<HydrationContainerOption> containerOptions = kDefaultHydrationContainers, required this.selectedContainer, this.lastCustomAmountMilliliters, final  List<CustomHydrationDrink> customDrinkOptions = const <CustomHydrationDrink>[], final  List<CustomHydrationDrink> frequentDrinkOptions = const <CustomHydrationDrink>[], this.editRecordId, this.editTime, this.saveCompleted = false, this.entryNotice, this.entryError, this.writeError}): _hydrationWritePermissions = hydrationWritePermissions,_nutritionWritePermissions = nutritionWritePermissions,_containerOptions = containerOptions,_customDrinkOptions = customDrinkOptions,_frequentDrinkOptions = frequentDrinkOptions,super._();
  

@override@JsonKey() final  bool isCheckingPermission;
 final  Set<String> _hydrationWritePermissions;
@override@JsonKey() Set<String> get hydrationWritePermissions {
  if (_hydrationWritePermissions is EqualUnmodifiableSetView) return _hydrationWritePermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_hydrationWritePermissions);
}

 final  Set<String> _nutritionWritePermissions;
@override@JsonKey() Set<String> get nutritionWritePermissions {
  if (_nutritionWritePermissions is EqualUnmodifiableSetView) return _nutritionWritePermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_nutritionWritePermissions);
}

@override@JsonKey() final  bool canWriteHydration;
@override@JsonKey() final  bool canWriteNutrition;
@override@JsonKey() final  double todayHydrationLiters;
@override@JsonKey() final  double dailyGoalLiters;
@override@JsonKey() final  bool isSavingEntry;
 final  List<HydrationContainerOption> _containerOptions;
@override@JsonKey() List<HydrationContainerOption> get containerOptions {
  if (_containerOptions is EqualUnmodifiableListView) return _containerOptions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_containerOptions);
}

@override final  HydrationContainerOption selectedContainer;
@override final  double? lastCustomAmountMilliliters;
 final  List<CustomHydrationDrink> _customDrinkOptions;
@override@JsonKey() List<CustomHydrationDrink> get customDrinkOptions {
  if (_customDrinkOptions is EqualUnmodifiableListView) return _customDrinkOptions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_customDrinkOptions);
}

/// The most-logged saved drinks, derived from Health Connect entries.
 final  List<CustomHydrationDrink> _frequentDrinkOptions;
/// The most-logged saved drinks, derived from Health Connect entries.
@override@JsonKey() List<CustomHydrationDrink> get frequentDrinkOptions {
  if (_frequentDrinkOptions is EqualUnmodifiableListView) return _frequentDrinkOptions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_frequentDrinkOptions);
}

@override final  String? editRecordId;
@override final  DateTime? editTime;
@override@JsonKey() final  bool saveCompleted;
@override final  HydrationEntryNotice? entryNotice;
@override final  HydrationEntryError? entryError;
@override final  ScreenError? writeError;

/// Create a copy of HydrationEntryState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationEntryStateCopyWith<_HydrationEntryState> get copyWith => __$HydrationEntryStateCopyWithImpl<_HydrationEntryState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationEntryState&&(identical(other.isCheckingPermission, isCheckingPermission) || other.isCheckingPermission == isCheckingPermission)&&const DeepCollectionEquality().equals(other._hydrationWritePermissions, _hydrationWritePermissions)&&const DeepCollectionEquality().equals(other._nutritionWritePermissions, _nutritionWritePermissions)&&(identical(other.canWriteHydration, canWriteHydration) || other.canWriteHydration == canWriteHydration)&&(identical(other.canWriteNutrition, canWriteNutrition) || other.canWriteNutrition == canWriteNutrition)&&(identical(other.todayHydrationLiters, todayHydrationLiters) || other.todayHydrationLiters == todayHydrationLiters)&&(identical(other.dailyGoalLiters, dailyGoalLiters) || other.dailyGoalLiters == dailyGoalLiters)&&(identical(other.isSavingEntry, isSavingEntry) || other.isSavingEntry == isSavingEntry)&&const DeepCollectionEquality().equals(other._containerOptions, _containerOptions)&&(identical(other.selectedContainer, selectedContainer) || other.selectedContainer == selectedContainer)&&(identical(other.lastCustomAmountMilliliters, lastCustomAmountMilliliters) || other.lastCustomAmountMilliliters == lastCustomAmountMilliliters)&&const DeepCollectionEquality().equals(other._customDrinkOptions, _customDrinkOptions)&&const DeepCollectionEquality().equals(other._frequentDrinkOptions, _frequentDrinkOptions)&&(identical(other.editRecordId, editRecordId) || other.editRecordId == editRecordId)&&(identical(other.editTime, editTime) || other.editTime == editTime)&&(identical(other.saveCompleted, saveCompleted) || other.saveCompleted == saveCompleted)&&(identical(other.entryNotice, entryNotice) || other.entryNotice == entryNotice)&&(identical(other.entryError, entryError) || other.entryError == entryError)&&(identical(other.writeError, writeError) || other.writeError == writeError));
}


@override
int get hashCode => Object.hashAll([runtimeType,isCheckingPermission,const DeepCollectionEquality().hash(_hydrationWritePermissions),const DeepCollectionEquality().hash(_nutritionWritePermissions),canWriteHydration,canWriteNutrition,todayHydrationLiters,dailyGoalLiters,isSavingEntry,const DeepCollectionEquality().hash(_containerOptions),selectedContainer,lastCustomAmountMilliliters,const DeepCollectionEquality().hash(_customDrinkOptions),const DeepCollectionEquality().hash(_frequentDrinkOptions),editRecordId,editTime,saveCompleted,entryNotice,entryError,writeError]);

@override
String toString() {
  return 'HydrationEntryState(isCheckingPermission: $isCheckingPermission, hydrationWritePermissions: $hydrationWritePermissions, nutritionWritePermissions: $nutritionWritePermissions, canWriteHydration: $canWriteHydration, canWriteNutrition: $canWriteNutrition, todayHydrationLiters: $todayHydrationLiters, dailyGoalLiters: $dailyGoalLiters, isSavingEntry: $isSavingEntry, containerOptions: $containerOptions, selectedContainer: $selectedContainer, lastCustomAmountMilliliters: $lastCustomAmountMilliliters, customDrinkOptions: $customDrinkOptions, frequentDrinkOptions: $frequentDrinkOptions, editRecordId: $editRecordId, editTime: $editTime, saveCompleted: $saveCompleted, entryNotice: $entryNotice, entryError: $entryError, writeError: $writeError)';
}


}

/// @nodoc
abstract mixin class _$HydrationEntryStateCopyWith<$Res> implements $HydrationEntryStateCopyWith<$Res> {
  factory _$HydrationEntryStateCopyWith(_HydrationEntryState value, $Res Function(_HydrationEntryState) _then) = __$HydrationEntryStateCopyWithImpl;
@override @useResult
$Res call({
 bool isCheckingPermission, Set<String> hydrationWritePermissions, Set<String> nutritionWritePermissions, bool canWriteHydration, bool canWriteNutrition, double todayHydrationLiters, double dailyGoalLiters, bool isSavingEntry, List<HydrationContainerOption> containerOptions, HydrationContainerOption selectedContainer, double? lastCustomAmountMilliliters, List<CustomHydrationDrink> customDrinkOptions, List<CustomHydrationDrink> frequentDrinkOptions, String? editRecordId, DateTime? editTime, bool saveCompleted, HydrationEntryNotice? entryNotice, HydrationEntryError? entryError, ScreenError? writeError
});


@override $HydrationContainerOptionCopyWith<$Res> get selectedContainer;

}
/// @nodoc
class __$HydrationEntryStateCopyWithImpl<$Res>
    implements _$HydrationEntryStateCopyWith<$Res> {
  __$HydrationEntryStateCopyWithImpl(this._self, this._then);

  final _HydrationEntryState _self;
  final $Res Function(_HydrationEntryState) _then;

/// Create a copy of HydrationEntryState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? isCheckingPermission = null,Object? hydrationWritePermissions = null,Object? nutritionWritePermissions = null,Object? canWriteHydration = null,Object? canWriteNutrition = null,Object? todayHydrationLiters = null,Object? dailyGoalLiters = null,Object? isSavingEntry = null,Object? containerOptions = null,Object? selectedContainer = null,Object? lastCustomAmountMilliliters = freezed,Object? customDrinkOptions = null,Object? frequentDrinkOptions = null,Object? editRecordId = freezed,Object? editTime = freezed,Object? saveCompleted = null,Object? entryNotice = freezed,Object? entryError = freezed,Object? writeError = freezed,}) {
  return _then(_HydrationEntryState(
isCheckingPermission: null == isCheckingPermission ? _self.isCheckingPermission : isCheckingPermission // ignore: cast_nullable_to_non_nullable
as bool,hydrationWritePermissions: null == hydrationWritePermissions ? _self._hydrationWritePermissions : hydrationWritePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,nutritionWritePermissions: null == nutritionWritePermissions ? _self._nutritionWritePermissions : nutritionWritePermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,canWriteHydration: null == canWriteHydration ? _self.canWriteHydration : canWriteHydration // ignore: cast_nullable_to_non_nullable
as bool,canWriteNutrition: null == canWriteNutrition ? _self.canWriteNutrition : canWriteNutrition // ignore: cast_nullable_to_non_nullable
as bool,todayHydrationLiters: null == todayHydrationLiters ? _self.todayHydrationLiters : todayHydrationLiters // ignore: cast_nullable_to_non_nullable
as double,dailyGoalLiters: null == dailyGoalLiters ? _self.dailyGoalLiters : dailyGoalLiters // ignore: cast_nullable_to_non_nullable
as double,isSavingEntry: null == isSavingEntry ? _self.isSavingEntry : isSavingEntry // ignore: cast_nullable_to_non_nullable
as bool,containerOptions: null == containerOptions ? _self._containerOptions : containerOptions // ignore: cast_nullable_to_non_nullable
as List<HydrationContainerOption>,selectedContainer: null == selectedContainer ? _self.selectedContainer : selectedContainer // ignore: cast_nullable_to_non_nullable
as HydrationContainerOption,lastCustomAmountMilliliters: freezed == lastCustomAmountMilliliters ? _self.lastCustomAmountMilliliters : lastCustomAmountMilliliters // ignore: cast_nullable_to_non_nullable
as double?,customDrinkOptions: null == customDrinkOptions ? _self._customDrinkOptions : customDrinkOptions // ignore: cast_nullable_to_non_nullable
as List<CustomHydrationDrink>,frequentDrinkOptions: null == frequentDrinkOptions ? _self._frequentDrinkOptions : frequentDrinkOptions // ignore: cast_nullable_to_non_nullable
as List<CustomHydrationDrink>,editRecordId: freezed == editRecordId ? _self.editRecordId : editRecordId // ignore: cast_nullable_to_non_nullable
as String?,editTime: freezed == editTime ? _self.editTime : editTime // ignore: cast_nullable_to_non_nullable
as DateTime?,saveCompleted: null == saveCompleted ? _self.saveCompleted : saveCompleted // ignore: cast_nullable_to_non_nullable
as bool,entryNotice: freezed == entryNotice ? _self.entryNotice : entryNotice // ignore: cast_nullable_to_non_nullable
as HydrationEntryNotice?,entryError: freezed == entryError ? _self.entryError : entryError // ignore: cast_nullable_to_non_nullable
as HydrationEntryError?,writeError: freezed == writeError ? _self.writeError : writeError // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}

/// Create a copy of HydrationEntryState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HydrationContainerOptionCopyWith<$Res> get selectedContainer {
  
  return $HydrationContainerOptionCopyWith<$Res>(_self.selectedContainer, (value) {
    return _then(_self.copyWith(selectedContainer: value));
  });
}
}

// dart format on
