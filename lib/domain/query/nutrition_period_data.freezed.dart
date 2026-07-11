// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'nutrition_period_data.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$NutritionPeriodData {

 List<DailyMacros> get dailyMacros; List<DailyMacros> get previousDailyMacros; List<DailyMacros> get baselineDailyMacros; List<NutritionEntry> get entries;
/// Create a copy of NutritionPeriodData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$NutritionPeriodDataCopyWith<NutritionPeriodData> get copyWith => _$NutritionPeriodDataCopyWithImpl<NutritionPeriodData>(this as NutritionPeriodData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is NutritionPeriodData&&const DeepCollectionEquality().equals(other.dailyMacros, dailyMacros)&&const DeepCollectionEquality().equals(other.previousDailyMacros, previousDailyMacros)&&const DeepCollectionEquality().equals(other.baselineDailyMacros, baselineDailyMacros)&&const DeepCollectionEquality().equals(other.entries, entries));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(dailyMacros),const DeepCollectionEquality().hash(previousDailyMacros),const DeepCollectionEquality().hash(baselineDailyMacros),const DeepCollectionEquality().hash(entries));

@override
String toString() {
  return 'NutritionPeriodData(dailyMacros: $dailyMacros, previousDailyMacros: $previousDailyMacros, baselineDailyMacros: $baselineDailyMacros, entries: $entries)';
}


}

/// @nodoc
abstract mixin class $NutritionPeriodDataCopyWith<$Res>  {
  factory $NutritionPeriodDataCopyWith(NutritionPeriodData value, $Res Function(NutritionPeriodData) _then) = _$NutritionPeriodDataCopyWithImpl;
@useResult
$Res call({
 List<DailyMacros> dailyMacros, List<DailyMacros> previousDailyMacros, List<DailyMacros> baselineDailyMacros, List<NutritionEntry> entries
});




}
/// @nodoc
class _$NutritionPeriodDataCopyWithImpl<$Res>
    implements $NutritionPeriodDataCopyWith<$Res> {
  _$NutritionPeriodDataCopyWithImpl(this._self, this._then);

  final NutritionPeriodData _self;
  final $Res Function(NutritionPeriodData) _then;

/// Create a copy of NutritionPeriodData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? dailyMacros = null,Object? previousDailyMacros = null,Object? baselineDailyMacros = null,Object? entries = null,}) {
  return _then(_self.copyWith(
dailyMacros: null == dailyMacros ? _self.dailyMacros : dailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,previousDailyMacros: null == previousDailyMacros ? _self.previousDailyMacros : previousDailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,baselineDailyMacros: null == baselineDailyMacros ? _self.baselineDailyMacros : baselineDailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,entries: null == entries ? _self.entries : entries // ignore: cast_nullable_to_non_nullable
as List<NutritionEntry>,
  ));
}

}


/// Adds pattern-matching-related methods to [NutritionPeriodData].
extension NutritionPeriodDataPatterns on NutritionPeriodData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _NutritionPeriodData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _NutritionPeriodData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _NutritionPeriodData value)  $default,){
final _that = this;
switch (_that) {
case _NutritionPeriodData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _NutritionPeriodData value)?  $default,){
final _that = this;
switch (_that) {
case _NutritionPeriodData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<DailyMacros> dailyMacros,  List<DailyMacros> previousDailyMacros,  List<DailyMacros> baselineDailyMacros,  List<NutritionEntry> entries)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _NutritionPeriodData() when $default != null:
return $default(_that.dailyMacros,_that.previousDailyMacros,_that.baselineDailyMacros,_that.entries);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<DailyMacros> dailyMacros,  List<DailyMacros> previousDailyMacros,  List<DailyMacros> baselineDailyMacros,  List<NutritionEntry> entries)  $default,) {final _that = this;
switch (_that) {
case _NutritionPeriodData():
return $default(_that.dailyMacros,_that.previousDailyMacros,_that.baselineDailyMacros,_that.entries);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<DailyMacros> dailyMacros,  List<DailyMacros> previousDailyMacros,  List<DailyMacros> baselineDailyMacros,  List<NutritionEntry> entries)?  $default,) {final _that = this;
switch (_that) {
case _NutritionPeriodData() when $default != null:
return $default(_that.dailyMacros,_that.previousDailyMacros,_that.baselineDailyMacros,_that.entries);case _:
  return null;

}
}

}

/// @nodoc


class _NutritionPeriodData implements NutritionPeriodData {
  const _NutritionPeriodData({final  List<DailyMacros> dailyMacros = const <DailyMacros>[], final  List<DailyMacros> previousDailyMacros = const <DailyMacros>[], final  List<DailyMacros> baselineDailyMacros = const <DailyMacros>[], final  List<NutritionEntry> entries = const <NutritionEntry>[]}): _dailyMacros = dailyMacros,_previousDailyMacros = previousDailyMacros,_baselineDailyMacros = baselineDailyMacros,_entries = entries;
  

 final  List<DailyMacros> _dailyMacros;
@override@JsonKey() List<DailyMacros> get dailyMacros {
  if (_dailyMacros is EqualUnmodifiableListView) return _dailyMacros;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyMacros);
}

 final  List<DailyMacros> _previousDailyMacros;
@override@JsonKey() List<DailyMacros> get previousDailyMacros {
  if (_previousDailyMacros is EqualUnmodifiableListView) return _previousDailyMacros;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDailyMacros);
}

 final  List<DailyMacros> _baselineDailyMacros;
@override@JsonKey() List<DailyMacros> get baselineDailyMacros {
  if (_baselineDailyMacros is EqualUnmodifiableListView) return _baselineDailyMacros;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineDailyMacros);
}

 final  List<NutritionEntry> _entries;
@override@JsonKey() List<NutritionEntry> get entries {
  if (_entries is EqualUnmodifiableListView) return _entries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entries);
}


/// Create a copy of NutritionPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$NutritionPeriodDataCopyWith<_NutritionPeriodData> get copyWith => __$NutritionPeriodDataCopyWithImpl<_NutritionPeriodData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _NutritionPeriodData&&const DeepCollectionEquality().equals(other._dailyMacros, _dailyMacros)&&const DeepCollectionEquality().equals(other._previousDailyMacros, _previousDailyMacros)&&const DeepCollectionEquality().equals(other._baselineDailyMacros, _baselineDailyMacros)&&const DeepCollectionEquality().equals(other._entries, _entries));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_dailyMacros),const DeepCollectionEquality().hash(_previousDailyMacros),const DeepCollectionEquality().hash(_baselineDailyMacros),const DeepCollectionEquality().hash(_entries));

@override
String toString() {
  return 'NutritionPeriodData(dailyMacros: $dailyMacros, previousDailyMacros: $previousDailyMacros, baselineDailyMacros: $baselineDailyMacros, entries: $entries)';
}


}

/// @nodoc
abstract mixin class _$NutritionPeriodDataCopyWith<$Res> implements $NutritionPeriodDataCopyWith<$Res> {
  factory _$NutritionPeriodDataCopyWith(_NutritionPeriodData value, $Res Function(_NutritionPeriodData) _then) = __$NutritionPeriodDataCopyWithImpl;
@override @useResult
$Res call({
 List<DailyMacros> dailyMacros, List<DailyMacros> previousDailyMacros, List<DailyMacros> baselineDailyMacros, List<NutritionEntry> entries
});




}
/// @nodoc
class __$NutritionPeriodDataCopyWithImpl<$Res>
    implements _$NutritionPeriodDataCopyWith<$Res> {
  __$NutritionPeriodDataCopyWithImpl(this._self, this._then);

  final _NutritionPeriodData _self;
  final $Res Function(_NutritionPeriodData) _then;

/// Create a copy of NutritionPeriodData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? dailyMacros = null,Object? previousDailyMacros = null,Object? baselineDailyMacros = null,Object? entries = null,}) {
  return _then(_NutritionPeriodData(
dailyMacros: null == dailyMacros ? _self._dailyMacros : dailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,previousDailyMacros: null == previousDailyMacros ? _self._previousDailyMacros : previousDailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,baselineDailyMacros: null == baselineDailyMacros ? _self._baselineDailyMacros : baselineDailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,entries: null == entries ? _self._entries : entries // ignore: cast_nullable_to_non_nullable
as List<NutritionEntry>,
  ));
}


}

// dart format on
