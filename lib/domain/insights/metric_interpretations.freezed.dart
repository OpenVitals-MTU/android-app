// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'metric_interpretations.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$BloodPressureInterpretation {

 BloodPressureCategory get category; InterpretationSeverity get severity;
/// Create a copy of BloodPressureInterpretation
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BloodPressureInterpretationCopyWith<BloodPressureInterpretation> get copyWith => _$BloodPressureInterpretationCopyWithImpl<BloodPressureInterpretation>(this as BloodPressureInterpretation, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BloodPressureInterpretation&&(identical(other.category, category) || other.category == category)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,category,severity);

@override
String toString() {
  return 'BloodPressureInterpretation(category: $category, severity: $severity)';
}


}

/// @nodoc
abstract mixin class $BloodPressureInterpretationCopyWith<$Res>  {
  factory $BloodPressureInterpretationCopyWith(BloodPressureInterpretation value, $Res Function(BloodPressureInterpretation) _then) = _$BloodPressureInterpretationCopyWithImpl;
@useResult
$Res call({
 BloodPressureCategory category, InterpretationSeverity severity
});




}
/// @nodoc
class _$BloodPressureInterpretationCopyWithImpl<$Res>
    implements $BloodPressureInterpretationCopyWith<$Res> {
  _$BloodPressureInterpretationCopyWithImpl(this._self, this._then);

  final BloodPressureInterpretation _self;
  final $Res Function(BloodPressureInterpretation) _then;

/// Create a copy of BloodPressureInterpretation
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? category = null,Object? severity = null,}) {
  return _then(_self.copyWith(
category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as BloodPressureCategory,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}

}


/// Adds pattern-matching-related methods to [BloodPressureInterpretation].
extension BloodPressureInterpretationPatterns on BloodPressureInterpretation {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BloodPressureInterpretation value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BloodPressureInterpretation() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BloodPressureInterpretation value)  $default,){
final _that = this;
switch (_that) {
case _BloodPressureInterpretation():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BloodPressureInterpretation value)?  $default,){
final _that = this;
switch (_that) {
case _BloodPressureInterpretation() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( BloodPressureCategory category,  InterpretationSeverity severity)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BloodPressureInterpretation() when $default != null:
return $default(_that.category,_that.severity);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( BloodPressureCategory category,  InterpretationSeverity severity)  $default,) {final _that = this;
switch (_that) {
case _BloodPressureInterpretation():
return $default(_that.category,_that.severity);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( BloodPressureCategory category,  InterpretationSeverity severity)?  $default,) {final _that = this;
switch (_that) {
case _BloodPressureInterpretation() when $default != null:
return $default(_that.category,_that.severity);case _:
  return null;

}
}

}

/// @nodoc


class _BloodPressureInterpretation implements BloodPressureInterpretation {
  const _BloodPressureInterpretation({required this.category, required this.severity});
  

@override final  BloodPressureCategory category;
@override final  InterpretationSeverity severity;

/// Create a copy of BloodPressureInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BloodPressureInterpretationCopyWith<_BloodPressureInterpretation> get copyWith => __$BloodPressureInterpretationCopyWithImpl<_BloodPressureInterpretation>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BloodPressureInterpretation&&(identical(other.category, category) || other.category == category)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,category,severity);

@override
String toString() {
  return 'BloodPressureInterpretation(category: $category, severity: $severity)';
}


}

/// @nodoc
abstract mixin class _$BloodPressureInterpretationCopyWith<$Res> implements $BloodPressureInterpretationCopyWith<$Res> {
  factory _$BloodPressureInterpretationCopyWith(_BloodPressureInterpretation value, $Res Function(_BloodPressureInterpretation) _then) = __$BloodPressureInterpretationCopyWithImpl;
@override @useResult
$Res call({
 BloodPressureCategory category, InterpretationSeverity severity
});




}
/// @nodoc
class __$BloodPressureInterpretationCopyWithImpl<$Res>
    implements _$BloodPressureInterpretationCopyWith<$Res> {
  __$BloodPressureInterpretationCopyWithImpl(this._self, this._then);

  final _BloodPressureInterpretation _self;
  final $Res Function(_BloodPressureInterpretation) _then;

/// Create a copy of BloodPressureInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? category = null,Object? severity = null,}) {
  return _then(_BloodPressureInterpretation(
category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as BloodPressureCategory,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}


}

/// @nodoc
mixin _$BmiInterpretation {

 BmiCategory get category; InterpretationSeverity get severity;
/// Create a copy of BmiInterpretation
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BmiInterpretationCopyWith<BmiInterpretation> get copyWith => _$BmiInterpretationCopyWithImpl<BmiInterpretation>(this as BmiInterpretation, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BmiInterpretation&&(identical(other.category, category) || other.category == category)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,category,severity);

@override
String toString() {
  return 'BmiInterpretation(category: $category, severity: $severity)';
}


}

/// @nodoc
abstract mixin class $BmiInterpretationCopyWith<$Res>  {
  factory $BmiInterpretationCopyWith(BmiInterpretation value, $Res Function(BmiInterpretation) _then) = _$BmiInterpretationCopyWithImpl;
@useResult
$Res call({
 BmiCategory category, InterpretationSeverity severity
});




}
/// @nodoc
class _$BmiInterpretationCopyWithImpl<$Res>
    implements $BmiInterpretationCopyWith<$Res> {
  _$BmiInterpretationCopyWithImpl(this._self, this._then);

  final BmiInterpretation _self;
  final $Res Function(BmiInterpretation) _then;

/// Create a copy of BmiInterpretation
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? category = null,Object? severity = null,}) {
  return _then(_self.copyWith(
category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as BmiCategory,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}

}


/// Adds pattern-matching-related methods to [BmiInterpretation].
extension BmiInterpretationPatterns on BmiInterpretation {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BmiInterpretation value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BmiInterpretation() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BmiInterpretation value)  $default,){
final _that = this;
switch (_that) {
case _BmiInterpretation():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BmiInterpretation value)?  $default,){
final _that = this;
switch (_that) {
case _BmiInterpretation() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( BmiCategory category,  InterpretationSeverity severity)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BmiInterpretation() when $default != null:
return $default(_that.category,_that.severity);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( BmiCategory category,  InterpretationSeverity severity)  $default,) {final _that = this;
switch (_that) {
case _BmiInterpretation():
return $default(_that.category,_that.severity);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( BmiCategory category,  InterpretationSeverity severity)?  $default,) {final _that = this;
switch (_that) {
case _BmiInterpretation() when $default != null:
return $default(_that.category,_that.severity);case _:
  return null;

}
}

}

/// @nodoc


class _BmiInterpretation implements BmiInterpretation {
  const _BmiInterpretation({required this.category, required this.severity});
  

@override final  BmiCategory category;
@override final  InterpretationSeverity severity;

/// Create a copy of BmiInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BmiInterpretationCopyWith<_BmiInterpretation> get copyWith => __$BmiInterpretationCopyWithImpl<_BmiInterpretation>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BmiInterpretation&&(identical(other.category, category) || other.category == category)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,category,severity);

@override
String toString() {
  return 'BmiInterpretation(category: $category, severity: $severity)';
}


}

/// @nodoc
abstract mixin class _$BmiInterpretationCopyWith<$Res> implements $BmiInterpretationCopyWith<$Res> {
  factory _$BmiInterpretationCopyWith(_BmiInterpretation value, $Res Function(_BmiInterpretation) _then) = __$BmiInterpretationCopyWithImpl;
@override @useResult
$Res call({
 BmiCategory category, InterpretationSeverity severity
});




}
/// @nodoc
class __$BmiInterpretationCopyWithImpl<$Res>
    implements _$BmiInterpretationCopyWith<$Res> {
  __$BmiInterpretationCopyWithImpl(this._self, this._then);

  final _BmiInterpretation _self;
  final $Res Function(_BmiInterpretation) _then;

/// Create a copy of BmiInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? category = null,Object? severity = null,}) {
  return _then(_BmiInterpretation(
category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as BmiCategory,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}


}

/// @nodoc
mixin _$FfmiInterpretation {

 FfmiCategory get category; InterpretationSeverity get severity;
/// Create a copy of FfmiInterpretation
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$FfmiInterpretationCopyWith<FfmiInterpretation> get copyWith => _$FfmiInterpretationCopyWithImpl<FfmiInterpretation>(this as FfmiInterpretation, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is FfmiInterpretation&&(identical(other.category, category) || other.category == category)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,category,severity);

@override
String toString() {
  return 'FfmiInterpretation(category: $category, severity: $severity)';
}


}

/// @nodoc
abstract mixin class $FfmiInterpretationCopyWith<$Res>  {
  factory $FfmiInterpretationCopyWith(FfmiInterpretation value, $Res Function(FfmiInterpretation) _then) = _$FfmiInterpretationCopyWithImpl;
@useResult
$Res call({
 FfmiCategory category, InterpretationSeverity severity
});




}
/// @nodoc
class _$FfmiInterpretationCopyWithImpl<$Res>
    implements $FfmiInterpretationCopyWith<$Res> {
  _$FfmiInterpretationCopyWithImpl(this._self, this._then);

  final FfmiInterpretation _self;
  final $Res Function(FfmiInterpretation) _then;

/// Create a copy of FfmiInterpretation
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? category = null,Object? severity = null,}) {
  return _then(_self.copyWith(
category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as FfmiCategory,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}

}


/// Adds pattern-matching-related methods to [FfmiInterpretation].
extension FfmiInterpretationPatterns on FfmiInterpretation {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _FfmiInterpretation value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _FfmiInterpretation() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _FfmiInterpretation value)  $default,){
final _that = this;
switch (_that) {
case _FfmiInterpretation():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _FfmiInterpretation value)?  $default,){
final _that = this;
switch (_that) {
case _FfmiInterpretation() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( FfmiCategory category,  InterpretationSeverity severity)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _FfmiInterpretation() when $default != null:
return $default(_that.category,_that.severity);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( FfmiCategory category,  InterpretationSeverity severity)  $default,) {final _that = this;
switch (_that) {
case _FfmiInterpretation():
return $default(_that.category,_that.severity);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( FfmiCategory category,  InterpretationSeverity severity)?  $default,) {final _that = this;
switch (_that) {
case _FfmiInterpretation() when $default != null:
return $default(_that.category,_that.severity);case _:
  return null;

}
}

}

/// @nodoc


class _FfmiInterpretation implements FfmiInterpretation {
  const _FfmiInterpretation({required this.category, required this.severity});
  

@override final  FfmiCategory category;
@override final  InterpretationSeverity severity;

/// Create a copy of FfmiInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$FfmiInterpretationCopyWith<_FfmiInterpretation> get copyWith => __$FfmiInterpretationCopyWithImpl<_FfmiInterpretation>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _FfmiInterpretation&&(identical(other.category, category) || other.category == category)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,category,severity);

@override
String toString() {
  return 'FfmiInterpretation(category: $category, severity: $severity)';
}


}

/// @nodoc
abstract mixin class _$FfmiInterpretationCopyWith<$Res> implements $FfmiInterpretationCopyWith<$Res> {
  factory _$FfmiInterpretationCopyWith(_FfmiInterpretation value, $Res Function(_FfmiInterpretation) _then) = __$FfmiInterpretationCopyWithImpl;
@override @useResult
$Res call({
 FfmiCategory category, InterpretationSeverity severity
});




}
/// @nodoc
class __$FfmiInterpretationCopyWithImpl<$Res>
    implements _$FfmiInterpretationCopyWith<$Res> {
  __$FfmiInterpretationCopyWithImpl(this._self, this._then);

  final _FfmiInterpretation _self;
  final $Res Function(_FfmiInterpretation) _then;

/// Create a copy of FfmiInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? category = null,Object? severity = null,}) {
  return _then(_FfmiInterpretation(
category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as FfmiCategory,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}


}

/// @nodoc
mixin _$SleepTargetInterpretation {

 SleepTargetStatus get status; double get averageHours; double get targetHours; double get gapHours; InterpretationSeverity get severity;
/// Create a copy of SleepTargetInterpretation
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepTargetInterpretationCopyWith<SleepTargetInterpretation> get copyWith => _$SleepTargetInterpretationCopyWithImpl<SleepTargetInterpretation>(this as SleepTargetInterpretation, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepTargetInterpretation&&(identical(other.status, status) || other.status == status)&&(identical(other.averageHours, averageHours) || other.averageHours == averageHours)&&(identical(other.targetHours, targetHours) || other.targetHours == targetHours)&&(identical(other.gapHours, gapHours) || other.gapHours == gapHours)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,status,averageHours,targetHours,gapHours,severity);

@override
String toString() {
  return 'SleepTargetInterpretation(status: $status, averageHours: $averageHours, targetHours: $targetHours, gapHours: $gapHours, severity: $severity)';
}


}

/// @nodoc
abstract mixin class $SleepTargetInterpretationCopyWith<$Res>  {
  factory $SleepTargetInterpretationCopyWith(SleepTargetInterpretation value, $Res Function(SleepTargetInterpretation) _then) = _$SleepTargetInterpretationCopyWithImpl;
@useResult
$Res call({
 SleepTargetStatus status, double averageHours, double targetHours, double gapHours, InterpretationSeverity severity
});




}
/// @nodoc
class _$SleepTargetInterpretationCopyWithImpl<$Res>
    implements $SleepTargetInterpretationCopyWith<$Res> {
  _$SleepTargetInterpretationCopyWithImpl(this._self, this._then);

  final SleepTargetInterpretation _self;
  final $Res Function(SleepTargetInterpretation) _then;

/// Create a copy of SleepTargetInterpretation
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? averageHours = null,Object? targetHours = null,Object? gapHours = null,Object? severity = null,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as SleepTargetStatus,averageHours: null == averageHours ? _self.averageHours : averageHours // ignore: cast_nullable_to_non_nullable
as double,targetHours: null == targetHours ? _self.targetHours : targetHours // ignore: cast_nullable_to_non_nullable
as double,gapHours: null == gapHours ? _self.gapHours : gapHours // ignore: cast_nullable_to_non_nullable
as double,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}

}


/// Adds pattern-matching-related methods to [SleepTargetInterpretation].
extension SleepTargetInterpretationPatterns on SleepTargetInterpretation {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepTargetInterpretation value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepTargetInterpretation() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepTargetInterpretation value)  $default,){
final _that = this;
switch (_that) {
case _SleepTargetInterpretation():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepTargetInterpretation value)?  $default,){
final _that = this;
switch (_that) {
case _SleepTargetInterpretation() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( SleepTargetStatus status,  double averageHours,  double targetHours,  double gapHours,  InterpretationSeverity severity)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepTargetInterpretation() when $default != null:
return $default(_that.status,_that.averageHours,_that.targetHours,_that.gapHours,_that.severity);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( SleepTargetStatus status,  double averageHours,  double targetHours,  double gapHours,  InterpretationSeverity severity)  $default,) {final _that = this;
switch (_that) {
case _SleepTargetInterpretation():
return $default(_that.status,_that.averageHours,_that.targetHours,_that.gapHours,_that.severity);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( SleepTargetStatus status,  double averageHours,  double targetHours,  double gapHours,  InterpretationSeverity severity)?  $default,) {final _that = this;
switch (_that) {
case _SleepTargetInterpretation() when $default != null:
return $default(_that.status,_that.averageHours,_that.targetHours,_that.gapHours,_that.severity);case _:
  return null;

}
}

}

/// @nodoc


class _SleepTargetInterpretation implements SleepTargetInterpretation {
  const _SleepTargetInterpretation({required this.status, required this.averageHours, required this.targetHours, required this.gapHours, required this.severity});
  

@override final  SleepTargetStatus status;
@override final  double averageHours;
@override final  double targetHours;
@override final  double gapHours;
@override final  InterpretationSeverity severity;

/// Create a copy of SleepTargetInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepTargetInterpretationCopyWith<_SleepTargetInterpretation> get copyWith => __$SleepTargetInterpretationCopyWithImpl<_SleepTargetInterpretation>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepTargetInterpretation&&(identical(other.status, status) || other.status == status)&&(identical(other.averageHours, averageHours) || other.averageHours == averageHours)&&(identical(other.targetHours, targetHours) || other.targetHours == targetHours)&&(identical(other.gapHours, gapHours) || other.gapHours == gapHours)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,status,averageHours,targetHours,gapHours,severity);

@override
String toString() {
  return 'SleepTargetInterpretation(status: $status, averageHours: $averageHours, targetHours: $targetHours, gapHours: $gapHours, severity: $severity)';
}


}

/// @nodoc
abstract mixin class _$SleepTargetInterpretationCopyWith<$Res> implements $SleepTargetInterpretationCopyWith<$Res> {
  factory _$SleepTargetInterpretationCopyWith(_SleepTargetInterpretation value, $Res Function(_SleepTargetInterpretation) _then) = __$SleepTargetInterpretationCopyWithImpl;
@override @useResult
$Res call({
 SleepTargetStatus status, double averageHours, double targetHours, double gapHours, InterpretationSeverity severity
});




}
/// @nodoc
class __$SleepTargetInterpretationCopyWithImpl<$Res>
    implements _$SleepTargetInterpretationCopyWith<$Res> {
  __$SleepTargetInterpretationCopyWithImpl(this._self, this._then);

  final _SleepTargetInterpretation _self;
  final $Res Function(_SleepTargetInterpretation) _then;

/// Create a copy of SleepTargetInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? averageHours = null,Object? targetHours = null,Object? gapHours = null,Object? severity = null,}) {
  return _then(_SleepTargetInterpretation(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as SleepTargetStatus,averageHours: null == averageHours ? _self.averageHours : averageHours // ignore: cast_nullable_to_non_nullable
as double,targetHours: null == targetHours ? _self.targetHours : targetHours // ignore: cast_nullable_to_non_nullable
as double,gapHours: null == gapHours ? _self.gapHours : gapHours // ignore: cast_nullable_to_non_nullable
as double,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}


}

/// @nodoc
mixin _$MacroSplitInterpretation {

 double get proteinPercent; double get carbsPercent; double get fatPercent; MacroRangeStatus get proteinStatus; MacroRangeStatus get carbsStatus; MacroRangeStatus get fatStatus; InterpretationSeverity get severity;
/// Create a copy of MacroSplitInterpretation
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MacroSplitInterpretationCopyWith<MacroSplitInterpretation> get copyWith => _$MacroSplitInterpretationCopyWithImpl<MacroSplitInterpretation>(this as MacroSplitInterpretation, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MacroSplitInterpretation&&(identical(other.proteinPercent, proteinPercent) || other.proteinPercent == proteinPercent)&&(identical(other.carbsPercent, carbsPercent) || other.carbsPercent == carbsPercent)&&(identical(other.fatPercent, fatPercent) || other.fatPercent == fatPercent)&&(identical(other.proteinStatus, proteinStatus) || other.proteinStatus == proteinStatus)&&(identical(other.carbsStatus, carbsStatus) || other.carbsStatus == carbsStatus)&&(identical(other.fatStatus, fatStatus) || other.fatStatus == fatStatus)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,proteinPercent,carbsPercent,fatPercent,proteinStatus,carbsStatus,fatStatus,severity);

@override
String toString() {
  return 'MacroSplitInterpretation(proteinPercent: $proteinPercent, carbsPercent: $carbsPercent, fatPercent: $fatPercent, proteinStatus: $proteinStatus, carbsStatus: $carbsStatus, fatStatus: $fatStatus, severity: $severity)';
}


}

/// @nodoc
abstract mixin class $MacroSplitInterpretationCopyWith<$Res>  {
  factory $MacroSplitInterpretationCopyWith(MacroSplitInterpretation value, $Res Function(MacroSplitInterpretation) _then) = _$MacroSplitInterpretationCopyWithImpl;
@useResult
$Res call({
 double proteinPercent, double carbsPercent, double fatPercent, MacroRangeStatus proteinStatus, MacroRangeStatus carbsStatus, MacroRangeStatus fatStatus, InterpretationSeverity severity
});




}
/// @nodoc
class _$MacroSplitInterpretationCopyWithImpl<$Res>
    implements $MacroSplitInterpretationCopyWith<$Res> {
  _$MacroSplitInterpretationCopyWithImpl(this._self, this._then);

  final MacroSplitInterpretation _self;
  final $Res Function(MacroSplitInterpretation) _then;

/// Create a copy of MacroSplitInterpretation
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? proteinPercent = null,Object? carbsPercent = null,Object? fatPercent = null,Object? proteinStatus = null,Object? carbsStatus = null,Object? fatStatus = null,Object? severity = null,}) {
  return _then(_self.copyWith(
proteinPercent: null == proteinPercent ? _self.proteinPercent : proteinPercent // ignore: cast_nullable_to_non_nullable
as double,carbsPercent: null == carbsPercent ? _self.carbsPercent : carbsPercent // ignore: cast_nullable_to_non_nullable
as double,fatPercent: null == fatPercent ? _self.fatPercent : fatPercent // ignore: cast_nullable_to_non_nullable
as double,proteinStatus: null == proteinStatus ? _self.proteinStatus : proteinStatus // ignore: cast_nullable_to_non_nullable
as MacroRangeStatus,carbsStatus: null == carbsStatus ? _self.carbsStatus : carbsStatus // ignore: cast_nullable_to_non_nullable
as MacroRangeStatus,fatStatus: null == fatStatus ? _self.fatStatus : fatStatus // ignore: cast_nullable_to_non_nullable
as MacroRangeStatus,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}

}


/// Adds pattern-matching-related methods to [MacroSplitInterpretation].
extension MacroSplitInterpretationPatterns on MacroSplitInterpretation {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MacroSplitInterpretation value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MacroSplitInterpretation() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MacroSplitInterpretation value)  $default,){
final _that = this;
switch (_that) {
case _MacroSplitInterpretation():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MacroSplitInterpretation value)?  $default,){
final _that = this;
switch (_that) {
case _MacroSplitInterpretation() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double proteinPercent,  double carbsPercent,  double fatPercent,  MacroRangeStatus proteinStatus,  MacroRangeStatus carbsStatus,  MacroRangeStatus fatStatus,  InterpretationSeverity severity)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MacroSplitInterpretation() when $default != null:
return $default(_that.proteinPercent,_that.carbsPercent,_that.fatPercent,_that.proteinStatus,_that.carbsStatus,_that.fatStatus,_that.severity);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double proteinPercent,  double carbsPercent,  double fatPercent,  MacroRangeStatus proteinStatus,  MacroRangeStatus carbsStatus,  MacroRangeStatus fatStatus,  InterpretationSeverity severity)  $default,) {final _that = this;
switch (_that) {
case _MacroSplitInterpretation():
return $default(_that.proteinPercent,_that.carbsPercent,_that.fatPercent,_that.proteinStatus,_that.carbsStatus,_that.fatStatus,_that.severity);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double proteinPercent,  double carbsPercent,  double fatPercent,  MacroRangeStatus proteinStatus,  MacroRangeStatus carbsStatus,  MacroRangeStatus fatStatus,  InterpretationSeverity severity)?  $default,) {final _that = this;
switch (_that) {
case _MacroSplitInterpretation() when $default != null:
return $default(_that.proteinPercent,_that.carbsPercent,_that.fatPercent,_that.proteinStatus,_that.carbsStatus,_that.fatStatus,_that.severity);case _:
  return null;

}
}

}

/// @nodoc


class _MacroSplitInterpretation extends MacroSplitInterpretation {
  const _MacroSplitInterpretation({required this.proteinPercent, required this.carbsPercent, required this.fatPercent, required this.proteinStatus, required this.carbsStatus, required this.fatStatus, required this.severity}): super._();
  

@override final  double proteinPercent;
@override final  double carbsPercent;
@override final  double fatPercent;
@override final  MacroRangeStatus proteinStatus;
@override final  MacroRangeStatus carbsStatus;
@override final  MacroRangeStatus fatStatus;
@override final  InterpretationSeverity severity;

/// Create a copy of MacroSplitInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MacroSplitInterpretationCopyWith<_MacroSplitInterpretation> get copyWith => __$MacroSplitInterpretationCopyWithImpl<_MacroSplitInterpretation>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MacroSplitInterpretation&&(identical(other.proteinPercent, proteinPercent) || other.proteinPercent == proteinPercent)&&(identical(other.carbsPercent, carbsPercent) || other.carbsPercent == carbsPercent)&&(identical(other.fatPercent, fatPercent) || other.fatPercent == fatPercent)&&(identical(other.proteinStatus, proteinStatus) || other.proteinStatus == proteinStatus)&&(identical(other.carbsStatus, carbsStatus) || other.carbsStatus == carbsStatus)&&(identical(other.fatStatus, fatStatus) || other.fatStatus == fatStatus)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,proteinPercent,carbsPercent,fatPercent,proteinStatus,carbsStatus,fatStatus,severity);

@override
String toString() {
  return 'MacroSplitInterpretation(proteinPercent: $proteinPercent, carbsPercent: $carbsPercent, fatPercent: $fatPercent, proteinStatus: $proteinStatus, carbsStatus: $carbsStatus, fatStatus: $fatStatus, severity: $severity)';
}


}

/// @nodoc
abstract mixin class _$MacroSplitInterpretationCopyWith<$Res> implements $MacroSplitInterpretationCopyWith<$Res> {
  factory _$MacroSplitInterpretationCopyWith(_MacroSplitInterpretation value, $Res Function(_MacroSplitInterpretation) _then) = __$MacroSplitInterpretationCopyWithImpl;
@override @useResult
$Res call({
 double proteinPercent, double carbsPercent, double fatPercent, MacroRangeStatus proteinStatus, MacroRangeStatus carbsStatus, MacroRangeStatus fatStatus, InterpretationSeverity severity
});




}
/// @nodoc
class __$MacroSplitInterpretationCopyWithImpl<$Res>
    implements _$MacroSplitInterpretationCopyWith<$Res> {
  __$MacroSplitInterpretationCopyWithImpl(this._self, this._then);

  final _MacroSplitInterpretation _self;
  final $Res Function(_MacroSplitInterpretation) _then;

/// Create a copy of MacroSplitInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? proteinPercent = null,Object? carbsPercent = null,Object? fatPercent = null,Object? proteinStatus = null,Object? carbsStatus = null,Object? fatStatus = null,Object? severity = null,}) {
  return _then(_MacroSplitInterpretation(
proteinPercent: null == proteinPercent ? _self.proteinPercent : proteinPercent // ignore: cast_nullable_to_non_nullable
as double,carbsPercent: null == carbsPercent ? _self.carbsPercent : carbsPercent // ignore: cast_nullable_to_non_nullable
as double,fatPercent: null == fatPercent ? _self.fatPercent : fatPercent // ignore: cast_nullable_to_non_nullable
as double,proteinStatus: null == proteinStatus ? _self.proteinStatus : proteinStatus // ignore: cast_nullable_to_non_nullable
as MacroRangeStatus,carbsStatus: null == carbsStatus ? _self.carbsStatus : carbsStatus // ignore: cast_nullable_to_non_nullable
as MacroRangeStatus,fatStatus: null == fatStatus ? _self.fatStatus : fatStatus // ignore: cast_nullable_to_non_nullable
as MacroRangeStatus,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}


}

/// @nodoc
mixin _$WorkoutGuidelineProgress {

 double get loggedMinutes; double get referenceMinutes; double get percentOfReference; WorkoutGuidelineStatus get status; InterpretationSeverity get severity;
/// Create a copy of WorkoutGuidelineProgress
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$WorkoutGuidelineProgressCopyWith<WorkoutGuidelineProgress> get copyWith => _$WorkoutGuidelineProgressCopyWithImpl<WorkoutGuidelineProgress>(this as WorkoutGuidelineProgress, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is WorkoutGuidelineProgress&&(identical(other.loggedMinutes, loggedMinutes) || other.loggedMinutes == loggedMinutes)&&(identical(other.referenceMinutes, referenceMinutes) || other.referenceMinutes == referenceMinutes)&&(identical(other.percentOfReference, percentOfReference) || other.percentOfReference == percentOfReference)&&(identical(other.status, status) || other.status == status)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,loggedMinutes,referenceMinutes,percentOfReference,status,severity);

@override
String toString() {
  return 'WorkoutGuidelineProgress(loggedMinutes: $loggedMinutes, referenceMinutes: $referenceMinutes, percentOfReference: $percentOfReference, status: $status, severity: $severity)';
}


}

/// @nodoc
abstract mixin class $WorkoutGuidelineProgressCopyWith<$Res>  {
  factory $WorkoutGuidelineProgressCopyWith(WorkoutGuidelineProgress value, $Res Function(WorkoutGuidelineProgress) _then) = _$WorkoutGuidelineProgressCopyWithImpl;
@useResult
$Res call({
 double loggedMinutes, double referenceMinutes, double percentOfReference, WorkoutGuidelineStatus status, InterpretationSeverity severity
});




}
/// @nodoc
class _$WorkoutGuidelineProgressCopyWithImpl<$Res>
    implements $WorkoutGuidelineProgressCopyWith<$Res> {
  _$WorkoutGuidelineProgressCopyWithImpl(this._self, this._then);

  final WorkoutGuidelineProgress _self;
  final $Res Function(WorkoutGuidelineProgress) _then;

/// Create a copy of WorkoutGuidelineProgress
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? loggedMinutes = null,Object? referenceMinutes = null,Object? percentOfReference = null,Object? status = null,Object? severity = null,}) {
  return _then(_self.copyWith(
loggedMinutes: null == loggedMinutes ? _self.loggedMinutes : loggedMinutes // ignore: cast_nullable_to_non_nullable
as double,referenceMinutes: null == referenceMinutes ? _self.referenceMinutes : referenceMinutes // ignore: cast_nullable_to_non_nullable
as double,percentOfReference: null == percentOfReference ? _self.percentOfReference : percentOfReference // ignore: cast_nullable_to_non_nullable
as double,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as WorkoutGuidelineStatus,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}

}


/// Adds pattern-matching-related methods to [WorkoutGuidelineProgress].
extension WorkoutGuidelineProgressPatterns on WorkoutGuidelineProgress {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _WorkoutGuidelineProgress value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _WorkoutGuidelineProgress() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _WorkoutGuidelineProgress value)  $default,){
final _that = this;
switch (_that) {
case _WorkoutGuidelineProgress():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _WorkoutGuidelineProgress value)?  $default,){
final _that = this;
switch (_that) {
case _WorkoutGuidelineProgress() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( double loggedMinutes,  double referenceMinutes,  double percentOfReference,  WorkoutGuidelineStatus status,  InterpretationSeverity severity)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _WorkoutGuidelineProgress() when $default != null:
return $default(_that.loggedMinutes,_that.referenceMinutes,_that.percentOfReference,_that.status,_that.severity);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( double loggedMinutes,  double referenceMinutes,  double percentOfReference,  WorkoutGuidelineStatus status,  InterpretationSeverity severity)  $default,) {final _that = this;
switch (_that) {
case _WorkoutGuidelineProgress():
return $default(_that.loggedMinutes,_that.referenceMinutes,_that.percentOfReference,_that.status,_that.severity);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( double loggedMinutes,  double referenceMinutes,  double percentOfReference,  WorkoutGuidelineStatus status,  InterpretationSeverity severity)?  $default,) {final _that = this;
switch (_that) {
case _WorkoutGuidelineProgress() when $default != null:
return $default(_that.loggedMinutes,_that.referenceMinutes,_that.percentOfReference,_that.status,_that.severity);case _:
  return null;

}
}

}

/// @nodoc


class _WorkoutGuidelineProgress implements WorkoutGuidelineProgress {
  const _WorkoutGuidelineProgress({required this.loggedMinutes, required this.referenceMinutes, required this.percentOfReference, required this.status, required this.severity});
  

@override final  double loggedMinutes;
@override final  double referenceMinutes;
@override final  double percentOfReference;
@override final  WorkoutGuidelineStatus status;
@override final  InterpretationSeverity severity;

/// Create a copy of WorkoutGuidelineProgress
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$WorkoutGuidelineProgressCopyWith<_WorkoutGuidelineProgress> get copyWith => __$WorkoutGuidelineProgressCopyWithImpl<_WorkoutGuidelineProgress>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _WorkoutGuidelineProgress&&(identical(other.loggedMinutes, loggedMinutes) || other.loggedMinutes == loggedMinutes)&&(identical(other.referenceMinutes, referenceMinutes) || other.referenceMinutes == referenceMinutes)&&(identical(other.percentOfReference, percentOfReference) || other.percentOfReference == percentOfReference)&&(identical(other.status, status) || other.status == status)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,loggedMinutes,referenceMinutes,percentOfReference,status,severity);

@override
String toString() {
  return 'WorkoutGuidelineProgress(loggedMinutes: $loggedMinutes, referenceMinutes: $referenceMinutes, percentOfReference: $percentOfReference, status: $status, severity: $severity)';
}


}

/// @nodoc
abstract mixin class _$WorkoutGuidelineProgressCopyWith<$Res> implements $WorkoutGuidelineProgressCopyWith<$Res> {
  factory _$WorkoutGuidelineProgressCopyWith(_WorkoutGuidelineProgress value, $Res Function(_WorkoutGuidelineProgress) _then) = __$WorkoutGuidelineProgressCopyWithImpl;
@override @useResult
$Res call({
 double loggedMinutes, double referenceMinutes, double percentOfReference, WorkoutGuidelineStatus status, InterpretationSeverity severity
});




}
/// @nodoc
class __$WorkoutGuidelineProgressCopyWithImpl<$Res>
    implements _$WorkoutGuidelineProgressCopyWith<$Res> {
  __$WorkoutGuidelineProgressCopyWithImpl(this._self, this._then);

  final _WorkoutGuidelineProgress _self;
  final $Res Function(_WorkoutGuidelineProgress) _then;

/// Create a copy of WorkoutGuidelineProgress
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? loggedMinutes = null,Object? referenceMinutes = null,Object? percentOfReference = null,Object? status = null,Object? severity = null,}) {
  return _then(_WorkoutGuidelineProgress(
loggedMinutes: null == loggedMinutes ? _self.loggedMinutes : loggedMinutes // ignore: cast_nullable_to_non_nullable
as double,referenceMinutes: null == referenceMinutes ? _self.referenceMinutes : referenceMinutes // ignore: cast_nullable_to_non_nullable
as double,percentOfReference: null == percentOfReference ? _self.percentOfReference : percentOfReference // ignore: cast_nullable_to_non_nullable
as double,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as WorkoutGuidelineStatus,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}


}

/// @nodoc
mixin _$VitalContextInterpretation {

 VitalContextStatus get status; InterpretationSeverity get severity;
/// Create a copy of VitalContextInterpretation
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$VitalContextInterpretationCopyWith<VitalContextInterpretation> get copyWith => _$VitalContextInterpretationCopyWithImpl<VitalContextInterpretation>(this as VitalContextInterpretation, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is VitalContextInterpretation&&(identical(other.status, status) || other.status == status)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,status,severity);

@override
String toString() {
  return 'VitalContextInterpretation(status: $status, severity: $severity)';
}


}

/// @nodoc
abstract mixin class $VitalContextInterpretationCopyWith<$Res>  {
  factory $VitalContextInterpretationCopyWith(VitalContextInterpretation value, $Res Function(VitalContextInterpretation) _then) = _$VitalContextInterpretationCopyWithImpl;
@useResult
$Res call({
 VitalContextStatus status, InterpretationSeverity severity
});




}
/// @nodoc
class _$VitalContextInterpretationCopyWithImpl<$Res>
    implements $VitalContextInterpretationCopyWith<$Res> {
  _$VitalContextInterpretationCopyWithImpl(this._self, this._then);

  final VitalContextInterpretation _self;
  final $Res Function(VitalContextInterpretation) _then;

/// Create a copy of VitalContextInterpretation
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? status = null,Object? severity = null,}) {
  return _then(_self.copyWith(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as VitalContextStatus,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}

}


/// Adds pattern-matching-related methods to [VitalContextInterpretation].
extension VitalContextInterpretationPatterns on VitalContextInterpretation {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _VitalContextInterpretation value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _VitalContextInterpretation() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _VitalContextInterpretation value)  $default,){
final _that = this;
switch (_that) {
case _VitalContextInterpretation():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _VitalContextInterpretation value)?  $default,){
final _that = this;
switch (_that) {
case _VitalContextInterpretation() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( VitalContextStatus status,  InterpretationSeverity severity)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _VitalContextInterpretation() when $default != null:
return $default(_that.status,_that.severity);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( VitalContextStatus status,  InterpretationSeverity severity)  $default,) {final _that = this;
switch (_that) {
case _VitalContextInterpretation():
return $default(_that.status,_that.severity);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( VitalContextStatus status,  InterpretationSeverity severity)?  $default,) {final _that = this;
switch (_that) {
case _VitalContextInterpretation() when $default != null:
return $default(_that.status,_that.severity);case _:
  return null;

}
}

}

/// @nodoc


class _VitalContextInterpretation implements VitalContextInterpretation {
  const _VitalContextInterpretation({required this.status, required this.severity});
  

@override final  VitalContextStatus status;
@override final  InterpretationSeverity severity;

/// Create a copy of VitalContextInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$VitalContextInterpretationCopyWith<_VitalContextInterpretation> get copyWith => __$VitalContextInterpretationCopyWithImpl<_VitalContextInterpretation>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _VitalContextInterpretation&&(identical(other.status, status) || other.status == status)&&(identical(other.severity, severity) || other.severity == severity));
}


@override
int get hashCode => Object.hash(runtimeType,status,severity);

@override
String toString() {
  return 'VitalContextInterpretation(status: $status, severity: $severity)';
}


}

/// @nodoc
abstract mixin class _$VitalContextInterpretationCopyWith<$Res> implements $VitalContextInterpretationCopyWith<$Res> {
  factory _$VitalContextInterpretationCopyWith(_VitalContextInterpretation value, $Res Function(_VitalContextInterpretation) _then) = __$VitalContextInterpretationCopyWithImpl;
@override @useResult
$Res call({
 VitalContextStatus status, InterpretationSeverity severity
});




}
/// @nodoc
class __$VitalContextInterpretationCopyWithImpl<$Res>
    implements _$VitalContextInterpretationCopyWith<$Res> {
  __$VitalContextInterpretationCopyWithImpl(this._self, this._then);

  final _VitalContextInterpretation _self;
  final $Res Function(_VitalContextInterpretation) _then;

/// Create a copy of VitalContextInterpretation
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? status = null,Object? severity = null,}) {
  return _then(_VitalContextInterpretation(
status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as VitalContextStatus,severity: null == severity ? _self.severity : severity // ignore: cast_nullable_to_non_nullable
as InterpretationSeverity,
  ));
}


}

// dart format on
