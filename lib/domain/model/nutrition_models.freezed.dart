// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'nutrition_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$DailyNutrition {

 LocalDate get date; double get hydrationLiters; double get caloriesBurnedKcal; CaloriesBurnedSource get caloriesBurnedSource; bool get hasCaloriesBurnedData;
/// Create a copy of DailyNutrition
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyNutritionCopyWith<DailyNutrition> get copyWith => _$DailyNutritionCopyWithImpl<DailyNutrition>(this as DailyNutrition, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyNutrition&&(identical(other.date, date) || other.date == date)&&(identical(other.hydrationLiters, hydrationLiters) || other.hydrationLiters == hydrationLiters)&&(identical(other.caloriesBurnedKcal, caloriesBurnedKcal) || other.caloriesBurnedKcal == caloriesBurnedKcal)&&(identical(other.caloriesBurnedSource, caloriesBurnedSource) || other.caloriesBurnedSource == caloriesBurnedSource)&&(identical(other.hasCaloriesBurnedData, hasCaloriesBurnedData) || other.hasCaloriesBurnedData == hasCaloriesBurnedData));
}


@override
int get hashCode => Object.hash(runtimeType,date,hydrationLiters,caloriesBurnedKcal,caloriesBurnedSource,hasCaloriesBurnedData);

@override
String toString() {
  return 'DailyNutrition(date: $date, hydrationLiters: $hydrationLiters, caloriesBurnedKcal: $caloriesBurnedKcal, caloriesBurnedSource: $caloriesBurnedSource, hasCaloriesBurnedData: $hasCaloriesBurnedData)';
}


}

/// @nodoc
abstract mixin class $DailyNutritionCopyWith<$Res>  {
  factory $DailyNutritionCopyWith(DailyNutrition value, $Res Function(DailyNutrition) _then) = _$DailyNutritionCopyWithImpl;
@useResult
$Res call({
 LocalDate date, double hydrationLiters, double caloriesBurnedKcal, CaloriesBurnedSource caloriesBurnedSource, bool hasCaloriesBurnedData
});




}
/// @nodoc
class _$DailyNutritionCopyWithImpl<$Res>
    implements $DailyNutritionCopyWith<$Res> {
  _$DailyNutritionCopyWithImpl(this._self, this._then);

  final DailyNutrition _self;
  final $Res Function(DailyNutrition) _then;

/// Create a copy of DailyNutrition
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? hydrationLiters = null,Object? caloriesBurnedKcal = null,Object? caloriesBurnedSource = null,Object? hasCaloriesBurnedData = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,hydrationLiters: null == hydrationLiters ? _self.hydrationLiters : hydrationLiters // ignore: cast_nullable_to_non_nullable
as double,caloriesBurnedKcal: null == caloriesBurnedKcal ? _self.caloriesBurnedKcal : caloriesBurnedKcal // ignore: cast_nullable_to_non_nullable
as double,caloriesBurnedSource: null == caloriesBurnedSource ? _self.caloriesBurnedSource : caloriesBurnedSource // ignore: cast_nullable_to_non_nullable
as CaloriesBurnedSource,hasCaloriesBurnedData: null == hasCaloriesBurnedData ? _self.hasCaloriesBurnedData : hasCaloriesBurnedData // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyNutrition].
extension DailyNutritionPatterns on DailyNutrition {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>({TResult Function( _DailyNutrition value)?  build,required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyNutrition() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult map<TResult extends Object?>({required TResult Function( _DailyNutrition value)  build,}){
final _that = this;
switch (_that) {
case _DailyNutrition():
return build(_that);case _:
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>({TResult? Function( _DailyNutrition value)?  build,}){
final _that = this;
switch (_that) {
case _DailyNutrition() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>({TResult Function( LocalDate date,  double hydrationLiters,  double caloriesBurnedKcal,  CaloriesBurnedSource caloriesBurnedSource,  bool hasCaloriesBurnedData)?  build,required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyNutrition() when build != null:
return build(_that.date,_that.hydrationLiters,_that.caloriesBurnedKcal,_that.caloriesBurnedSource,_that.hasCaloriesBurnedData);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>({required TResult Function( LocalDate date,  double hydrationLiters,  double caloriesBurnedKcal,  CaloriesBurnedSource caloriesBurnedSource,  bool hasCaloriesBurnedData)  build,}) {final _that = this;
switch (_that) {
case _DailyNutrition():
return build(_that.date,_that.hydrationLiters,_that.caloriesBurnedKcal,_that.caloriesBurnedSource,_that.hasCaloriesBurnedData);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>({TResult? Function( LocalDate date,  double hydrationLiters,  double caloriesBurnedKcal,  CaloriesBurnedSource caloriesBurnedSource,  bool hasCaloriesBurnedData)?  build,}) {final _that = this;
switch (_that) {
case _DailyNutrition() when build != null:
return build(_that.date,_that.hydrationLiters,_that.caloriesBurnedKcal,_that.caloriesBurnedSource,_that.hasCaloriesBurnedData);case _:
  return null;

}
}

}

/// @nodoc


class _DailyNutrition implements DailyNutrition {
  const _DailyNutrition({required this.date, required this.hydrationLiters, required this.caloriesBurnedKcal, required this.caloriesBurnedSource, required this.hasCaloriesBurnedData});
  

@override final  LocalDate date;
@override final  double hydrationLiters;
@override final  double caloriesBurnedKcal;
@override final  CaloriesBurnedSource caloriesBurnedSource;
@override final  bool hasCaloriesBurnedData;

/// Create a copy of DailyNutrition
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyNutritionCopyWith<_DailyNutrition> get copyWith => __$DailyNutritionCopyWithImpl<_DailyNutrition>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyNutrition&&(identical(other.date, date) || other.date == date)&&(identical(other.hydrationLiters, hydrationLiters) || other.hydrationLiters == hydrationLiters)&&(identical(other.caloriesBurnedKcal, caloriesBurnedKcal) || other.caloriesBurnedKcal == caloriesBurnedKcal)&&(identical(other.caloriesBurnedSource, caloriesBurnedSource) || other.caloriesBurnedSource == caloriesBurnedSource)&&(identical(other.hasCaloriesBurnedData, hasCaloriesBurnedData) || other.hasCaloriesBurnedData == hasCaloriesBurnedData));
}


@override
int get hashCode => Object.hash(runtimeType,date,hydrationLiters,caloriesBurnedKcal,caloriesBurnedSource,hasCaloriesBurnedData);

@override
String toString() {
  return 'DailyNutrition.build(date: $date, hydrationLiters: $hydrationLiters, caloriesBurnedKcal: $caloriesBurnedKcal, caloriesBurnedSource: $caloriesBurnedSource, hasCaloriesBurnedData: $hasCaloriesBurnedData)';
}


}

/// @nodoc
abstract mixin class _$DailyNutritionCopyWith<$Res> implements $DailyNutritionCopyWith<$Res> {
  factory _$DailyNutritionCopyWith(_DailyNutrition value, $Res Function(_DailyNutrition) _then) = __$DailyNutritionCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, double hydrationLiters, double caloriesBurnedKcal, CaloriesBurnedSource caloriesBurnedSource, bool hasCaloriesBurnedData
});




}
/// @nodoc
class __$DailyNutritionCopyWithImpl<$Res>
    implements _$DailyNutritionCopyWith<$Res> {
  __$DailyNutritionCopyWithImpl(this._self, this._then);

  final _DailyNutrition _self;
  final $Res Function(_DailyNutrition) _then;

/// Create a copy of DailyNutrition
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? hydrationLiters = null,Object? caloriesBurnedKcal = null,Object? caloriesBurnedSource = null,Object? hasCaloriesBurnedData = null,}) {
  return _then(_DailyNutrition(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,hydrationLiters: null == hydrationLiters ? _self.hydrationLiters : hydrationLiters // ignore: cast_nullable_to_non_nullable
as double,caloriesBurnedKcal: null == caloriesBurnedKcal ? _self.caloriesBurnedKcal : caloriesBurnedKcal // ignore: cast_nullable_to_non_nullable
as double,caloriesBurnedSource: null == caloriesBurnedSource ? _self.caloriesBurnedSource : caloriesBurnedSource // ignore: cast_nullable_to_non_nullable
as CaloriesBurnedSource,hasCaloriesBurnedData: null == hasCaloriesBurnedData ? _self.hasCaloriesBurnedData : hasCaloriesBurnedData // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$CaloriesBurnedValue {

 double get kcal; CaloriesBurnedSource get source;
/// Create a copy of CaloriesBurnedValue
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaloriesBurnedValueCopyWith<CaloriesBurnedValue> get copyWith => _$CaloriesBurnedValueCopyWithImpl<CaloriesBurnedValue>(this as CaloriesBurnedValue, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaloriesBurnedValue&&(identical(other.kcal, kcal) || other.kcal == kcal)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,kcal,source);

@override
String toString() {
  return 'CaloriesBurnedValue(kcal: $kcal, source: $source)';
}


}

/// @nodoc
abstract mixin class $CaloriesBurnedValueCopyWith<$Res>  {
  factory $CaloriesBurnedValueCopyWith(CaloriesBurnedValue value, $Res Function(CaloriesBurnedValue) _then) = _$CaloriesBurnedValueCopyWithImpl;
@useResult
$Res call({
 double kcal, CaloriesBurnedSource source
});




}
/// @nodoc
class _$CaloriesBurnedValueCopyWithImpl<$Res>
    implements $CaloriesBurnedValueCopyWith<$Res> {
  _$CaloriesBurnedValueCopyWithImpl(this._self, this._then);

  final CaloriesBurnedValue _self;
  final $Res Function(CaloriesBurnedValue) _then;

/// Create a copy of CaloriesBurnedValue
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? kcal = null,Object? source = null,}) {
  return _then(_self.copyWith(
kcal: null == kcal ? _self.kcal : kcal // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as CaloriesBurnedSource,
  ));
}

}


/// Adds pattern-matching-related methods to [CaloriesBurnedValue].
extension CaloriesBurnedValuePatterns on CaloriesBurnedValue {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaloriesBurnedValue value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaloriesBurnedValue() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaloriesBurnedValue value)  $default,){
final _that = this;
switch (_that) {
case _CaloriesBurnedValue():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaloriesBurnedValue value)?  $default,){
final _that = this;
switch (_that) {
case _CaloriesBurnedValue() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double kcal,  CaloriesBurnedSource source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaloriesBurnedValue() when $default != null:
return $default(_that.kcal,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double kcal,  CaloriesBurnedSource source)  $default,) {final _that = this;
switch (_that) {
case _CaloriesBurnedValue():
return $default(_that.kcal,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double kcal,  CaloriesBurnedSource source)?  $default,) {final _that = this;
switch (_that) {
case _CaloriesBurnedValue() when $default != null:
return $default(_that.kcal,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _CaloriesBurnedValue implements CaloriesBurnedValue {
  const _CaloriesBurnedValue({required this.kcal, required this.source});
  

@override final  double kcal;
@override final  CaloriesBurnedSource source;

/// Create a copy of CaloriesBurnedValue
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaloriesBurnedValueCopyWith<_CaloriesBurnedValue> get copyWith => __$CaloriesBurnedValueCopyWithImpl<_CaloriesBurnedValue>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaloriesBurnedValue&&(identical(other.kcal, kcal) || other.kcal == kcal)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,kcal,source);

@override
String toString() {
  return 'CaloriesBurnedValue(kcal: $kcal, source: $source)';
}


}

/// @nodoc
abstract mixin class _$CaloriesBurnedValueCopyWith<$Res> implements $CaloriesBurnedValueCopyWith<$Res> {
  factory _$CaloriesBurnedValueCopyWith(_CaloriesBurnedValue value, $Res Function(_CaloriesBurnedValue) _then) = __$CaloriesBurnedValueCopyWithImpl;
@override @useResult
$Res call({
 double kcal, CaloriesBurnedSource source
});




}
/// @nodoc
class __$CaloriesBurnedValueCopyWithImpl<$Res>
    implements _$CaloriesBurnedValueCopyWith<$Res> {
  __$CaloriesBurnedValueCopyWithImpl(this._self, this._then);

  final _CaloriesBurnedValue _self;
  final $Res Function(_CaloriesBurnedValue) _then;

/// Create a copy of CaloriesBurnedValue
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? kcal = null,Object? source = null,}) {
  return _then(_CaloriesBurnedValue(
kcal: null == kcal ? _self.kcal : kcal // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as CaloriesBurnedSource,
  ));
}


}

/// @nodoc
mixin _$DailyHydration {

 LocalDate get date; double get liters;
/// Create a copy of DailyHydration
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyHydrationCopyWith<DailyHydration> get copyWith => _$DailyHydrationCopyWithImpl<DailyHydration>(this as DailyHydration, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyHydration&&(identical(other.date, date) || other.date == date)&&(identical(other.liters, liters) || other.liters == liters));
}


@override
int get hashCode => Object.hash(runtimeType,date,liters);

@override
String toString() {
  return 'DailyHydration(date: $date, liters: $liters)';
}


}

/// @nodoc
abstract mixin class $DailyHydrationCopyWith<$Res>  {
  factory $DailyHydrationCopyWith(DailyHydration value, $Res Function(DailyHydration) _then) = _$DailyHydrationCopyWithImpl;
@useResult
$Res call({
 LocalDate date, double liters
});




}
/// @nodoc
class _$DailyHydrationCopyWithImpl<$Res>
    implements $DailyHydrationCopyWith<$Res> {
  _$DailyHydrationCopyWithImpl(this._self, this._then);

  final DailyHydration _self;
  final $Res Function(DailyHydration) _then;

/// Create a copy of DailyHydration
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? liters = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,liters: null == liters ? _self.liters : liters // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyHydration].
extension DailyHydrationPatterns on DailyHydration {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyHydration value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyHydration() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyHydration value)  $default,){
final _that = this;
switch (_that) {
case _DailyHydration():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyHydration value)?  $default,){
final _that = this;
switch (_that) {
case _DailyHydration() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  double liters)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyHydration() when $default != null:
return $default(_that.date,_that.liters);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  double liters)  $default,) {final _that = this;
switch (_that) {
case _DailyHydration():
return $default(_that.date,_that.liters);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  double liters)?  $default,) {final _that = this;
switch (_that) {
case _DailyHydration() when $default != null:
return $default(_that.date,_that.liters);case _:
  return null;

}
}

}

/// @nodoc


class _DailyHydration implements DailyHydration {
  const _DailyHydration({required this.date, required this.liters});
  

@override final  LocalDate date;
@override final  double liters;

/// Create a copy of DailyHydration
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyHydrationCopyWith<_DailyHydration> get copyWith => __$DailyHydrationCopyWithImpl<_DailyHydration>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyHydration&&(identical(other.date, date) || other.date == date)&&(identical(other.liters, liters) || other.liters == liters));
}


@override
int get hashCode => Object.hash(runtimeType,date,liters);

@override
String toString() {
  return 'DailyHydration(date: $date, liters: $liters)';
}


}

/// @nodoc
abstract mixin class _$DailyHydrationCopyWith<$Res> implements $DailyHydrationCopyWith<$Res> {
  factory _$DailyHydrationCopyWith(_DailyHydration value, $Res Function(_DailyHydration) _then) = __$DailyHydrationCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, double liters
});




}
/// @nodoc
class __$DailyHydrationCopyWithImpl<$Res>
    implements _$DailyHydrationCopyWith<$Res> {
  __$DailyHydrationCopyWithImpl(this._self, this._then);

  final _DailyHydration _self;
  final $Res Function(_DailyHydration) _then;

/// Create a copy of DailyHydration
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? liters = null,}) {
  return _then(_DailyHydration(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,liters: null == liters ? _self.liters : liters // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$HydrationEntry {

 DateTime get startTime; DateTime get endTime; double get liters; String get source; String get id; String? get clientRecordId; bool get isOpenVitalsEntry; HydrationEntryRecordType get recordType; String? get displayName; Map<NutritionNutrient, double> get nutrientValues;
/// Create a copy of HydrationEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationEntryCopyWith<HydrationEntry> get copyWith => _$HydrationEntryCopyWithImpl<HydrationEntry>(this as HydrationEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationEntry&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.liters, liters) || other.liters == liters)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry)&&(identical(other.recordType, recordType) || other.recordType == recordType)&&(identical(other.displayName, displayName) || other.displayName == displayName)&&const DeepCollectionEquality().equals(other.nutrientValues, nutrientValues));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,liters,source,id,clientRecordId,isOpenVitalsEntry,recordType,displayName,const DeepCollectionEquality().hash(nutrientValues));

@override
String toString() {
  return 'HydrationEntry(startTime: $startTime, endTime: $endTime, liters: $liters, source: $source, id: $id, clientRecordId: $clientRecordId, isOpenVitalsEntry: $isOpenVitalsEntry, recordType: $recordType, displayName: $displayName, nutrientValues: $nutrientValues)';
}


}

/// @nodoc
abstract mixin class $HydrationEntryCopyWith<$Res>  {
  factory $HydrationEntryCopyWith(HydrationEntry value, $Res Function(HydrationEntry) _then) = _$HydrationEntryCopyWithImpl;
@useResult
$Res call({
 DateTime startTime, DateTime endTime, double liters, String source, String id, String? clientRecordId, bool isOpenVitalsEntry, HydrationEntryRecordType recordType, String? displayName, Map<NutritionNutrient, double> nutrientValues
});




}
/// @nodoc
class _$HydrationEntryCopyWithImpl<$Res>
    implements $HydrationEntryCopyWith<$Res> {
  _$HydrationEntryCopyWithImpl(this._self, this._then);

  final HydrationEntry _self;
  final $Res Function(HydrationEntry) _then;

/// Create a copy of HydrationEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? startTime = null,Object? endTime = null,Object? liters = null,Object? source = null,Object? id = null,Object? clientRecordId = freezed,Object? isOpenVitalsEntry = null,Object? recordType = null,Object? displayName = freezed,Object? nutrientValues = null,}) {
  return _then(_self.copyWith(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,liters: null == liters ? _self.liters : liters // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,recordType: null == recordType ? _self.recordType : recordType // ignore: cast_nullable_to_non_nullable
as HydrationEntryRecordType,displayName: freezed == displayName ? _self.displayName : displayName // ignore: cast_nullable_to_non_nullable
as String?,nutrientValues: null == nutrientValues ? _self.nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,
  ));
}

}


/// Adds pattern-matching-related methods to [HydrationEntry].
extension HydrationEntryPatterns on HydrationEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationEntry value)  $default,){
final _that = this;
switch (_that) {
case _HydrationEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationEntry value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  double liters,  String source,  String id,  String? clientRecordId,  bool isOpenVitalsEntry,  HydrationEntryRecordType recordType,  String? displayName,  Map<NutritionNutrient, double> nutrientValues)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationEntry() when $default != null:
return $default(_that.startTime,_that.endTime,_that.liters,_that.source,_that.id,_that.clientRecordId,_that.isOpenVitalsEntry,_that.recordType,_that.displayName,_that.nutrientValues);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  double liters,  String source,  String id,  String? clientRecordId,  bool isOpenVitalsEntry,  HydrationEntryRecordType recordType,  String? displayName,  Map<NutritionNutrient, double> nutrientValues)  $default,) {final _that = this;
switch (_that) {
case _HydrationEntry():
return $default(_that.startTime,_that.endTime,_that.liters,_that.source,_that.id,_that.clientRecordId,_that.isOpenVitalsEntry,_that.recordType,_that.displayName,_that.nutrientValues);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime startTime,  DateTime endTime,  double liters,  String source,  String id,  String? clientRecordId,  bool isOpenVitalsEntry,  HydrationEntryRecordType recordType,  String? displayName,  Map<NutritionNutrient, double> nutrientValues)?  $default,) {final _that = this;
switch (_that) {
case _HydrationEntry() when $default != null:
return $default(_that.startTime,_that.endTime,_that.liters,_that.source,_that.id,_that.clientRecordId,_that.isOpenVitalsEntry,_that.recordType,_that.displayName,_that.nutrientValues);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationEntry implements HydrationEntry {
  const _HydrationEntry({required this.startTime, required this.endTime, required this.liters, required this.source, this.id = '', this.clientRecordId, this.isOpenVitalsEntry = false, this.recordType = HydrationEntryRecordType.hydration, this.displayName, final  Map<NutritionNutrient, double> nutrientValues = const <NutritionNutrient, double>{}}): _nutrientValues = nutrientValues;
  

@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  double liters;
@override final  String source;
@override@JsonKey() final  String id;
@override final  String? clientRecordId;
@override@JsonKey() final  bool isOpenVitalsEntry;
@override@JsonKey() final  HydrationEntryRecordType recordType;
@override final  String? displayName;
 final  Map<NutritionNutrient, double> _nutrientValues;
@override@JsonKey() Map<NutritionNutrient, double> get nutrientValues {
  if (_nutrientValues is EqualUnmodifiableMapView) return _nutrientValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_nutrientValues);
}


/// Create a copy of HydrationEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationEntryCopyWith<_HydrationEntry> get copyWith => __$HydrationEntryCopyWithImpl<_HydrationEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationEntry&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.liters, liters) || other.liters == liters)&&(identical(other.source, source) || other.source == source)&&(identical(other.id, id) || other.id == id)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry)&&(identical(other.recordType, recordType) || other.recordType == recordType)&&(identical(other.displayName, displayName) || other.displayName == displayName)&&const DeepCollectionEquality().equals(other._nutrientValues, _nutrientValues));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,liters,source,id,clientRecordId,isOpenVitalsEntry,recordType,displayName,const DeepCollectionEquality().hash(_nutrientValues));

@override
String toString() {
  return 'HydrationEntry(startTime: $startTime, endTime: $endTime, liters: $liters, source: $source, id: $id, clientRecordId: $clientRecordId, isOpenVitalsEntry: $isOpenVitalsEntry, recordType: $recordType, displayName: $displayName, nutrientValues: $nutrientValues)';
}


}

/// @nodoc
abstract mixin class _$HydrationEntryCopyWith<$Res> implements $HydrationEntryCopyWith<$Res> {
  factory _$HydrationEntryCopyWith(_HydrationEntry value, $Res Function(_HydrationEntry) _then) = __$HydrationEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime startTime, DateTime endTime, double liters, String source, String id, String? clientRecordId, bool isOpenVitalsEntry, HydrationEntryRecordType recordType, String? displayName, Map<NutritionNutrient, double> nutrientValues
});




}
/// @nodoc
class __$HydrationEntryCopyWithImpl<$Res>
    implements _$HydrationEntryCopyWith<$Res> {
  __$HydrationEntryCopyWithImpl(this._self, this._then);

  final _HydrationEntry _self;
  final $Res Function(_HydrationEntry) _then;

/// Create a copy of HydrationEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? startTime = null,Object? endTime = null,Object? liters = null,Object? source = null,Object? id = null,Object? clientRecordId = freezed,Object? isOpenVitalsEntry = null,Object? recordType = null,Object? displayName = freezed,Object? nutrientValues = null,}) {
  return _then(_HydrationEntry(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,liters: null == liters ? _self.liters : liters // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,recordType: null == recordType ? _self.recordType : recordType // ignore: cast_nullable_to_non_nullable
as HydrationEntryRecordType,displayName: freezed == displayName ? _self.displayName : displayName // ignore: cast_nullable_to_non_nullable
as String?,nutrientValues: null == nutrientValues ? _self._nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,
  ));
}


}

/// @nodoc
mixin _$HydrationWriteRequest {

 DateTime get time; double get volumeLiters; String? get drinkId;
/// Create a copy of HydrationWriteRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationWriteRequestCopyWith<HydrationWriteRequest> get copyWith => _$HydrationWriteRequestCopyWithImpl<HydrationWriteRequest>(this as HydrationWriteRequest, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationWriteRequest&&(identical(other.time, time) || other.time == time)&&(identical(other.volumeLiters, volumeLiters) || other.volumeLiters == volumeLiters)&&(identical(other.drinkId, drinkId) || other.drinkId == drinkId));
}


@override
int get hashCode => Object.hash(runtimeType,time,volumeLiters,drinkId);

@override
String toString() {
  return 'HydrationWriteRequest(time: $time, volumeLiters: $volumeLiters, drinkId: $drinkId)';
}


}

/// @nodoc
abstract mixin class $HydrationWriteRequestCopyWith<$Res>  {
  factory $HydrationWriteRequestCopyWith(HydrationWriteRequest value, $Res Function(HydrationWriteRequest) _then) = _$HydrationWriteRequestCopyWithImpl;
@useResult
$Res call({
 DateTime time, double volumeLiters, String? drinkId
});




}
/// @nodoc
class _$HydrationWriteRequestCopyWithImpl<$Res>
    implements $HydrationWriteRequestCopyWith<$Res> {
  _$HydrationWriteRequestCopyWithImpl(this._self, this._then);

  final HydrationWriteRequest _self;
  final $Res Function(HydrationWriteRequest) _then;

/// Create a copy of HydrationWriteRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? volumeLiters = null,Object? drinkId = freezed,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,volumeLiters: null == volumeLiters ? _self.volumeLiters : volumeLiters // ignore: cast_nullable_to_non_nullable
as double,drinkId: freezed == drinkId ? _self.drinkId : drinkId // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [HydrationWriteRequest].
extension HydrationWriteRequestPatterns on HydrationWriteRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationWriteRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationWriteRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationWriteRequest value)  $default,){
final _that = this;
switch (_that) {
case _HydrationWriteRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationWriteRequest value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationWriteRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double volumeLiters,  String? drinkId)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationWriteRequest() when $default != null:
return $default(_that.time,_that.volumeLiters,_that.drinkId);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double volumeLiters,  String? drinkId)  $default,) {final _that = this;
switch (_that) {
case _HydrationWriteRequest():
return $default(_that.time,_that.volumeLiters,_that.drinkId);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double volumeLiters,  String? drinkId)?  $default,) {final _that = this;
switch (_that) {
case _HydrationWriteRequest() when $default != null:
return $default(_that.time,_that.volumeLiters,_that.drinkId);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationWriteRequest implements HydrationWriteRequest {
  const _HydrationWriteRequest({required this.time, required this.volumeLiters, this.drinkId});
  

@override final  DateTime time;
@override final  double volumeLiters;
@override final  String? drinkId;

/// Create a copy of HydrationWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationWriteRequestCopyWith<_HydrationWriteRequest> get copyWith => __$HydrationWriteRequestCopyWithImpl<_HydrationWriteRequest>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationWriteRequest&&(identical(other.time, time) || other.time == time)&&(identical(other.volumeLiters, volumeLiters) || other.volumeLiters == volumeLiters)&&(identical(other.drinkId, drinkId) || other.drinkId == drinkId));
}


@override
int get hashCode => Object.hash(runtimeType,time,volumeLiters,drinkId);

@override
String toString() {
  return 'HydrationWriteRequest(time: $time, volumeLiters: $volumeLiters, drinkId: $drinkId)';
}


}

/// @nodoc
abstract mixin class _$HydrationWriteRequestCopyWith<$Res> implements $HydrationWriteRequestCopyWith<$Res> {
  factory _$HydrationWriteRequestCopyWith(_HydrationWriteRequest value, $Res Function(_HydrationWriteRequest) _then) = __$HydrationWriteRequestCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double volumeLiters, String? drinkId
});




}
/// @nodoc
class __$HydrationWriteRequestCopyWithImpl<$Res>
    implements _$HydrationWriteRequestCopyWith<$Res> {
  __$HydrationWriteRequestCopyWithImpl(this._self, this._then);

  final _HydrationWriteRequest _self;
  final $Res Function(_HydrationWriteRequest) _then;

/// Create a copy of HydrationWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? volumeLiters = null,Object? drinkId = freezed,}) {
  return _then(_HydrationWriteRequest(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,volumeLiters: null == volumeLiters ? _self.volumeLiters : volumeLiters // ignore: cast_nullable_to_non_nullable
as double,drinkId: freezed == drinkId ? _self.drinkId : drinkId // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

/// @nodoc
mixin _$CustomHydrationDrink {

 String get id; String get name; double get volumeMilliliters; double get hydrationMultiplier; Map<NutritionNutrient, double> get nutrientValues; CaffeineSourceCategory? get category; bool get isPreloaded;
/// Create a copy of CustomHydrationDrink
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CustomHydrationDrinkCopyWith<CustomHydrationDrink> get copyWith => _$CustomHydrationDrinkCopyWithImpl<CustomHydrationDrink>(this as CustomHydrationDrink, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CustomHydrationDrink&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name)&&(identical(other.volumeMilliliters, volumeMilliliters) || other.volumeMilliliters == volumeMilliliters)&&(identical(other.hydrationMultiplier, hydrationMultiplier) || other.hydrationMultiplier == hydrationMultiplier)&&const DeepCollectionEquality().equals(other.nutrientValues, nutrientValues)&&(identical(other.category, category) || other.category == category)&&(identical(other.isPreloaded, isPreloaded) || other.isPreloaded == isPreloaded));
}


@override
int get hashCode => Object.hash(runtimeType,id,name,volumeMilliliters,hydrationMultiplier,const DeepCollectionEquality().hash(nutrientValues),category,isPreloaded);

@override
String toString() {
  return 'CustomHydrationDrink(id: $id, name: $name, volumeMilliliters: $volumeMilliliters, hydrationMultiplier: $hydrationMultiplier, nutrientValues: $nutrientValues, category: $category, isPreloaded: $isPreloaded)';
}


}

/// @nodoc
abstract mixin class $CustomHydrationDrinkCopyWith<$Res>  {
  factory $CustomHydrationDrinkCopyWith(CustomHydrationDrink value, $Res Function(CustomHydrationDrink) _then) = _$CustomHydrationDrinkCopyWithImpl;
@useResult
$Res call({
 String id, String name, double volumeMilliliters, double hydrationMultiplier, Map<NutritionNutrient, double> nutrientValues, CaffeineSourceCategory? category, bool isPreloaded
});




}
/// @nodoc
class _$CustomHydrationDrinkCopyWithImpl<$Res>
    implements $CustomHydrationDrinkCopyWith<$Res> {
  _$CustomHydrationDrinkCopyWithImpl(this._self, this._then);

  final CustomHydrationDrink _self;
  final $Res Function(CustomHydrationDrink) _then;

/// Create a copy of CustomHydrationDrink
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? name = null,Object? volumeMilliliters = null,Object? hydrationMultiplier = null,Object? nutrientValues = null,Object? category = freezed,Object? isPreloaded = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,volumeMilliliters: null == volumeMilliliters ? _self.volumeMilliliters : volumeMilliliters // ignore: cast_nullable_to_non_nullable
as double,hydrationMultiplier: null == hydrationMultiplier ? _self.hydrationMultiplier : hydrationMultiplier // ignore: cast_nullable_to_non_nullable
as double,nutrientValues: null == nutrientValues ? _self.nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,category: freezed == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as CaffeineSourceCategory?,isPreloaded: null == isPreloaded ? _self.isPreloaded : isPreloaded // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [CustomHydrationDrink].
extension CustomHydrationDrinkPatterns on CustomHydrationDrink {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CustomHydrationDrink value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CustomHydrationDrink() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CustomHydrationDrink value)  $default,){
final _that = this;
switch (_that) {
case _CustomHydrationDrink():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CustomHydrationDrink value)?  $default,){
final _that = this;
switch (_that) {
case _CustomHydrationDrink() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String name,  double volumeMilliliters,  double hydrationMultiplier,  Map<NutritionNutrient, double> nutrientValues,  CaffeineSourceCategory? category,  bool isPreloaded)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CustomHydrationDrink() when $default != null:
return $default(_that.id,_that.name,_that.volumeMilliliters,_that.hydrationMultiplier,_that.nutrientValues,_that.category,_that.isPreloaded);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String name,  double volumeMilliliters,  double hydrationMultiplier,  Map<NutritionNutrient, double> nutrientValues,  CaffeineSourceCategory? category,  bool isPreloaded)  $default,) {final _that = this;
switch (_that) {
case _CustomHydrationDrink():
return $default(_that.id,_that.name,_that.volumeMilliliters,_that.hydrationMultiplier,_that.nutrientValues,_that.category,_that.isPreloaded);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String name,  double volumeMilliliters,  double hydrationMultiplier,  Map<NutritionNutrient, double> nutrientValues,  CaffeineSourceCategory? category,  bool isPreloaded)?  $default,) {final _that = this;
switch (_that) {
case _CustomHydrationDrink() when $default != null:
return $default(_that.id,_that.name,_that.volumeMilliliters,_that.hydrationMultiplier,_that.nutrientValues,_that.category,_that.isPreloaded);case _:
  return null;

}
}

}

/// @nodoc


class _CustomHydrationDrink extends CustomHydrationDrink {
  const _CustomHydrationDrink({required this.id, required this.name, required this.volumeMilliliters, this.hydrationMultiplier = 1.0, final  Map<NutritionNutrient, double> nutrientValues = const <NutritionNutrient, double>{}, this.category, this.isPreloaded = false}): _nutrientValues = nutrientValues,super._();
  

@override final  String id;
@override final  String name;
@override final  double volumeMilliliters;
@override@JsonKey() final  double hydrationMultiplier;
 final  Map<NutritionNutrient, double> _nutrientValues;
@override@JsonKey() Map<NutritionNutrient, double> get nutrientValues {
  if (_nutrientValues is EqualUnmodifiableMapView) return _nutrientValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_nutrientValues);
}

@override final  CaffeineSourceCategory? category;
@override@JsonKey() final  bool isPreloaded;

/// Create a copy of CustomHydrationDrink
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CustomHydrationDrinkCopyWith<_CustomHydrationDrink> get copyWith => __$CustomHydrationDrinkCopyWithImpl<_CustomHydrationDrink>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CustomHydrationDrink&&(identical(other.id, id) || other.id == id)&&(identical(other.name, name) || other.name == name)&&(identical(other.volumeMilliliters, volumeMilliliters) || other.volumeMilliliters == volumeMilliliters)&&(identical(other.hydrationMultiplier, hydrationMultiplier) || other.hydrationMultiplier == hydrationMultiplier)&&const DeepCollectionEquality().equals(other._nutrientValues, _nutrientValues)&&(identical(other.category, category) || other.category == category)&&(identical(other.isPreloaded, isPreloaded) || other.isPreloaded == isPreloaded));
}


@override
int get hashCode => Object.hash(runtimeType,id,name,volumeMilliliters,hydrationMultiplier,const DeepCollectionEquality().hash(_nutrientValues),category,isPreloaded);

@override
String toString() {
  return 'CustomHydrationDrink(id: $id, name: $name, volumeMilliliters: $volumeMilliliters, hydrationMultiplier: $hydrationMultiplier, nutrientValues: $nutrientValues, category: $category, isPreloaded: $isPreloaded)';
}


}

/// @nodoc
abstract mixin class _$CustomHydrationDrinkCopyWith<$Res> implements $CustomHydrationDrinkCopyWith<$Res> {
  factory _$CustomHydrationDrinkCopyWith(_CustomHydrationDrink value, $Res Function(_CustomHydrationDrink) _then) = __$CustomHydrationDrinkCopyWithImpl;
@override @useResult
$Res call({
 String id, String name, double volumeMilliliters, double hydrationMultiplier, Map<NutritionNutrient, double> nutrientValues, CaffeineSourceCategory? category, bool isPreloaded
});




}
/// @nodoc
class __$CustomHydrationDrinkCopyWithImpl<$Res>
    implements _$CustomHydrationDrinkCopyWith<$Res> {
  __$CustomHydrationDrinkCopyWithImpl(this._self, this._then);

  final _CustomHydrationDrink _self;
  final $Res Function(_CustomHydrationDrink) _then;

/// Create a copy of CustomHydrationDrink
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? name = null,Object? volumeMilliliters = null,Object? hydrationMultiplier = null,Object? nutrientValues = null,Object? category = freezed,Object? isPreloaded = null,}) {
  return _then(_CustomHydrationDrink(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,name: null == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String,volumeMilliliters: null == volumeMilliliters ? _self.volumeMilliliters : volumeMilliliters // ignore: cast_nullable_to_non_nullable
as double,hydrationMultiplier: null == hydrationMultiplier ? _self.hydrationMultiplier : hydrationMultiplier // ignore: cast_nullable_to_non_nullable
as double,nutrientValues: null == nutrientValues ? _self._nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,category: freezed == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as CaffeineSourceCategory?,isPreloaded: null == isPreloaded ? _self.isPreloaded : isPreloaded // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$NutritionEntry {

 DateTime get time; DateTime get endTime; int get mealType; String? get name; double? get energyKcal; double? get proteinGrams; double? get carbsGrams; double? get fatGrams; double? get fiberGrams; double? get sugarGrams; String get source; Map<NutritionNutrient, double> get nutrientValues; String get id; String? get clientRecordId; bool get isOpenVitalsEntry;
/// Create a copy of NutritionEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$NutritionEntryCopyWith<NutritionEntry> get copyWith => _$NutritionEntryCopyWithImpl<NutritionEntry>(this as NutritionEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is NutritionEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.mealType, mealType) || other.mealType == mealType)&&(identical(other.name, name) || other.name == name)&&(identical(other.energyKcal, energyKcal) || other.energyKcal == energyKcal)&&(identical(other.proteinGrams, proteinGrams) || other.proteinGrams == proteinGrams)&&(identical(other.carbsGrams, carbsGrams) || other.carbsGrams == carbsGrams)&&(identical(other.fatGrams, fatGrams) || other.fatGrams == fatGrams)&&(identical(other.fiberGrams, fiberGrams) || other.fiberGrams == fiberGrams)&&(identical(other.sugarGrams, sugarGrams) || other.sugarGrams == sugarGrams)&&(identical(other.source, source) || other.source == source)&&const DeepCollectionEquality().equals(other.nutrientValues, nutrientValues)&&(identical(other.id, id) || other.id == id)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,endTime,mealType,name,energyKcal,proteinGrams,carbsGrams,fatGrams,fiberGrams,sugarGrams,source,const DeepCollectionEquality().hash(nutrientValues),id,clientRecordId,isOpenVitalsEntry);

@override
String toString() {
  return 'NutritionEntry(time: $time, endTime: $endTime, mealType: $mealType, name: $name, energyKcal: $energyKcal, proteinGrams: $proteinGrams, carbsGrams: $carbsGrams, fatGrams: $fatGrams, fiberGrams: $fiberGrams, sugarGrams: $sugarGrams, source: $source, nutrientValues: $nutrientValues, id: $id, clientRecordId: $clientRecordId, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $NutritionEntryCopyWith<$Res>  {
  factory $NutritionEntryCopyWith(NutritionEntry value, $Res Function(NutritionEntry) _then) = _$NutritionEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, DateTime endTime, int mealType, String? name, double? energyKcal, double? proteinGrams, double? carbsGrams, double? fatGrams, double? fiberGrams, double? sugarGrams, String source, Map<NutritionNutrient, double> nutrientValues, String id, String? clientRecordId, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$NutritionEntryCopyWithImpl<$Res>
    implements $NutritionEntryCopyWith<$Res> {
  _$NutritionEntryCopyWithImpl(this._self, this._then);

  final NutritionEntry _self;
  final $Res Function(NutritionEntry) _then;

/// Create a copy of NutritionEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? endTime = null,Object? mealType = null,Object? name = freezed,Object? energyKcal = freezed,Object? proteinGrams = freezed,Object? carbsGrams = freezed,Object? fatGrams = freezed,Object? fiberGrams = freezed,Object? sugarGrams = freezed,Object? source = null,Object? nutrientValues = null,Object? id = null,Object? clientRecordId = freezed,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,mealType: null == mealType ? _self.mealType : mealType // ignore: cast_nullable_to_non_nullable
as int,name: freezed == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String?,energyKcal: freezed == energyKcal ? _self.energyKcal : energyKcal // ignore: cast_nullable_to_non_nullable
as double?,proteinGrams: freezed == proteinGrams ? _self.proteinGrams : proteinGrams // ignore: cast_nullable_to_non_nullable
as double?,carbsGrams: freezed == carbsGrams ? _self.carbsGrams : carbsGrams // ignore: cast_nullable_to_non_nullable
as double?,fatGrams: freezed == fatGrams ? _self.fatGrams : fatGrams // ignore: cast_nullable_to_non_nullable
as double?,fiberGrams: freezed == fiberGrams ? _self.fiberGrams : fiberGrams // ignore: cast_nullable_to_non_nullable
as double?,sugarGrams: freezed == sugarGrams ? _self.sugarGrams : sugarGrams // ignore: cast_nullable_to_non_nullable
as double?,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,nutrientValues: null == nutrientValues ? _self.nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [NutritionEntry].
extension NutritionEntryPatterns on NutritionEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>({TResult Function( _NutritionEntry value)?  build,required TResult orElse(),}){
final _that = this;
switch (_that) {
case _NutritionEntry() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult map<TResult extends Object?>({required TResult Function( _NutritionEntry value)  build,}){
final _that = this;
switch (_that) {
case _NutritionEntry():
return build(_that);case _:
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>({TResult? Function( _NutritionEntry value)?  build,}){
final _that = this;
switch (_that) {
case _NutritionEntry() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>({TResult Function( DateTime time,  DateTime endTime,  int mealType,  String? name,  double? energyKcal,  double? proteinGrams,  double? carbsGrams,  double? fatGrams,  double? fiberGrams,  double? sugarGrams,  String source,  Map<NutritionNutrient, double> nutrientValues,  String id,  String? clientRecordId,  bool isOpenVitalsEntry)?  build,required TResult orElse(),}) {final _that = this;
switch (_that) {
case _NutritionEntry() when build != null:
return build(_that.time,_that.endTime,_that.mealType,_that.name,_that.energyKcal,_that.proteinGrams,_that.carbsGrams,_that.fatGrams,_that.fiberGrams,_that.sugarGrams,_that.source,_that.nutrientValues,_that.id,_that.clientRecordId,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>({required TResult Function( DateTime time,  DateTime endTime,  int mealType,  String? name,  double? energyKcal,  double? proteinGrams,  double? carbsGrams,  double? fatGrams,  double? fiberGrams,  double? sugarGrams,  String source,  Map<NutritionNutrient, double> nutrientValues,  String id,  String? clientRecordId,  bool isOpenVitalsEntry)  build,}) {final _that = this;
switch (_that) {
case _NutritionEntry():
return build(_that.time,_that.endTime,_that.mealType,_that.name,_that.energyKcal,_that.proteinGrams,_that.carbsGrams,_that.fatGrams,_that.fiberGrams,_that.sugarGrams,_that.source,_that.nutrientValues,_that.id,_that.clientRecordId,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>({TResult? Function( DateTime time,  DateTime endTime,  int mealType,  String? name,  double? energyKcal,  double? proteinGrams,  double? carbsGrams,  double? fatGrams,  double? fiberGrams,  double? sugarGrams,  String source,  Map<NutritionNutrient, double> nutrientValues,  String id,  String? clientRecordId,  bool isOpenVitalsEntry)?  build,}) {final _that = this;
switch (_that) {
case _NutritionEntry() when build != null:
return build(_that.time,_that.endTime,_that.mealType,_that.name,_that.energyKcal,_that.proteinGrams,_that.carbsGrams,_that.fatGrams,_that.fiberGrams,_that.sugarGrams,_that.source,_that.nutrientValues,_that.id,_that.clientRecordId,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _NutritionEntry implements NutritionEntry {
  const _NutritionEntry({required this.time, required this.endTime, required this.mealType, required this.name, required this.energyKcal, required this.proteinGrams, required this.carbsGrams, required this.fatGrams, required this.fiberGrams, required this.sugarGrams, required this.source, required final  Map<NutritionNutrient, double> nutrientValues, required this.id, required this.clientRecordId, required this.isOpenVitalsEntry}): _nutrientValues = nutrientValues;
  

@override final  DateTime time;
@override final  DateTime endTime;
@override final  int mealType;
@override final  String? name;
@override final  double? energyKcal;
@override final  double? proteinGrams;
@override final  double? carbsGrams;
@override final  double? fatGrams;
@override final  double? fiberGrams;
@override final  double? sugarGrams;
@override final  String source;
 final  Map<NutritionNutrient, double> _nutrientValues;
@override Map<NutritionNutrient, double> get nutrientValues {
  if (_nutrientValues is EqualUnmodifiableMapView) return _nutrientValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_nutrientValues);
}

@override final  String id;
@override final  String? clientRecordId;
@override final  bool isOpenVitalsEntry;

/// Create a copy of NutritionEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$NutritionEntryCopyWith<_NutritionEntry> get copyWith => __$NutritionEntryCopyWithImpl<_NutritionEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _NutritionEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.mealType, mealType) || other.mealType == mealType)&&(identical(other.name, name) || other.name == name)&&(identical(other.energyKcal, energyKcal) || other.energyKcal == energyKcal)&&(identical(other.proteinGrams, proteinGrams) || other.proteinGrams == proteinGrams)&&(identical(other.carbsGrams, carbsGrams) || other.carbsGrams == carbsGrams)&&(identical(other.fatGrams, fatGrams) || other.fatGrams == fatGrams)&&(identical(other.fiberGrams, fiberGrams) || other.fiberGrams == fiberGrams)&&(identical(other.sugarGrams, sugarGrams) || other.sugarGrams == sugarGrams)&&(identical(other.source, source) || other.source == source)&&const DeepCollectionEquality().equals(other._nutrientValues, _nutrientValues)&&(identical(other.id, id) || other.id == id)&&(identical(other.clientRecordId, clientRecordId) || other.clientRecordId == clientRecordId)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,time,endTime,mealType,name,energyKcal,proteinGrams,carbsGrams,fatGrams,fiberGrams,sugarGrams,source,const DeepCollectionEquality().hash(_nutrientValues),id,clientRecordId,isOpenVitalsEntry);

@override
String toString() {
  return 'NutritionEntry.build(time: $time, endTime: $endTime, mealType: $mealType, name: $name, energyKcal: $energyKcal, proteinGrams: $proteinGrams, carbsGrams: $carbsGrams, fatGrams: $fatGrams, fiberGrams: $fiberGrams, sugarGrams: $sugarGrams, source: $source, nutrientValues: $nutrientValues, id: $id, clientRecordId: $clientRecordId, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$NutritionEntryCopyWith<$Res> implements $NutritionEntryCopyWith<$Res> {
  factory _$NutritionEntryCopyWith(_NutritionEntry value, $Res Function(_NutritionEntry) _then) = __$NutritionEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, DateTime endTime, int mealType, String? name, double? energyKcal, double? proteinGrams, double? carbsGrams, double? fatGrams, double? fiberGrams, double? sugarGrams, String source, Map<NutritionNutrient, double> nutrientValues, String id, String? clientRecordId, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$NutritionEntryCopyWithImpl<$Res>
    implements _$NutritionEntryCopyWith<$Res> {
  __$NutritionEntryCopyWithImpl(this._self, this._then);

  final _NutritionEntry _self;
  final $Res Function(_NutritionEntry) _then;

/// Create a copy of NutritionEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? endTime = null,Object? mealType = null,Object? name = freezed,Object? energyKcal = freezed,Object? proteinGrams = freezed,Object? carbsGrams = freezed,Object? fatGrams = freezed,Object? fiberGrams = freezed,Object? sugarGrams = freezed,Object? source = null,Object? nutrientValues = null,Object? id = null,Object? clientRecordId = freezed,Object? isOpenVitalsEntry = null,}) {
  return _then(_NutritionEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,mealType: null == mealType ? _self.mealType : mealType // ignore: cast_nullable_to_non_nullable
as int,name: freezed == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String?,energyKcal: freezed == energyKcal ? _self.energyKcal : energyKcal // ignore: cast_nullable_to_non_nullable
as double?,proteinGrams: freezed == proteinGrams ? _self.proteinGrams : proteinGrams // ignore: cast_nullable_to_non_nullable
as double?,carbsGrams: freezed == carbsGrams ? _self.carbsGrams : carbsGrams // ignore: cast_nullable_to_non_nullable
as double?,fatGrams: freezed == fatGrams ? _self.fatGrams : fatGrams // ignore: cast_nullable_to_non_nullable
as double?,fiberGrams: freezed == fiberGrams ? _self.fiberGrams : fiberGrams // ignore: cast_nullable_to_non_nullable
as double?,sugarGrams: freezed == sugarGrams ? _self.sugarGrams : sugarGrams // ignore: cast_nullable_to_non_nullable
as double?,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,nutrientValues: null == nutrientValues ? _self._nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,clientRecordId: freezed == clientRecordId ? _self.clientRecordId : clientRecordId // ignore: cast_nullable_to_non_nullable
as String?,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$NutritionWriteRequest {

 DateTime get time; Map<NutritionNutrient, double> get nutrientValues; String? get name; String? get associatedHydrationClientRecordId;
/// Create a copy of NutritionWriteRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$NutritionWriteRequestCopyWith<NutritionWriteRequest> get copyWith => _$NutritionWriteRequestCopyWithImpl<NutritionWriteRequest>(this as NutritionWriteRequest, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is NutritionWriteRequest&&(identical(other.time, time) || other.time == time)&&const DeepCollectionEquality().equals(other.nutrientValues, nutrientValues)&&(identical(other.name, name) || other.name == name)&&(identical(other.associatedHydrationClientRecordId, associatedHydrationClientRecordId) || other.associatedHydrationClientRecordId == associatedHydrationClientRecordId));
}


@override
int get hashCode => Object.hash(runtimeType,time,const DeepCollectionEquality().hash(nutrientValues),name,associatedHydrationClientRecordId);

@override
String toString() {
  return 'NutritionWriteRequest(time: $time, nutrientValues: $nutrientValues, name: $name, associatedHydrationClientRecordId: $associatedHydrationClientRecordId)';
}


}

/// @nodoc
abstract mixin class $NutritionWriteRequestCopyWith<$Res>  {
  factory $NutritionWriteRequestCopyWith(NutritionWriteRequest value, $Res Function(NutritionWriteRequest) _then) = _$NutritionWriteRequestCopyWithImpl;
@useResult
$Res call({
 DateTime time, Map<NutritionNutrient, double> nutrientValues, String? name, String? associatedHydrationClientRecordId
});




}
/// @nodoc
class _$NutritionWriteRequestCopyWithImpl<$Res>
    implements $NutritionWriteRequestCopyWith<$Res> {
  _$NutritionWriteRequestCopyWithImpl(this._self, this._then);

  final NutritionWriteRequest _self;
  final $Res Function(NutritionWriteRequest) _then;

/// Create a copy of NutritionWriteRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? nutrientValues = null,Object? name = freezed,Object? associatedHydrationClientRecordId = freezed,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,nutrientValues: null == nutrientValues ? _self.nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,name: freezed == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String?,associatedHydrationClientRecordId: freezed == associatedHydrationClientRecordId ? _self.associatedHydrationClientRecordId : associatedHydrationClientRecordId // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [NutritionWriteRequest].
extension NutritionWriteRequestPatterns on NutritionWriteRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _NutritionWriteRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _NutritionWriteRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _NutritionWriteRequest value)  $default,){
final _that = this;
switch (_that) {
case _NutritionWriteRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _NutritionWriteRequest value)?  $default,){
final _that = this;
switch (_that) {
case _NutritionWriteRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  Map<NutritionNutrient, double> nutrientValues,  String? name,  String? associatedHydrationClientRecordId)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _NutritionWriteRequest() when $default != null:
return $default(_that.time,_that.nutrientValues,_that.name,_that.associatedHydrationClientRecordId);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  Map<NutritionNutrient, double> nutrientValues,  String? name,  String? associatedHydrationClientRecordId)  $default,) {final _that = this;
switch (_that) {
case _NutritionWriteRequest():
return $default(_that.time,_that.nutrientValues,_that.name,_that.associatedHydrationClientRecordId);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  Map<NutritionNutrient, double> nutrientValues,  String? name,  String? associatedHydrationClientRecordId)?  $default,) {final _that = this;
switch (_that) {
case _NutritionWriteRequest() when $default != null:
return $default(_that.time,_that.nutrientValues,_that.name,_that.associatedHydrationClientRecordId);case _:
  return null;

}
}

}

/// @nodoc


class _NutritionWriteRequest extends NutritionWriteRequest {
  const _NutritionWriteRequest({required this.time, required final  Map<NutritionNutrient, double> nutrientValues, this.name, this.associatedHydrationClientRecordId}): _nutrientValues = nutrientValues,super._();
  

@override final  DateTime time;
 final  Map<NutritionNutrient, double> _nutrientValues;
@override Map<NutritionNutrient, double> get nutrientValues {
  if (_nutrientValues is EqualUnmodifiableMapView) return _nutrientValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_nutrientValues);
}

@override final  String? name;
@override final  String? associatedHydrationClientRecordId;

/// Create a copy of NutritionWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$NutritionWriteRequestCopyWith<_NutritionWriteRequest> get copyWith => __$NutritionWriteRequestCopyWithImpl<_NutritionWriteRequest>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _NutritionWriteRequest&&(identical(other.time, time) || other.time == time)&&const DeepCollectionEquality().equals(other._nutrientValues, _nutrientValues)&&(identical(other.name, name) || other.name == name)&&(identical(other.associatedHydrationClientRecordId, associatedHydrationClientRecordId) || other.associatedHydrationClientRecordId == associatedHydrationClientRecordId));
}


@override
int get hashCode => Object.hash(runtimeType,time,const DeepCollectionEquality().hash(_nutrientValues),name,associatedHydrationClientRecordId);

@override
String toString() {
  return 'NutritionWriteRequest(time: $time, nutrientValues: $nutrientValues, name: $name, associatedHydrationClientRecordId: $associatedHydrationClientRecordId)';
}


}

/// @nodoc
abstract mixin class _$NutritionWriteRequestCopyWith<$Res> implements $NutritionWriteRequestCopyWith<$Res> {
  factory _$NutritionWriteRequestCopyWith(_NutritionWriteRequest value, $Res Function(_NutritionWriteRequest) _then) = __$NutritionWriteRequestCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, Map<NutritionNutrient, double> nutrientValues, String? name, String? associatedHydrationClientRecordId
});




}
/// @nodoc
class __$NutritionWriteRequestCopyWithImpl<$Res>
    implements _$NutritionWriteRequestCopyWith<$Res> {
  __$NutritionWriteRequestCopyWithImpl(this._self, this._then);

  final _NutritionWriteRequest _self;
  final $Res Function(_NutritionWriteRequest) _then;

/// Create a copy of NutritionWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? nutrientValues = null,Object? name = freezed,Object? associatedHydrationClientRecordId = freezed,}) {
  return _then(_NutritionWriteRequest(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,nutrientValues: null == nutrientValues ? _self._nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,name: freezed == name ? _self.name : name // ignore: cast_nullable_to_non_nullable
as String?,associatedHydrationClientRecordId: freezed == associatedHydrationClientRecordId ? _self.associatedHydrationClientRecordId : associatedHydrationClientRecordId // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}

/// @nodoc
mixin _$DailyMacros {

 LocalDate get date; Map<NutritionNutrient, double> get nutrientValues; double get energyKcal; double get proteinGrams; double get carbsGrams; double get fatGrams;
/// Create a copy of DailyMacros
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyMacrosCopyWith<DailyMacros> get copyWith => _$DailyMacrosCopyWithImpl<DailyMacros>(this as DailyMacros, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyMacros&&(identical(other.date, date) || other.date == date)&&const DeepCollectionEquality().equals(other.nutrientValues, nutrientValues)&&(identical(other.energyKcal, energyKcal) || other.energyKcal == energyKcal)&&(identical(other.proteinGrams, proteinGrams) || other.proteinGrams == proteinGrams)&&(identical(other.carbsGrams, carbsGrams) || other.carbsGrams == carbsGrams)&&(identical(other.fatGrams, fatGrams) || other.fatGrams == fatGrams));
}


@override
int get hashCode => Object.hash(runtimeType,date,const DeepCollectionEquality().hash(nutrientValues),energyKcal,proteinGrams,carbsGrams,fatGrams);

@override
String toString() {
  return 'DailyMacros(date: $date, nutrientValues: $nutrientValues, energyKcal: $energyKcal, proteinGrams: $proteinGrams, carbsGrams: $carbsGrams, fatGrams: $fatGrams)';
}


}

/// @nodoc
abstract mixin class $DailyMacrosCopyWith<$Res>  {
  factory $DailyMacrosCopyWith(DailyMacros value, $Res Function(DailyMacros) _then) = _$DailyMacrosCopyWithImpl;
@useResult
$Res call({
 LocalDate date, Map<NutritionNutrient, double> nutrientValues, double energyKcal, double proteinGrams, double carbsGrams, double fatGrams
});




}
/// @nodoc
class _$DailyMacrosCopyWithImpl<$Res>
    implements $DailyMacrosCopyWith<$Res> {
  _$DailyMacrosCopyWithImpl(this._self, this._then);

  final DailyMacros _self;
  final $Res Function(DailyMacros) _then;

/// Create a copy of DailyMacros
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? nutrientValues = null,Object? energyKcal = null,Object? proteinGrams = null,Object? carbsGrams = null,Object? fatGrams = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,nutrientValues: null == nutrientValues ? _self.nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,energyKcal: null == energyKcal ? _self.energyKcal : energyKcal // ignore: cast_nullable_to_non_nullable
as double,proteinGrams: null == proteinGrams ? _self.proteinGrams : proteinGrams // ignore: cast_nullable_to_non_nullable
as double,carbsGrams: null == carbsGrams ? _self.carbsGrams : carbsGrams // ignore: cast_nullable_to_non_nullable
as double,fatGrams: null == fatGrams ? _self.fatGrams : fatGrams // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyMacros].
extension DailyMacrosPatterns on DailyMacros {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>({TResult Function( _DailyMacros value)?  build,required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyMacros() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult map<TResult extends Object?>({required TResult Function( _DailyMacros value)  build,}){
final _that = this;
switch (_that) {
case _DailyMacros():
return build(_that);case _:
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>({TResult? Function( _DailyMacros value)?  build,}){
final _that = this;
switch (_that) {
case _DailyMacros() when build != null:
return build(_that);case _:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>({TResult Function( LocalDate date,  Map<NutritionNutrient, double> nutrientValues,  double energyKcal,  double proteinGrams,  double carbsGrams,  double fatGrams)?  build,required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyMacros() when build != null:
return build(_that.date,_that.nutrientValues,_that.energyKcal,_that.proteinGrams,_that.carbsGrams,_that.fatGrams);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>({required TResult Function( LocalDate date,  Map<NutritionNutrient, double> nutrientValues,  double energyKcal,  double proteinGrams,  double carbsGrams,  double fatGrams)  build,}) {final _that = this;
switch (_that) {
case _DailyMacros():
return build(_that.date,_that.nutrientValues,_that.energyKcal,_that.proteinGrams,_that.carbsGrams,_that.fatGrams);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>({TResult? Function( LocalDate date,  Map<NutritionNutrient, double> nutrientValues,  double energyKcal,  double proteinGrams,  double carbsGrams,  double fatGrams)?  build,}) {final _that = this;
switch (_that) {
case _DailyMacros() when build != null:
return build(_that.date,_that.nutrientValues,_that.energyKcal,_that.proteinGrams,_that.carbsGrams,_that.fatGrams);case _:
  return null;

}
}

}

/// @nodoc


class _DailyMacros implements DailyMacros {
  const _DailyMacros({required this.date, required final  Map<NutritionNutrient, double> nutrientValues, required this.energyKcal, required this.proteinGrams, required this.carbsGrams, required this.fatGrams}): _nutrientValues = nutrientValues;
  

@override final  LocalDate date;
 final  Map<NutritionNutrient, double> _nutrientValues;
@override Map<NutritionNutrient, double> get nutrientValues {
  if (_nutrientValues is EqualUnmodifiableMapView) return _nutrientValues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_nutrientValues);
}

@override final  double energyKcal;
@override final  double proteinGrams;
@override final  double carbsGrams;
@override final  double fatGrams;

/// Create a copy of DailyMacros
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyMacrosCopyWith<_DailyMacros> get copyWith => __$DailyMacrosCopyWithImpl<_DailyMacros>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyMacros&&(identical(other.date, date) || other.date == date)&&const DeepCollectionEquality().equals(other._nutrientValues, _nutrientValues)&&(identical(other.energyKcal, energyKcal) || other.energyKcal == energyKcal)&&(identical(other.proteinGrams, proteinGrams) || other.proteinGrams == proteinGrams)&&(identical(other.carbsGrams, carbsGrams) || other.carbsGrams == carbsGrams)&&(identical(other.fatGrams, fatGrams) || other.fatGrams == fatGrams));
}


@override
int get hashCode => Object.hash(runtimeType,date,const DeepCollectionEquality().hash(_nutrientValues),energyKcal,proteinGrams,carbsGrams,fatGrams);

@override
String toString() {
  return 'DailyMacros.build(date: $date, nutrientValues: $nutrientValues, energyKcal: $energyKcal, proteinGrams: $proteinGrams, carbsGrams: $carbsGrams, fatGrams: $fatGrams)';
}


}

/// @nodoc
abstract mixin class _$DailyMacrosCopyWith<$Res> implements $DailyMacrosCopyWith<$Res> {
  factory _$DailyMacrosCopyWith(_DailyMacros value, $Res Function(_DailyMacros) _then) = __$DailyMacrosCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, Map<NutritionNutrient, double> nutrientValues, double energyKcal, double proteinGrams, double carbsGrams, double fatGrams
});




}
/// @nodoc
class __$DailyMacrosCopyWithImpl<$Res>
    implements _$DailyMacrosCopyWith<$Res> {
  __$DailyMacrosCopyWithImpl(this._self, this._then);

  final _DailyMacros _self;
  final $Res Function(_DailyMacros) _then;

/// Create a copy of DailyMacros
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? nutrientValues = null,Object? energyKcal = null,Object? proteinGrams = null,Object? carbsGrams = null,Object? fatGrams = null,}) {
  return _then(_DailyMacros(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,nutrientValues: null == nutrientValues ? _self._nutrientValues : nutrientValues // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrient, double>,energyKcal: null == energyKcal ? _self.energyKcal : energyKcal // ignore: cast_nullable_to_non_nullable
as double,proteinGrams: null == proteinGrams ? _self.proteinGrams : proteinGrams // ignore: cast_nullable_to_non_nullable
as double,carbsGrams: null == carbsGrams ? _self.carbsGrams : carbsGrams // ignore: cast_nullable_to_non_nullable
as double,fatGrams: null == fatGrams ? _self.fatGrams : fatGrams // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

// dart format on
