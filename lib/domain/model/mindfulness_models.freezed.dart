// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'mindfulness_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$MindfulnessSession {

 String get id; String? get title; DateTime get startTime; DateTime get endTime; int get durationMs; String get source; bool get isOpenVitalsEntry;
/// Create a copy of MindfulnessSession
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MindfulnessSessionCopyWith<MindfulnessSession> get copyWith => _$MindfulnessSessionCopyWithImpl<MindfulnessSession>(this as MindfulnessSession, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MindfulnessSession&&(identical(other.id, id) || other.id == id)&&(identical(other.title, title) || other.title == title)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.durationMs, durationMs) || other.durationMs == durationMs)&&(identical(other.source, source) || other.source == source)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,id,title,startTime,endTime,durationMs,source,isOpenVitalsEntry);

@override
String toString() {
  return 'MindfulnessSession(id: $id, title: $title, startTime: $startTime, endTime: $endTime, durationMs: $durationMs, source: $source, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class $MindfulnessSessionCopyWith<$Res>  {
  factory $MindfulnessSessionCopyWith(MindfulnessSession value, $Res Function(MindfulnessSession) _then) = _$MindfulnessSessionCopyWithImpl;
@useResult
$Res call({
 String id, String? title, DateTime startTime, DateTime endTime, int durationMs, String source, bool isOpenVitalsEntry
});




}
/// @nodoc
class _$MindfulnessSessionCopyWithImpl<$Res>
    implements $MindfulnessSessionCopyWith<$Res> {
  _$MindfulnessSessionCopyWithImpl(this._self, this._then);

  final MindfulnessSession _self;
  final $Res Function(MindfulnessSession) _then;

/// Create a copy of MindfulnessSession
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? id = null,Object? title = freezed,Object? startTime = null,Object? endTime = null,Object? durationMs = null,Object? source = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_self.copyWith(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,durationMs: null == durationMs ? _self.durationMs : durationMs // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [MindfulnessSession].
extension MindfulnessSessionPatterns on MindfulnessSession {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MindfulnessSession value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MindfulnessSession() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MindfulnessSession value)  $default,){
final _that = this;
switch (_that) {
case _MindfulnessSession():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MindfulnessSession value)?  $default,){
final _that = this;
switch (_that) {
case _MindfulnessSession() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String id,  String? title,  DateTime startTime,  DateTime endTime,  int durationMs,  String source,  bool isOpenVitalsEntry)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MindfulnessSession() when $default != null:
return $default(_that.id,_that.title,_that.startTime,_that.endTime,_that.durationMs,_that.source,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String id,  String? title,  DateTime startTime,  DateTime endTime,  int durationMs,  String source,  bool isOpenVitalsEntry)  $default,) {final _that = this;
switch (_that) {
case _MindfulnessSession():
return $default(_that.id,_that.title,_that.startTime,_that.endTime,_that.durationMs,_that.source,_that.isOpenVitalsEntry);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String id,  String? title,  DateTime startTime,  DateTime endTime,  int durationMs,  String source,  bool isOpenVitalsEntry)?  $default,) {final _that = this;
switch (_that) {
case _MindfulnessSession() when $default != null:
return $default(_that.id,_that.title,_that.startTime,_that.endTime,_that.durationMs,_that.source,_that.isOpenVitalsEntry);case _:
  return null;

}
}

}

/// @nodoc


class _MindfulnessSession extends MindfulnessSession {
  const _MindfulnessSession({required this.id, required this.title, required this.startTime, required this.endTime, required this.durationMs, required this.source, this.isOpenVitalsEntry = false}): super._();
  

@override final  String id;
@override final  String? title;
@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  int durationMs;
@override final  String source;
@override@JsonKey() final  bool isOpenVitalsEntry;

/// Create a copy of MindfulnessSession
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MindfulnessSessionCopyWith<_MindfulnessSession> get copyWith => __$MindfulnessSessionCopyWithImpl<_MindfulnessSession>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MindfulnessSession&&(identical(other.id, id) || other.id == id)&&(identical(other.title, title) || other.title == title)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.durationMs, durationMs) || other.durationMs == durationMs)&&(identical(other.source, source) || other.source == source)&&(identical(other.isOpenVitalsEntry, isOpenVitalsEntry) || other.isOpenVitalsEntry == isOpenVitalsEntry));
}


@override
int get hashCode => Object.hash(runtimeType,id,title,startTime,endTime,durationMs,source,isOpenVitalsEntry);

@override
String toString() {
  return 'MindfulnessSession(id: $id, title: $title, startTime: $startTime, endTime: $endTime, durationMs: $durationMs, source: $source, isOpenVitalsEntry: $isOpenVitalsEntry)';
}


}

/// @nodoc
abstract mixin class _$MindfulnessSessionCopyWith<$Res> implements $MindfulnessSessionCopyWith<$Res> {
  factory _$MindfulnessSessionCopyWith(_MindfulnessSession value, $Res Function(_MindfulnessSession) _then) = __$MindfulnessSessionCopyWithImpl;
@override @useResult
$Res call({
 String id, String? title, DateTime startTime, DateTime endTime, int durationMs, String source, bool isOpenVitalsEntry
});




}
/// @nodoc
class __$MindfulnessSessionCopyWithImpl<$Res>
    implements _$MindfulnessSessionCopyWith<$Res> {
  __$MindfulnessSessionCopyWithImpl(this._self, this._then);

  final _MindfulnessSession _self;
  final $Res Function(_MindfulnessSession) _then;

/// Create a copy of MindfulnessSession
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? id = null,Object? title = freezed,Object? startTime = null,Object? endTime = null,Object? durationMs = null,Object? source = null,Object? isOpenVitalsEntry = null,}) {
  return _then(_MindfulnessSession(
id: null == id ? _self.id : id // ignore: cast_nullable_to_non_nullable
as String,title: freezed == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String?,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,durationMs: null == durationMs ? _self.durationMs : durationMs // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,isOpenVitalsEntry: null == isOpenVitalsEntry ? _self.isOpenVitalsEntry : isOpenVitalsEntry // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$MindfulnessTimerConfig {

 int get durationMinutes; int? get intervalMinutes; MindfulnessBellSound get bellSound; MindfulnessBackgroundSound get backgroundSound;
/// Create a copy of MindfulnessTimerConfig
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MindfulnessTimerConfigCopyWith<MindfulnessTimerConfig> get copyWith => _$MindfulnessTimerConfigCopyWithImpl<MindfulnessTimerConfig>(this as MindfulnessTimerConfig, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MindfulnessTimerConfig&&(identical(other.durationMinutes, durationMinutes) || other.durationMinutes == durationMinutes)&&(identical(other.intervalMinutes, intervalMinutes) || other.intervalMinutes == intervalMinutes)&&(identical(other.bellSound, bellSound) || other.bellSound == bellSound)&&(identical(other.backgroundSound, backgroundSound) || other.backgroundSound == backgroundSound));
}


@override
int get hashCode => Object.hash(runtimeType,durationMinutes,intervalMinutes,bellSound,backgroundSound);

@override
String toString() {
  return 'MindfulnessTimerConfig(durationMinutes: $durationMinutes, intervalMinutes: $intervalMinutes, bellSound: $bellSound, backgroundSound: $backgroundSound)';
}


}

/// @nodoc
abstract mixin class $MindfulnessTimerConfigCopyWith<$Res>  {
  factory $MindfulnessTimerConfigCopyWith(MindfulnessTimerConfig value, $Res Function(MindfulnessTimerConfig) _then) = _$MindfulnessTimerConfigCopyWithImpl;
@useResult
$Res call({
 int durationMinutes, int? intervalMinutes, MindfulnessBellSound bellSound, MindfulnessBackgroundSound backgroundSound
});




}
/// @nodoc
class _$MindfulnessTimerConfigCopyWithImpl<$Res>
    implements $MindfulnessTimerConfigCopyWith<$Res> {
  _$MindfulnessTimerConfigCopyWithImpl(this._self, this._then);

  final MindfulnessTimerConfig _self;
  final $Res Function(MindfulnessTimerConfig) _then;

/// Create a copy of MindfulnessTimerConfig
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? durationMinutes = null,Object? intervalMinutes = freezed,Object? bellSound = null,Object? backgroundSound = null,}) {
  return _then(_self.copyWith(
durationMinutes: null == durationMinutes ? _self.durationMinutes : durationMinutes // ignore: cast_nullable_to_non_nullable
as int,intervalMinutes: freezed == intervalMinutes ? _self.intervalMinutes : intervalMinutes // ignore: cast_nullable_to_non_nullable
as int?,bellSound: null == bellSound ? _self.bellSound : bellSound // ignore: cast_nullable_to_non_nullable
as MindfulnessBellSound,backgroundSound: null == backgroundSound ? _self.backgroundSound : backgroundSound // ignore: cast_nullable_to_non_nullable
as MindfulnessBackgroundSound,
  ));
}

}


/// Adds pattern-matching-related methods to [MindfulnessTimerConfig].
extension MindfulnessTimerConfigPatterns on MindfulnessTimerConfig {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MindfulnessTimerConfig value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MindfulnessTimerConfig() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MindfulnessTimerConfig value)  $default,){
final _that = this;
switch (_that) {
case _MindfulnessTimerConfig():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MindfulnessTimerConfig value)?  $default,){
final _that = this;
switch (_that) {
case _MindfulnessTimerConfig() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int durationMinutes,  int? intervalMinutes,  MindfulnessBellSound bellSound,  MindfulnessBackgroundSound backgroundSound)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MindfulnessTimerConfig() when $default != null:
return $default(_that.durationMinutes,_that.intervalMinutes,_that.bellSound,_that.backgroundSound);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int durationMinutes,  int? intervalMinutes,  MindfulnessBellSound bellSound,  MindfulnessBackgroundSound backgroundSound)  $default,) {final _that = this;
switch (_that) {
case _MindfulnessTimerConfig():
return $default(_that.durationMinutes,_that.intervalMinutes,_that.bellSound,_that.backgroundSound);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int durationMinutes,  int? intervalMinutes,  MindfulnessBellSound bellSound,  MindfulnessBackgroundSound backgroundSound)?  $default,) {final _that = this;
switch (_that) {
case _MindfulnessTimerConfig() when $default != null:
return $default(_that.durationMinutes,_that.intervalMinutes,_that.bellSound,_that.backgroundSound);case _:
  return null;

}
}

}

/// @nodoc


class _MindfulnessTimerConfig implements MindfulnessTimerConfig {
  const _MindfulnessTimerConfig({required this.durationMinutes, required this.intervalMinutes, required this.bellSound, this.backgroundSound = MindfulnessBackgroundSound.none});
  

@override final  int durationMinutes;
@override final  int? intervalMinutes;
@override final  MindfulnessBellSound bellSound;
@override@JsonKey() final  MindfulnessBackgroundSound backgroundSound;

/// Create a copy of MindfulnessTimerConfig
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MindfulnessTimerConfigCopyWith<_MindfulnessTimerConfig> get copyWith => __$MindfulnessTimerConfigCopyWithImpl<_MindfulnessTimerConfig>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MindfulnessTimerConfig&&(identical(other.durationMinutes, durationMinutes) || other.durationMinutes == durationMinutes)&&(identical(other.intervalMinutes, intervalMinutes) || other.intervalMinutes == intervalMinutes)&&(identical(other.bellSound, bellSound) || other.bellSound == bellSound)&&(identical(other.backgroundSound, backgroundSound) || other.backgroundSound == backgroundSound));
}


@override
int get hashCode => Object.hash(runtimeType,durationMinutes,intervalMinutes,bellSound,backgroundSound);

@override
String toString() {
  return 'MindfulnessTimerConfig(durationMinutes: $durationMinutes, intervalMinutes: $intervalMinutes, bellSound: $bellSound, backgroundSound: $backgroundSound)';
}


}

/// @nodoc
abstract mixin class _$MindfulnessTimerConfigCopyWith<$Res> implements $MindfulnessTimerConfigCopyWith<$Res> {
  factory _$MindfulnessTimerConfigCopyWith(_MindfulnessTimerConfig value, $Res Function(_MindfulnessTimerConfig) _then) = __$MindfulnessTimerConfigCopyWithImpl;
@override @useResult
$Res call({
 int durationMinutes, int? intervalMinutes, MindfulnessBellSound bellSound, MindfulnessBackgroundSound backgroundSound
});




}
/// @nodoc
class __$MindfulnessTimerConfigCopyWithImpl<$Res>
    implements _$MindfulnessTimerConfigCopyWith<$Res> {
  __$MindfulnessTimerConfigCopyWithImpl(this._self, this._then);

  final _MindfulnessTimerConfig _self;
  final $Res Function(_MindfulnessTimerConfig) _then;

/// Create a copy of MindfulnessTimerConfig
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? durationMinutes = null,Object? intervalMinutes = freezed,Object? bellSound = null,Object? backgroundSound = null,}) {
  return _then(_MindfulnessTimerConfig(
durationMinutes: null == durationMinutes ? _self.durationMinutes : durationMinutes // ignore: cast_nullable_to_non_nullable
as int,intervalMinutes: freezed == intervalMinutes ? _self.intervalMinutes : intervalMinutes // ignore: cast_nullable_to_non_nullable
as int?,bellSound: null == bellSound ? _self.bellSound : bellSound // ignore: cast_nullable_to_non_nullable
as MindfulnessBellSound,backgroundSound: null == backgroundSound ? _self.backgroundSound : backgroundSound // ignore: cast_nullable_to_non_nullable
as MindfulnessBackgroundSound,
  ));
}


}

/// @nodoc
mixin _$MindfulnessSessionWriteRequest {

 String get title; DateTime get startTime; DateTime get endTime;
/// Create a copy of MindfulnessSessionWriteRequest
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MindfulnessSessionWriteRequestCopyWith<MindfulnessSessionWriteRequest> get copyWith => _$MindfulnessSessionWriteRequestCopyWithImpl<MindfulnessSessionWriteRequest>(this as MindfulnessSessionWriteRequest, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MindfulnessSessionWriteRequest&&(identical(other.title, title) || other.title == title)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime));
}


@override
int get hashCode => Object.hash(runtimeType,title,startTime,endTime);

@override
String toString() {
  return 'MindfulnessSessionWriteRequest(title: $title, startTime: $startTime, endTime: $endTime)';
}


}

/// @nodoc
abstract mixin class $MindfulnessSessionWriteRequestCopyWith<$Res>  {
  factory $MindfulnessSessionWriteRequestCopyWith(MindfulnessSessionWriteRequest value, $Res Function(MindfulnessSessionWriteRequest) _then) = _$MindfulnessSessionWriteRequestCopyWithImpl;
@useResult
$Res call({
 String title, DateTime startTime, DateTime endTime
});




}
/// @nodoc
class _$MindfulnessSessionWriteRequestCopyWithImpl<$Res>
    implements $MindfulnessSessionWriteRequestCopyWith<$Res> {
  _$MindfulnessSessionWriteRequestCopyWithImpl(this._self, this._then);

  final MindfulnessSessionWriteRequest _self;
  final $Res Function(MindfulnessSessionWriteRequest) _then;

/// Create a copy of MindfulnessSessionWriteRequest
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? title = null,Object? startTime = null,Object? endTime = null,}) {
  return _then(_self.copyWith(
title: null == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}

}


/// Adds pattern-matching-related methods to [MindfulnessSessionWriteRequest].
extension MindfulnessSessionWriteRequestPatterns on MindfulnessSessionWriteRequest {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MindfulnessSessionWriteRequest value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MindfulnessSessionWriteRequest() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MindfulnessSessionWriteRequest value)  $default,){
final _that = this;
switch (_that) {
case _MindfulnessSessionWriteRequest():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MindfulnessSessionWriteRequest value)?  $default,){
final _that = this;
switch (_that) {
case _MindfulnessSessionWriteRequest() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String title,  DateTime startTime,  DateTime endTime)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MindfulnessSessionWriteRequest() when $default != null:
return $default(_that.title,_that.startTime,_that.endTime);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String title,  DateTime startTime,  DateTime endTime)  $default,) {final _that = this;
switch (_that) {
case _MindfulnessSessionWriteRequest():
return $default(_that.title,_that.startTime,_that.endTime);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String title,  DateTime startTime,  DateTime endTime)?  $default,) {final _that = this;
switch (_that) {
case _MindfulnessSessionWriteRequest() when $default != null:
return $default(_that.title,_that.startTime,_that.endTime);case _:
  return null;

}
}

}

/// @nodoc


class _MindfulnessSessionWriteRequest implements MindfulnessSessionWriteRequest {
  const _MindfulnessSessionWriteRequest({required this.title, required this.startTime, required this.endTime});
  

@override final  String title;
@override final  DateTime startTime;
@override final  DateTime endTime;

/// Create a copy of MindfulnessSessionWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MindfulnessSessionWriteRequestCopyWith<_MindfulnessSessionWriteRequest> get copyWith => __$MindfulnessSessionWriteRequestCopyWithImpl<_MindfulnessSessionWriteRequest>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MindfulnessSessionWriteRequest&&(identical(other.title, title) || other.title == title)&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime));
}


@override
int get hashCode => Object.hash(runtimeType,title,startTime,endTime);

@override
String toString() {
  return 'MindfulnessSessionWriteRequest(title: $title, startTime: $startTime, endTime: $endTime)';
}


}

/// @nodoc
abstract mixin class _$MindfulnessSessionWriteRequestCopyWith<$Res> implements $MindfulnessSessionWriteRequestCopyWith<$Res> {
  factory _$MindfulnessSessionWriteRequestCopyWith(_MindfulnessSessionWriteRequest value, $Res Function(_MindfulnessSessionWriteRequest) _then) = __$MindfulnessSessionWriteRequestCopyWithImpl;
@override @useResult
$Res call({
 String title, DateTime startTime, DateTime endTime
});




}
/// @nodoc
class __$MindfulnessSessionWriteRequestCopyWithImpl<$Res>
    implements _$MindfulnessSessionWriteRequestCopyWith<$Res> {
  __$MindfulnessSessionWriteRequestCopyWithImpl(this._self, this._then);

  final _MindfulnessSessionWriteRequest _self;
  final $Res Function(_MindfulnessSessionWriteRequest) _then;

/// Create a copy of MindfulnessSessionWriteRequest
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? title = null,Object? startTime = null,Object? endTime = null,}) {
  return _then(_MindfulnessSessionWriteRequest(
title: null == title ? _self.title : title // ignore: cast_nullable_to_non_nullable
as String,startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}


}

// dart format on
