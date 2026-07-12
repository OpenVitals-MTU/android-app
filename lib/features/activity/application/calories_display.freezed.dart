// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'calories_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CaloriesMetricSeries {

 List<PeriodChartValue> get values; bool get hasData; double get total;
/// Create a copy of CaloriesMetricSeries
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaloriesMetricSeriesCopyWith<CaloriesMetricSeries> get copyWith => _$CaloriesMetricSeriesCopyWithImpl<CaloriesMetricSeries>(this as CaloriesMetricSeries, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaloriesMetricSeries&&const DeepCollectionEquality().equals(other.values, values)&&(identical(other.hasData, hasData) || other.hasData == hasData)&&(identical(other.total, total) || other.total == total));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(values),hasData,total);

@override
String toString() {
  return 'CaloriesMetricSeries(values: $values, hasData: $hasData, total: $total)';
}


}

/// @nodoc
abstract mixin class $CaloriesMetricSeriesCopyWith<$Res>  {
  factory $CaloriesMetricSeriesCopyWith(CaloriesMetricSeries value, $Res Function(CaloriesMetricSeries) _then) = _$CaloriesMetricSeriesCopyWithImpl;
@useResult
$Res call({
 List<PeriodChartValue> values, bool hasData, double total
});




}
/// @nodoc
class _$CaloriesMetricSeriesCopyWithImpl<$Res>
    implements $CaloriesMetricSeriesCopyWith<$Res> {
  _$CaloriesMetricSeriesCopyWithImpl(this._self, this._then);

  final CaloriesMetricSeries _self;
  final $Res Function(CaloriesMetricSeries) _then;

/// Create a copy of CaloriesMetricSeries
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? values = null,Object? hasData = null,Object? total = null,}) {
  return _then(_self.copyWith(
values: null == values ? _self.values : values // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,total: null == total ? _self.total : total // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [CaloriesMetricSeries].
extension CaloriesMetricSeriesPatterns on CaloriesMetricSeries {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaloriesMetricSeries value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaloriesMetricSeries() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaloriesMetricSeries value)  $default,){
final _that = this;
switch (_that) {
case _CaloriesMetricSeries():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaloriesMetricSeries value)?  $default,){
final _that = this;
switch (_that) {
case _CaloriesMetricSeries() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<PeriodChartValue> values,  bool hasData,  double total)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaloriesMetricSeries() when $default != null:
return $default(_that.values,_that.hasData,_that.total);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<PeriodChartValue> values,  bool hasData,  double total)  $default,) {final _that = this;
switch (_that) {
case _CaloriesMetricSeries():
return $default(_that.values,_that.hasData,_that.total);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<PeriodChartValue> values,  bool hasData,  double total)?  $default,) {final _that = this;
switch (_that) {
case _CaloriesMetricSeries() when $default != null:
return $default(_that.values,_that.hasData,_that.total);case _:
  return null;

}
}

}

/// @nodoc


class _CaloriesMetricSeries implements CaloriesMetricSeries {
  const _CaloriesMetricSeries({required final  List<PeriodChartValue> values, required this.hasData, required this.total}): _values = values;
  

 final  List<PeriodChartValue> _values;
@override List<PeriodChartValue> get values {
  if (_values is EqualUnmodifiableListView) return _values;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_values);
}

@override final  bool hasData;
@override final  double total;

/// Create a copy of CaloriesMetricSeries
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaloriesMetricSeriesCopyWith<_CaloriesMetricSeries> get copyWith => __$CaloriesMetricSeriesCopyWithImpl<_CaloriesMetricSeries>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaloriesMetricSeries&&const DeepCollectionEquality().equals(other._values, _values)&&(identical(other.hasData, hasData) || other.hasData == hasData)&&(identical(other.total, total) || other.total == total));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_values),hasData,total);

@override
String toString() {
  return 'CaloriesMetricSeries(values: $values, hasData: $hasData, total: $total)';
}


}

/// @nodoc
abstract mixin class _$CaloriesMetricSeriesCopyWith<$Res> implements $CaloriesMetricSeriesCopyWith<$Res> {
  factory _$CaloriesMetricSeriesCopyWith(_CaloriesMetricSeries value, $Res Function(_CaloriesMetricSeries) _then) = __$CaloriesMetricSeriesCopyWithImpl;
@override @useResult
$Res call({
 List<PeriodChartValue> values, bool hasData, double total
});




}
/// @nodoc
class __$CaloriesMetricSeriesCopyWithImpl<$Res>
    implements _$CaloriesMetricSeriesCopyWith<$Res> {
  __$CaloriesMetricSeriesCopyWithImpl(this._self, this._then);

  final _CaloriesMetricSeries _self;
  final $Res Function(_CaloriesMetricSeries) _then;

/// Create a copy of CaloriesMetricSeries
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? values = null,Object? hasData = null,Object? total = null,}) {
  return _then(_CaloriesMetricSeries(
values: null == values ? _self._values : values // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,total: null == total ? _self.total : total // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$CaloriesDisplay {

 CaloriesMetricSeries get caloriesOut; CaloriesMetricSeries get activeCalories;
/// Create a copy of CaloriesDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaloriesDisplayCopyWith<CaloriesDisplay> get copyWith => _$CaloriesDisplayCopyWithImpl<CaloriesDisplay>(this as CaloriesDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaloriesDisplay&&(identical(other.caloriesOut, caloriesOut) || other.caloriesOut == caloriesOut)&&(identical(other.activeCalories, activeCalories) || other.activeCalories == activeCalories));
}


@override
int get hashCode => Object.hash(runtimeType,caloriesOut,activeCalories);

@override
String toString() {
  return 'CaloriesDisplay(caloriesOut: $caloriesOut, activeCalories: $activeCalories)';
}


}

/// @nodoc
abstract mixin class $CaloriesDisplayCopyWith<$Res>  {
  factory $CaloriesDisplayCopyWith(CaloriesDisplay value, $Res Function(CaloriesDisplay) _then) = _$CaloriesDisplayCopyWithImpl;
@useResult
$Res call({
 CaloriesMetricSeries caloriesOut, CaloriesMetricSeries activeCalories
});


$CaloriesMetricSeriesCopyWith<$Res> get caloriesOut;$CaloriesMetricSeriesCopyWith<$Res> get activeCalories;

}
/// @nodoc
class _$CaloriesDisplayCopyWithImpl<$Res>
    implements $CaloriesDisplayCopyWith<$Res> {
  _$CaloriesDisplayCopyWithImpl(this._self, this._then);

  final CaloriesDisplay _self;
  final $Res Function(CaloriesDisplay) _then;

/// Create a copy of CaloriesDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? caloriesOut = null,Object? activeCalories = null,}) {
  return _then(_self.copyWith(
caloriesOut: null == caloriesOut ? _self.caloriesOut : caloriesOut // ignore: cast_nullable_to_non_nullable
as CaloriesMetricSeries,activeCalories: null == activeCalories ? _self.activeCalories : activeCalories // ignore: cast_nullable_to_non_nullable
as CaloriesMetricSeries,
  ));
}
/// Create a copy of CaloriesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaloriesMetricSeriesCopyWith<$Res> get caloriesOut {
  
  return $CaloriesMetricSeriesCopyWith<$Res>(_self.caloriesOut, (value) {
    return _then(_self.copyWith(caloriesOut: value));
  });
}/// Create a copy of CaloriesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaloriesMetricSeriesCopyWith<$Res> get activeCalories {
  
  return $CaloriesMetricSeriesCopyWith<$Res>(_self.activeCalories, (value) {
    return _then(_self.copyWith(activeCalories: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaloriesDisplay].
extension CaloriesDisplayPatterns on CaloriesDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaloriesDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaloriesDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaloriesDisplay value)  $default,){
final _that = this;
switch (_that) {
case _CaloriesDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaloriesDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _CaloriesDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaloriesMetricSeries caloriesOut,  CaloriesMetricSeries activeCalories)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaloriesDisplay() when $default != null:
return $default(_that.caloriesOut,_that.activeCalories);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaloriesMetricSeries caloriesOut,  CaloriesMetricSeries activeCalories)  $default,) {final _that = this;
switch (_that) {
case _CaloriesDisplay():
return $default(_that.caloriesOut,_that.activeCalories);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaloriesMetricSeries caloriesOut,  CaloriesMetricSeries activeCalories)?  $default,) {final _that = this;
switch (_that) {
case _CaloriesDisplay() when $default != null:
return $default(_that.caloriesOut,_that.activeCalories);case _:
  return null;

}
}

}

/// @nodoc


class _CaloriesDisplay implements CaloriesDisplay {
  const _CaloriesDisplay({required this.caloriesOut, required this.activeCalories});
  

@override final  CaloriesMetricSeries caloriesOut;
@override final  CaloriesMetricSeries activeCalories;

/// Create a copy of CaloriesDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaloriesDisplayCopyWith<_CaloriesDisplay> get copyWith => __$CaloriesDisplayCopyWithImpl<_CaloriesDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaloriesDisplay&&(identical(other.caloriesOut, caloriesOut) || other.caloriesOut == caloriesOut)&&(identical(other.activeCalories, activeCalories) || other.activeCalories == activeCalories));
}


@override
int get hashCode => Object.hash(runtimeType,caloriesOut,activeCalories);

@override
String toString() {
  return 'CaloriesDisplay(caloriesOut: $caloriesOut, activeCalories: $activeCalories)';
}


}

/// @nodoc
abstract mixin class _$CaloriesDisplayCopyWith<$Res> implements $CaloriesDisplayCopyWith<$Res> {
  factory _$CaloriesDisplayCopyWith(_CaloriesDisplay value, $Res Function(_CaloriesDisplay) _then) = __$CaloriesDisplayCopyWithImpl;
@override @useResult
$Res call({
 CaloriesMetricSeries caloriesOut, CaloriesMetricSeries activeCalories
});


@override $CaloriesMetricSeriesCopyWith<$Res> get caloriesOut;@override $CaloriesMetricSeriesCopyWith<$Res> get activeCalories;

}
/// @nodoc
class __$CaloriesDisplayCopyWithImpl<$Res>
    implements _$CaloriesDisplayCopyWith<$Res> {
  __$CaloriesDisplayCopyWithImpl(this._self, this._then);

  final _CaloriesDisplay _self;
  final $Res Function(_CaloriesDisplay) _then;

/// Create a copy of CaloriesDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? caloriesOut = null,Object? activeCalories = null,}) {
  return _then(_CaloriesDisplay(
caloriesOut: null == caloriesOut ? _self.caloriesOut : caloriesOut // ignore: cast_nullable_to_non_nullable
as CaloriesMetricSeries,activeCalories: null == activeCalories ? _self.activeCalories : activeCalories // ignore: cast_nullable_to_non_nullable
as CaloriesMetricSeries,
  ));
}

/// Create a copy of CaloriesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaloriesMetricSeriesCopyWith<$Res> get caloriesOut {
  
  return $CaloriesMetricSeriesCopyWith<$Res>(_self.caloriesOut, (value) {
    return _then(_self.copyWith(caloriesOut: value));
  });
}/// Create a copy of CaloriesDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaloriesMetricSeriesCopyWith<$Res> get activeCalories {
  
  return $CaloriesMetricSeriesCopyWith<$Res>(_self.activeCalories, (value) {
    return _then(_self.copyWith(activeCalories: value));
  });
}
}

// dart format on
