// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'caffeine_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CaffeineDisplay {

 CaffeineHomeDisplay get home; CaffeineAnalyticsDisplay get analytics;
/// Create a copy of CaffeineDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineDisplayCopyWith<CaffeineDisplay> get copyWith => _$CaffeineDisplayCopyWithImpl<CaffeineDisplay>(this as CaffeineDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineDisplay&&(identical(other.home, home) || other.home == home)&&(identical(other.analytics, analytics) || other.analytics == analytics));
}


@override
int get hashCode => Object.hash(runtimeType,home,analytics);

@override
String toString() {
  return 'CaffeineDisplay(home: $home, analytics: $analytics)';
}


}

/// @nodoc
abstract mixin class $CaffeineDisplayCopyWith<$Res>  {
  factory $CaffeineDisplayCopyWith(CaffeineDisplay value, $Res Function(CaffeineDisplay) _then) = _$CaffeineDisplayCopyWithImpl;
@useResult
$Res call({
 CaffeineHomeDisplay home, CaffeineAnalyticsDisplay analytics
});


$CaffeineHomeDisplayCopyWith<$Res> get home;$CaffeineAnalyticsDisplayCopyWith<$Res> get analytics;

}
/// @nodoc
class _$CaffeineDisplayCopyWithImpl<$Res>
    implements $CaffeineDisplayCopyWith<$Res> {
  _$CaffeineDisplayCopyWithImpl(this._self, this._then);

  final CaffeineDisplay _self;
  final $Res Function(CaffeineDisplay) _then;

/// Create a copy of CaffeineDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? home = null,Object? analytics = null,}) {
  return _then(_self.copyWith(
home: null == home ? _self.home : home // ignore: cast_nullable_to_non_nullable
as CaffeineHomeDisplay,analytics: null == analytics ? _self.analytics : analytics // ignore: cast_nullable_to_non_nullable
as CaffeineAnalyticsDisplay,
  ));
}
/// Create a copy of CaffeineDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineHomeDisplayCopyWith<$Res> get home {
  
  return $CaffeineHomeDisplayCopyWith<$Res>(_self.home, (value) {
    return _then(_self.copyWith(home: value));
  });
}/// Create a copy of CaffeineDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineAnalyticsDisplayCopyWith<$Res> get analytics {
  
  return $CaffeineAnalyticsDisplayCopyWith<$Res>(_self.analytics, (value) {
    return _then(_self.copyWith(analytics: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaffeineDisplay].
extension CaffeineDisplayPatterns on CaffeineDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineDisplay value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaffeineHomeDisplay home,  CaffeineAnalyticsDisplay analytics)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineDisplay() when $default != null:
return $default(_that.home,_that.analytics);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaffeineHomeDisplay home,  CaffeineAnalyticsDisplay analytics)  $default,) {final _that = this;
switch (_that) {
case _CaffeineDisplay():
return $default(_that.home,_that.analytics);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaffeineHomeDisplay home,  CaffeineAnalyticsDisplay analytics)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineDisplay() when $default != null:
return $default(_that.home,_that.analytics);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineDisplay implements CaffeineDisplay {
  const _CaffeineDisplay({this.home = const CaffeineHomeDisplay(), this.analytics = const CaffeineAnalyticsDisplay()});
  

@override@JsonKey() final  CaffeineHomeDisplay home;
@override@JsonKey() final  CaffeineAnalyticsDisplay analytics;

/// Create a copy of CaffeineDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineDisplayCopyWith<_CaffeineDisplay> get copyWith => __$CaffeineDisplayCopyWithImpl<_CaffeineDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineDisplay&&(identical(other.home, home) || other.home == home)&&(identical(other.analytics, analytics) || other.analytics == analytics));
}


@override
int get hashCode => Object.hash(runtimeType,home,analytics);

@override
String toString() {
  return 'CaffeineDisplay(home: $home, analytics: $analytics)';
}


}

/// @nodoc
abstract mixin class _$CaffeineDisplayCopyWith<$Res> implements $CaffeineDisplayCopyWith<$Res> {
  factory _$CaffeineDisplayCopyWith(_CaffeineDisplay value, $Res Function(_CaffeineDisplay) _then) = __$CaffeineDisplayCopyWithImpl;
@override @useResult
$Res call({
 CaffeineHomeDisplay home, CaffeineAnalyticsDisplay analytics
});


@override $CaffeineHomeDisplayCopyWith<$Res> get home;@override $CaffeineAnalyticsDisplayCopyWith<$Res> get analytics;

}
/// @nodoc
class __$CaffeineDisplayCopyWithImpl<$Res>
    implements _$CaffeineDisplayCopyWith<$Res> {
  __$CaffeineDisplayCopyWithImpl(this._self, this._then);

  final _CaffeineDisplay _self;
  final $Res Function(_CaffeineDisplay) _then;

/// Create a copy of CaffeineDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? home = null,Object? analytics = null,}) {
  return _then(_CaffeineDisplay(
home: null == home ? _self.home : home // ignore: cast_nullable_to_non_nullable
as CaffeineHomeDisplay,analytics: null == analytics ? _self.analytics : analytics // ignore: cast_nullable_to_non_nullable
as CaffeineAnalyticsDisplay,
  ));
}

/// Create a copy of CaffeineDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineHomeDisplayCopyWith<$Res> get home {
  
  return $CaffeineHomeDisplayCopyWith<$Res>(_self.home, (value) {
    return _then(_self.copyWith(home: value));
  });
}/// Create a copy of CaffeineDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineAnalyticsDisplayCopyWith<$Res> get analytics {
  
  return $CaffeineAnalyticsDisplayCopyWith<$Res>(_self.analytics, (value) {
    return _then(_self.copyWith(analytics: value));
  });
}
}

/// @nodoc
mixin _$CaffeineHomeDisplay {

 CaffeineInsights get insights; CaffeineSleepImpactStatus get sleepImpactStatus;/// Kotlin's bedtime card colours on this: is the projection under the line?
 bool get bedtimeIsSafe;/// When each logged drink lands on the curve, for the entry markers.
 List<DateTime> get curveEntryTimes;/// The curve's y-axis maximum: the tallest of the threshold and the points,
/// floored at 1.0 so an empty day divides by something.
 double get curveMaxMg;
/// Create a copy of CaffeineHomeDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineHomeDisplayCopyWith<CaffeineHomeDisplay> get copyWith => _$CaffeineHomeDisplayCopyWithImpl<CaffeineHomeDisplay>(this as CaffeineHomeDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineHomeDisplay&&(identical(other.insights, insights) || other.insights == insights)&&(identical(other.sleepImpactStatus, sleepImpactStatus) || other.sleepImpactStatus == sleepImpactStatus)&&(identical(other.bedtimeIsSafe, bedtimeIsSafe) || other.bedtimeIsSafe == bedtimeIsSafe)&&const DeepCollectionEquality().equals(other.curveEntryTimes, curveEntryTimes)&&(identical(other.curveMaxMg, curveMaxMg) || other.curveMaxMg == curveMaxMg));
}


@override
int get hashCode => Object.hash(runtimeType,insights,sleepImpactStatus,bedtimeIsSafe,const DeepCollectionEquality().hash(curveEntryTimes),curveMaxMg);

@override
String toString() {
  return 'CaffeineHomeDisplay(insights: $insights, sleepImpactStatus: $sleepImpactStatus, bedtimeIsSafe: $bedtimeIsSafe, curveEntryTimes: $curveEntryTimes, curveMaxMg: $curveMaxMg)';
}


}

/// @nodoc
abstract mixin class $CaffeineHomeDisplayCopyWith<$Res>  {
  factory $CaffeineHomeDisplayCopyWith(CaffeineHomeDisplay value, $Res Function(CaffeineHomeDisplay) _then) = _$CaffeineHomeDisplayCopyWithImpl;
@useResult
$Res call({
 CaffeineInsights insights, CaffeineSleepImpactStatus sleepImpactStatus, bool bedtimeIsSafe, List<DateTime> curveEntryTimes, double curveMaxMg
});


$CaffeineInsightsCopyWith<$Res> get insights;

}
/// @nodoc
class _$CaffeineHomeDisplayCopyWithImpl<$Res>
    implements $CaffeineHomeDisplayCopyWith<$Res> {
  _$CaffeineHomeDisplayCopyWithImpl(this._self, this._then);

  final CaffeineHomeDisplay _self;
  final $Res Function(CaffeineHomeDisplay) _then;

/// Create a copy of CaffeineHomeDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? insights = null,Object? sleepImpactStatus = null,Object? bedtimeIsSafe = null,Object? curveEntryTimes = null,Object? curveMaxMg = null,}) {
  return _then(_self.copyWith(
insights: null == insights ? _self.insights : insights // ignore: cast_nullable_to_non_nullable
as CaffeineInsights,sleepImpactStatus: null == sleepImpactStatus ? _self.sleepImpactStatus : sleepImpactStatus // ignore: cast_nullable_to_non_nullable
as CaffeineSleepImpactStatus,bedtimeIsSafe: null == bedtimeIsSafe ? _self.bedtimeIsSafe : bedtimeIsSafe // ignore: cast_nullable_to_non_nullable
as bool,curveEntryTimes: null == curveEntryTimes ? _self.curveEntryTimes : curveEntryTimes // ignore: cast_nullable_to_non_nullable
as List<DateTime>,curveMaxMg: null == curveMaxMg ? _self.curveMaxMg : curveMaxMg // ignore: cast_nullable_to_non_nullable
as double,
  ));
}
/// Create a copy of CaffeineHomeDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineInsightsCopyWith<$Res> get insights {
  
  return $CaffeineInsightsCopyWith<$Res>(_self.insights, (value) {
    return _then(_self.copyWith(insights: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaffeineHomeDisplay].
extension CaffeineHomeDisplayPatterns on CaffeineHomeDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineHomeDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineHomeDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineHomeDisplay value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineHomeDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineHomeDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineHomeDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaffeineInsights insights,  CaffeineSleepImpactStatus sleepImpactStatus,  bool bedtimeIsSafe,  List<DateTime> curveEntryTimes,  double curveMaxMg)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineHomeDisplay() when $default != null:
return $default(_that.insights,_that.sleepImpactStatus,_that.bedtimeIsSafe,_that.curveEntryTimes,_that.curveMaxMg);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaffeineInsights insights,  CaffeineSleepImpactStatus sleepImpactStatus,  bool bedtimeIsSafe,  List<DateTime> curveEntryTimes,  double curveMaxMg)  $default,) {final _that = this;
switch (_that) {
case _CaffeineHomeDisplay():
return $default(_that.insights,_that.sleepImpactStatus,_that.bedtimeIsSafe,_that.curveEntryTimes,_that.curveMaxMg);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaffeineInsights insights,  CaffeineSleepImpactStatus sleepImpactStatus,  bool bedtimeIsSafe,  List<DateTime> curveEntryTimes,  double curveMaxMg)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineHomeDisplay() when $default != null:
return $default(_that.insights,_that.sleepImpactStatus,_that.bedtimeIsSafe,_that.curveEntryTimes,_that.curveMaxMg);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineHomeDisplay implements CaffeineHomeDisplay {
  const _CaffeineHomeDisplay({this.insights = const CaffeineInsights(), this.sleepImpactStatus = CaffeineSleepImpactStatus.unlikely, this.bedtimeIsSafe = true, final  List<DateTime> curveEntryTimes = const <DateTime>[], this.curveMaxMg = 1.0}): _curveEntryTimes = curveEntryTimes;
  

@override@JsonKey() final  CaffeineInsights insights;
@override@JsonKey() final  CaffeineSleepImpactStatus sleepImpactStatus;
/// Kotlin's bedtime card colours on this: is the projection under the line?
@override@JsonKey() final  bool bedtimeIsSafe;
/// When each logged drink lands on the curve, for the entry markers.
 final  List<DateTime> _curveEntryTimes;
/// When each logged drink lands on the curve, for the entry markers.
@override@JsonKey() List<DateTime> get curveEntryTimes {
  if (_curveEntryTimes is EqualUnmodifiableListView) return _curveEntryTimes;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_curveEntryTimes);
}

/// The curve's y-axis maximum: the tallest of the threshold and the points,
/// floored at 1.0 so an empty day divides by something.
@override@JsonKey() final  double curveMaxMg;

/// Create a copy of CaffeineHomeDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineHomeDisplayCopyWith<_CaffeineHomeDisplay> get copyWith => __$CaffeineHomeDisplayCopyWithImpl<_CaffeineHomeDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineHomeDisplay&&(identical(other.insights, insights) || other.insights == insights)&&(identical(other.sleepImpactStatus, sleepImpactStatus) || other.sleepImpactStatus == sleepImpactStatus)&&(identical(other.bedtimeIsSafe, bedtimeIsSafe) || other.bedtimeIsSafe == bedtimeIsSafe)&&const DeepCollectionEquality().equals(other._curveEntryTimes, _curveEntryTimes)&&(identical(other.curveMaxMg, curveMaxMg) || other.curveMaxMg == curveMaxMg));
}


@override
int get hashCode => Object.hash(runtimeType,insights,sleepImpactStatus,bedtimeIsSafe,const DeepCollectionEquality().hash(_curveEntryTimes),curveMaxMg);

@override
String toString() {
  return 'CaffeineHomeDisplay(insights: $insights, sleepImpactStatus: $sleepImpactStatus, bedtimeIsSafe: $bedtimeIsSafe, curveEntryTimes: $curveEntryTimes, curveMaxMg: $curveMaxMg)';
}


}

/// @nodoc
abstract mixin class _$CaffeineHomeDisplayCopyWith<$Res> implements $CaffeineHomeDisplayCopyWith<$Res> {
  factory _$CaffeineHomeDisplayCopyWith(_CaffeineHomeDisplay value, $Res Function(_CaffeineHomeDisplay) _then) = __$CaffeineHomeDisplayCopyWithImpl;
@override @useResult
$Res call({
 CaffeineInsights insights, CaffeineSleepImpactStatus sleepImpactStatus, bool bedtimeIsSafe, List<DateTime> curveEntryTimes, double curveMaxMg
});


@override $CaffeineInsightsCopyWith<$Res> get insights;

}
/// @nodoc
class __$CaffeineHomeDisplayCopyWithImpl<$Res>
    implements _$CaffeineHomeDisplayCopyWith<$Res> {
  __$CaffeineHomeDisplayCopyWithImpl(this._self, this._then);

  final _CaffeineHomeDisplay _self;
  final $Res Function(_CaffeineHomeDisplay) _then;

/// Create a copy of CaffeineHomeDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? insights = null,Object? sleepImpactStatus = null,Object? bedtimeIsSafe = null,Object? curveEntryTimes = null,Object? curveMaxMg = null,}) {
  return _then(_CaffeineHomeDisplay(
insights: null == insights ? _self.insights : insights // ignore: cast_nullable_to_non_nullable
as CaffeineInsights,sleepImpactStatus: null == sleepImpactStatus ? _self.sleepImpactStatus : sleepImpactStatus // ignore: cast_nullable_to_non_nullable
as CaffeineSleepImpactStatus,bedtimeIsSafe: null == bedtimeIsSafe ? _self.bedtimeIsSafe : bedtimeIsSafe // ignore: cast_nullable_to_non_nullable
as bool,curveEntryTimes: null == curveEntryTimes ? _self._curveEntryTimes : curveEntryTimes // ignore: cast_nullable_to_non_nullable
as List<DateTime>,curveMaxMg: null == curveMaxMg ? _self.curveMaxMg : curveMaxMg // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

/// Create a copy of CaffeineHomeDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineInsightsCopyWith<$Res> get insights {
  
  return $CaffeineInsightsCopyWith<$Res>(_self.insights, (value) {
    return _then(_self.copyWith(insights: value));
  });
}
}

/// @nodoc
mixin _$CaffeineAnalyticsDisplay {

 CaffeineInsights get insights; List<CaffeineBar> get sourceBars; List<CaffeineBar> get itemBars; List<CaffeineBar> get categoryBars; List<CaffeineTimeBucketBar> get timeBucketBars;/// The biggest source over the window, or null when nothing was logged.
 String? get topSourceLabel;
/// Create a copy of CaffeineAnalyticsDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineAnalyticsDisplayCopyWith<CaffeineAnalyticsDisplay> get copyWith => _$CaffeineAnalyticsDisplayCopyWithImpl<CaffeineAnalyticsDisplay>(this as CaffeineAnalyticsDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineAnalyticsDisplay&&(identical(other.insights, insights) || other.insights == insights)&&const DeepCollectionEquality().equals(other.sourceBars, sourceBars)&&const DeepCollectionEquality().equals(other.itemBars, itemBars)&&const DeepCollectionEquality().equals(other.categoryBars, categoryBars)&&const DeepCollectionEquality().equals(other.timeBucketBars, timeBucketBars)&&(identical(other.topSourceLabel, topSourceLabel) || other.topSourceLabel == topSourceLabel));
}


@override
int get hashCode => Object.hash(runtimeType,insights,const DeepCollectionEquality().hash(sourceBars),const DeepCollectionEquality().hash(itemBars),const DeepCollectionEquality().hash(categoryBars),const DeepCollectionEquality().hash(timeBucketBars),topSourceLabel);

@override
String toString() {
  return 'CaffeineAnalyticsDisplay(insights: $insights, sourceBars: $sourceBars, itemBars: $itemBars, categoryBars: $categoryBars, timeBucketBars: $timeBucketBars, topSourceLabel: $topSourceLabel)';
}


}

/// @nodoc
abstract mixin class $CaffeineAnalyticsDisplayCopyWith<$Res>  {
  factory $CaffeineAnalyticsDisplayCopyWith(CaffeineAnalyticsDisplay value, $Res Function(CaffeineAnalyticsDisplay) _then) = _$CaffeineAnalyticsDisplayCopyWithImpl;
@useResult
$Res call({
 CaffeineInsights insights, List<CaffeineBar> sourceBars, List<CaffeineBar> itemBars, List<CaffeineBar> categoryBars, List<CaffeineTimeBucketBar> timeBucketBars, String? topSourceLabel
});


$CaffeineInsightsCopyWith<$Res> get insights;

}
/// @nodoc
class _$CaffeineAnalyticsDisplayCopyWithImpl<$Res>
    implements $CaffeineAnalyticsDisplayCopyWith<$Res> {
  _$CaffeineAnalyticsDisplayCopyWithImpl(this._self, this._then);

  final CaffeineAnalyticsDisplay _self;
  final $Res Function(CaffeineAnalyticsDisplay) _then;

/// Create a copy of CaffeineAnalyticsDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? insights = null,Object? sourceBars = null,Object? itemBars = null,Object? categoryBars = null,Object? timeBucketBars = null,Object? topSourceLabel = freezed,}) {
  return _then(_self.copyWith(
insights: null == insights ? _self.insights : insights // ignore: cast_nullable_to_non_nullable
as CaffeineInsights,sourceBars: null == sourceBars ? _self.sourceBars : sourceBars // ignore: cast_nullable_to_non_nullable
as List<CaffeineBar>,itemBars: null == itemBars ? _self.itemBars : itemBars // ignore: cast_nullable_to_non_nullable
as List<CaffeineBar>,categoryBars: null == categoryBars ? _self.categoryBars : categoryBars // ignore: cast_nullable_to_non_nullable
as List<CaffeineBar>,timeBucketBars: null == timeBucketBars ? _self.timeBucketBars : timeBucketBars // ignore: cast_nullable_to_non_nullable
as List<CaffeineTimeBucketBar>,topSourceLabel: freezed == topSourceLabel ? _self.topSourceLabel : topSourceLabel // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}
/// Create a copy of CaffeineAnalyticsDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineInsightsCopyWith<$Res> get insights {
  
  return $CaffeineInsightsCopyWith<$Res>(_self.insights, (value) {
    return _then(_self.copyWith(insights: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaffeineAnalyticsDisplay].
extension CaffeineAnalyticsDisplayPatterns on CaffeineAnalyticsDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineAnalyticsDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineAnalyticsDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineAnalyticsDisplay value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineAnalyticsDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineAnalyticsDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineAnalyticsDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaffeineInsights insights,  List<CaffeineBar> sourceBars,  List<CaffeineBar> itemBars,  List<CaffeineBar> categoryBars,  List<CaffeineTimeBucketBar> timeBucketBars,  String? topSourceLabel)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineAnalyticsDisplay() when $default != null:
return $default(_that.insights,_that.sourceBars,_that.itemBars,_that.categoryBars,_that.timeBucketBars,_that.topSourceLabel);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaffeineInsights insights,  List<CaffeineBar> sourceBars,  List<CaffeineBar> itemBars,  List<CaffeineBar> categoryBars,  List<CaffeineTimeBucketBar> timeBucketBars,  String? topSourceLabel)  $default,) {final _that = this;
switch (_that) {
case _CaffeineAnalyticsDisplay():
return $default(_that.insights,_that.sourceBars,_that.itemBars,_that.categoryBars,_that.timeBucketBars,_that.topSourceLabel);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaffeineInsights insights,  List<CaffeineBar> sourceBars,  List<CaffeineBar> itemBars,  List<CaffeineBar> categoryBars,  List<CaffeineTimeBucketBar> timeBucketBars,  String? topSourceLabel)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineAnalyticsDisplay() when $default != null:
return $default(_that.insights,_that.sourceBars,_that.itemBars,_that.categoryBars,_that.timeBucketBars,_that.topSourceLabel);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineAnalyticsDisplay implements CaffeineAnalyticsDisplay {
  const _CaffeineAnalyticsDisplay({this.insights = const CaffeineInsights(), final  List<CaffeineBar> sourceBars = const <CaffeineBar>[], final  List<CaffeineBar> itemBars = const <CaffeineBar>[], final  List<CaffeineBar> categoryBars = const <CaffeineBar>[], final  List<CaffeineTimeBucketBar> timeBucketBars = const <CaffeineTimeBucketBar>[], this.topSourceLabel}): _sourceBars = sourceBars,_itemBars = itemBars,_categoryBars = categoryBars,_timeBucketBars = timeBucketBars;
  

@override@JsonKey() final  CaffeineInsights insights;
 final  List<CaffeineBar> _sourceBars;
@override@JsonKey() List<CaffeineBar> get sourceBars {
  if (_sourceBars is EqualUnmodifiableListView) return _sourceBars;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sourceBars);
}

 final  List<CaffeineBar> _itemBars;
@override@JsonKey() List<CaffeineBar> get itemBars {
  if (_itemBars is EqualUnmodifiableListView) return _itemBars;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_itemBars);
}

 final  List<CaffeineBar> _categoryBars;
@override@JsonKey() List<CaffeineBar> get categoryBars {
  if (_categoryBars is EqualUnmodifiableListView) return _categoryBars;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_categoryBars);
}

 final  List<CaffeineTimeBucketBar> _timeBucketBars;
@override@JsonKey() List<CaffeineTimeBucketBar> get timeBucketBars {
  if (_timeBucketBars is EqualUnmodifiableListView) return _timeBucketBars;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_timeBucketBars);
}

/// The biggest source over the window, or null when nothing was logged.
@override final  String? topSourceLabel;

/// Create a copy of CaffeineAnalyticsDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineAnalyticsDisplayCopyWith<_CaffeineAnalyticsDisplay> get copyWith => __$CaffeineAnalyticsDisplayCopyWithImpl<_CaffeineAnalyticsDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineAnalyticsDisplay&&(identical(other.insights, insights) || other.insights == insights)&&const DeepCollectionEquality().equals(other._sourceBars, _sourceBars)&&const DeepCollectionEquality().equals(other._itemBars, _itemBars)&&const DeepCollectionEquality().equals(other._categoryBars, _categoryBars)&&const DeepCollectionEquality().equals(other._timeBucketBars, _timeBucketBars)&&(identical(other.topSourceLabel, topSourceLabel) || other.topSourceLabel == topSourceLabel));
}


@override
int get hashCode => Object.hash(runtimeType,insights,const DeepCollectionEquality().hash(_sourceBars),const DeepCollectionEquality().hash(_itemBars),const DeepCollectionEquality().hash(_categoryBars),const DeepCollectionEquality().hash(_timeBucketBars),topSourceLabel);

@override
String toString() {
  return 'CaffeineAnalyticsDisplay(insights: $insights, sourceBars: $sourceBars, itemBars: $itemBars, categoryBars: $categoryBars, timeBucketBars: $timeBucketBars, topSourceLabel: $topSourceLabel)';
}


}

/// @nodoc
abstract mixin class _$CaffeineAnalyticsDisplayCopyWith<$Res> implements $CaffeineAnalyticsDisplayCopyWith<$Res> {
  factory _$CaffeineAnalyticsDisplayCopyWith(_CaffeineAnalyticsDisplay value, $Res Function(_CaffeineAnalyticsDisplay) _then) = __$CaffeineAnalyticsDisplayCopyWithImpl;
@override @useResult
$Res call({
 CaffeineInsights insights, List<CaffeineBar> sourceBars, List<CaffeineBar> itemBars, List<CaffeineBar> categoryBars, List<CaffeineTimeBucketBar> timeBucketBars, String? topSourceLabel
});


@override $CaffeineInsightsCopyWith<$Res> get insights;

}
/// @nodoc
class __$CaffeineAnalyticsDisplayCopyWithImpl<$Res>
    implements _$CaffeineAnalyticsDisplayCopyWith<$Res> {
  __$CaffeineAnalyticsDisplayCopyWithImpl(this._self, this._then);

  final _CaffeineAnalyticsDisplay _self;
  final $Res Function(_CaffeineAnalyticsDisplay) _then;

/// Create a copy of CaffeineAnalyticsDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? insights = null,Object? sourceBars = null,Object? itemBars = null,Object? categoryBars = null,Object? timeBucketBars = null,Object? topSourceLabel = freezed,}) {
  return _then(_CaffeineAnalyticsDisplay(
insights: null == insights ? _self.insights : insights // ignore: cast_nullable_to_non_nullable
as CaffeineInsights,sourceBars: null == sourceBars ? _self._sourceBars : sourceBars // ignore: cast_nullable_to_non_nullable
as List<CaffeineBar>,itemBars: null == itemBars ? _self._itemBars : itemBars // ignore: cast_nullable_to_non_nullable
as List<CaffeineBar>,categoryBars: null == categoryBars ? _self._categoryBars : categoryBars // ignore: cast_nullable_to_non_nullable
as List<CaffeineBar>,timeBucketBars: null == timeBucketBars ? _self._timeBucketBars : timeBucketBars // ignore: cast_nullable_to_non_nullable
as List<CaffeineTimeBucketBar>,topSourceLabel: freezed == topSourceLabel ? _self.topSourceLabel : topSourceLabel // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

/// Create a copy of CaffeineAnalyticsDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineInsightsCopyWith<$Res> get insights {
  
  return $CaffeineInsightsCopyWith<$Res>(_self.insights, (value) {
    return _then(_self.copyWith(insights: value));
  });
}
}

/// @nodoc
mixin _$CaffeineBar {

 String get label; double get valueMg; double get fraction;
/// Create a copy of CaffeineBar
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineBarCopyWith<CaffeineBar> get copyWith => _$CaffeineBarCopyWithImpl<CaffeineBar>(this as CaffeineBar, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineBar&&(identical(other.label, label) || other.label == label)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg)&&(identical(other.fraction, fraction) || other.fraction == fraction));
}


@override
int get hashCode => Object.hash(runtimeType,label,valueMg,fraction);

@override
String toString() {
  return 'CaffeineBar(label: $label, valueMg: $valueMg, fraction: $fraction)';
}


}

/// @nodoc
abstract mixin class $CaffeineBarCopyWith<$Res>  {
  factory $CaffeineBarCopyWith(CaffeineBar value, $Res Function(CaffeineBar) _then) = _$CaffeineBarCopyWithImpl;
@useResult
$Res call({
 String label, double valueMg, double fraction
});




}
/// @nodoc
class _$CaffeineBarCopyWithImpl<$Res>
    implements $CaffeineBarCopyWith<$Res> {
  _$CaffeineBarCopyWithImpl(this._self, this._then);

  final CaffeineBar _self;
  final $Res Function(CaffeineBar) _then;

/// Create a copy of CaffeineBar
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? label = null,Object? valueMg = null,Object? fraction = null,}) {
  return _then(_self.copyWith(
label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,fraction: null == fraction ? _self.fraction : fraction // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [CaffeineBar].
extension CaffeineBarPatterns on CaffeineBar {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineBar value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineBar() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineBar value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineBar():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineBar value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineBar() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String label,  double valueMg,  double fraction)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineBar() when $default != null:
return $default(_that.label,_that.valueMg,_that.fraction);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String label,  double valueMg,  double fraction)  $default,) {final _that = this;
switch (_that) {
case _CaffeineBar():
return $default(_that.label,_that.valueMg,_that.fraction);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String label,  double valueMg,  double fraction)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineBar() when $default != null:
return $default(_that.label,_that.valueMg,_that.fraction);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineBar implements CaffeineBar {
  const _CaffeineBar({required this.label, required this.valueMg, required this.fraction});
  

@override final  String label;
@override final  double valueMg;
@override final  double fraction;

/// Create a copy of CaffeineBar
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineBarCopyWith<_CaffeineBar> get copyWith => __$CaffeineBarCopyWithImpl<_CaffeineBar>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineBar&&(identical(other.label, label) || other.label == label)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg)&&(identical(other.fraction, fraction) || other.fraction == fraction));
}


@override
int get hashCode => Object.hash(runtimeType,label,valueMg,fraction);

@override
String toString() {
  return 'CaffeineBar(label: $label, valueMg: $valueMg, fraction: $fraction)';
}


}

/// @nodoc
abstract mixin class _$CaffeineBarCopyWith<$Res> implements $CaffeineBarCopyWith<$Res> {
  factory _$CaffeineBarCopyWith(_CaffeineBar value, $Res Function(_CaffeineBar) _then) = __$CaffeineBarCopyWithImpl;
@override @useResult
$Res call({
 String label, double valueMg, double fraction
});




}
/// @nodoc
class __$CaffeineBarCopyWithImpl<$Res>
    implements _$CaffeineBarCopyWith<$Res> {
  __$CaffeineBarCopyWithImpl(this._self, this._then);

  final _CaffeineBar _self;
  final $Res Function(_CaffeineBar) _then;

/// Create a copy of CaffeineBar
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? label = null,Object? valueMg = null,Object? fraction = null,}) {
  return _then(_CaffeineBar(
label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,fraction: null == fraction ? _self.fraction : fraction // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$CaffeineTimeBucketBar {

 CaffeineTimeOfDayBucket get bucket; double get valueMg; double get fraction;
/// Create a copy of CaffeineTimeBucketBar
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineTimeBucketBarCopyWith<CaffeineTimeBucketBar> get copyWith => _$CaffeineTimeBucketBarCopyWithImpl<CaffeineTimeBucketBar>(this as CaffeineTimeBucketBar, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineTimeBucketBar&&(identical(other.bucket, bucket) || other.bucket == bucket)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg)&&(identical(other.fraction, fraction) || other.fraction == fraction));
}


@override
int get hashCode => Object.hash(runtimeType,bucket,valueMg,fraction);

@override
String toString() {
  return 'CaffeineTimeBucketBar(bucket: $bucket, valueMg: $valueMg, fraction: $fraction)';
}


}

/// @nodoc
abstract mixin class $CaffeineTimeBucketBarCopyWith<$Res>  {
  factory $CaffeineTimeBucketBarCopyWith(CaffeineTimeBucketBar value, $Res Function(CaffeineTimeBucketBar) _then) = _$CaffeineTimeBucketBarCopyWithImpl;
@useResult
$Res call({
 CaffeineTimeOfDayBucket bucket, double valueMg, double fraction
});




}
/// @nodoc
class _$CaffeineTimeBucketBarCopyWithImpl<$Res>
    implements $CaffeineTimeBucketBarCopyWith<$Res> {
  _$CaffeineTimeBucketBarCopyWithImpl(this._self, this._then);

  final CaffeineTimeBucketBar _self;
  final $Res Function(CaffeineTimeBucketBar) _then;

/// Create a copy of CaffeineTimeBucketBar
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? bucket = null,Object? valueMg = null,Object? fraction = null,}) {
  return _then(_self.copyWith(
bucket: null == bucket ? _self.bucket : bucket // ignore: cast_nullable_to_non_nullable
as CaffeineTimeOfDayBucket,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,fraction: null == fraction ? _self.fraction : fraction // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [CaffeineTimeBucketBar].
extension CaffeineTimeBucketBarPatterns on CaffeineTimeBucketBar {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineTimeBucketBar value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineTimeBucketBar() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineTimeBucketBar value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineTimeBucketBar():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineTimeBucketBar value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineTimeBucketBar() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( CaffeineTimeOfDayBucket bucket,  double valueMg,  double fraction)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineTimeBucketBar() when $default != null:
return $default(_that.bucket,_that.valueMg,_that.fraction);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( CaffeineTimeOfDayBucket bucket,  double valueMg,  double fraction)  $default,) {final _that = this;
switch (_that) {
case _CaffeineTimeBucketBar():
return $default(_that.bucket,_that.valueMg,_that.fraction);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( CaffeineTimeOfDayBucket bucket,  double valueMg,  double fraction)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineTimeBucketBar() when $default != null:
return $default(_that.bucket,_that.valueMg,_that.fraction);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineTimeBucketBar implements CaffeineTimeBucketBar {
  const _CaffeineTimeBucketBar({required this.bucket, required this.valueMg, required this.fraction});
  

@override final  CaffeineTimeOfDayBucket bucket;
@override final  double valueMg;
@override final  double fraction;

/// Create a copy of CaffeineTimeBucketBar
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineTimeBucketBarCopyWith<_CaffeineTimeBucketBar> get copyWith => __$CaffeineTimeBucketBarCopyWithImpl<_CaffeineTimeBucketBar>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineTimeBucketBar&&(identical(other.bucket, bucket) || other.bucket == bucket)&&(identical(other.valueMg, valueMg) || other.valueMg == valueMg)&&(identical(other.fraction, fraction) || other.fraction == fraction));
}


@override
int get hashCode => Object.hash(runtimeType,bucket,valueMg,fraction);

@override
String toString() {
  return 'CaffeineTimeBucketBar(bucket: $bucket, valueMg: $valueMg, fraction: $fraction)';
}


}

/// @nodoc
abstract mixin class _$CaffeineTimeBucketBarCopyWith<$Res> implements $CaffeineTimeBucketBarCopyWith<$Res> {
  factory _$CaffeineTimeBucketBarCopyWith(_CaffeineTimeBucketBar value, $Res Function(_CaffeineTimeBucketBar) _then) = __$CaffeineTimeBucketBarCopyWithImpl;
@override @useResult
$Res call({
 CaffeineTimeOfDayBucket bucket, double valueMg, double fraction
});




}
/// @nodoc
class __$CaffeineTimeBucketBarCopyWithImpl<$Res>
    implements _$CaffeineTimeBucketBarCopyWith<$Res> {
  __$CaffeineTimeBucketBarCopyWithImpl(this._self, this._then);

  final _CaffeineTimeBucketBar _self;
  final $Res Function(_CaffeineTimeBucketBar) _then;

/// Create a copy of CaffeineTimeBucketBar
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? bucket = null,Object? valueMg = null,Object? fraction = null,}) {
  return _then(_CaffeineTimeBucketBar(
bucket: null == bucket ? _self.bucket : bucket // ignore: cast_nullable_to_non_nullable
as CaffeineTimeOfDayBucket,valueMg: null == valueMg ? _self.valueMg : valueMg // ignore: cast_nullable_to_non_nullable
as double,fraction: null == fraction ? _self.fraction : fraction // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

// dart format on
