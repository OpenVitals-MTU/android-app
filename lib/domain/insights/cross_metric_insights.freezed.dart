// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'cross_metric_insights.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CrossMetricValue {

 LocalDate get date; double get value;
/// Create a copy of CrossMetricValue
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CrossMetricValueCopyWith<CrossMetricValue> get copyWith => _$CrossMetricValueCopyWithImpl<CrossMetricValue>(this as CrossMetricValue, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CrossMetricValue&&(identical(other.date, date) || other.date == date)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,date,value);

@override
String toString() {
  return 'CrossMetricValue(date: $date, value: $value)';
}


}

/// @nodoc
abstract mixin class $CrossMetricValueCopyWith<$Res>  {
  factory $CrossMetricValueCopyWith(CrossMetricValue value, $Res Function(CrossMetricValue) _then) = _$CrossMetricValueCopyWithImpl;
@useResult
$Res call({
 LocalDate date, double value
});




}
/// @nodoc
class _$CrossMetricValueCopyWithImpl<$Res>
    implements $CrossMetricValueCopyWith<$Res> {
  _$CrossMetricValueCopyWithImpl(this._self, this._then);

  final CrossMetricValue _self;
  final $Res Function(CrossMetricValue) _then;

/// Create a copy of CrossMetricValue
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? value = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [CrossMetricValue].
extension CrossMetricValuePatterns on CrossMetricValue {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CrossMetricValue value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CrossMetricValue() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CrossMetricValue value)  $default,){
final _that = this;
switch (_that) {
case _CrossMetricValue():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CrossMetricValue value)?  $default,){
final _that = this;
switch (_that) {
case _CrossMetricValue() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  double value)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CrossMetricValue() when $default != null:
return $default(_that.date,_that.value);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  double value)  $default,) {final _that = this;
switch (_that) {
case _CrossMetricValue():
return $default(_that.date,_that.value);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  double value)?  $default,) {final _that = this;
switch (_that) {
case _CrossMetricValue() when $default != null:
return $default(_that.date,_that.value);case _:
  return null;

}
}

}

/// @nodoc


class _CrossMetricValue implements CrossMetricValue {
  const _CrossMetricValue({required this.date, required this.value});
  

@override final  LocalDate date;
@override final  double value;

/// Create a copy of CrossMetricValue
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CrossMetricValueCopyWith<_CrossMetricValue> get copyWith => __$CrossMetricValueCopyWithImpl<_CrossMetricValue>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CrossMetricValue&&(identical(other.date, date) || other.date == date)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,date,value);

@override
String toString() {
  return 'CrossMetricValue(date: $date, value: $value)';
}


}

/// @nodoc
abstract mixin class _$CrossMetricValueCopyWith<$Res> implements $CrossMetricValueCopyWith<$Res> {
  factory _$CrossMetricValueCopyWith(_CrossMetricValue value, $Res Function(_CrossMetricValue) _then) = __$CrossMetricValueCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, double value
});




}
/// @nodoc
class __$CrossMetricValueCopyWithImpl<$Res>
    implements _$CrossMetricValueCopyWith<$Res> {
  __$CrossMetricValueCopyWithImpl(this._self, this._then);

  final _CrossMetricValue _self;
  final $Res Function(_CrossMetricValue) _then;

/// Create a copy of CrossMetricValue
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? value = null,}) {
  return _then(_CrossMetricValue(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$CrossMetricInsight {

 double get correlation; int get pairedDays;
/// Create a copy of CrossMetricInsight
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CrossMetricInsightCopyWith<CrossMetricInsight> get copyWith => _$CrossMetricInsightCopyWithImpl<CrossMetricInsight>(this as CrossMetricInsight, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CrossMetricInsight&&(identical(other.correlation, correlation) || other.correlation == correlation)&&(identical(other.pairedDays, pairedDays) || other.pairedDays == pairedDays));
}


@override
int get hashCode => Object.hash(runtimeType,correlation,pairedDays);

@override
String toString() {
  return 'CrossMetricInsight(correlation: $correlation, pairedDays: $pairedDays)';
}


}

/// @nodoc
abstract mixin class $CrossMetricInsightCopyWith<$Res>  {
  factory $CrossMetricInsightCopyWith(CrossMetricInsight value, $Res Function(CrossMetricInsight) _then) = _$CrossMetricInsightCopyWithImpl;
@useResult
$Res call({
 double correlation, int pairedDays
});




}
/// @nodoc
class _$CrossMetricInsightCopyWithImpl<$Res>
    implements $CrossMetricInsightCopyWith<$Res> {
  _$CrossMetricInsightCopyWithImpl(this._self, this._then);

  final CrossMetricInsight _self;
  final $Res Function(CrossMetricInsight) _then;

/// Create a copy of CrossMetricInsight
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? correlation = null,Object? pairedDays = null,}) {
  return _then(_self.copyWith(
correlation: null == correlation ? _self.correlation : correlation // ignore: cast_nullable_to_non_nullable
as double,pairedDays: null == pairedDays ? _self.pairedDays : pairedDays // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [CrossMetricInsight].
extension CrossMetricInsightPatterns on CrossMetricInsight {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CrossMetricInsight value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CrossMetricInsight() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CrossMetricInsight value)  $default,){
final _that = this;
switch (_that) {
case _CrossMetricInsight():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CrossMetricInsight value)?  $default,){
final _that = this;
switch (_that) {
case _CrossMetricInsight() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double correlation,  int pairedDays)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CrossMetricInsight() when $default != null:
return $default(_that.correlation,_that.pairedDays);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double correlation,  int pairedDays)  $default,) {final _that = this;
switch (_that) {
case _CrossMetricInsight():
return $default(_that.correlation,_that.pairedDays);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double correlation,  int pairedDays)?  $default,) {final _that = this;
switch (_that) {
case _CrossMetricInsight() when $default != null:
return $default(_that.correlation,_that.pairedDays);case _:
  return null;

}
}

}

/// @nodoc


class _CrossMetricInsight extends CrossMetricInsight {
  const _CrossMetricInsight({required this.correlation, required this.pairedDays}): super._();
  

@override final  double correlation;
@override final  int pairedDays;

/// Create a copy of CrossMetricInsight
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CrossMetricInsightCopyWith<_CrossMetricInsight> get copyWith => __$CrossMetricInsightCopyWithImpl<_CrossMetricInsight>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CrossMetricInsight&&(identical(other.correlation, correlation) || other.correlation == correlation)&&(identical(other.pairedDays, pairedDays) || other.pairedDays == pairedDays));
}


@override
int get hashCode => Object.hash(runtimeType,correlation,pairedDays);

@override
String toString() {
  return 'CrossMetricInsight(correlation: $correlation, pairedDays: $pairedDays)';
}


}

/// @nodoc
abstract mixin class _$CrossMetricInsightCopyWith<$Res> implements $CrossMetricInsightCopyWith<$Res> {
  factory _$CrossMetricInsightCopyWith(_CrossMetricInsight value, $Res Function(_CrossMetricInsight) _then) = __$CrossMetricInsightCopyWithImpl;
@override @useResult
$Res call({
 double correlation, int pairedDays
});




}
/// @nodoc
class __$CrossMetricInsightCopyWithImpl<$Res>
    implements _$CrossMetricInsightCopyWith<$Res> {
  __$CrossMetricInsightCopyWithImpl(this._self, this._then);

  final _CrossMetricInsight _self;
  final $Res Function(_CrossMetricInsight) _then;

/// Create a copy of CrossMetricInsight
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? correlation = null,Object? pairedDays = null,}) {
  return _then(_CrossMetricInsight(
correlation: null == correlation ? _self.correlation : correlation // ignore: cast_nullable_to_non_nullable
as double,pairedDays: null == pairedDays ? _self.pairedDays : pairedDays // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

// dart format on
