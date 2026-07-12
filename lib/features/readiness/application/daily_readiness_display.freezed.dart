// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'daily_readiness_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$DailyReadinessDisplay {

 String get confidenceText; String get hrvStatusValue; String get intensityMinutesValue; String get stressValue; String get strainValue; List<DailyReadinessFactor> get topFactors;
/// Create a copy of DailyReadinessDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DailyReadinessDisplayCopyWith<DailyReadinessDisplay> get copyWith => _$DailyReadinessDisplayCopyWithImpl<DailyReadinessDisplay>(this as DailyReadinessDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DailyReadinessDisplay&&(identical(other.confidenceText, confidenceText) || other.confidenceText == confidenceText)&&(identical(other.hrvStatusValue, hrvStatusValue) || other.hrvStatusValue == hrvStatusValue)&&(identical(other.intensityMinutesValue, intensityMinutesValue) || other.intensityMinutesValue == intensityMinutesValue)&&(identical(other.stressValue, stressValue) || other.stressValue == stressValue)&&(identical(other.strainValue, strainValue) || other.strainValue == strainValue)&&const DeepCollectionEquality().equals(other.topFactors, topFactors));
}


@override
int get hashCode => Object.hash(runtimeType,confidenceText,hrvStatusValue,intensityMinutesValue,stressValue,strainValue,const DeepCollectionEquality().hash(topFactors));

@override
String toString() {
  return 'DailyReadinessDisplay(confidenceText: $confidenceText, hrvStatusValue: $hrvStatusValue, intensityMinutesValue: $intensityMinutesValue, stressValue: $stressValue, strainValue: $strainValue, topFactors: $topFactors)';
}


}

/// @nodoc
abstract mixin class $DailyReadinessDisplayCopyWith<$Res>  {
  factory $DailyReadinessDisplayCopyWith(DailyReadinessDisplay value, $Res Function(DailyReadinessDisplay) _then) = _$DailyReadinessDisplayCopyWithImpl;
@useResult
$Res call({
 String confidenceText, String hrvStatusValue, String intensityMinutesValue, String stressValue, String strainValue, List<DailyReadinessFactor> topFactors
});




}
/// @nodoc
class _$DailyReadinessDisplayCopyWithImpl<$Res>
    implements $DailyReadinessDisplayCopyWith<$Res> {
  _$DailyReadinessDisplayCopyWithImpl(this._self, this._then);

  final DailyReadinessDisplay _self;
  final $Res Function(DailyReadinessDisplay) _then;

/// Create a copy of DailyReadinessDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? confidenceText = null,Object? hrvStatusValue = null,Object? intensityMinutesValue = null,Object? stressValue = null,Object? strainValue = null,Object? topFactors = null,}) {
  return _then(_self.copyWith(
confidenceText: null == confidenceText ? _self.confidenceText : confidenceText // ignore: cast_nullable_to_non_nullable
as String,hrvStatusValue: null == hrvStatusValue ? _self.hrvStatusValue : hrvStatusValue // ignore: cast_nullable_to_non_nullable
as String,intensityMinutesValue: null == intensityMinutesValue ? _self.intensityMinutesValue : intensityMinutesValue // ignore: cast_nullable_to_non_nullable
as String,stressValue: null == stressValue ? _self.stressValue : stressValue // ignore: cast_nullable_to_non_nullable
as String,strainValue: null == strainValue ? _self.strainValue : strainValue // ignore: cast_nullable_to_non_nullable
as String,topFactors: null == topFactors ? _self.topFactors : topFactors // ignore: cast_nullable_to_non_nullable
as List<DailyReadinessFactor>,
  ));
}

}


/// Adds pattern-matching-related methods to [DailyReadinessDisplay].
extension DailyReadinessDisplayPatterns on DailyReadinessDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DailyReadinessDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DailyReadinessDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DailyReadinessDisplay value)  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DailyReadinessDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _DailyReadinessDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String confidenceText,  String hrvStatusValue,  String intensityMinutesValue,  String stressValue,  String strainValue,  List<DailyReadinessFactor> topFactors)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DailyReadinessDisplay() when $default != null:
return $default(_that.confidenceText,_that.hrvStatusValue,_that.intensityMinutesValue,_that.stressValue,_that.strainValue,_that.topFactors);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String confidenceText,  String hrvStatusValue,  String intensityMinutesValue,  String stressValue,  String strainValue,  List<DailyReadinessFactor> topFactors)  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessDisplay():
return $default(_that.confidenceText,_that.hrvStatusValue,_that.intensityMinutesValue,_that.stressValue,_that.strainValue,_that.topFactors);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String confidenceText,  String hrvStatusValue,  String intensityMinutesValue,  String stressValue,  String strainValue,  List<DailyReadinessFactor> topFactors)?  $default,) {final _that = this;
switch (_that) {
case _DailyReadinessDisplay() when $default != null:
return $default(_that.confidenceText,_that.hrvStatusValue,_that.intensityMinutesValue,_that.stressValue,_that.strainValue,_that.topFactors);case _:
  return null;

}
}

}

/// @nodoc


class _DailyReadinessDisplay implements DailyReadinessDisplay {
  const _DailyReadinessDisplay({required this.confidenceText, required this.hrvStatusValue, required this.intensityMinutesValue, required this.stressValue, required this.strainValue, required final  List<DailyReadinessFactor> topFactors}): _topFactors = topFactors;
  

@override final  String confidenceText;
@override final  String hrvStatusValue;
@override final  String intensityMinutesValue;
@override final  String stressValue;
@override final  String strainValue;
 final  List<DailyReadinessFactor> _topFactors;
@override List<DailyReadinessFactor> get topFactors {
  if (_topFactors is EqualUnmodifiableListView) return _topFactors;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_topFactors);
}


/// Create a copy of DailyReadinessDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DailyReadinessDisplayCopyWith<_DailyReadinessDisplay> get copyWith => __$DailyReadinessDisplayCopyWithImpl<_DailyReadinessDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DailyReadinessDisplay&&(identical(other.confidenceText, confidenceText) || other.confidenceText == confidenceText)&&(identical(other.hrvStatusValue, hrvStatusValue) || other.hrvStatusValue == hrvStatusValue)&&(identical(other.intensityMinutesValue, intensityMinutesValue) || other.intensityMinutesValue == intensityMinutesValue)&&(identical(other.stressValue, stressValue) || other.stressValue == stressValue)&&(identical(other.strainValue, strainValue) || other.strainValue == strainValue)&&const DeepCollectionEquality().equals(other._topFactors, _topFactors));
}


@override
int get hashCode => Object.hash(runtimeType,confidenceText,hrvStatusValue,intensityMinutesValue,stressValue,strainValue,const DeepCollectionEquality().hash(_topFactors));

@override
String toString() {
  return 'DailyReadinessDisplay(confidenceText: $confidenceText, hrvStatusValue: $hrvStatusValue, intensityMinutesValue: $intensityMinutesValue, stressValue: $stressValue, strainValue: $strainValue, topFactors: $topFactors)';
}


}

/// @nodoc
abstract mixin class _$DailyReadinessDisplayCopyWith<$Res> implements $DailyReadinessDisplayCopyWith<$Res> {
  factory _$DailyReadinessDisplayCopyWith(_DailyReadinessDisplay value, $Res Function(_DailyReadinessDisplay) _then) = __$DailyReadinessDisplayCopyWithImpl;
@override @useResult
$Res call({
 String confidenceText, String hrvStatusValue, String intensityMinutesValue, String stressValue, String strainValue, List<DailyReadinessFactor> topFactors
});




}
/// @nodoc
class __$DailyReadinessDisplayCopyWithImpl<$Res>
    implements _$DailyReadinessDisplayCopyWith<$Res> {
  __$DailyReadinessDisplayCopyWithImpl(this._self, this._then);

  final _DailyReadinessDisplay _self;
  final $Res Function(_DailyReadinessDisplay) _then;

/// Create a copy of DailyReadinessDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? confidenceText = null,Object? hrvStatusValue = null,Object? intensityMinutesValue = null,Object? stressValue = null,Object? strainValue = null,Object? topFactors = null,}) {
  return _then(_DailyReadinessDisplay(
confidenceText: null == confidenceText ? _self.confidenceText : confidenceText // ignore: cast_nullable_to_non_nullable
as String,hrvStatusValue: null == hrvStatusValue ? _self.hrvStatusValue : hrvStatusValue // ignore: cast_nullable_to_non_nullable
as String,intensityMinutesValue: null == intensityMinutesValue ? _self.intensityMinutesValue : intensityMinutesValue // ignore: cast_nullable_to_non_nullable
as String,stressValue: null == stressValue ? _self.stressValue : stressValue // ignore: cast_nullable_to_non_nullable
as String,strainValue: null == strainValue ? _self.strainValue : strainValue // ignore: cast_nullable_to_non_nullable
as String,topFactors: null == topFactors ? _self._topFactors : topFactors // ignore: cast_nullable_to_non_nullable
as List<DailyReadinessFactor>,
  ));
}


}

// dart format on
