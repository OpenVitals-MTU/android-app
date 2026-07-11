// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'daily_goals.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$DailyGoalValue {

 LocalDate get date; double get value;
/// Create a copy of DailyGoalValue
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyGoalValueCopyWith<DailyGoalValue> get copyWith => _$DailyGoalValueCopyWithImpl<DailyGoalValue>(this as DailyGoalValue, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyGoalValue&&(identical(other.date, date) || other.date == date)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,date,value);

@override
String toString() {
  return 'DailyGoalValue(date: $date, value: $value)';
}


}

/// @nodoc
abstract mixin class $DailyGoalValueCopyWith<$Res>  {
  factory $DailyGoalValueCopyWith(DailyGoalValue value, $Res Function(DailyGoalValue) _then) = _$DailyGoalValueCopyWithImpl;
@useResult
$Res call({
 LocalDate date, double value
});




}
/// @nodoc
class _$DailyGoalValueCopyWithImpl<$Res>
    implements $DailyGoalValueCopyWith<$Res> {
  _$DailyGoalValueCopyWithImpl(this._self, this._then);

  final DailyGoalValue _self;
  final $Res Function(DailyGoalValue) _then;

/// Create a copy of DailyGoalValue
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? value = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyGoalValue].
extension DailyGoalValuePatterns on DailyGoalValue {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyGoalValue value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyGoalValue() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyGoalValue value)  $default,){
final _that = this;
switch (_that) {
case _DailyGoalValue():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyGoalValue value)?  $default,){
final _that = this;
switch (_that) {
case _DailyGoalValue() when $default != null:
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
case _DailyGoalValue() when $default != null:
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
case _DailyGoalValue():
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
case _DailyGoalValue() when $default != null:
return $default(_that.date,_that.value);case _:
  return null;

}
}

}

/// @nodoc


class _DailyGoalValue implements DailyGoalValue {
  const _DailyGoalValue({required this.date, required this.value});
  

@override final  LocalDate date;
@override final  double value;

/// Create a copy of DailyGoalValue
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyGoalValueCopyWith<_DailyGoalValue> get copyWith => __$DailyGoalValueCopyWithImpl<_DailyGoalValue>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyGoalValue&&(identical(other.date, date) || other.date == date)&&(identical(other.value, value) || other.value == value));
}


@override
int get hashCode => Object.hash(runtimeType,date,value);

@override
String toString() {
  return 'DailyGoalValue(date: $date, value: $value)';
}


}

/// @nodoc
abstract mixin class _$DailyGoalValueCopyWith<$Res> implements $DailyGoalValueCopyWith<$Res> {
  factory _$DailyGoalValueCopyWith(_DailyGoalValue value, $Res Function(_DailyGoalValue) _then) = __$DailyGoalValueCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, double value
});




}
/// @nodoc
class __$DailyGoalValueCopyWithImpl<$Res>
    implements _$DailyGoalValueCopyWith<$Res> {
  __$DailyGoalValueCopyWithImpl(this._self, this._then);

  final _DailyGoalValue _self;
  final $Res Function(_DailyGoalValue) _then;

/// Create a copy of DailyGoalValue
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? value = null,}) {
  return _then(_DailyGoalValue(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$DailyGoalDay {

 LocalDate get date; double get value; bool get isTracked; bool get isMet;
/// Create a copy of DailyGoalDay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyGoalDayCopyWith<DailyGoalDay> get copyWith => _$DailyGoalDayCopyWithImpl<DailyGoalDay>(this as DailyGoalDay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyGoalDay&&(identical(other.date, date) || other.date == date)&&(identical(other.value, value) || other.value == value)&&(identical(other.isTracked, isTracked) || other.isTracked == isTracked)&&(identical(other.isMet, isMet) || other.isMet == isMet));
}


@override
int get hashCode => Object.hash(runtimeType,date,value,isTracked,isMet);

@override
String toString() {
  return 'DailyGoalDay(date: $date, value: $value, isTracked: $isTracked, isMet: $isMet)';
}


}

/// @nodoc
abstract mixin class $DailyGoalDayCopyWith<$Res>  {
  factory $DailyGoalDayCopyWith(DailyGoalDay value, $Res Function(DailyGoalDay) _then) = _$DailyGoalDayCopyWithImpl;
@useResult
$Res call({
 LocalDate date, double value, bool isTracked, bool isMet
});




}
/// @nodoc
class _$DailyGoalDayCopyWithImpl<$Res>
    implements $DailyGoalDayCopyWith<$Res> {
  _$DailyGoalDayCopyWithImpl(this._self, this._then);

  final DailyGoalDay _self;
  final $Res Function(DailyGoalDay) _then;

/// Create a copy of DailyGoalDay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? value = null,Object? isTracked = null,Object? isMet = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,isTracked: null == isTracked ? _self.isTracked : isTracked // ignore: cast_nullable_to_non_nullable
as bool,isMet: null == isMet ? _self.isMet : isMet // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyGoalDay].
extension DailyGoalDayPatterns on DailyGoalDay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyGoalDay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyGoalDay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyGoalDay value)  $default,){
final _that = this;
switch (_that) {
case _DailyGoalDay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyGoalDay value)?  $default,){
final _that = this;
switch (_that) {
case _DailyGoalDay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  double value,  bool isTracked,  bool isMet)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyGoalDay() when $default != null:
return $default(_that.date,_that.value,_that.isTracked,_that.isMet);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  double value,  bool isTracked,  bool isMet)  $default,) {final _that = this;
switch (_that) {
case _DailyGoalDay():
return $default(_that.date,_that.value,_that.isTracked,_that.isMet);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  double value,  bool isTracked,  bool isMet)?  $default,) {final _that = this;
switch (_that) {
case _DailyGoalDay() when $default != null:
return $default(_that.date,_that.value,_that.isTracked,_that.isMet);case _:
  return null;

}
}

}

/// @nodoc


class _DailyGoalDay implements DailyGoalDay {
  const _DailyGoalDay({required this.date, required this.value, required this.isTracked, required this.isMet});
  

@override final  LocalDate date;
@override final  double value;
@override final  bool isTracked;
@override final  bool isMet;

/// Create a copy of DailyGoalDay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyGoalDayCopyWith<_DailyGoalDay> get copyWith => __$DailyGoalDayCopyWithImpl<_DailyGoalDay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyGoalDay&&(identical(other.date, date) || other.date == date)&&(identical(other.value, value) || other.value == value)&&(identical(other.isTracked, isTracked) || other.isTracked == isTracked)&&(identical(other.isMet, isMet) || other.isMet == isMet));
}


@override
int get hashCode => Object.hash(runtimeType,date,value,isTracked,isMet);

@override
String toString() {
  return 'DailyGoalDay(date: $date, value: $value, isTracked: $isTracked, isMet: $isMet)';
}


}

/// @nodoc
abstract mixin class _$DailyGoalDayCopyWith<$Res> implements $DailyGoalDayCopyWith<$Res> {
  factory _$DailyGoalDayCopyWith(_DailyGoalDay value, $Res Function(_DailyGoalDay) _then) = __$DailyGoalDayCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, double value, bool isTracked, bool isMet
});




}
/// @nodoc
class __$DailyGoalDayCopyWithImpl<$Res>
    implements _$DailyGoalDayCopyWith<$Res> {
  __$DailyGoalDayCopyWithImpl(this._self, this._then);

  final _DailyGoalDay _self;
  final $Res Function(_DailyGoalDay) _then;

/// Create a copy of DailyGoalDay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? value = null,Object? isTracked = null,Object? isMet = null,}) {
  return _then(_DailyGoalDay(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,value: null == value ? _self.value : value // ignore: cast_nullable_to_non_nullable
as double,isTracked: null == isTracked ? _self.isTracked : isTracked // ignore: cast_nullable_to_non_nullable
as bool,isMet: null == isMet ? _self.isMet : isMet // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$DailyGoalProgress {

 double get target; DailyGoalDirection get direction; List<DailyGoalDay> get days;
/// Create a copy of DailyGoalProgress
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyGoalProgressCopyWith<DailyGoalProgress> get copyWith => _$DailyGoalProgressCopyWithImpl<DailyGoalProgress>(this as DailyGoalProgress, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyGoalProgress&&(identical(other.target, target) || other.target == target)&&(identical(other.direction, direction) || other.direction == direction)&&const DeepCollectionEquality().equals(other.days, days));
}


@override
int get hashCode => Object.hash(runtimeType,target,direction,const DeepCollectionEquality().hash(days));

@override
String toString() {
  return 'DailyGoalProgress(target: $target, direction: $direction, days: $days)';
}


}

/// @nodoc
abstract mixin class $DailyGoalProgressCopyWith<$Res>  {
  factory $DailyGoalProgressCopyWith(DailyGoalProgress value, $Res Function(DailyGoalProgress) _then) = _$DailyGoalProgressCopyWithImpl;
@useResult
$Res call({
 double target, DailyGoalDirection direction, List<DailyGoalDay> days
});




}
/// @nodoc
class _$DailyGoalProgressCopyWithImpl<$Res>
    implements $DailyGoalProgressCopyWith<$Res> {
  _$DailyGoalProgressCopyWithImpl(this._self, this._then);

  final DailyGoalProgress _self;
  final $Res Function(DailyGoalProgress) _then;

/// Create a copy of DailyGoalProgress
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? target = null,Object? direction = null,Object? days = null,}) {
  return _then(_self.copyWith(
target: null == target ? _self.target : target // ignore: cast_nullable_to_non_nullable
as double,direction: null == direction ? _self.direction : direction // ignore: cast_nullable_to_non_nullable
as DailyGoalDirection,days: null == days ? _self.days : days // ignore: cast_nullable_to_non_nullable
as List<DailyGoalDay>,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyGoalProgress].
extension DailyGoalProgressPatterns on DailyGoalProgress {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyGoalProgress value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyGoalProgress() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyGoalProgress value)  $default,){
final _that = this;
switch (_that) {
case _DailyGoalProgress():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyGoalProgress value)?  $default,){
final _that = this;
switch (_that) {
case _DailyGoalProgress() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double target,  DailyGoalDirection direction,  List<DailyGoalDay> days)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyGoalProgress() when $default != null:
return $default(_that.target,_that.direction,_that.days);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double target,  DailyGoalDirection direction,  List<DailyGoalDay> days)  $default,) {final _that = this;
switch (_that) {
case _DailyGoalProgress():
return $default(_that.target,_that.direction,_that.days);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double target,  DailyGoalDirection direction,  List<DailyGoalDay> days)?  $default,) {final _that = this;
switch (_that) {
case _DailyGoalProgress() when $default != null:
return $default(_that.target,_that.direction,_that.days);case _:
  return null;

}
}

}

/// @nodoc


class _DailyGoalProgress extends DailyGoalProgress {
  const _DailyGoalProgress({required this.target, required this.direction, required final  List<DailyGoalDay> days}): _days = days,super._();
  

@override final  double target;
@override final  DailyGoalDirection direction;
 final  List<DailyGoalDay> _days;
@override List<DailyGoalDay> get days {
  if (_days is EqualUnmodifiableListView) return _days;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_days);
}


/// Create a copy of DailyGoalProgress
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyGoalProgressCopyWith<_DailyGoalProgress> get copyWith => __$DailyGoalProgressCopyWithImpl<_DailyGoalProgress>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyGoalProgress&&(identical(other.target, target) || other.target == target)&&(identical(other.direction, direction) || other.direction == direction)&&const DeepCollectionEquality().equals(other._days, _days));
}


@override
int get hashCode => Object.hash(runtimeType,target,direction,const DeepCollectionEquality().hash(_days));

@override
String toString() {
  return 'DailyGoalProgress(target: $target, direction: $direction, days: $days)';
}


}

/// @nodoc
abstract mixin class _$DailyGoalProgressCopyWith<$Res> implements $DailyGoalProgressCopyWith<$Res> {
  factory _$DailyGoalProgressCopyWith(_DailyGoalProgress value, $Res Function(_DailyGoalProgress) _then) = __$DailyGoalProgressCopyWithImpl;
@override @useResult
$Res call({
 double target, DailyGoalDirection direction, List<DailyGoalDay> days
});




}
/// @nodoc
class __$DailyGoalProgressCopyWithImpl<$Res>
    implements _$DailyGoalProgressCopyWith<$Res> {
  __$DailyGoalProgressCopyWithImpl(this._self, this._then);

  final _DailyGoalProgress _self;
  final $Res Function(_DailyGoalProgress) _then;

/// Create a copy of DailyGoalProgress
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? target = null,Object? direction = null,Object? days = null,}) {
  return _then(_DailyGoalProgress(
target: null == target ? _self.target : target // ignore: cast_nullable_to_non_nullable
as double,direction: null == direction ? _self.direction : direction // ignore: cast_nullable_to_non_nullable
as DailyGoalDirection,days: null == days ? _self._days : days // ignore: cast_nullable_to_non_nullable
as List<DailyGoalDay>,
  ));
}


}

// dart format on
