// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'heart_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HeartRateSample {

 DateTime get time; int get beatsPerMinute; String get source;
/// Create a copy of HeartRateSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartRateSampleCopyWith<HeartRateSample> get copyWith => _$HeartRateSampleCopyWithImpl<HeartRateSample>(this as HeartRateSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartRateSample&&(identical(other.time, time) || other.time == time)&&(identical(other.beatsPerMinute, beatsPerMinute) || other.beatsPerMinute == beatsPerMinute)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,beatsPerMinute,source);

@override
String toString() {
  return 'HeartRateSample(time: $time, beatsPerMinute: $beatsPerMinute, source: $source)';
}


}

/// @nodoc
abstract mixin class $HeartRateSampleCopyWith<$Res>  {
  factory $HeartRateSampleCopyWith(HeartRateSample value, $Res Function(HeartRateSample) _then) = _$HeartRateSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, int beatsPerMinute, String source
});




}
/// @nodoc
class _$HeartRateSampleCopyWithImpl<$Res>
    implements $HeartRateSampleCopyWith<$Res> {
  _$HeartRateSampleCopyWithImpl(this._self, this._then);

  final HeartRateSample _self;
  final $Res Function(HeartRateSample) _then;

/// Create a copy of HeartRateSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? beatsPerMinute = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,beatsPerMinute: null == beatsPerMinute ? _self.beatsPerMinute : beatsPerMinute // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartRateSample].
extension HeartRateSamplePatterns on HeartRateSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartRateSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartRateSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartRateSample value)  $default,){
final _that = this;
switch (_that) {
case _HeartRateSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartRateSample value)?  $default,){
final _that = this;
switch (_that) {
case _HeartRateSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int beatsPerMinute,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartRateSample() when $default != null:
return $default(_that.time,_that.beatsPerMinute,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int beatsPerMinute,  String source)  $default,) {final _that = this;
switch (_that) {
case _HeartRateSample():
return $default(_that.time,_that.beatsPerMinute,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int beatsPerMinute,  String source)?  $default,) {final _that = this;
switch (_that) {
case _HeartRateSample() when $default != null:
return $default(_that.time,_that.beatsPerMinute,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _HeartRateSample implements HeartRateSample {
  const _HeartRateSample({required this.time, required this.beatsPerMinute, required this.source});
  

@override final  DateTime time;
@override final  int beatsPerMinute;
@override final  String source;

/// Create a copy of HeartRateSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartRateSampleCopyWith<_HeartRateSample> get copyWith => __$HeartRateSampleCopyWithImpl<_HeartRateSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartRateSample&&(identical(other.time, time) || other.time == time)&&(identical(other.beatsPerMinute, beatsPerMinute) || other.beatsPerMinute == beatsPerMinute)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,beatsPerMinute,source);

@override
String toString() {
  return 'HeartRateSample(time: $time, beatsPerMinute: $beatsPerMinute, source: $source)';
}


}

/// @nodoc
abstract mixin class _$HeartRateSampleCopyWith<$Res> implements $HeartRateSampleCopyWith<$Res> {
  factory _$HeartRateSampleCopyWith(_HeartRateSample value, $Res Function(_HeartRateSample) _then) = __$HeartRateSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int beatsPerMinute, String source
});




}
/// @nodoc
class __$HeartRateSampleCopyWithImpl<$Res>
    implements _$HeartRateSampleCopyWith<$Res> {
  __$HeartRateSampleCopyWithImpl(this._self, this._then);

  final _HeartRateSample _self;
  final $Res Function(_HeartRateSample) _then;

/// Create a copy of HeartRateSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? beatsPerMinute = null,Object? source = null,}) {
  return _then(_HeartRateSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,beatsPerMinute: null == beatsPerMinute ? _self.beatsPerMinute : beatsPerMinute // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$RestingHeartRateSample {

 DateTime get time; int get beatsPerMinute; String get source;
/// Create a copy of RestingHeartRateSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RestingHeartRateSampleCopyWith<RestingHeartRateSample> get copyWith => _$RestingHeartRateSampleCopyWithImpl<RestingHeartRateSample>(this as RestingHeartRateSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RestingHeartRateSample&&(identical(other.time, time) || other.time == time)&&(identical(other.beatsPerMinute, beatsPerMinute) || other.beatsPerMinute == beatsPerMinute)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,beatsPerMinute,source);

@override
String toString() {
  return 'RestingHeartRateSample(time: $time, beatsPerMinute: $beatsPerMinute, source: $source)';
}


}

/// @nodoc
abstract mixin class $RestingHeartRateSampleCopyWith<$Res>  {
  factory $RestingHeartRateSampleCopyWith(RestingHeartRateSample value, $Res Function(RestingHeartRateSample) _then) = _$RestingHeartRateSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, int beatsPerMinute, String source
});




}
/// @nodoc
class _$RestingHeartRateSampleCopyWithImpl<$Res>
    implements $RestingHeartRateSampleCopyWith<$Res> {
  _$RestingHeartRateSampleCopyWithImpl(this._self, this._then);

  final RestingHeartRateSample _self;
  final $Res Function(RestingHeartRateSample) _then;

/// Create a copy of RestingHeartRateSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? beatsPerMinute = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,beatsPerMinute: null == beatsPerMinute ? _self.beatsPerMinute : beatsPerMinute // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [RestingHeartRateSample].
extension RestingHeartRateSamplePatterns on RestingHeartRateSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RestingHeartRateSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RestingHeartRateSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RestingHeartRateSample value)  $default,){
final _that = this;
switch (_that) {
case _RestingHeartRateSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RestingHeartRateSample value)?  $default,){
final _that = this;
switch (_that) {
case _RestingHeartRateSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int beatsPerMinute,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RestingHeartRateSample() when $default != null:
return $default(_that.time,_that.beatsPerMinute,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int beatsPerMinute,  String source)  $default,) {final _that = this;
switch (_that) {
case _RestingHeartRateSample():
return $default(_that.time,_that.beatsPerMinute,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int beatsPerMinute,  String source)?  $default,) {final _that = this;
switch (_that) {
case _RestingHeartRateSample() when $default != null:
return $default(_that.time,_that.beatsPerMinute,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _RestingHeartRateSample implements RestingHeartRateSample {
  const _RestingHeartRateSample({required this.time, required this.beatsPerMinute, required this.source});
  

@override final  DateTime time;
@override final  int beatsPerMinute;
@override final  String source;

/// Create a copy of RestingHeartRateSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RestingHeartRateSampleCopyWith<_RestingHeartRateSample> get copyWith => __$RestingHeartRateSampleCopyWithImpl<_RestingHeartRateSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RestingHeartRateSample&&(identical(other.time, time) || other.time == time)&&(identical(other.beatsPerMinute, beatsPerMinute) || other.beatsPerMinute == beatsPerMinute)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,beatsPerMinute,source);

@override
String toString() {
  return 'RestingHeartRateSample(time: $time, beatsPerMinute: $beatsPerMinute, source: $source)';
}


}

/// @nodoc
abstract mixin class _$RestingHeartRateSampleCopyWith<$Res> implements $RestingHeartRateSampleCopyWith<$Res> {
  factory _$RestingHeartRateSampleCopyWith(_RestingHeartRateSample value, $Res Function(_RestingHeartRateSample) _then) = __$RestingHeartRateSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int beatsPerMinute, String source
});




}
/// @nodoc
class __$RestingHeartRateSampleCopyWithImpl<$Res>
    implements _$RestingHeartRateSampleCopyWith<$Res> {
  __$RestingHeartRateSampleCopyWithImpl(this._self, this._then);

  final _RestingHeartRateSample _self;
  final $Res Function(_RestingHeartRateSample) _then;

/// Create a copy of RestingHeartRateSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? beatsPerMinute = null,Object? source = null,}) {
  return _then(_RestingHeartRateSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,beatsPerMinute: null == beatsPerMinute ? _self.beatsPerMinute : beatsPerMinute // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$HrvSample {

 DateTime get time; double get rmssdMs; String get source;
/// Create a copy of HrvSample
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HrvSampleCopyWith<HrvSample> get copyWith => _$HrvSampleCopyWithImpl<HrvSample>(this as HrvSample, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HrvSample&&(identical(other.time, time) || other.time == time)&&(identical(other.rmssdMs, rmssdMs) || other.rmssdMs == rmssdMs)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,rmssdMs,source);

@override
String toString() {
  return 'HrvSample(time: $time, rmssdMs: $rmssdMs, source: $source)';
}


}

/// @nodoc
abstract mixin class $HrvSampleCopyWith<$Res>  {
  factory $HrvSampleCopyWith(HrvSample value, $Res Function(HrvSample) _then) = _$HrvSampleCopyWithImpl;
@useResult
$Res call({
 DateTime time, double rmssdMs, String source
});




}
/// @nodoc
class _$HrvSampleCopyWithImpl<$Res>
    implements $HrvSampleCopyWith<$Res> {
  _$HrvSampleCopyWithImpl(this._self, this._then);

  final HrvSample _self;
  final $Res Function(HrvSample) _then;

/// Create a copy of HrvSample
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? rmssdMs = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,rmssdMs: null == rmssdMs ? _self.rmssdMs : rmssdMs // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [HrvSample].
extension HrvSamplePatterns on HrvSample {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HrvSample value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HrvSample() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HrvSample value)  $default,){
final _that = this;
switch (_that) {
case _HrvSample():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HrvSample value)?  $default,){
final _that = this;
switch (_that) {
case _HrvSample() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double rmssdMs,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HrvSample() when $default != null:
return $default(_that.time,_that.rmssdMs,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double rmssdMs,  String source)  $default,) {final _that = this;
switch (_that) {
case _HrvSample():
return $default(_that.time,_that.rmssdMs,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double rmssdMs,  String source)?  $default,) {final _that = this;
switch (_that) {
case _HrvSample() when $default != null:
return $default(_that.time,_that.rmssdMs,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _HrvSample implements HrvSample {
  const _HrvSample({required this.time, required this.rmssdMs, required this.source});
  

@override final  DateTime time;
@override final  double rmssdMs;
@override final  String source;

/// Create a copy of HrvSample
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HrvSampleCopyWith<_HrvSample> get copyWith => __$HrvSampleCopyWithImpl<_HrvSample>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HrvSample&&(identical(other.time, time) || other.time == time)&&(identical(other.rmssdMs, rmssdMs) || other.rmssdMs == rmssdMs)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,rmssdMs,source);

@override
String toString() {
  return 'HrvSample(time: $time, rmssdMs: $rmssdMs, source: $source)';
}


}

/// @nodoc
abstract mixin class _$HrvSampleCopyWith<$Res> implements $HrvSampleCopyWith<$Res> {
  factory _$HrvSampleCopyWith(_HrvSample value, $Res Function(_HrvSample) _then) = __$HrvSampleCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double rmssdMs, String source
});




}
/// @nodoc
class __$HrvSampleCopyWithImpl<$Res>
    implements _$HrvSampleCopyWith<$Res> {
  __$HrvSampleCopyWithImpl(this._self, this._then);

  final _HrvSample _self;
  final $Res Function(_HrvSample) _then;

/// Create a copy of HrvSample
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? rmssdMs = null,Object? source = null,}) {
  return _then(_HrvSample(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,rmssdMs: null == rmssdMs ? _self.rmssdMs : rmssdMs // ignore: cast_nullable_to_non_nullable
as double,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$HeartRateSummary {

 LocalDate get date; int get avgBpm; int get minBpm; int get maxBpm;
/// Create a copy of HeartRateSummary
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HeartRateSummaryCopyWith<HeartRateSummary> get copyWith => _$HeartRateSummaryCopyWithImpl<HeartRateSummary>(this as HeartRateSummary, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HeartRateSummary&&(identical(other.date, date) || other.date == date)&&(identical(other.avgBpm, avgBpm) || other.avgBpm == avgBpm)&&(identical(other.minBpm, minBpm) || other.minBpm == minBpm)&&(identical(other.maxBpm, maxBpm) || other.maxBpm == maxBpm));
}


@override
int get hashCode => Object.hash(runtimeType,date,avgBpm,minBpm,maxBpm);

@override
String toString() {
  return 'HeartRateSummary(date: $date, avgBpm: $avgBpm, minBpm: $minBpm, maxBpm: $maxBpm)';
}


}

/// @nodoc
abstract mixin class $HeartRateSummaryCopyWith<$Res>  {
  factory $HeartRateSummaryCopyWith(HeartRateSummary value, $Res Function(HeartRateSummary) _then) = _$HeartRateSummaryCopyWithImpl;
@useResult
$Res call({
 LocalDate date, int avgBpm, int minBpm, int maxBpm
});




}
/// @nodoc
class _$HeartRateSummaryCopyWithImpl<$Res>
    implements $HeartRateSummaryCopyWith<$Res> {
  _$HeartRateSummaryCopyWithImpl(this._self, this._then);

  final HeartRateSummary _self;
  final $Res Function(HeartRateSummary) _then;

/// Create a copy of HeartRateSummary
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? avgBpm = null,Object? minBpm = null,Object? maxBpm = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,avgBpm: null == avgBpm ? _self.avgBpm : avgBpm // ignore: cast_nullable_to_non_nullable
as int,minBpm: null == minBpm ? _self.minBpm : minBpm // ignore: cast_nullable_to_non_nullable
as int,maxBpm: null == maxBpm ? _self.maxBpm : maxBpm // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [HeartRateSummary].
extension HeartRateSummaryPatterns on HeartRateSummary {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HeartRateSummary value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HeartRateSummary() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HeartRateSummary value)  $default,){
final _that = this;
switch (_that) {
case _HeartRateSummary():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HeartRateSummary value)?  $default,){
final _that = this;
switch (_that) {
case _HeartRateSummary() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  int avgBpm,  int minBpm,  int maxBpm)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HeartRateSummary() when $default != null:
return $default(_that.date,_that.avgBpm,_that.minBpm,_that.maxBpm);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  int avgBpm,  int minBpm,  int maxBpm)  $default,) {final _that = this;
switch (_that) {
case _HeartRateSummary():
return $default(_that.date,_that.avgBpm,_that.minBpm,_that.maxBpm);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  int avgBpm,  int minBpm,  int maxBpm)?  $default,) {final _that = this;
switch (_that) {
case _HeartRateSummary() when $default != null:
return $default(_that.date,_that.avgBpm,_that.minBpm,_that.maxBpm);case _:
  return null;

}
}

}

/// @nodoc


class _HeartRateSummary implements HeartRateSummary {
  const _HeartRateSummary({required this.date, required this.avgBpm, required this.minBpm, required this.maxBpm});
  

@override final  LocalDate date;
@override final  int avgBpm;
@override final  int minBpm;
@override final  int maxBpm;

/// Create a copy of HeartRateSummary
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HeartRateSummaryCopyWith<_HeartRateSummary> get copyWith => __$HeartRateSummaryCopyWithImpl<_HeartRateSummary>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HeartRateSummary&&(identical(other.date, date) || other.date == date)&&(identical(other.avgBpm, avgBpm) || other.avgBpm == avgBpm)&&(identical(other.minBpm, minBpm) || other.minBpm == minBpm)&&(identical(other.maxBpm, maxBpm) || other.maxBpm == maxBpm));
}


@override
int get hashCode => Object.hash(runtimeType,date,avgBpm,minBpm,maxBpm);

@override
String toString() {
  return 'HeartRateSummary(date: $date, avgBpm: $avgBpm, minBpm: $minBpm, maxBpm: $maxBpm)';
}


}

/// @nodoc
abstract mixin class _$HeartRateSummaryCopyWith<$Res> implements $HeartRateSummaryCopyWith<$Res> {
  factory _$HeartRateSummaryCopyWith(_HeartRateSummary value, $Res Function(_HeartRateSummary) _then) = __$HeartRateSummaryCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, int avgBpm, int minBpm, int maxBpm
});




}
/// @nodoc
class __$HeartRateSummaryCopyWithImpl<$Res>
    implements _$HeartRateSummaryCopyWith<$Res> {
  __$HeartRateSummaryCopyWithImpl(this._self, this._then);

  final _HeartRateSummary _self;
  final $Res Function(_HeartRateSummary) _then;

/// Create a copy of HeartRateSummary
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? avgBpm = null,Object? minBpm = null,Object? maxBpm = null,}) {
  return _then(_HeartRateSummary(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,avgBpm: null == avgBpm ? _self.avgBpm : avgBpm // ignore: cast_nullable_to_non_nullable
as int,minBpm: null == minBpm ? _self.minBpm : minBpm // ignore: cast_nullable_to_non_nullable
as int,maxBpm: null == maxBpm ? _self.maxBpm : maxBpm // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$DailyRestingHR {

 LocalDate get date; int get bpm;
/// Create a copy of DailyRestingHR
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyRestingHRCopyWith<DailyRestingHR> get copyWith => _$DailyRestingHRCopyWithImpl<DailyRestingHR>(this as DailyRestingHR, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyRestingHR&&(identical(other.date, date) || other.date == date)&&(identical(other.bpm, bpm) || other.bpm == bpm));
}


@override
int get hashCode => Object.hash(runtimeType,date,bpm);

@override
String toString() {
  return 'DailyRestingHR(date: $date, bpm: $bpm)';
}


}

/// @nodoc
abstract mixin class $DailyRestingHRCopyWith<$Res>  {
  factory $DailyRestingHRCopyWith(DailyRestingHR value, $Res Function(DailyRestingHR) _then) = _$DailyRestingHRCopyWithImpl;
@useResult
$Res call({
 LocalDate date, int bpm
});




}
/// @nodoc
class _$DailyRestingHRCopyWithImpl<$Res>
    implements $DailyRestingHRCopyWith<$Res> {
  _$DailyRestingHRCopyWithImpl(this._self, this._then);

  final DailyRestingHR _self;
  final $Res Function(DailyRestingHR) _then;

/// Create a copy of DailyRestingHR
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? bpm = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,bpm: null == bpm ? _self.bpm : bpm // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyRestingHR].
extension DailyRestingHRPatterns on DailyRestingHR {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyRestingHR value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyRestingHR() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyRestingHR value)  $default,){
final _that = this;
switch (_that) {
case _DailyRestingHR():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyRestingHR value)?  $default,){
final _that = this;
switch (_that) {
case _DailyRestingHR() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  int bpm)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyRestingHR() when $default != null:
return $default(_that.date,_that.bpm);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  int bpm)  $default,) {final _that = this;
switch (_that) {
case _DailyRestingHR():
return $default(_that.date,_that.bpm);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  int bpm)?  $default,) {final _that = this;
switch (_that) {
case _DailyRestingHR() when $default != null:
return $default(_that.date,_that.bpm);case _:
  return null;

}
}

}

/// @nodoc


class _DailyRestingHR implements DailyRestingHR {
  const _DailyRestingHR({required this.date, required this.bpm});
  

@override final  LocalDate date;
@override final  int bpm;

/// Create a copy of DailyRestingHR
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyRestingHRCopyWith<_DailyRestingHR> get copyWith => __$DailyRestingHRCopyWithImpl<_DailyRestingHR>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyRestingHR&&(identical(other.date, date) || other.date == date)&&(identical(other.bpm, bpm) || other.bpm == bpm));
}


@override
int get hashCode => Object.hash(runtimeType,date,bpm);

@override
String toString() {
  return 'DailyRestingHR(date: $date, bpm: $bpm)';
}


}

/// @nodoc
abstract mixin class _$DailyRestingHRCopyWith<$Res> implements $DailyRestingHRCopyWith<$Res> {
  factory _$DailyRestingHRCopyWith(_DailyRestingHR value, $Res Function(_DailyRestingHR) _then) = __$DailyRestingHRCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, int bpm
});




}
/// @nodoc
class __$DailyRestingHRCopyWithImpl<$Res>
    implements _$DailyRestingHRCopyWith<$Res> {
  __$DailyRestingHRCopyWithImpl(this._self, this._then);

  final _DailyRestingHR _self;
  final $Res Function(_DailyRestingHR) _then;

/// Create a copy of DailyRestingHR
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? bpm = null,}) {
  return _then(_DailyRestingHR(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,bpm: null == bpm ? _self.bpm : bpm // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

/// @nodoc
mixin _$DailyHrv {

 LocalDate get date; double get rmssdMs;
/// Create a copy of DailyHrv
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyHrvCopyWith<DailyHrv> get copyWith => _$DailyHrvCopyWithImpl<DailyHrv>(this as DailyHrv, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyHrv&&(identical(other.date, date) || other.date == date)&&(identical(other.rmssdMs, rmssdMs) || other.rmssdMs == rmssdMs));
}


@override
int get hashCode => Object.hash(runtimeType,date,rmssdMs);

@override
String toString() {
  return 'DailyHrv(date: $date, rmssdMs: $rmssdMs)';
}


}

/// @nodoc
abstract mixin class $DailyHrvCopyWith<$Res>  {
  factory $DailyHrvCopyWith(DailyHrv value, $Res Function(DailyHrv) _then) = _$DailyHrvCopyWithImpl;
@useResult
$Res call({
 LocalDate date, double rmssdMs
});




}
/// @nodoc
class _$DailyHrvCopyWithImpl<$Res>
    implements $DailyHrvCopyWith<$Res> {
  _$DailyHrvCopyWithImpl(this._self, this._then);

  final DailyHrv _self;
  final $Res Function(DailyHrv) _then;

/// Create a copy of DailyHrv
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? rmssdMs = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,rmssdMs: null == rmssdMs ? _self.rmssdMs : rmssdMs // ignore: cast_nullable_to_non_nullable
as double,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyHrv].
extension DailyHrvPatterns on DailyHrv {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyHrv value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyHrv() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyHrv value)  $default,){
final _that = this;
switch (_that) {
case _DailyHrv():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyHrv value)?  $default,){
final _that = this;
switch (_that) {
case _DailyHrv() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  double rmssdMs)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyHrv() when $default != null:
return $default(_that.date,_that.rmssdMs);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  double rmssdMs)  $default,) {final _that = this;
switch (_that) {
case _DailyHrv():
return $default(_that.date,_that.rmssdMs);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  double rmssdMs)?  $default,) {final _that = this;
switch (_that) {
case _DailyHrv() when $default != null:
return $default(_that.date,_that.rmssdMs);case _:
  return null;

}
}

}

/// @nodoc


class _DailyHrv implements DailyHrv {
  const _DailyHrv({required this.date, required this.rmssdMs});
  

@override final  LocalDate date;
@override final  double rmssdMs;

/// Create a copy of DailyHrv
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyHrvCopyWith<_DailyHrv> get copyWith => __$DailyHrvCopyWithImpl<_DailyHrv>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyHrv&&(identical(other.date, date) || other.date == date)&&(identical(other.rmssdMs, rmssdMs) || other.rmssdMs == rmssdMs));
}


@override
int get hashCode => Object.hash(runtimeType,date,rmssdMs);

@override
String toString() {
  return 'DailyHrv(date: $date, rmssdMs: $rmssdMs)';
}


}

/// @nodoc
abstract mixin class _$DailyHrvCopyWith<$Res> implements $DailyHrvCopyWith<$Res> {
  factory _$DailyHrvCopyWith(_DailyHrv value, $Res Function(_DailyHrv) _then) = __$DailyHrvCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, double rmssdMs
});




}
/// @nodoc
class __$DailyHrvCopyWithImpl<$Res>
    implements _$DailyHrvCopyWith<$Res> {
  __$DailyHrvCopyWithImpl(this._self, this._then);

  final _DailyHrv _self;
  final $Res Function(_DailyHrv) _then;

/// Create a copy of DailyHrv
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? rmssdMs = null,}) {
  return _then(_DailyHrv(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,rmssdMs: null == rmssdMs ? _self.rmssdMs : rmssdMs // ignore: cast_nullable_to_non_nullable
as double,
  ));
}


}

// dart format on
