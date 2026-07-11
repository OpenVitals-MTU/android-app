// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'daily_readiness.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HrvStatusInsight {

 HrvStatus get status; String get label; String get detail; double? get currentRmssdMs; double? get baselineRmssdMs; int? get percentFromBaseline;
/// Create a copy of HrvStatusInsight
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HrvStatusInsightCopyWith<HrvStatusInsight> get copyWith => _$HrvStatusInsightCopyWithImpl<HrvStatusInsight>(this as HrvStatusInsight, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HrvStatusInsight&&(identical(other.status, status) || other.status == status)&&(identical(other.label, label) || other.label == label)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.currentRmssdMs, currentRmssdMs) || other.currentRmssdMs == currentRmssdMs)&&(identical(other.baselineRmssdMs, baselineRmssdMs) || other.baselineRmssdMs == baselineRmssdMs)&&(identical(other.percentFromBaseline, percentFromBaseline) || other.percentFromBaseline == percentFromBaseline));
}


@override
int get hashCode => Object.hash(runtimeType,status,label,detail,currentRmssdMs,baselineRmssdMs,percentFromBaseline);

@override
String toString() {
  return 'HrvStatusInsight(status: $status, label: $label, detail: $detail, currentRmssdMs: $currentRmssdMs, baselineRmssdMs: $baselineRmssdMs, percentFromBaseline: $percentFromBaseline)';
}


}

/// @nodoc
abstract mixin class $HrvStatusInsightCopyWith<$Res>  {
  factory $HrvStatusInsightCopyWith(HrvStatusInsight value, $Res Function(HrvStatusInsight) _then) = _$HrvStatusInsightCopyWithImpl;
@useResult
$Res call({
 HrvStatus status, String label, String detail, double? currentRmssdMs, double? baselineRmssdMs, int? percentFromBaseline
});




}
/// @nodoc
class _$HrvStatusInsightCopyWithImpl<$Res>
    implements $HrvStatusInsightCopyWith<$Res> {
  _$HrvStatusInsightCopyWithImpl(this._self, this._then);

  final HrvStatusInsight _self;
  final $Res Function(HrvStatusInsight) _then;

/// Create a copy of HrvStatusInsight
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? label = null,Object? detail = null,Object? currentRmssdMs = freezed,Object? baselineRmssdMs = freezed,Object? percentFromBaseline = freezed,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as HrvStatus,label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as String,currentRmssdMs: freezed == currentRmssdMs ? _self.currentRmssdMs : currentRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,baselineRmssdMs: freezed == baselineRmssdMs ? _self.baselineRmssdMs : baselineRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,percentFromBaseline: freezed == percentFromBaseline ? _self.percentFromBaseline : percentFromBaseline // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}

}


/// Adds pattern-matching-related methods to [HrvStatusInsight].
extension HrvStatusInsightPatterns on HrvStatusInsight {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HrvStatusInsight value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HrvStatusInsight() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HrvStatusInsight value)  $default,){
final _that = this;
switch (_that) {
case _HrvStatusInsight():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HrvStatusInsight value)?  $default,){
final _that = this;
switch (_that) {
case _HrvStatusInsight() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( HrvStatus status,  String label,  String detail,  double? currentRmssdMs,  double? baselineRmssdMs,  int? percentFromBaseline)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HrvStatusInsight() when $default != null:
return $default(_that.status,_that.label,_that.detail,_that.currentRmssdMs,_that.baselineRmssdMs,_that.percentFromBaseline);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( HrvStatus status,  String label,  String detail,  double? currentRmssdMs,  double? baselineRmssdMs,  int? percentFromBaseline)  $default,) {final _that = this;
switch (_that) {
case _HrvStatusInsight():
return $default(_that.status,_that.label,_that.detail,_that.currentRmssdMs,_that.baselineRmssdMs,_that.percentFromBaseline);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( HrvStatus status,  String label,  String detail,  double? currentRmssdMs,  double? baselineRmssdMs,  int? percentFromBaseline)?  $default,) {final _that = this;
switch (_that) {
case _HrvStatusInsight() when $default != null:
return $default(_that.status,_that.label,_that.detail,_that.currentRmssdMs,_that.baselineRmssdMs,_that.percentFromBaseline);case _:
  return null;

}
}

}

/// @nodoc


class _HrvStatusInsight implements HrvStatusInsight {
  const _HrvStatusInsight({required this.status, required this.label, required this.detail, required this.currentRmssdMs, required this.baselineRmssdMs, required this.percentFromBaseline});
  

@override final  HrvStatus status;
@override final  String label;
@override final  String detail;
@override final  double? currentRmssdMs;
@override final  double? baselineRmssdMs;
@override final  int? percentFromBaseline;

/// Create a copy of HrvStatusInsight
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HrvStatusInsightCopyWith<_HrvStatusInsight> get copyWith => __$HrvStatusInsightCopyWithImpl<_HrvStatusInsight>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HrvStatusInsight&&(identical(other.status, status) || other.status == status)&&(identical(other.label, label) || other.label == label)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.currentRmssdMs, currentRmssdMs) || other.currentRmssdMs == currentRmssdMs)&&(identical(other.baselineRmssdMs, baselineRmssdMs) || other.baselineRmssdMs == baselineRmssdMs)&&(identical(other.percentFromBaseline, percentFromBaseline) || other.percentFromBaseline == percentFromBaseline));
}


@override
int get hashCode => Object.hash(runtimeType,status,label,detail,currentRmssdMs,baselineRmssdMs,percentFromBaseline);

@override
String toString() {
  return 'HrvStatusInsight(status: $status, label: $label, detail: $detail, currentRmssdMs: $currentRmssdMs, baselineRmssdMs: $baselineRmssdMs, percentFromBaseline: $percentFromBaseline)';
}


}

/// @nodoc
abstract mixin class _$HrvStatusInsightCopyWith<$Res> implements $HrvStatusInsightCopyWith<$Res> {
  factory _$HrvStatusInsightCopyWith(_HrvStatusInsight value, $Res Function(_HrvStatusInsight) _then) = __$HrvStatusInsightCopyWithImpl;
@override @useResult
$Res call({
 HrvStatus status, String label, String detail, double? currentRmssdMs, double? baselineRmssdMs, int? percentFromBaseline
});




}
/// @nodoc
class __$HrvStatusInsightCopyWithImpl<$Res>
    implements _$HrvStatusInsightCopyWith<$Res> {
  __$HrvStatusInsightCopyWithImpl(this._self, this._then);

  final _HrvStatusInsight _self;
  final $Res Function(_HrvStatusInsight) _then;

/// Create a copy of HrvStatusInsight
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? label = null,Object? detail = null,Object? currentRmssdMs = freezed,Object? baselineRmssdMs = freezed,Object? percentFromBaseline = freezed,}) {
  return _then(_HrvStatusInsight(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as HrvStatus,label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as String,currentRmssdMs: freezed == currentRmssdMs ? _self.currentRmssdMs : currentRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,baselineRmssdMs: freezed == baselineRmssdMs ? _self.baselineRmssdMs : baselineRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,percentFromBaseline: freezed == percentFromBaseline ? _self.percentFromBaseline : percentFromBaseline // ignore: cast_nullable_to_non_nullable
as int?,
  ));
}


}

/// @nodoc
mixin _$IntensityMinutesReadinessInsight {

 IntensityMinutesStatus get status; String get label; String get detail; int? get moderateEquivalentMinutes; int get targetMinutes; int? get todayModerateEquivalentMinutes; int get progressPercent; IntensityMinutesConfidence get confidence;
/// Create a copy of IntensityMinutesReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$IntensityMinutesReadinessInsightCopyWith<IntensityMinutesReadinessInsight> get copyWith => _$IntensityMinutesReadinessInsightCopyWithImpl<IntensityMinutesReadinessInsight>(this as IntensityMinutesReadinessInsight, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is IntensityMinutesReadinessInsight&&(identical(other.status, status) || other.status == status)&&(identical(other.label, label) || other.label == label)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.moderateEquivalentMinutes, moderateEquivalentMinutes) || other.moderateEquivalentMinutes == moderateEquivalentMinutes)&&(identical(other.targetMinutes, targetMinutes) || other.targetMinutes == targetMinutes)&&(identical(other.todayModerateEquivalentMinutes, todayModerateEquivalentMinutes) || other.todayModerateEquivalentMinutes == todayModerateEquivalentMinutes)&&(identical(other.progressPercent, progressPercent) || other.progressPercent == progressPercent)&&(identical(other.confidence, confidence) || other.confidence == confidence));
}


@override
int get hashCode => Object.hash(runtimeType,status,label,detail,moderateEquivalentMinutes,targetMinutes,todayModerateEquivalentMinutes,progressPercent,confidence);

@override
String toString() {
  return 'IntensityMinutesReadinessInsight(status: $status, label: $label, detail: $detail, moderateEquivalentMinutes: $moderateEquivalentMinutes, targetMinutes: $targetMinutes, todayModerateEquivalentMinutes: $todayModerateEquivalentMinutes, progressPercent: $progressPercent, confidence: $confidence)';
}


}

/// @nodoc
abstract mixin class $IntensityMinutesReadinessInsightCopyWith<$Res>  {
  factory $IntensityMinutesReadinessInsightCopyWith(IntensityMinutesReadinessInsight value, $Res Function(IntensityMinutesReadinessInsight) _then) = _$IntensityMinutesReadinessInsightCopyWithImpl;
@useResult
$Res call({
 IntensityMinutesStatus status, String label, String detail, int? moderateEquivalentMinutes, int targetMinutes, int? todayModerateEquivalentMinutes, int progressPercent, IntensityMinutesConfidence confidence
});




}
/// @nodoc
class _$IntensityMinutesReadinessInsightCopyWithImpl<$Res>
    implements $IntensityMinutesReadinessInsightCopyWith<$Res> {
  _$IntensityMinutesReadinessInsightCopyWithImpl(this._self, this._then);

  final IntensityMinutesReadinessInsight _self;
  final $Res Function(IntensityMinutesReadinessInsight) _then;

/// Create a copy of IntensityMinutesReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? label = null,Object? detail = null,Object? moderateEquivalentMinutes = freezed,Object? targetMinutes = null,Object? todayModerateEquivalentMinutes = freezed,Object? progressPercent = null,Object? confidence = null,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as IntensityMinutesStatus,label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as String,moderateEquivalentMinutes: freezed == moderateEquivalentMinutes ? _self.moderateEquivalentMinutes : moderateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int?,targetMinutes: null == targetMinutes ? _self.targetMinutes : targetMinutes // ignore: cast_nullable_to_non_nullable
as int,todayModerateEquivalentMinutes: freezed == todayModerateEquivalentMinutes ? _self.todayModerateEquivalentMinutes : todayModerateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int?,progressPercent: null == progressPercent ? _self.progressPercent : progressPercent // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as IntensityMinutesConfidence,
  ));
}

}


/// Adds pattern-matching-related methods to [IntensityMinutesReadinessInsight].
extension IntensityMinutesReadinessInsightPatterns on IntensityMinutesReadinessInsight {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _IntensityMinutesReadinessInsight value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _IntensityMinutesReadinessInsight() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _IntensityMinutesReadinessInsight value)  $default,){
final _that = this;
switch (_that) {
case _IntensityMinutesReadinessInsight():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _IntensityMinutesReadinessInsight value)?  $default,){
final _that = this;
switch (_that) {
case _IntensityMinutesReadinessInsight() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( IntensityMinutesStatus status,  String label,  String detail,  int? moderateEquivalentMinutes,  int targetMinutes,  int? todayModerateEquivalentMinutes,  int progressPercent,  IntensityMinutesConfidence confidence)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _IntensityMinutesReadinessInsight() when $default != null:
return $default(_that.status,_that.label,_that.detail,_that.moderateEquivalentMinutes,_that.targetMinutes,_that.todayModerateEquivalentMinutes,_that.progressPercent,_that.confidence);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( IntensityMinutesStatus status,  String label,  String detail,  int? moderateEquivalentMinutes,  int targetMinutes,  int? todayModerateEquivalentMinutes,  int progressPercent,  IntensityMinutesConfidence confidence)  $default,) {final _that = this;
switch (_that) {
case _IntensityMinutesReadinessInsight():
return $default(_that.status,_that.label,_that.detail,_that.moderateEquivalentMinutes,_that.targetMinutes,_that.todayModerateEquivalentMinutes,_that.progressPercent,_that.confidence);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( IntensityMinutesStatus status,  String label,  String detail,  int? moderateEquivalentMinutes,  int targetMinutes,  int? todayModerateEquivalentMinutes,  int progressPercent,  IntensityMinutesConfidence confidence)?  $default,) {final _that = this;
switch (_that) {
case _IntensityMinutesReadinessInsight() when $default != null:
return $default(_that.status,_that.label,_that.detail,_that.moderateEquivalentMinutes,_that.targetMinutes,_that.todayModerateEquivalentMinutes,_that.progressPercent,_that.confidence);case _:
  return null;

}
}

}

/// @nodoc


class _IntensityMinutesReadinessInsight implements IntensityMinutesReadinessInsight {
  const _IntensityMinutesReadinessInsight({required this.status, required this.label, required this.detail, required this.moderateEquivalentMinutes, required this.targetMinutes, required this.todayModerateEquivalentMinutes, required this.progressPercent, required this.confidence});
  

@override final  IntensityMinutesStatus status;
@override final  String label;
@override final  String detail;
@override final  int? moderateEquivalentMinutes;
@override final  int targetMinutes;
@override final  int? todayModerateEquivalentMinutes;
@override final  int progressPercent;
@override final  IntensityMinutesConfidence confidence;

/// Create a copy of IntensityMinutesReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$IntensityMinutesReadinessInsightCopyWith<_IntensityMinutesReadinessInsight> get copyWith => __$IntensityMinutesReadinessInsightCopyWithImpl<_IntensityMinutesReadinessInsight>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _IntensityMinutesReadinessInsight&&(identical(other.status, status) || other.status == status)&&(identical(other.label, label) || other.label == label)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.moderateEquivalentMinutes, moderateEquivalentMinutes) || other.moderateEquivalentMinutes == moderateEquivalentMinutes)&&(identical(other.targetMinutes, targetMinutes) || other.targetMinutes == targetMinutes)&&(identical(other.todayModerateEquivalentMinutes, todayModerateEquivalentMinutes) || other.todayModerateEquivalentMinutes == todayModerateEquivalentMinutes)&&(identical(other.progressPercent, progressPercent) || other.progressPercent == progressPercent)&&(identical(other.confidence, confidence) || other.confidence == confidence));
}


@override
int get hashCode => Object.hash(runtimeType,status,label,detail,moderateEquivalentMinutes,targetMinutes,todayModerateEquivalentMinutes,progressPercent,confidence);

@override
String toString() {
  return 'IntensityMinutesReadinessInsight(status: $status, label: $label, detail: $detail, moderateEquivalentMinutes: $moderateEquivalentMinutes, targetMinutes: $targetMinutes, todayModerateEquivalentMinutes: $todayModerateEquivalentMinutes, progressPercent: $progressPercent, confidence: $confidence)';
}


}

/// @nodoc
abstract mixin class _$IntensityMinutesReadinessInsightCopyWith<$Res> implements $IntensityMinutesReadinessInsightCopyWith<$Res> {
  factory _$IntensityMinutesReadinessInsightCopyWith(_IntensityMinutesReadinessInsight value, $Res Function(_IntensityMinutesReadinessInsight) _then) = __$IntensityMinutesReadinessInsightCopyWithImpl;
@override @useResult
$Res call({
 IntensityMinutesStatus status, String label, String detail, int? moderateEquivalentMinutes, int targetMinutes, int? todayModerateEquivalentMinutes, int progressPercent, IntensityMinutesConfidence confidence
});




}
/// @nodoc
class __$IntensityMinutesReadinessInsightCopyWithImpl<$Res>
    implements _$IntensityMinutesReadinessInsightCopyWith<$Res> {
  __$IntensityMinutesReadinessInsightCopyWithImpl(this._self, this._then);

  final _IntensityMinutesReadinessInsight _self;
  final $Res Function(_IntensityMinutesReadinessInsight) _then;

/// Create a copy of IntensityMinutesReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? label = null,Object? detail = null,Object? moderateEquivalentMinutes = freezed,Object? targetMinutes = null,Object? todayModerateEquivalentMinutes = freezed,Object? progressPercent = null,Object? confidence = null,}) {
  return _then(_IntensityMinutesReadinessInsight(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as IntensityMinutesStatus,label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as String,moderateEquivalentMinutes: freezed == moderateEquivalentMinutes ? _self.moderateEquivalentMinutes : moderateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int?,targetMinutes: null == targetMinutes ? _self.targetMinutes : targetMinutes // ignore: cast_nullable_to_non_nullable
as int,todayModerateEquivalentMinutes: freezed == todayModerateEquivalentMinutes ? _self.todayModerateEquivalentMinutes : todayModerateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int?,progressPercent: null == progressPercent ? _self.progressPercent : progressPercent // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as IntensityMinutesConfidence,
  ));
}


}

/// @nodoc
mixin _$DailyReadinessGoalInputs {

 double get stepsGoal; double get hydrationLitersGoal; double get activeMinutesGoal;
/// Create a copy of DailyReadinessGoalInputs
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyReadinessGoalInputsCopyWith<DailyReadinessGoalInputs> get copyWith => _$DailyReadinessGoalInputsCopyWithImpl<DailyReadinessGoalInputs>(this as DailyReadinessGoalInputs, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyReadinessGoalInputs&&(identical(other.stepsGoal, stepsGoal) || other.stepsGoal == stepsGoal)&&(identical(other.hydrationLitersGoal, hydrationLitersGoal) || other.hydrationLitersGoal == hydrationLitersGoal)&&(identical(other.activeMinutesGoal, activeMinutesGoal) || other.activeMinutesGoal == activeMinutesGoal));
}


@override
int get hashCode => Object.hash(runtimeType,stepsGoal,hydrationLitersGoal,activeMinutesGoal);

@override
String toString() {
  return 'DailyReadinessGoalInputs(stepsGoal: $stepsGoal, hydrationLitersGoal: $hydrationLitersGoal, activeMinutesGoal: $activeMinutesGoal)';
}


}

/// @nodoc
abstract mixin class $DailyReadinessGoalInputsCopyWith<$Res>  {
  factory $DailyReadinessGoalInputsCopyWith(DailyReadinessGoalInputs value, $Res Function(DailyReadinessGoalInputs) _then) = _$DailyReadinessGoalInputsCopyWithImpl;
@useResult
$Res call({
 double stepsGoal, double hydrationLitersGoal, double activeMinutesGoal
});




}
/// @nodoc
class _$DailyReadinessGoalInputsCopyWithImpl<$Res>
    implements $DailyReadinessGoalInputsCopyWith<$Res> {
  _$DailyReadinessGoalInputsCopyWithImpl(this._self, this._then);

  final DailyReadinessGoalInputs _self;
  final $Res Function(DailyReadinessGoalInputs) _then;

/// Create a copy of DailyReadinessGoalInputs
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? stepsGoal = null,Object? hydrationLitersGoal = null,Object? activeMinutesGoal = null,}) {
  return _then(_self.copyWith(
stepsGoal: null == stepsGoal ? _self.stepsGoal : stepsGoal // ignore: cast_nullable_to_non_nullable
as double,hydrationLitersGoal: null == hydrationLitersGoal ? _self.hydrationLitersGoal : hydrationLitersGoal // ignore: cast_nullable_to_non_nullable
as double,activeMinutesGoal: null == activeMinutesGoal ? _self.activeMinutesGoal : activeMinutesGoal // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyReadinessGoalInputs].
extension DailyReadinessGoalInputsPatterns on DailyReadinessGoalInputs {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyReadinessGoalInputs value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyReadinessGoalInputs() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyReadinessGoalInputs value)  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessGoalInputs():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyReadinessGoalInputs value)?  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessGoalInputs() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double stepsGoal,  double hydrationLitersGoal,  double activeMinutesGoal)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyReadinessGoalInputs() when $default != null:
return $default(_that.stepsGoal,_that.hydrationLitersGoal,_that.activeMinutesGoal);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double stepsGoal,  double hydrationLitersGoal,  double activeMinutesGoal)  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessGoalInputs():
return $default(_that.stepsGoal,_that.hydrationLitersGoal,_that.activeMinutesGoal);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double stepsGoal,  double hydrationLitersGoal,  double activeMinutesGoal)?  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessGoalInputs() when $default != null:
return $default(_that.stepsGoal,_that.hydrationLitersGoal,_that.activeMinutesGoal);case _:
  return null;

}
}

}

/// @nodoc


class _DailyReadinessGoalInputs implements DailyReadinessGoalInputs {
  const _DailyReadinessGoalInputs({this.stepsGoal = 8000.0, this.hydrationLitersGoal = 2.0, this.activeMinutesGoal = 45.0});
  

@override@JsonKey() final  double stepsGoal;
@override@JsonKey() final  double hydrationLitersGoal;
@override@JsonKey() final  double activeMinutesGoal;

/// Create a copy of DailyReadinessGoalInputs
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyReadinessGoalInputsCopyWith<_DailyReadinessGoalInputs> get copyWith => __$DailyReadinessGoalInputsCopyWithImpl<_DailyReadinessGoalInputs>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyReadinessGoalInputs&&(identical(other.stepsGoal, stepsGoal) || other.stepsGoal == stepsGoal)&&(identical(other.hydrationLitersGoal, hydrationLitersGoal) || other.hydrationLitersGoal == hydrationLitersGoal)&&(identical(other.activeMinutesGoal, activeMinutesGoal) || other.activeMinutesGoal == activeMinutesGoal));
}


@override
int get hashCode => Object.hash(runtimeType,stepsGoal,hydrationLitersGoal,activeMinutesGoal);

@override
String toString() {
  return 'DailyReadinessGoalInputs(stepsGoal: $stepsGoal, hydrationLitersGoal: $hydrationLitersGoal, activeMinutesGoal: $activeMinutesGoal)';
}


}

/// @nodoc
abstract mixin class _$DailyReadinessGoalInputsCopyWith<$Res> implements $DailyReadinessGoalInputsCopyWith<$Res> {
  factory _$DailyReadinessGoalInputsCopyWith(_DailyReadinessGoalInputs value, $Res Function(_DailyReadinessGoalInputs) _then) = __$DailyReadinessGoalInputsCopyWithImpl;
@override @useResult
$Res call({
 double stepsGoal, double hydrationLitersGoal, double activeMinutesGoal
});




}
/// @nodoc
class __$DailyReadinessGoalInputsCopyWithImpl<$Res>
    implements _$DailyReadinessGoalInputsCopyWith<$Res> {
  __$DailyReadinessGoalInputsCopyWithImpl(this._self, this._then);

  final _DailyReadinessGoalInputs _self;
  final $Res Function(_DailyReadinessGoalInputs) _then;

/// Create a copy of DailyReadinessGoalInputs
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? stepsGoal = null,Object? hydrationLitersGoal = null,Object? activeMinutesGoal = null,}) {
  return _then(_DailyReadinessGoalInputs(
stepsGoal: null == stepsGoal ? _self.stepsGoal : stepsGoal // ignore: cast_nullable_to_non_nullable
as double,hydrationLitersGoal: null == hydrationLitersGoal ? _self.hydrationLitersGoal : hydrationLitersGoal // ignore: cast_nullable_to_non_nullable
as double,activeMinutesGoal: null == activeMinutesGoal ? _self.activeMinutesGoal : activeMinutesGoal // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

/// @nodoc
mixin _$DailyReadinessFactor {

 ReadinessFactorKind get kind; String get label; String get detail; ReadinessFactorImpact get impact;
/// Create a copy of DailyReadinessFactor
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyReadinessFactorCopyWith<DailyReadinessFactor> get copyWith => _$DailyReadinessFactorCopyWithImpl<DailyReadinessFactor>(this as DailyReadinessFactor, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyReadinessFactor&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.label, label) || other.label == label)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.impact, impact) || other.impact == impact));
}


@override
int get hashCode => Object.hash(runtimeType,kind,label,detail,impact);

@override
String toString() {
  return 'DailyReadinessFactor(kind: $kind, label: $label, detail: $detail, impact: $impact)';
}


}

/// @nodoc
abstract mixin class $DailyReadinessFactorCopyWith<$Res>  {
  factory $DailyReadinessFactorCopyWith(DailyReadinessFactor value, $Res Function(DailyReadinessFactor) _then) = _$DailyReadinessFactorCopyWithImpl;
@useResult
$Res call({
 ReadinessFactorKind kind, String label, String detail, ReadinessFactorImpact impact
});




}
/// @nodoc
class _$DailyReadinessFactorCopyWithImpl<$Res>
    implements $DailyReadinessFactorCopyWith<$Res> {
  _$DailyReadinessFactorCopyWithImpl(this._self, this._then);

  final DailyReadinessFactor _self;
  final $Res Function(DailyReadinessFactor) _then;

/// Create a copy of DailyReadinessFactor
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? kind = null,Object? label = null,Object? detail = null,Object? impact = null,}) {
  return _then(_self.copyWith(
kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as ReadinessFactorKind,label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as String,impact: null == impact ? _self.impact : impact // ignore: cast_nullable_to_non_nullable
as ReadinessFactorImpact,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyReadinessFactor].
extension DailyReadinessFactorPatterns on DailyReadinessFactor {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyReadinessFactor value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyReadinessFactor() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyReadinessFactor value)  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessFactor():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyReadinessFactor value)?  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessFactor() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( ReadinessFactorKind kind,  String label,  String detail,  ReadinessFactorImpact impact)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyReadinessFactor() when $default != null:
return $default(_that.kind,_that.label,_that.detail,_that.impact);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( ReadinessFactorKind kind,  String label,  String detail,  ReadinessFactorImpact impact)  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessFactor():
return $default(_that.kind,_that.label,_that.detail,_that.impact);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( ReadinessFactorKind kind,  String label,  String detail,  ReadinessFactorImpact impact)?  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessFactor() when $default != null:
return $default(_that.kind,_that.label,_that.detail,_that.impact);case _:
  return null;

}
}

}

/// @nodoc


class _DailyReadinessFactor implements DailyReadinessFactor {
  const _DailyReadinessFactor({required this.kind, required this.label, required this.detail, required this.impact});
  

@override final  ReadinessFactorKind kind;
@override final  String label;
@override final  String detail;
@override final  ReadinessFactorImpact impact;

/// Create a copy of DailyReadinessFactor
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyReadinessFactorCopyWith<_DailyReadinessFactor> get copyWith => __$DailyReadinessFactorCopyWithImpl<_DailyReadinessFactor>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyReadinessFactor&&(identical(other.kind, kind) || other.kind == kind)&&(identical(other.label, label) || other.label == label)&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.impact, impact) || other.impact == impact));
}


@override
int get hashCode => Object.hash(runtimeType,kind,label,detail,impact);

@override
String toString() {
  return 'DailyReadinessFactor(kind: $kind, label: $label, detail: $detail, impact: $impact)';
}


}

/// @nodoc
abstract mixin class _$DailyReadinessFactorCopyWith<$Res> implements $DailyReadinessFactorCopyWith<$Res> {
  factory _$DailyReadinessFactorCopyWith(_DailyReadinessFactor value, $Res Function(_DailyReadinessFactor) _then) = __$DailyReadinessFactorCopyWithImpl;
@override @useResult
$Res call({
 ReadinessFactorKind kind, String label, String detail, ReadinessFactorImpact impact
});




}
/// @nodoc
class __$DailyReadinessFactorCopyWithImpl<$Res>
    implements _$DailyReadinessFactorCopyWith<$Res> {
  __$DailyReadinessFactorCopyWithImpl(this._self, this._then);

  final _DailyReadinessFactor _self;
  final $Res Function(_DailyReadinessFactor) _then;

/// Create a copy of DailyReadinessFactor
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? kind = null,Object? label = null,Object? detail = null,Object? impact = null,}) {
  return _then(_DailyReadinessFactor(
kind: null == kind ? _self.kind : kind // ignore: cast_nullable_to_non_nullable
as ReadinessFactorKind,label: null == label ? _self.label : label // ignore: cast_nullable_to_non_nullable
as String,detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as String,impact: null == impact ? _self.impact : impact // ignore: cast_nullable_to_non_nullable
as ReadinessFactorImpact,
  ));
}


}

/// @nodoc
mixin _$DailyReadinessInsight {

 ReadinessState get state; int get score; int get bodyEnergyScore; int get trainingReadinessScore; ReadinessRecommendationType get recommendationType; String get statusTitle; String get recommendation; String get explanation; String get alternative; String get suggestedWorkout; String get avoid; String get strainTarget; String? get currentStrain; String get adaptiveGoal; ReadinessConfidence get confidence; String get confidenceReason; HrvStatusInsight get hrvStatus; IntensityMinutesReadinessInsight get intensityMinutes; PhysiologicalStressEstimate get physiologicalStress; List<DailyReadinessFactor> get factors; bool get recoveryModeSuggested;
/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyReadinessInsightCopyWith<DailyReadinessInsight> get copyWith => _$DailyReadinessInsightCopyWithImpl<DailyReadinessInsight>(this as DailyReadinessInsight, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyReadinessInsight&&(identical(other.state, state) || other.state == state)&&(identical(other.score, score) || other.score == score)&&(identical(other.bodyEnergyScore, bodyEnergyScore) || other.bodyEnergyScore == bodyEnergyScore)&&(identical(other.trainingReadinessScore, trainingReadinessScore) || other.trainingReadinessScore == trainingReadinessScore)&&(identical(other.recommendationType, recommendationType) || other.recommendationType == recommendationType)&&(identical(other.statusTitle, statusTitle) || other.statusTitle == statusTitle)&&(identical(other.recommendation, recommendation) || other.recommendation == recommendation)&&(identical(other.explanation, explanation) || other.explanation == explanation)&&(identical(other.alternative, alternative) || other.alternative == alternative)&&(identical(other.suggestedWorkout, suggestedWorkout) || other.suggestedWorkout == suggestedWorkout)&&(identical(other.avoid, avoid) || other.avoid == avoid)&&(identical(other.strainTarget, strainTarget) || other.strainTarget == strainTarget)&&(identical(other.currentStrain, currentStrain) || other.currentStrain == currentStrain)&&(identical(other.adaptiveGoal, adaptiveGoal) || other.adaptiveGoal == adaptiveGoal)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.confidenceReason, confidenceReason) || other.confidenceReason == confidenceReason)&&(identical(other.hrvStatus, hrvStatus) || other.hrvStatus == hrvStatus)&&(identical(other.intensityMinutes, intensityMinutes) || other.intensityMinutes == intensityMinutes)&&(identical(other.physiologicalStress, physiologicalStress) || other.physiologicalStress == physiologicalStress)&&const DeepCollectionEquality().equals(other.factors, factors)&&(identical(other.recoveryModeSuggested, recoveryModeSuggested) || other.recoveryModeSuggested == recoveryModeSuggested));
}


@override
int get hashCode => Object.hashAll([runtimeType,state,score,bodyEnergyScore,trainingReadinessScore,recommendationType,statusTitle,recommendation,explanation,alternative,suggestedWorkout,avoid,strainTarget,currentStrain,adaptiveGoal,confidence,confidenceReason,hrvStatus,intensityMinutes,physiologicalStress,const DeepCollectionEquality().hash(factors),recoveryModeSuggested]);

@override
String toString() {
  return 'DailyReadinessInsight(state: $state, score: $score, bodyEnergyScore: $bodyEnergyScore, trainingReadinessScore: $trainingReadinessScore, recommendationType: $recommendationType, statusTitle: $statusTitle, recommendation: $recommendation, explanation: $explanation, alternative: $alternative, suggestedWorkout: $suggestedWorkout, avoid: $avoid, strainTarget: $strainTarget, currentStrain: $currentStrain, adaptiveGoal: $adaptiveGoal, confidence: $confidence, confidenceReason: $confidenceReason, hrvStatus: $hrvStatus, intensityMinutes: $intensityMinutes, physiologicalStress: $physiologicalStress, factors: $factors, recoveryModeSuggested: $recoveryModeSuggested)';
}


}

/// @nodoc
abstract mixin class $DailyReadinessInsightCopyWith<$Res>  {
  factory $DailyReadinessInsightCopyWith(DailyReadinessInsight value, $Res Function(DailyReadinessInsight) _then) = _$DailyReadinessInsightCopyWithImpl;
@useResult
$Res call({
 ReadinessState state, int score, int bodyEnergyScore, int trainingReadinessScore, ReadinessRecommendationType recommendationType, String statusTitle, String recommendation, String explanation, String alternative, String suggestedWorkout, String avoid, String strainTarget, String? currentStrain, String adaptiveGoal, ReadinessConfidence confidence, String confidenceReason, HrvStatusInsight hrvStatus, IntensityMinutesReadinessInsight intensityMinutes, PhysiologicalStressEstimate physiologicalStress, List<DailyReadinessFactor> factors, bool recoveryModeSuggested
});


$HrvStatusInsightCopyWith<$Res> get hrvStatus;$IntensityMinutesReadinessInsightCopyWith<$Res> get intensityMinutes;$PhysiologicalStressEstimateCopyWith<$Res> get physiologicalStress;

}
/// @nodoc
class _$DailyReadinessInsightCopyWithImpl<$Res>
    implements $DailyReadinessInsightCopyWith<$Res> {
  _$DailyReadinessInsightCopyWithImpl(this._self, this._then);

  final DailyReadinessInsight _self;
  final $Res Function(DailyReadinessInsight) _then;

/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? state = null,Object? score = null,Object? bodyEnergyScore = null,Object? trainingReadinessScore = null,Object? recommendationType = null,Object? statusTitle = null,Object? recommendation = null,Object? explanation = null,Object? alternative = null,Object? suggestedWorkout = null,Object? avoid = null,Object? strainTarget = null,Object? currentStrain = freezed,Object? adaptiveGoal = null,Object? confidence = null,Object? confidenceReason = null,Object? hrvStatus = null,Object? intensityMinutes = null,Object? physiologicalStress = null,Object? factors = null,Object? recoveryModeSuggested = null,}) {
  return _then(_self.copyWith(
state: null == state ? _self.state : state // ignore: cast_nullable_to_non_nullable
as ReadinessState,score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,bodyEnergyScore: null == bodyEnergyScore ? _self.bodyEnergyScore : bodyEnergyScore // ignore: cast_nullable_to_non_nullable
as int,trainingReadinessScore: null == trainingReadinessScore ? _self.trainingReadinessScore : trainingReadinessScore // ignore: cast_nullable_to_non_nullable
as int,recommendationType: null == recommendationType ? _self.recommendationType : recommendationType // ignore: cast_nullable_to_non_nullable
as ReadinessRecommendationType,statusTitle: null == statusTitle ? _self.statusTitle : statusTitle // ignore: cast_nullable_to_non_nullable
as String,recommendation: null == recommendation ? _self.recommendation : recommendation // ignore: cast_nullable_to_non_nullable
as String,explanation: null == explanation ? _self.explanation : explanation // ignore: cast_nullable_to_non_nullable
as String,alternative: null == alternative ? _self.alternative : alternative // ignore: cast_nullable_to_non_nullable
as String,suggestedWorkout: null == suggestedWorkout ? _self.suggestedWorkout : suggestedWorkout // ignore: cast_nullable_to_non_nullable
as String,avoid: null == avoid ? _self.avoid : avoid // ignore: cast_nullable_to_non_nullable
as String,strainTarget: null == strainTarget ? _self.strainTarget : strainTarget // ignore: cast_nullable_to_non_nullable
as String,currentStrain: freezed == currentStrain ? _self.currentStrain : currentStrain // ignore: cast_nullable_to_non_nullable
as String?,adaptiveGoal: null == adaptiveGoal ? _self.adaptiveGoal : adaptiveGoal // ignore: cast_nullable_to_non_nullable
as String,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as ReadinessConfidence,confidenceReason: null == confidenceReason ? _self.confidenceReason : confidenceReason // ignore: cast_nullable_to_non_nullable
as String,hrvStatus: null == hrvStatus ? _self.hrvStatus : hrvStatus // ignore: cast_nullable_to_non_nullable
as HrvStatusInsight,intensityMinutes: null == intensityMinutes ? _self.intensityMinutes : intensityMinutes // ignore: cast_nullable_to_non_nullable
as IntensityMinutesReadinessInsight,physiologicalStress: null == physiologicalStress ? _self.physiologicalStress : physiologicalStress // ignore: cast_nullable_to_non_nullable
as PhysiologicalStressEstimate,factors: null == factors ? _self.factors : factors // ignore: cast_nullable_to_non_nullable
as List<DailyReadinessFactor>,recoveryModeSuggested: null == recoveryModeSuggested ? _self.recoveryModeSuggested : recoveryModeSuggested // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}
/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HrvStatusInsightCopyWith<$Res> get hrvStatus {
  
  return $HrvStatusInsightCopyWith<$Res>(_self.hrvStatus, (value) {
    return _then(_self.copyWith(hrvStatus: value));
  });
}/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$IntensityMinutesReadinessInsightCopyWith<$Res> get intensityMinutes {
  
  return $IntensityMinutesReadinessInsightCopyWith<$Res>(_self.intensityMinutes, (value) {
    return _then(_self.copyWith(intensityMinutes: value));
  });
}/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PhysiologicalStressEstimateCopyWith<$Res> get physiologicalStress {
  
  return $PhysiologicalStressEstimateCopyWith<$Res>(_self.physiologicalStress, (value) {
    return _then(_self.copyWith(physiologicalStress: value));
  });
}
}


/// Adds pattern-matching-related methods to [DailyReadinessInsight].
extension DailyReadinessInsightPatterns on DailyReadinessInsight {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyReadinessInsight value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyReadinessInsight() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyReadinessInsight value)  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessInsight():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyReadinessInsight value)?  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessInsight() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( ReadinessState state,  int score,  int bodyEnergyScore,  int trainingReadinessScore,  ReadinessRecommendationType recommendationType,  String statusTitle,  String recommendation,  String explanation,  String alternative,  String suggestedWorkout,  String avoid,  String strainTarget,  String? currentStrain,  String adaptiveGoal,  ReadinessConfidence confidence,  String confidenceReason,  HrvStatusInsight hrvStatus,  IntensityMinutesReadinessInsight intensityMinutes,  PhysiologicalStressEstimate physiologicalStress,  List<DailyReadinessFactor> factors,  bool recoveryModeSuggested)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyReadinessInsight() when $default != null:
return $default(_that.state,_that.score,_that.bodyEnergyScore,_that.trainingReadinessScore,_that.recommendationType,_that.statusTitle,_that.recommendation,_that.explanation,_that.alternative,_that.suggestedWorkout,_that.avoid,_that.strainTarget,_that.currentStrain,_that.adaptiveGoal,_that.confidence,_that.confidenceReason,_that.hrvStatus,_that.intensityMinutes,_that.physiologicalStress,_that.factors,_that.recoveryModeSuggested);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( ReadinessState state,  int score,  int bodyEnergyScore,  int trainingReadinessScore,  ReadinessRecommendationType recommendationType,  String statusTitle,  String recommendation,  String explanation,  String alternative,  String suggestedWorkout,  String avoid,  String strainTarget,  String? currentStrain,  String adaptiveGoal,  ReadinessConfidence confidence,  String confidenceReason,  HrvStatusInsight hrvStatus,  IntensityMinutesReadinessInsight intensityMinutes,  PhysiologicalStressEstimate physiologicalStress,  List<DailyReadinessFactor> factors,  bool recoveryModeSuggested)  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessInsight():
return $default(_that.state,_that.score,_that.bodyEnergyScore,_that.trainingReadinessScore,_that.recommendationType,_that.statusTitle,_that.recommendation,_that.explanation,_that.alternative,_that.suggestedWorkout,_that.avoid,_that.strainTarget,_that.currentStrain,_that.adaptiveGoal,_that.confidence,_that.confidenceReason,_that.hrvStatus,_that.intensityMinutes,_that.physiologicalStress,_that.factors,_that.recoveryModeSuggested);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( ReadinessState state,  int score,  int bodyEnergyScore,  int trainingReadinessScore,  ReadinessRecommendationType recommendationType,  String statusTitle,  String recommendation,  String explanation,  String alternative,  String suggestedWorkout,  String avoid,  String strainTarget,  String? currentStrain,  String adaptiveGoal,  ReadinessConfidence confidence,  String confidenceReason,  HrvStatusInsight hrvStatus,  IntensityMinutesReadinessInsight intensityMinutes,  PhysiologicalStressEstimate physiologicalStress,  List<DailyReadinessFactor> factors,  bool recoveryModeSuggested)?  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessInsight() when $default != null:
return $default(_that.state,_that.score,_that.bodyEnergyScore,_that.trainingReadinessScore,_that.recommendationType,_that.statusTitle,_that.recommendation,_that.explanation,_that.alternative,_that.suggestedWorkout,_that.avoid,_that.strainTarget,_that.currentStrain,_that.adaptiveGoal,_that.confidence,_that.confidenceReason,_that.hrvStatus,_that.intensityMinutes,_that.physiologicalStress,_that.factors,_that.recoveryModeSuggested);case _:
  return null;

}
}

}

/// @nodoc


class _DailyReadinessInsight implements DailyReadinessInsight {
  const _DailyReadinessInsight({required this.state, required this.score, required this.bodyEnergyScore, required this.trainingReadinessScore, required this.recommendationType, required this.statusTitle, required this.recommendation, required this.explanation, required this.alternative, required this.suggestedWorkout, required this.avoid, required this.strainTarget, required this.currentStrain, required this.adaptiveGoal, required this.confidence, required this.confidenceReason, required this.hrvStatus, required this.intensityMinutes, required this.physiologicalStress, required final  List<DailyReadinessFactor> factors, required this.recoveryModeSuggested}): _factors = factors;
  

@override final  ReadinessState state;
@override final  int score;
@override final  int bodyEnergyScore;
@override final  int trainingReadinessScore;
@override final  ReadinessRecommendationType recommendationType;
@override final  String statusTitle;
@override final  String recommendation;
@override final  String explanation;
@override final  String alternative;
@override final  String suggestedWorkout;
@override final  String avoid;
@override final  String strainTarget;
@override final  String? currentStrain;
@override final  String adaptiveGoal;
@override final  ReadinessConfidence confidence;
@override final  String confidenceReason;
@override final  HrvStatusInsight hrvStatus;
@override final  IntensityMinutesReadinessInsight intensityMinutes;
@override final  PhysiologicalStressEstimate physiologicalStress;
 final  List<DailyReadinessFactor> _factors;
@override List<DailyReadinessFactor> get factors {
  if (_factors is EqualUnmodifiableListView) return _factors;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_factors);
}

@override final  bool recoveryModeSuggested;

/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyReadinessInsightCopyWith<_DailyReadinessInsight> get copyWith => __$DailyReadinessInsightCopyWithImpl<_DailyReadinessInsight>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyReadinessInsight&&(identical(other.state, state) || other.state == state)&&(identical(other.score, score) || other.score == score)&&(identical(other.bodyEnergyScore, bodyEnergyScore) || other.bodyEnergyScore == bodyEnergyScore)&&(identical(other.trainingReadinessScore, trainingReadinessScore) || other.trainingReadinessScore == trainingReadinessScore)&&(identical(other.recommendationType, recommendationType) || other.recommendationType == recommendationType)&&(identical(other.statusTitle, statusTitle) || other.statusTitle == statusTitle)&&(identical(other.recommendation, recommendation) || other.recommendation == recommendation)&&(identical(other.explanation, explanation) || other.explanation == explanation)&&(identical(other.alternative, alternative) || other.alternative == alternative)&&(identical(other.suggestedWorkout, suggestedWorkout) || other.suggestedWorkout == suggestedWorkout)&&(identical(other.avoid, avoid) || other.avoid == avoid)&&(identical(other.strainTarget, strainTarget) || other.strainTarget == strainTarget)&&(identical(other.currentStrain, currentStrain) || other.currentStrain == currentStrain)&&(identical(other.adaptiveGoal, adaptiveGoal) || other.adaptiveGoal == adaptiveGoal)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.confidenceReason, confidenceReason) || other.confidenceReason == confidenceReason)&&(identical(other.hrvStatus, hrvStatus) || other.hrvStatus == hrvStatus)&&(identical(other.intensityMinutes, intensityMinutes) || other.intensityMinutes == intensityMinutes)&&(identical(other.physiologicalStress, physiologicalStress) || other.physiologicalStress == physiologicalStress)&&const DeepCollectionEquality().equals(other._factors, _factors)&&(identical(other.recoveryModeSuggested, recoveryModeSuggested) || other.recoveryModeSuggested == recoveryModeSuggested));
}


@override
int get hashCode => Object.hashAll([runtimeType,state,score,bodyEnergyScore,trainingReadinessScore,recommendationType,statusTitle,recommendation,explanation,alternative,suggestedWorkout,avoid,strainTarget,currentStrain,adaptiveGoal,confidence,confidenceReason,hrvStatus,intensityMinutes,physiologicalStress,const DeepCollectionEquality().hash(_factors),recoveryModeSuggested]);

@override
String toString() {
  return 'DailyReadinessInsight(state: $state, score: $score, bodyEnergyScore: $bodyEnergyScore, trainingReadinessScore: $trainingReadinessScore, recommendationType: $recommendationType, statusTitle: $statusTitle, recommendation: $recommendation, explanation: $explanation, alternative: $alternative, suggestedWorkout: $suggestedWorkout, avoid: $avoid, strainTarget: $strainTarget, currentStrain: $currentStrain, adaptiveGoal: $adaptiveGoal, confidence: $confidence, confidenceReason: $confidenceReason, hrvStatus: $hrvStatus, intensityMinutes: $intensityMinutes, physiologicalStress: $physiologicalStress, factors: $factors, recoveryModeSuggested: $recoveryModeSuggested)';
}


}

/// @nodoc
abstract mixin class _$DailyReadinessInsightCopyWith<$Res> implements $DailyReadinessInsightCopyWith<$Res> {
  factory _$DailyReadinessInsightCopyWith(_DailyReadinessInsight value, $Res Function(_DailyReadinessInsight) _then) = __$DailyReadinessInsightCopyWithImpl;
@override @useResult
$Res call({
 ReadinessState state, int score, int bodyEnergyScore, int trainingReadinessScore, ReadinessRecommendationType recommendationType, String statusTitle, String recommendation, String explanation, String alternative, String suggestedWorkout, String avoid, String strainTarget, String? currentStrain, String adaptiveGoal, ReadinessConfidence confidence, String confidenceReason, HrvStatusInsight hrvStatus, IntensityMinutesReadinessInsight intensityMinutes, PhysiologicalStressEstimate physiologicalStress, List<DailyReadinessFactor> factors, bool recoveryModeSuggested
});


@override $HrvStatusInsightCopyWith<$Res> get hrvStatus;@override $IntensityMinutesReadinessInsightCopyWith<$Res> get intensityMinutes;@override $PhysiologicalStressEstimateCopyWith<$Res> get physiologicalStress;

}
/// @nodoc
class __$DailyReadinessInsightCopyWithImpl<$Res>
    implements _$DailyReadinessInsightCopyWith<$Res> {
  __$DailyReadinessInsightCopyWithImpl(this._self, this._then);

  final _DailyReadinessInsight _self;
  final $Res Function(_DailyReadinessInsight) _then;

/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? state = null,Object? score = null,Object? bodyEnergyScore = null,Object? trainingReadinessScore = null,Object? recommendationType = null,Object? statusTitle = null,Object? recommendation = null,Object? explanation = null,Object? alternative = null,Object? suggestedWorkout = null,Object? avoid = null,Object? strainTarget = null,Object? currentStrain = freezed,Object? adaptiveGoal = null,Object? confidence = null,Object? confidenceReason = null,Object? hrvStatus = null,Object? intensityMinutes = null,Object? physiologicalStress = null,Object? factors = null,Object? recoveryModeSuggested = null,}) {
  return _then(_DailyReadinessInsight(
state: null == state ? _self.state : state // ignore: cast_nullable_to_non_nullable
as ReadinessState,score: null == score ? _self.score : score // ignore: cast_nullable_to_non_nullable
as int,bodyEnergyScore: null == bodyEnergyScore ? _self.bodyEnergyScore : bodyEnergyScore // ignore: cast_nullable_to_non_nullable
as int,trainingReadinessScore: null == trainingReadinessScore ? _self.trainingReadinessScore : trainingReadinessScore // ignore: cast_nullable_to_non_nullable
as int,recommendationType: null == recommendationType ? _self.recommendationType : recommendationType // ignore: cast_nullable_to_non_nullable
as ReadinessRecommendationType,statusTitle: null == statusTitle ? _self.statusTitle : statusTitle // ignore: cast_nullable_to_non_nullable
as String,recommendation: null == recommendation ? _self.recommendation : recommendation // ignore: cast_nullable_to_non_nullable
as String,explanation: null == explanation ? _self.explanation : explanation // ignore: cast_nullable_to_non_nullable
as String,alternative: null == alternative ? _self.alternative : alternative // ignore: cast_nullable_to_non_nullable
as String,suggestedWorkout: null == suggestedWorkout ? _self.suggestedWorkout : suggestedWorkout // ignore: cast_nullable_to_non_nullable
as String,avoid: null == avoid ? _self.avoid : avoid // ignore: cast_nullable_to_non_nullable
as String,strainTarget: null == strainTarget ? _self.strainTarget : strainTarget // ignore: cast_nullable_to_non_nullable
as String,currentStrain: freezed == currentStrain ? _self.currentStrain : currentStrain // ignore: cast_nullable_to_non_nullable
as String?,adaptiveGoal: null == adaptiveGoal ? _self.adaptiveGoal : adaptiveGoal // ignore: cast_nullable_to_non_nullable
as String,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as ReadinessConfidence,confidenceReason: null == confidenceReason ? _self.confidenceReason : confidenceReason // ignore: cast_nullable_to_non_nullable
as String,hrvStatus: null == hrvStatus ? _self.hrvStatus : hrvStatus // ignore: cast_nullable_to_non_nullable
as HrvStatusInsight,intensityMinutes: null == intensityMinutes ? _self.intensityMinutes : intensityMinutes // ignore: cast_nullable_to_non_nullable
as IntensityMinutesReadinessInsight,physiologicalStress: null == physiologicalStress ? _self.physiologicalStress : physiologicalStress // ignore: cast_nullable_to_non_nullable
as PhysiologicalStressEstimate,factors: null == factors ? _self._factors : factors // ignore: cast_nullable_to_non_nullable
as List<DailyReadinessFactor>,recoveryModeSuggested: null == recoveryModeSuggested ? _self.recoveryModeSuggested : recoveryModeSuggested // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HrvStatusInsightCopyWith<$Res> get hrvStatus {
  
  return $HrvStatusInsightCopyWith<$Res>(_self.hrvStatus, (value) {
    return _then(_self.copyWith(hrvStatus: value));
  });
}/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$IntensityMinutesReadinessInsightCopyWith<$Res> get intensityMinutes {
  
  return $IntensityMinutesReadinessInsightCopyWith<$Res>(_self.intensityMinutes, (value) {
    return _then(_self.copyWith(intensityMinutes: value));
  });
}/// Create a copy of DailyReadinessInsight
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PhysiologicalStressEstimateCopyWith<$Res> get physiologicalStress {
  
  return $PhysiologicalStressEstimateCopyWith<$Res>(_self.physiologicalStress, (value) {
    return _then(_self.copyWith(physiologicalStress: value));
  });
}
}

// dart format on
