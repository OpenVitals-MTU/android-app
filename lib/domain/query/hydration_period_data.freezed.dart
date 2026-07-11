// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'hydration_period_data.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HydrationPeriodData {

 List<DailyHydration> get dailyHydration; List<DailyHydration> get previousDailyHydration; List<DailyHydration> get baselineDailyHydration; List<HydrationEntry> get hydrationEntries;
/// Create a copy of HydrationPeriodData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationPeriodDataCopyWith<HydrationPeriodData> get copyWith => _$HydrationPeriodDataCopyWithImpl<HydrationPeriodData>(this as HydrationPeriodData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationPeriodData&&const DeepCollectionEquality().equals(other.dailyHydration, dailyHydration)&&const DeepCollectionEquality().equals(other.previousDailyHydration, previousDailyHydration)&&const DeepCollectionEquality().equals(other.baselineDailyHydration, baselineDailyHydration)&&const DeepCollectionEquality().equals(other.hydrationEntries, hydrationEntries));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(dailyHydration),const DeepCollectionEquality().hash(previousDailyHydration),const DeepCollectionEquality().hash(baselineDailyHydration),const DeepCollectionEquality().hash(hydrationEntries));

@override
String toString() {
  return 'HydrationPeriodData(dailyHydration: $dailyHydration, previousDailyHydration: $previousDailyHydration, baselineDailyHydration: $baselineDailyHydration, hydrationEntries: $hydrationEntries)';
}


}

/// @nodoc
abstract mixin class $HydrationPeriodDataCopyWith<$Res>  {
  factory $HydrationPeriodDataCopyWith(HydrationPeriodData value, $Res Function(HydrationPeriodData) _then) = _$HydrationPeriodDataCopyWithImpl;
@useResult
$Res call({
 List<DailyHydration> dailyHydration, List<DailyHydration> previousDailyHydration, List<DailyHydration> baselineDailyHydration, List<HydrationEntry> hydrationEntries
});




}
/// @nodoc
class _$HydrationPeriodDataCopyWithImpl<$Res>
    implements $HydrationPeriodDataCopyWith<$Res> {
  _$HydrationPeriodDataCopyWithImpl(this._self, this._then);

  final HydrationPeriodData _self;
  final $Res Function(HydrationPeriodData) _then;

/// Create a copy of HydrationPeriodData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? dailyHydration = null,Object? previousDailyHydration = null,Object? baselineDailyHydration = null,Object? hydrationEntries = null,}) {
  return _then(_self.copyWith(
dailyHydration: null == dailyHydration ? _self.dailyHydration : dailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,previousDailyHydration: null == previousDailyHydration ? _self.previousDailyHydration : previousDailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,baselineDailyHydration: null == baselineDailyHydration ? _self.baselineDailyHydration : baselineDailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,hydrationEntries: null == hydrationEntries ? _self.hydrationEntries : hydrationEntries // ignore: cast_nullable_to_non_nullable
as List<HydrationEntry>,
  ));
}

}


/// Adds pattern-matching-related methods to [HydrationPeriodData].
extension HydrationPeriodDataPatterns on HydrationPeriodData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationPeriodData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationPeriodData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationPeriodData value)  $default,){
final _that = this;
switch (_that) {
case _HydrationPeriodData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationPeriodData value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationPeriodData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<DailyHydration> dailyHydration,  List<DailyHydration> previousDailyHydration,  List<DailyHydration> baselineDailyHydration,  List<HydrationEntry> hydrationEntries)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationPeriodData() when $default != null:
return $default(_that.dailyHydration,_that.previousDailyHydration,_that.baselineDailyHydration,_that.hydrationEntries);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<DailyHydration> dailyHydration,  List<DailyHydration> previousDailyHydration,  List<DailyHydration> baselineDailyHydration,  List<HydrationEntry> hydrationEntries)  $default,) {final _that = this;
switch (_that) {
case _HydrationPeriodData():
return $default(_that.dailyHydration,_that.previousDailyHydration,_that.baselineDailyHydration,_that.hydrationEntries);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<DailyHydration> dailyHydration,  List<DailyHydration> previousDailyHydration,  List<DailyHydration> baselineDailyHydration,  List<HydrationEntry> hydrationEntries)?  $default,) {final _that = this;
switch (_that) {
case _HydrationPeriodData() when $default != null:
return $default(_that.dailyHydration,_that.previousDailyHydration,_that.baselineDailyHydration,_that.hydrationEntries);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationPeriodData implements HydrationPeriodData {
  const _HydrationPeriodData({final  List<DailyHydration> dailyHydration = const <DailyHydration>[], final  List<DailyHydration> previousDailyHydration = const <DailyHydration>[], final  List<DailyHydration> baselineDailyHydration = const <DailyHydration>[], final  List<HydrationEntry> hydrationEntries = const <HydrationEntry>[]}): _dailyHydration = dailyHydration,_previousDailyHydration = previousDailyHydration,_baselineDailyHydration = baselineDailyHydration,_hydrationEntries = hydrationEntries;
  

 final  List<DailyHydration> _dailyHydration;
@override@JsonKey() List<DailyHydration> get dailyHydration {
  if (_dailyHydration is EqualUnmodifiableListView) return _dailyHydration;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyHydration);
}

 final  List<DailyHydration> _previousDailyHydration;
@override@JsonKey() List<DailyHydration> get previousDailyHydration {
  if (_previousDailyHydration is EqualUnmodifiableListView) return _previousDailyHydration;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDailyHydration);
}

 final  List<DailyHydration> _baselineDailyHydration;
@override@JsonKey() List<DailyHydration> get baselineDailyHydration {
  if (_baselineDailyHydration is EqualUnmodifiableListView) return _baselineDailyHydration;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineDailyHydration);
}

 final  List<HydrationEntry> _hydrationEntries;
@override@JsonKey() List<HydrationEntry> get hydrationEntries {
  if (_hydrationEntries is EqualUnmodifiableListView) return _hydrationEntries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_hydrationEntries);
}


/// Create a copy of HydrationPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationPeriodDataCopyWith<_HydrationPeriodData> get copyWith => __$HydrationPeriodDataCopyWithImpl<_HydrationPeriodData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationPeriodData&&const DeepCollectionEquality().equals(other._dailyHydration, _dailyHydration)&&const DeepCollectionEquality().equals(other._previousDailyHydration, _previousDailyHydration)&&const DeepCollectionEquality().equals(other._baselineDailyHydration, _baselineDailyHydration)&&const DeepCollectionEquality().equals(other._hydrationEntries, _hydrationEntries));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_dailyHydration),const DeepCollectionEquality().hash(_previousDailyHydration),const DeepCollectionEquality().hash(_baselineDailyHydration),const DeepCollectionEquality().hash(_hydrationEntries));

@override
String toString() {
  return 'HydrationPeriodData(dailyHydration: $dailyHydration, previousDailyHydration: $previousDailyHydration, baselineDailyHydration: $baselineDailyHydration, hydrationEntries: $hydrationEntries)';
}


}

/// @nodoc
abstract mixin class _$HydrationPeriodDataCopyWith<$Res> implements $HydrationPeriodDataCopyWith<$Res> {
  factory _$HydrationPeriodDataCopyWith(_HydrationPeriodData value, $Res Function(_HydrationPeriodData) _then) = __$HydrationPeriodDataCopyWithImpl;
@override @useResult
$Res call({
 List<DailyHydration> dailyHydration, List<DailyHydration> previousDailyHydration, List<DailyHydration> baselineDailyHydration, List<HydrationEntry> hydrationEntries
});




}
/// @nodoc
class __$HydrationPeriodDataCopyWithImpl<$Res>
    implements _$HydrationPeriodDataCopyWith<$Res> {
  __$HydrationPeriodDataCopyWithImpl(this._self, this._then);

  final _HydrationPeriodData _self;
  final $Res Function(_HydrationPeriodData) _then;

/// Create a copy of HydrationPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? dailyHydration = null,Object? previousDailyHydration = null,Object? baselineDailyHydration = null,Object? hydrationEntries = null,}) {
  return _then(_HydrationPeriodData(
dailyHydration: null == dailyHydration ? _self._dailyHydration : dailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,previousDailyHydration: null == previousDailyHydration ? _self._previousDailyHydration : previousDailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,baselineDailyHydration: null == baselineDailyHydration ? _self._baselineDailyHydration : baselineDailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,hydrationEntries: null == hydrationEntries ? _self._hydrationEntries : hydrationEntries // ignore: cast_nullable_to_non_nullable
as List<HydrationEntry>,
  ));
}


}

// dart format on
