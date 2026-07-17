// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'heart_rate_recovery.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HeartRateRecoveryMark {

 Duration get offset; int? get heartRateBpm; int? get dropBpm; DateTime? get sampleTime;/// How far the sample actually sat from the mark. Lets the UI be honest: "+58s".
 Duration? get sampleSkew;
/// Create a copy of HeartRateRecoveryMark
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartRateRecoveryMarkCopyWith<HeartRateRecoveryMark> get copyWith => _$HeartRateRecoveryMarkCopyWithImpl<HeartRateRecoveryMark>(this as HeartRateRecoveryMark, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartRateRecoveryMark&&(identical(other.offset, offset) || other.offset == offset)&&(identical(other.heartRateBpm, heartRateBpm) || other.heartRateBpm == heartRateBpm)&&(identical(other.dropBpm, dropBpm) || other.dropBpm == dropBpm)&&(identical(other.sampleTime, sampleTime) || other.sampleTime == sampleTime)&&(identical(other.sampleSkew, sampleSkew) || other.sampleSkew == sampleSkew));
}


@override
int get hashCode => Object.hash(runtimeType,offset,heartRateBpm,dropBpm,sampleTime,sampleSkew);

@override
String toString() {
  return 'HeartRateRecoveryMark(offset: $offset, heartRateBpm: $heartRateBpm, dropBpm: $dropBpm, sampleTime: $sampleTime, sampleSkew: $sampleSkew)';
}


}

/// @nodoc
abstract mixin class $HeartRateRecoveryMarkCopyWith<$Res>  {
  factory $HeartRateRecoveryMarkCopyWith(HeartRateRecoveryMark value, $Res Function(HeartRateRecoveryMark) _then) = _$HeartRateRecoveryMarkCopyWithImpl;
@useResult
$Res call({
 Duration offset, int? heartRateBpm, int? dropBpm, DateTime? sampleTime, Duration? sampleSkew
});




}
/// @nodoc
class _$HeartRateRecoveryMarkCopyWithImpl<$Res>
    implements $HeartRateRecoveryMarkCopyWith<$Res> {
  _$HeartRateRecoveryMarkCopyWithImpl(this._self, this._then);

  final HeartRateRecoveryMark _self;
  final $Res Function(HeartRateRecoveryMark) _then;

/// Create a copy of HeartRateRecoveryMark
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? offset = null,Object? heartRateBpm = freezed,Object? dropBpm = freezed,Object? sampleTime = freezed,Object? sampleSkew = freezed,}) {
  return _then(_self.copyWith(
offset: null == offset ? _self.offset : offset // ignore: cast_nullable_to_non_nullable
as Duration,heartRateBpm: freezed == heartRateBpm ? _self.heartRateBpm : heartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,dropBpm: freezed == dropBpm ? _self.dropBpm : dropBpm // ignore: cast_nullable_to_non_nullable
as int?,sampleTime: freezed == sampleTime ? _self.sampleTime : sampleTime // ignore: cast_nullable_to_non_nullable
as DateTime?,sampleSkew: freezed == sampleSkew ? _self.sampleSkew : sampleSkew // ignore: cast_nullable_to_non_nullable
as Duration?,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartRateRecoveryMark].
extension HeartRateRecoveryMarkPatterns on HeartRateRecoveryMark {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartRateRecoveryMark value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartRateRecoveryMark() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartRateRecoveryMark value)  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryMark():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartRateRecoveryMark value)?  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryMark() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( Duration offset,  int? heartRateBpm,  int? dropBpm,  DateTime? sampleTime,  Duration? sampleSkew)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartRateRecoveryMark() when $default != null:
return $default(_that.offset,_that.heartRateBpm,_that.dropBpm,_that.sampleTime,_that.sampleSkew);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( Duration offset,  int? heartRateBpm,  int? dropBpm,  DateTime? sampleTime,  Duration? sampleSkew)  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryMark():
return $default(_that.offset,_that.heartRateBpm,_that.dropBpm,_that.sampleTime,_that.sampleSkew);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( Duration offset,  int? heartRateBpm,  int? dropBpm,  DateTime? sampleTime,  Duration? sampleSkew)?  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryMark() when $default != null:
return $default(_that.offset,_that.heartRateBpm,_that.dropBpm,_that.sampleTime,_that.sampleSkew);case _:
  return null;

}
}

}

/// @nodoc


class _HeartRateRecoveryMark implements HeartRateRecoveryMark {
  const _HeartRateRecoveryMark({required this.offset, required this.heartRateBpm, required this.dropBpm, required this.sampleTime, required this.sampleSkew});
  

@override final  Duration offset;
@override final  int? heartRateBpm;
@override final  int? dropBpm;
@override final  DateTime? sampleTime;
/// How far the sample actually sat from the mark. Lets the UI be honest: "+58s".
@override final  Duration? sampleSkew;

/// Create a copy of HeartRateRecoveryMark
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartRateRecoveryMarkCopyWith<_HeartRateRecoveryMark> get copyWith => __$HeartRateRecoveryMarkCopyWithImpl<_HeartRateRecoveryMark>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartRateRecoveryMark&&(identical(other.offset, offset) || other.offset == offset)&&(identical(other.heartRateBpm, heartRateBpm) || other.heartRateBpm == heartRateBpm)&&(identical(other.dropBpm, dropBpm) || other.dropBpm == dropBpm)&&(identical(other.sampleTime, sampleTime) || other.sampleTime == sampleTime)&&(identical(other.sampleSkew, sampleSkew) || other.sampleSkew == sampleSkew));
}


@override
int get hashCode => Object.hash(runtimeType,offset,heartRateBpm,dropBpm,sampleTime,sampleSkew);

@override
String toString() {
  return 'HeartRateRecoveryMark(offset: $offset, heartRateBpm: $heartRateBpm, dropBpm: $dropBpm, sampleTime: $sampleTime, sampleSkew: $sampleSkew)';
}


}

/// @nodoc
abstract mixin class _$HeartRateRecoveryMarkCopyWith<$Res> implements $HeartRateRecoveryMarkCopyWith<$Res> {
  factory _$HeartRateRecoveryMarkCopyWith(_HeartRateRecoveryMark value, $Res Function(_HeartRateRecoveryMark) _then) = __$HeartRateRecoveryMarkCopyWithImpl;
@override @useResult
$Res call({
 Duration offset, int? heartRateBpm, int? dropBpm, DateTime? sampleTime, Duration? sampleSkew
});




}
/// @nodoc
class __$HeartRateRecoveryMarkCopyWithImpl<$Res>
    implements _$HeartRateRecoveryMarkCopyWith<$Res> {
  __$HeartRateRecoveryMarkCopyWithImpl(this._self, this._then);

  final _HeartRateRecoveryMark _self;
  final $Res Function(_HeartRateRecoveryMark) _then;

/// Create a copy of HeartRateRecoveryMark
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? offset = null,Object? heartRateBpm = freezed,Object? dropBpm = freezed,Object? sampleTime = freezed,Object? sampleSkew = freezed,}) {
  return _then(_HeartRateRecoveryMark(
offset: null == offset ? _self.offset : offset // ignore: cast_nullable_to_non_nullable
as Duration,heartRateBpm: freezed == heartRateBpm ? _self.heartRateBpm : heartRateBpm // ignore: cast_nullable_to_non_nullable
as int?,dropBpm: freezed == dropBpm ? _self.dropBpm : dropBpm // ignore: cast_nullable_to_non_nullable
as int?,sampleTime: freezed == sampleTime ? _self.sampleTime : sampleTime // ignore: cast_nullable_to_non_nullable
as DateTime?,sampleSkew: freezed == sampleSkew ? _self.sampleSkew : sampleSkew // ignore: cast_nullable_to_non_nullable
as Duration?,
  ));
}


}

/// @nodoc
mixin _$HeartRateRecoveryReading {

 DateTime? get recoveryStart; HeartRateRecoveryStartSource get source; int? get peakBpm; DateTime? get peakTime; int get peakWindowSeconds; int get peakWindowSampleCount; List<HeartRateRecoveryMark> get marks; int? get maxHeartRateBpmUsed; bool get maxHeartRateEstimated; double? get peakFractionOfMax; double? get medianRecoveryGapSeconds; int get recoverySampleCount; HeartRateRecoveryQuality get quality; Set<HeartRateRecoveryIssue> get issues;
/// Create a copy of HeartRateRecoveryReading
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartRateRecoveryReadingCopyWith<HeartRateRecoveryReading> get copyWith => _$HeartRateRecoveryReadingCopyWithImpl<HeartRateRecoveryReading>(this as HeartRateRecoveryReading, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartRateRecoveryReading&&(identical(other.recoveryStart, recoveryStart) || other.recoveryStart == recoveryStart)&&(identical(other.source, source) || other.source == source)&&(identical(other.peakBpm, peakBpm) || other.peakBpm == peakBpm)&&(identical(other.peakTime, peakTime) || other.peakTime == peakTime)&&(identical(other.peakWindowSeconds, peakWindowSeconds) || other.peakWindowSeconds == peakWindowSeconds)&&(identical(other.peakWindowSampleCount, peakWindowSampleCount) || other.peakWindowSampleCount == peakWindowSampleCount)&&const DeepCollectionEquality().equals(other.marks, marks)&&(identical(other.maxHeartRateBpmUsed, maxHeartRateBpmUsed) || other.maxHeartRateBpmUsed == maxHeartRateBpmUsed)&&(identical(other.maxHeartRateEstimated, maxHeartRateEstimated) || other.maxHeartRateEstimated == maxHeartRateEstimated)&&(identical(other.peakFractionOfMax, peakFractionOfMax) || other.peakFractionOfMax == peakFractionOfMax)&&(identical(other.medianRecoveryGapSeconds, medianRecoveryGapSeconds) || other.medianRecoveryGapSeconds == medianRecoveryGapSeconds)&&(identical(other.recoverySampleCount, recoverySampleCount) || other.recoverySampleCount == recoverySampleCount)&&(identical(other.quality, quality) || other.quality == quality)&&const DeepCollectionEquality().equals(other.issues, issues));
}


@override
int get hashCode => Object.hash(runtimeType,recoveryStart,source,peakBpm,peakTime,peakWindowSeconds,peakWindowSampleCount,const DeepCollectionEquality().hash(marks),maxHeartRateBpmUsed,maxHeartRateEstimated,peakFractionOfMax,medianRecoveryGapSeconds,recoverySampleCount,quality,const DeepCollectionEquality().hash(issues));

@override
String toString() {
  return 'HeartRateRecoveryReading(recoveryStart: $recoveryStart, source: $source, peakBpm: $peakBpm, peakTime: $peakTime, peakWindowSeconds: $peakWindowSeconds, peakWindowSampleCount: $peakWindowSampleCount, marks: $marks, maxHeartRateBpmUsed: $maxHeartRateBpmUsed, maxHeartRateEstimated: $maxHeartRateEstimated, peakFractionOfMax: $peakFractionOfMax, medianRecoveryGapSeconds: $medianRecoveryGapSeconds, recoverySampleCount: $recoverySampleCount, quality: $quality, issues: $issues)';
}


}

/// @nodoc
abstract mixin class $HeartRateRecoveryReadingCopyWith<$Res>  {
  factory $HeartRateRecoveryReadingCopyWith(HeartRateRecoveryReading value, $Res Function(HeartRateRecoveryReading) _then) = _$HeartRateRecoveryReadingCopyWithImpl;
@useResult
$Res call({
 DateTime? recoveryStart, HeartRateRecoveryStartSource source, int? peakBpm, DateTime? peakTime, int peakWindowSeconds, int peakWindowSampleCount, List<HeartRateRecoveryMark> marks, int? maxHeartRateBpmUsed, bool maxHeartRateEstimated, double? peakFractionOfMax, double? medianRecoveryGapSeconds, int recoverySampleCount, HeartRateRecoveryQuality quality, Set<HeartRateRecoveryIssue> issues
});




}
/// @nodoc
class _$HeartRateRecoveryReadingCopyWithImpl<$Res>
    implements $HeartRateRecoveryReadingCopyWith<$Res> {
  _$HeartRateRecoveryReadingCopyWithImpl(this._self, this._then);

  final HeartRateRecoveryReading _self;
  final $Res Function(HeartRateRecoveryReading) _then;

/// Create a copy of HeartRateRecoveryReading
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? recoveryStart = freezed,Object? source = null,Object? peakBpm = freezed,Object? peakTime = freezed,Object? peakWindowSeconds = null,Object? peakWindowSampleCount = null,Object? marks = null,Object? maxHeartRateBpmUsed = freezed,Object? maxHeartRateEstimated = null,Object? peakFractionOfMax = freezed,Object? medianRecoveryGapSeconds = freezed,Object? recoverySampleCount = null,Object? quality = null,Object? issues = null,}) {
  return _then(_self.copyWith(
recoveryStart: freezed == recoveryStart ? _self.recoveryStart : recoveryStart // ignore: cast_nullable_to_non_nullable
as DateTime?,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryStartSource,peakBpm: freezed == peakBpm ? _self.peakBpm : peakBpm // ignore: cast_nullable_to_non_nullable
as int?,peakTime: freezed == peakTime ? _self.peakTime : peakTime // ignore: cast_nullable_to_non_nullable
as DateTime?,peakWindowSeconds: null == peakWindowSeconds ? _self.peakWindowSeconds : peakWindowSeconds // ignore: cast_nullable_to_non_nullable
as int,peakWindowSampleCount: null == peakWindowSampleCount ? _self.peakWindowSampleCount : peakWindowSampleCount // ignore: cast_nullable_to_non_nullable
as int,marks: null == marks ? _self.marks : marks // ignore: cast_nullable_to_non_nullable
as List<HeartRateRecoveryMark>,maxHeartRateBpmUsed: freezed == maxHeartRateBpmUsed ? _self.maxHeartRateBpmUsed : maxHeartRateBpmUsed // ignore: cast_nullable_to_non_nullable
as int?,maxHeartRateEstimated: null == maxHeartRateEstimated ? _self.maxHeartRateEstimated : maxHeartRateEstimated // ignore: cast_nullable_to_non_nullable
as bool,peakFractionOfMax: freezed == peakFractionOfMax ? _self.peakFractionOfMax : peakFractionOfMax // ignore: cast_nullable_to_non_nullable
as double?,medianRecoveryGapSeconds: freezed == medianRecoveryGapSeconds ? _self.medianRecoveryGapSeconds : medianRecoveryGapSeconds // ignore: cast_nullable_to_non_nullable
as double?,recoverySampleCount: null == recoverySampleCount ? _self.recoverySampleCount : recoverySampleCount // ignore: cast_nullable_to_non_nullable
as int,quality: null == quality ? _self.quality : quality // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryQuality,issues: null == issues ? _self.issues : issues // ignore: cast_nullable_to_non_nullable
as Set<HeartRateRecoveryIssue>,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartRateRecoveryReading].
extension HeartRateRecoveryReadingPatterns on HeartRateRecoveryReading {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartRateRecoveryReading value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartRateRecoveryReading() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartRateRecoveryReading value)  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryReading():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartRateRecoveryReading value)?  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryReading() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime? recoveryStart,  HeartRateRecoveryStartSource source,  int? peakBpm,  DateTime? peakTime,  int peakWindowSeconds,  int peakWindowSampleCount,  List<HeartRateRecoveryMark> marks,  int? maxHeartRateBpmUsed,  bool maxHeartRateEstimated,  double? peakFractionOfMax,  double? medianRecoveryGapSeconds,  int recoverySampleCount,  HeartRateRecoveryQuality quality,  Set<HeartRateRecoveryIssue> issues)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartRateRecoveryReading() when $default != null:
return $default(_that.recoveryStart,_that.source,_that.peakBpm,_that.peakTime,_that.peakWindowSeconds,_that.peakWindowSampleCount,_that.marks,_that.maxHeartRateBpmUsed,_that.maxHeartRateEstimated,_that.peakFractionOfMax,_that.medianRecoveryGapSeconds,_that.recoverySampleCount,_that.quality,_that.issues);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime? recoveryStart,  HeartRateRecoveryStartSource source,  int? peakBpm,  DateTime? peakTime,  int peakWindowSeconds,  int peakWindowSampleCount,  List<HeartRateRecoveryMark> marks,  int? maxHeartRateBpmUsed,  bool maxHeartRateEstimated,  double? peakFractionOfMax,  double? medianRecoveryGapSeconds,  int recoverySampleCount,  HeartRateRecoveryQuality quality,  Set<HeartRateRecoveryIssue> issues)  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryReading():
return $default(_that.recoveryStart,_that.source,_that.peakBpm,_that.peakTime,_that.peakWindowSeconds,_that.peakWindowSampleCount,_that.marks,_that.maxHeartRateBpmUsed,_that.maxHeartRateEstimated,_that.peakFractionOfMax,_that.medianRecoveryGapSeconds,_that.recoverySampleCount,_that.quality,_that.issues);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime? recoveryStart,  HeartRateRecoveryStartSource source,  int? peakBpm,  DateTime? peakTime,  int peakWindowSeconds,  int peakWindowSampleCount,  List<HeartRateRecoveryMark> marks,  int? maxHeartRateBpmUsed,  bool maxHeartRateEstimated,  double? peakFractionOfMax,  double? medianRecoveryGapSeconds,  int recoverySampleCount,  HeartRateRecoveryQuality quality,  Set<HeartRateRecoveryIssue> issues)?  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryReading() when $default != null:
return $default(_that.recoveryStart,_that.source,_that.peakBpm,_that.peakTime,_that.peakWindowSeconds,_that.peakWindowSampleCount,_that.marks,_that.maxHeartRateBpmUsed,_that.maxHeartRateEstimated,_that.peakFractionOfMax,_that.medianRecoveryGapSeconds,_that.recoverySampleCount,_that.quality,_that.issues);case _:
  return null;

}
}

}

/// @nodoc


class _HeartRateRecoveryReading extends HeartRateRecoveryReading {
  const _HeartRateRecoveryReading({required this.recoveryStart, this.source = HeartRateRecoveryStartSource.sessionEnd, this.peakBpm, this.peakTime, this.peakWindowSeconds = 0, this.peakWindowSampleCount = 0, final  List<HeartRateRecoveryMark> marks = const <HeartRateRecoveryMark>[], this.maxHeartRateBpmUsed, this.maxHeartRateEstimated = false, this.peakFractionOfMax, this.medianRecoveryGapSeconds, this.recoverySampleCount = 0, this.quality = HeartRateRecoveryQuality.noData, final  Set<HeartRateRecoveryIssue> issues = const <HeartRateRecoveryIssue>{}}): _marks = marks,_issues = issues,super._();
  

@override final  DateTime? recoveryStart;
@override@JsonKey() final  HeartRateRecoveryStartSource source;
@override final  int? peakBpm;
@override final  DateTime? peakTime;
@override@JsonKey() final  int peakWindowSeconds;
@override@JsonKey() final  int peakWindowSampleCount;
 final  List<HeartRateRecoveryMark> _marks;
@override@JsonKey() List<HeartRateRecoveryMark> get marks {
  if (_marks is EqualUnmodifiableListView) return _marks;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_marks);
}

@override final  int? maxHeartRateBpmUsed;
@override@JsonKey() final  bool maxHeartRateEstimated;
@override final  double? peakFractionOfMax;
@override final  double? medianRecoveryGapSeconds;
@override@JsonKey() final  int recoverySampleCount;
@override@JsonKey() final  HeartRateRecoveryQuality quality;
 final  Set<HeartRateRecoveryIssue> _issues;
@override@JsonKey() Set<HeartRateRecoveryIssue> get issues {
  if (_issues is EqualUnmodifiableSetView) return _issues;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_issues);
}


/// Create a copy of HeartRateRecoveryReading
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartRateRecoveryReadingCopyWith<_HeartRateRecoveryReading> get copyWith => __$HeartRateRecoveryReadingCopyWithImpl<_HeartRateRecoveryReading>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartRateRecoveryReading&&(identical(other.recoveryStart, recoveryStart) || other.recoveryStart == recoveryStart)&&(identical(other.source, source) || other.source == source)&&(identical(other.peakBpm, peakBpm) || other.peakBpm == peakBpm)&&(identical(other.peakTime, peakTime) || other.peakTime == peakTime)&&(identical(other.peakWindowSeconds, peakWindowSeconds) || other.peakWindowSeconds == peakWindowSeconds)&&(identical(other.peakWindowSampleCount, peakWindowSampleCount) || other.peakWindowSampleCount == peakWindowSampleCount)&&const DeepCollectionEquality().equals(other._marks, _marks)&&(identical(other.maxHeartRateBpmUsed, maxHeartRateBpmUsed) || other.maxHeartRateBpmUsed == maxHeartRateBpmUsed)&&(identical(other.maxHeartRateEstimated, maxHeartRateEstimated) || other.maxHeartRateEstimated == maxHeartRateEstimated)&&(identical(other.peakFractionOfMax, peakFractionOfMax) || other.peakFractionOfMax == peakFractionOfMax)&&(identical(other.medianRecoveryGapSeconds, medianRecoveryGapSeconds) || other.medianRecoveryGapSeconds == medianRecoveryGapSeconds)&&(identical(other.recoverySampleCount, recoverySampleCount) || other.recoverySampleCount == recoverySampleCount)&&(identical(other.quality, quality) || other.quality == quality)&&const DeepCollectionEquality().equals(other._issues, _issues));
}


@override
int get hashCode => Object.hash(runtimeType,recoveryStart,source,peakBpm,peakTime,peakWindowSeconds,peakWindowSampleCount,const DeepCollectionEquality().hash(_marks),maxHeartRateBpmUsed,maxHeartRateEstimated,peakFractionOfMax,medianRecoveryGapSeconds,recoverySampleCount,quality,const DeepCollectionEquality().hash(_issues));

@override
String toString() {
  return 'HeartRateRecoveryReading(recoveryStart: $recoveryStart, source: $source, peakBpm: $peakBpm, peakTime: $peakTime, peakWindowSeconds: $peakWindowSeconds, peakWindowSampleCount: $peakWindowSampleCount, marks: $marks, maxHeartRateBpmUsed: $maxHeartRateBpmUsed, maxHeartRateEstimated: $maxHeartRateEstimated, peakFractionOfMax: $peakFractionOfMax, medianRecoveryGapSeconds: $medianRecoveryGapSeconds, recoverySampleCount: $recoverySampleCount, quality: $quality, issues: $issues)';
}


}

/// @nodoc
abstract mixin class _$HeartRateRecoveryReadingCopyWith<$Res> implements $HeartRateRecoveryReadingCopyWith<$Res> {
  factory _$HeartRateRecoveryReadingCopyWith(_HeartRateRecoveryReading value, $Res Function(_HeartRateRecoveryReading) _then) = __$HeartRateRecoveryReadingCopyWithImpl;
@override @useResult
$Res call({
 DateTime? recoveryStart, HeartRateRecoveryStartSource source, int? peakBpm, DateTime? peakTime, int peakWindowSeconds, int peakWindowSampleCount, List<HeartRateRecoveryMark> marks, int? maxHeartRateBpmUsed, bool maxHeartRateEstimated, double? peakFractionOfMax, double? medianRecoveryGapSeconds, int recoverySampleCount, HeartRateRecoveryQuality quality, Set<HeartRateRecoveryIssue> issues
});




}
/// @nodoc
class __$HeartRateRecoveryReadingCopyWithImpl<$Res>
    implements _$HeartRateRecoveryReadingCopyWith<$Res> {
  __$HeartRateRecoveryReadingCopyWithImpl(this._self, this._then);

  final _HeartRateRecoveryReading _self;
  final $Res Function(_HeartRateRecoveryReading) _then;

/// Create a copy of HeartRateRecoveryReading
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? recoveryStart = freezed,Object? source = null,Object? peakBpm = freezed,Object? peakTime = freezed,Object? peakWindowSeconds = null,Object? peakWindowSampleCount = null,Object? marks = null,Object? maxHeartRateBpmUsed = freezed,Object? maxHeartRateEstimated = null,Object? peakFractionOfMax = freezed,Object? medianRecoveryGapSeconds = freezed,Object? recoverySampleCount = null,Object? quality = null,Object? issues = null,}) {
  return _then(_HeartRateRecoveryReading(
recoveryStart: freezed == recoveryStart ? _self.recoveryStart : recoveryStart // ignore: cast_nullable_to_non_nullable
as DateTime?,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryStartSource,peakBpm: freezed == peakBpm ? _self.peakBpm : peakBpm // ignore: cast_nullable_to_non_nullable
as int?,peakTime: freezed == peakTime ? _self.peakTime : peakTime // ignore: cast_nullable_to_non_nullable
as DateTime?,peakWindowSeconds: null == peakWindowSeconds ? _self.peakWindowSeconds : peakWindowSeconds // ignore: cast_nullable_to_non_nullable
as int,peakWindowSampleCount: null == peakWindowSampleCount ? _self.peakWindowSampleCount : peakWindowSampleCount // ignore: cast_nullable_to_non_nullable
as int,marks: null == marks ? _self._marks : marks // ignore: cast_nullable_to_non_nullable
as List<HeartRateRecoveryMark>,maxHeartRateBpmUsed: freezed == maxHeartRateBpmUsed ? _self.maxHeartRateBpmUsed : maxHeartRateBpmUsed // ignore: cast_nullable_to_non_nullable
as int?,maxHeartRateEstimated: null == maxHeartRateEstimated ? _self.maxHeartRateEstimated : maxHeartRateEstimated // ignore: cast_nullable_to_non_nullable
as bool,peakFractionOfMax: freezed == peakFractionOfMax ? _self.peakFractionOfMax : peakFractionOfMax // ignore: cast_nullable_to_non_nullable
as double?,medianRecoveryGapSeconds: freezed == medianRecoveryGapSeconds ? _self.medianRecoveryGapSeconds : medianRecoveryGapSeconds // ignore: cast_nullable_to_non_nullable
as double?,recoverySampleCount: null == recoverySampleCount ? _self.recoverySampleCount : recoverySampleCount // ignore: cast_nullable_to_non_nullable
as int,quality: null == quality ? _self.quality : quality // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryQuality,issues: null == issues ? _self._issues : issues // ignore: cast_nullable_to_non_nullable
as Set<HeartRateRecoveryIssue>,
  ));
}


}

/// @nodoc
mixin _$HeartRateRecoveryWindow {

 DateTime get recoveryStart; DateTime get readStart; DateTime get readEnd; HeartRateRecoveryStartSource get source;
/// Create a copy of HeartRateRecoveryWindow
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartRateRecoveryWindowCopyWith<HeartRateRecoveryWindow> get copyWith => _$HeartRateRecoveryWindowCopyWithImpl<HeartRateRecoveryWindow>(this as HeartRateRecoveryWindow, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartRateRecoveryWindow&&(identical(other.recoveryStart, recoveryStart) || other.recoveryStart == recoveryStart)&&(identical(other.readStart, readStart) || other.readStart == readStart)&&(identical(other.readEnd, readEnd) || other.readEnd == readEnd)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,recoveryStart,readStart,readEnd,source);

@override
String toString() {
  return 'HeartRateRecoveryWindow(recoveryStart: $recoveryStart, readStart: $readStart, readEnd: $readEnd, source: $source)';
}


}

/// @nodoc
abstract mixin class $HeartRateRecoveryWindowCopyWith<$Res>  {
  factory $HeartRateRecoveryWindowCopyWith(HeartRateRecoveryWindow value, $Res Function(HeartRateRecoveryWindow) _then) = _$HeartRateRecoveryWindowCopyWithImpl;
@useResult
$Res call({
 DateTime recoveryStart, DateTime readStart, DateTime readEnd, HeartRateRecoveryStartSource source
});




}
/// @nodoc
class _$HeartRateRecoveryWindowCopyWithImpl<$Res>
    implements $HeartRateRecoveryWindowCopyWith<$Res> {
  _$HeartRateRecoveryWindowCopyWithImpl(this._self, this._then);

  final HeartRateRecoveryWindow _self;
  final $Res Function(HeartRateRecoveryWindow) _then;

/// Create a copy of HeartRateRecoveryWindow
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? recoveryStart = null,Object? readStart = null,Object? readEnd = null,Object? source = null,}) {
  return _then(_self.copyWith(
recoveryStart: null == recoveryStart ? _self.recoveryStart : recoveryStart // ignore: cast_nullable_to_non_nullable
as DateTime,readStart: null == readStart ? _self.readStart : readStart // ignore: cast_nullable_to_non_nullable
as DateTime,readEnd: null == readEnd ? _self.readEnd : readEnd // ignore: cast_nullable_to_non_nullable
as DateTime,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryStartSource,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartRateRecoveryWindow].
extension HeartRateRecoveryWindowPatterns on HeartRateRecoveryWindow {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartRateRecoveryWindow value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartRateRecoveryWindow() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartRateRecoveryWindow value)  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryWindow():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartRateRecoveryWindow value)?  $default,){
final _that = this;
switch (_that) {
case _HeartRateRecoveryWindow() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime recoveryStart,  DateTime readStart,  DateTime readEnd,  HeartRateRecoveryStartSource source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartRateRecoveryWindow() when $default != null:
return $default(_that.recoveryStart,_that.readStart,_that.readEnd,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime recoveryStart,  DateTime readStart,  DateTime readEnd,  HeartRateRecoveryStartSource source)  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryWindow():
return $default(_that.recoveryStart,_that.readStart,_that.readEnd,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime recoveryStart,  DateTime readStart,  DateTime readEnd,  HeartRateRecoveryStartSource source)?  $default,) {final _that = this;
switch (_that) {
case _HeartRateRecoveryWindow() when $default != null:
return $default(_that.recoveryStart,_that.readStart,_that.readEnd,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _HeartRateRecoveryWindow implements HeartRateRecoveryWindow {
  const _HeartRateRecoveryWindow({required this.recoveryStart, required this.readStart, required this.readEnd, required this.source});
  

@override final  DateTime recoveryStart;
@override final  DateTime readStart;
@override final  DateTime readEnd;
@override final  HeartRateRecoveryStartSource source;

/// Create a copy of HeartRateRecoveryWindow
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartRateRecoveryWindowCopyWith<_HeartRateRecoveryWindow> get copyWith => __$HeartRateRecoveryWindowCopyWithImpl<_HeartRateRecoveryWindow>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartRateRecoveryWindow&&(identical(other.recoveryStart, recoveryStart) || other.recoveryStart == recoveryStart)&&(identical(other.readStart, readStart) || other.readStart == readStart)&&(identical(other.readEnd, readEnd) || other.readEnd == readEnd)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,recoveryStart,readStart,readEnd,source);

@override
String toString() {
  return 'HeartRateRecoveryWindow(recoveryStart: $recoveryStart, readStart: $readStart, readEnd: $readEnd, source: $source)';
}


}

/// @nodoc
abstract mixin class _$HeartRateRecoveryWindowCopyWith<$Res> implements $HeartRateRecoveryWindowCopyWith<$Res> {
  factory _$HeartRateRecoveryWindowCopyWith(_HeartRateRecoveryWindow value, $Res Function(_HeartRateRecoveryWindow) _then) = __$HeartRateRecoveryWindowCopyWithImpl;
@override @useResult
$Res call({
 DateTime recoveryStart, DateTime readStart, DateTime readEnd, HeartRateRecoveryStartSource source
});




}
/// @nodoc
class __$HeartRateRecoveryWindowCopyWithImpl<$Res>
    implements _$HeartRateRecoveryWindowCopyWith<$Res> {
  __$HeartRateRecoveryWindowCopyWithImpl(this._self, this._then);

  final _HeartRateRecoveryWindow _self;
  final $Res Function(_HeartRateRecoveryWindow) _then;

/// Create a copy of HeartRateRecoveryWindow
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? recoveryStart = null,Object? readStart = null,Object? readEnd = null,Object? source = null,}) {
  return _then(_HeartRateRecoveryWindow(
recoveryStart: null == recoveryStart ? _self.recoveryStart : recoveryStart // ignore: cast_nullable_to_non_nullable
as DateTime,readStart: null == readStart ? _self.readStart : readStart // ignore: cast_nullable_to_non_nullable
as DateTime,readEnd: null == readEnd ? _self.readEnd : readEnd // ignore: cast_nullable_to_non_nullable
as DateTime,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as HeartRateRecoveryStartSource,
  ));
}


}

// dart format on
