// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'personal_baseline.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BaselineValue {

 LocalDate get date; double get value;
/// Create a copy of BaselineValue
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BaselineValueCopyWith<BaselineValue> get copyWith => _$BaselineValueCopyWithImpl<BaselineValue>(this as BaselineValue, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BaselineValue&&(identical(other.date, date) || other.date == date)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,date,value);

@override
String toString() {
  return 'BaselineValue(date: $date, value: $value)';
}


}

/// @nodoc
abstract mixin class $BaselineValueCopyWith<$Res>  {
  factory $BaselineValueCopyWith(BaselineValue value, $Res Function(BaselineValue) _then) = _$BaselineValueCopyWithImpl;
@useResult
$Res call({
 LocalDate date, double value
});




}
/// @nodoc
class _$BaselineValueCopyWithImpl<$Res>
    implements $BaselineValueCopyWith<$Res> {
  _$BaselineValueCopyWithImpl(this._self, this._then);

  final BaselineValue _self;
  final $Res Function(BaselineValue) _then;

/// Create a copy of BaselineValue
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? value = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [BaselineValue].
extension BaselineValuePatterns on BaselineValue {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BaselineValue value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BaselineValue() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BaselineValue value)  $default,){
final _that = this;
switch (_that) {
case _BaselineValue():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BaselineValue value)?  $default,){
final _that = this;
switch (_that) {
case _BaselineValue() when $default != null:
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
case _BaselineValue() when $default != null:
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
case _BaselineValue():
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
case _BaselineValue() when $default != null:
return $default(_that.date,_that.value);case _:
  return null;

}
}

}

/// @nodoc


class _BaselineValue implements BaselineValue {
  const _BaselineValue({required this.date, required this.value});
  

@override final  LocalDate date;
@override final  double value;

/// Create a copy of BaselineValue
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BaselineValueCopyWith<_BaselineValue> get copyWith => __$BaselineValueCopyWithImpl<_BaselineValue>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BaselineValue&&(identical(other.date, date) || other.date == date)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,date,value);

@override
String toString() {
  return 'BaselineValue(date: $date, value: $value)';
}


}

/// @nodoc
abstract mixin class _$BaselineValueCopyWith<$Res> implements $BaselineValueCopyWith<$Res> {
  factory _$BaselineValueCopyWith(_BaselineValue value, $Res Function(_BaselineValue) _then) = __$BaselineValueCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, double value
});




}
/// @nodoc
class __$BaselineValueCopyWithImpl<$Res>
    implements _$BaselineValueCopyWith<$Res> {
  __$BaselineValueCopyWithImpl(this._self, this._then);

  final _BaselineValue _self;
  final $Res Function(_BaselineValue) _then;

/// Create a copy of BaselineValue
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? value = null,}) {
  return _then(_BaselineValue(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$BaselineSummary {

 int get windowDays; double get average; double get standardDeviation; int get sampleCount;
/// Create a copy of BaselineSummary
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BaselineSummaryCopyWith<BaselineSummary> get copyWith => _$BaselineSummaryCopyWithImpl<BaselineSummary>(this as BaselineSummary, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BaselineSummary&&(identical(other.windowDays, windowDays) || other.windowDays == windowDays)&&(identical(other.average, average) || other.average == average)&&(identical(other.standardDeviation, standardDeviation) || other.standardDeviation == standardDeviation)&&(identical(other.sampleCount, sampleCount) || other.sampleCount == sampleCount));
}


@override
int get hashCode => Object.hash(runtimeType,windowDays,average,standardDeviation,sampleCount);

@override
String toString() {
  return 'BaselineSummary(windowDays: $windowDays, average: $average, standardDeviation: $standardDeviation, sampleCount: $sampleCount)';
}


}

/// @nodoc
abstract mixin class $BaselineSummaryCopyWith<$Res>  {
  factory $BaselineSummaryCopyWith(BaselineSummary value, $Res Function(BaselineSummary) _then) = _$BaselineSummaryCopyWithImpl;
@useResult
$Res call({
 int windowDays, double average, double standardDeviation, int sampleCount
});




}
/// @nodoc
class _$BaselineSummaryCopyWithImpl<$Res>
    implements $BaselineSummaryCopyWith<$Res> {
  _$BaselineSummaryCopyWithImpl(this._self, this._then);

  final BaselineSummary _self;
  final $Res Function(BaselineSummary) _then;

/// Create a copy of BaselineSummary
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? windowDays = null,Object? average = null,Object? standardDeviation = null,Object? sampleCount = null,}) {
  return _then(_self.copyWith(
windowDays: null == windowDays ? _self.windowDays : windowDays // ignore: cast_nullable_to_non_nullable
as int,average: null == average ? _self.average : average // ignore: cast_nullable_to_non_nullable
as double,standardDeviation: null == standardDeviation ? _self.standardDeviation : standardDeviation // ignore: cast_nullable_to_non_nullable
as double,sampleCount: null == sampleCount ? _self.sampleCount : sampleCount // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [BaselineSummary].
extension BaselineSummaryPatterns on BaselineSummary {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BaselineSummary value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BaselineSummary() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BaselineSummary value)  $default,){
final _that = this;
switch (_that) {
case _BaselineSummary():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BaselineSummary value)?  $default,){
final _that = this;
switch (_that) {
case _BaselineSummary() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int windowDays,  double average,  double standardDeviation,  int sampleCount)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BaselineSummary() when $default != null:
return $default(_that.windowDays,_that.average,_that.standardDeviation,_that.sampleCount);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int windowDays,  double average,  double standardDeviation,  int sampleCount)  $default,) {final _that = this;
switch (_that) {
case _BaselineSummary():
return $default(_that.windowDays,_that.average,_that.standardDeviation,_that.sampleCount);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int windowDays,  double average,  double standardDeviation,  int sampleCount)?  $default,) {final _that = this;
switch (_that) {
case _BaselineSummary() when $default != null:
return $default(_that.windowDays,_that.average,_that.standardDeviation,_that.sampleCount);case _:
  return null;

}
}

}

/// @nodoc


class _BaselineSummary extends BaselineSummary {
  const _BaselineSummary({required this.windowDays, required this.average, required this.standardDeviation, required this.sampleCount}): super._();
  

@override final  int windowDays;
@override final  double average;
@override final  double standardDeviation;
@override final  int sampleCount;

/// Create a copy of BaselineSummary
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BaselineSummaryCopyWith<_BaselineSummary> get copyWith => __$BaselineSummaryCopyWithImpl<_BaselineSummary>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BaselineSummary&&(identical(other.windowDays, windowDays) || other.windowDays == windowDays)&&(identical(other.average, average) || other.average == average)&&(identical(other.standardDeviation, standardDeviation) || other.standardDeviation == standardDeviation)&&(identical(other.sampleCount, sampleCount) || other.sampleCount == sampleCount));
}


@override
int get hashCode => Object.hash(runtimeType,windowDays,average,standardDeviation,sampleCount);

@override
String toString() {
  return 'BaselineSummary(windowDays: $windowDays, average: $average, standardDeviation: $standardDeviation, sampleCount: $sampleCount)';
}


}

/// @nodoc
abstract mixin class _$BaselineSummaryCopyWith<$Res> implements $BaselineSummaryCopyWith<$Res> {
  factory _$BaselineSummaryCopyWith(_BaselineSummary value, $Res Function(_BaselineSummary) _then) = __$BaselineSummaryCopyWithImpl;
@override @useResult
$Res call({
 int windowDays, double average, double standardDeviation, int sampleCount
});




}
/// @nodoc
class __$BaselineSummaryCopyWithImpl<$Res>
    implements _$BaselineSummaryCopyWith<$Res> {
  __$BaselineSummaryCopyWithImpl(this._self, this._then);

  final _BaselineSummary _self;
  final $Res Function(_BaselineSummary) _then;

/// Create a copy of BaselineSummary
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? windowDays = null,Object? average = null,Object? standardDeviation = null,Object? sampleCount = null,}) {
  return _then(_BaselineSummary(
windowDays: null == windowDays ? _self.windowDays : windowDays // ignore: cast_nullable_to_non_nullable
as int,average: null == average ? _self.average : average // ignore: cast_nullable_to_non_nullable
as double,standardDeviation: null == standardDeviation ? _self.standardDeviation : standardDeviation // ignore: cast_nullable_to_non_nullable
as double,sampleCount: null == sampleCount ? _self.sampleCount : sampleCount // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$PersonalBaselineInsight {

 double get currentValue; BaselineSummary get primarySummary; List<BaselineSummary> get summaries; BaselineStatus get status;
/// Create a copy of PersonalBaselineInsight
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$PersonalBaselineInsightCopyWith<PersonalBaselineInsight> get copyWith => _$PersonalBaselineInsightCopyWithImpl<PersonalBaselineInsight>(this as PersonalBaselineInsight, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is PersonalBaselineInsight&&(identical(other.currentValue, currentValue) || other.currentValue == currentValue)&&(identical(other.primarySummary, primarySummary) || other.primarySummary == primarySummary)&&const DeepCollectionEquality().equals(other.summaries, summaries)&&(identical(other.status, status) || other.status == status));
}


@override
int get hashCode => Object.hash(runtimeType,currentValue,primarySummary,const DeepCollectionEquality().hash(summaries),status);

@override
String toString() {
  return 'PersonalBaselineInsight(currentValue: $currentValue, primarySummary: $primarySummary, summaries: $summaries, status: $status)';
}


}

/// @nodoc
abstract mixin class $PersonalBaselineInsightCopyWith<$Res>  {
  factory $PersonalBaselineInsightCopyWith(PersonalBaselineInsight value, $Res Function(PersonalBaselineInsight) _then) = _$PersonalBaselineInsightCopyWithImpl;
@useResult
$Res call({
 double currentValue, BaselineSummary primarySummary, List<BaselineSummary> summaries, BaselineStatus status
});


$BaselineSummaryCopyWith<$Res> get primarySummary;

}
/// @nodoc
class _$PersonalBaselineInsightCopyWithImpl<$Res>
    implements $PersonalBaselineInsightCopyWith<$Res> {
  _$PersonalBaselineInsightCopyWithImpl(this._self, this._then);

  final PersonalBaselineInsight _self;
  final $Res Function(PersonalBaselineInsight) _then;

/// Create a copy of PersonalBaselineInsight
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? currentValue = null,Object? primarySummary = null,Object? summaries = null,Object? status = null,}) {
  return _then(_self.copyWith(
currentValue: null == currentValue ? _self.currentValue : currentValue // ignore: cast_nullable_to_non_nullable
as double,primarySummary: null == primarySummary ? _self.primarySummary : primarySummary // ignore: cast_nullable_to_non_nullable
as BaselineSummary,summaries: null == summaries ? _self.summaries : summaries // ignore: cast_nullable_to_non_nullable
as List<BaselineSummary>,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as BaselineStatus,
  ));
}
/// Create a copy of PersonalBaselineInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BaselineSummaryCopyWith<$Res> get primarySummary {
  
  return $BaselineSummaryCopyWith<$Res>(_self.primarySummary, (value) {
    return _then(_self.copyWith(primarySummary: value));
  });
}
}


/// Adds pattern-matching-related methods to [PersonalBaselineInsight].
extension PersonalBaselineInsightPatterns on PersonalBaselineInsight {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _PersonalBaselineInsight value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _PersonalBaselineInsight() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _PersonalBaselineInsight value)  $default,){
final _that = this;
switch (_that) {
case _PersonalBaselineInsight():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _PersonalBaselineInsight value)?  $default,){
final _that = this;
switch (_that) {
case _PersonalBaselineInsight() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double currentValue,  BaselineSummary primarySummary,  List<BaselineSummary> summaries,  BaselineStatus status)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _PersonalBaselineInsight() when $default != null:
return $default(_that.currentValue,_that.primarySummary,_that.summaries,_that.status);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double currentValue,  BaselineSummary primarySummary,  List<BaselineSummary> summaries,  BaselineStatus status)  $default,) {final _that = this;
switch (_that) {
case _PersonalBaselineInsight():
return $default(_that.currentValue,_that.primarySummary,_that.summaries,_that.status);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double currentValue,  BaselineSummary primarySummary,  List<BaselineSummary> summaries,  BaselineStatus status)?  $default,) {final _that = this;
switch (_that) {
case _PersonalBaselineInsight() when $default != null:
return $default(_that.currentValue,_that.primarySummary,_that.summaries,_that.status);case _:
  return null;

}
}

}

/// @nodoc


class _PersonalBaselineInsight extends PersonalBaselineInsight {
  const _PersonalBaselineInsight({required this.currentValue, required this.primarySummary, required final  List<BaselineSummary> summaries, required this.status}): _summaries = summaries,super._();
  

@override final  double currentValue;
@override final  BaselineSummary primarySummary;
 final  List<BaselineSummary> _summaries;
@override List<BaselineSummary> get summaries {
  if (_summaries is EqualUnmodifiableListView) return _summaries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_summaries);
}

@override final  BaselineStatus status;

/// Create a copy of PersonalBaselineInsight
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$PersonalBaselineInsightCopyWith<_PersonalBaselineInsight> get copyWith => __$PersonalBaselineInsightCopyWithImpl<_PersonalBaselineInsight>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _PersonalBaselineInsight&&(identical(other.currentValue, currentValue) || other.currentValue == currentValue)&&(identical(other.primarySummary, primarySummary) || other.primarySummary == primarySummary)&&const DeepCollectionEquality().equals(other._summaries, _summaries)&&(identical(other.status, status) || other.status == status));
}


@override
int get hashCode => Object.hash(runtimeType,currentValue,primarySummary,const DeepCollectionEquality().hash(_summaries),status);

@override
String toString() {
  return 'PersonalBaselineInsight(currentValue: $currentValue, primarySummary: $primarySummary, summaries: $summaries, status: $status)';
}


}

/// @nodoc
abstract mixin class _$PersonalBaselineInsightCopyWith<$Res> implements $PersonalBaselineInsightCopyWith<$Res> {
  factory _$PersonalBaselineInsightCopyWith(_PersonalBaselineInsight value, $Res Function(_PersonalBaselineInsight) _then) = __$PersonalBaselineInsightCopyWithImpl;
@override @useResult
$Res call({
 double currentValue, BaselineSummary primarySummary, List<BaselineSummary> summaries, BaselineStatus status
});


@override $BaselineSummaryCopyWith<$Res> get primarySummary;

}
/// @nodoc
class __$PersonalBaselineInsightCopyWithImpl<$Res>
    implements _$PersonalBaselineInsightCopyWith<$Res> {
  __$PersonalBaselineInsightCopyWithImpl(this._self, this._then);

  final _PersonalBaselineInsight _self;
  final $Res Function(_PersonalBaselineInsight) _then;

/// Create a copy of PersonalBaselineInsight
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? currentValue = null,Object? primarySummary = null,Object? summaries = null,Object? status = null,}) {
  return _then(_PersonalBaselineInsight(
currentValue: null == currentValue ? _self.currentValue : currentValue // ignore: cast_nullable_to_non_nullable
as double,primarySummary: null == primarySummary ? _self.primarySummary : primarySummary // ignore: cast_nullable_to_non_nullable
as BaselineSummary,summaries: null == summaries ? _self._summaries : summaries // ignore: cast_nullable_to_non_nullable
as List<BaselineSummary>,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as BaselineStatus,
  ));
}

/// Create a copy of PersonalBaselineInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BaselineSummaryCopyWith<$Res> get primarySummary {
  
  return $BaselineSummaryCopyWith<$Res>(_self.primarySummary, (value) {
    return _then(_self.copyWith(primarySummary: value));
  });
}
}

// dart format on
