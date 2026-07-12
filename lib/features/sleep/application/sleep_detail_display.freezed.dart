// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'sleep_detail_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$SleepDetailDisplay {

 SleepData get session;/// The night's stages, earliest first.
 List<SleepStage> get sortedStages;/// Longest stage type first, as the breakdown card lists them.
 List<SleepStageTotal> get stageTotals;
/// Create a copy of SleepDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SleepDetailDisplayCopyWith<SleepDetailDisplay> get copyWith => _$SleepDetailDisplayCopyWithImpl<SleepDetailDisplay>(this as SleepDetailDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SleepDetailDisplay&&(identical(other.session, session) || other.session == session)&&const DeepCollectionEquality().equals(other.sortedStages, sortedStages)&&const DeepCollectionEquality().equals(other.stageTotals, stageTotals));
}


@override
int get hashCode => Object.hash(runtimeType,session,const DeepCollectionEquality().hash(sortedStages),const DeepCollectionEquality().hash(stageTotals));

@override
String toString() {
  return 'SleepDetailDisplay(session: $session, sortedStages: $sortedStages, stageTotals: $stageTotals)';
}


}

/// @nodoc
abstract mixin class $SleepDetailDisplayCopyWith<$Res>  {
  factory $SleepDetailDisplayCopyWith(SleepDetailDisplay value, $Res Function(SleepDetailDisplay) _then) = _$SleepDetailDisplayCopyWithImpl;
@useResult
$Res call({
 SleepData session, List<SleepStage> sortedStages, List<SleepStageTotal> stageTotals
});


$SleepDataCopyWith<$Res> get session;

}
/// @nodoc
class _$SleepDetailDisplayCopyWithImpl<$Res>
    implements $SleepDetailDisplayCopyWith<$Res> {
  _$SleepDetailDisplayCopyWithImpl(this._self, this._then);

  final SleepDetailDisplay _self;
  final $Res Function(SleepDetailDisplay) _then;

/// Create a copy of SleepDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? session = null,Object? sortedStages = null,Object? stageTotals = null,}) {
  return _then(_self.copyWith(
session: null == session ? _self.session : session // ignore: cast_nullable_to_non_nullable
as SleepData,sortedStages: null == sortedStages ? _self.sortedStages : sortedStages // ignore: cast_nullable_to_non_nullable
as List<SleepStage>,stageTotals: null == stageTotals ? _self.stageTotals : stageTotals // ignore: cast_nullable_to_non_nullable
as List<SleepStageTotal>,
  ));
}
/// Create a copy of SleepDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res> get session {
  
  return $SleepDataCopyWith<$Res>(_self.session, (value) {
    return _then(_self.copyWith(session: value));
  });
}
}


/// Adds pattern-matching-related methods to [SleepDetailDisplay].
extension SleepDetailDisplayPatterns on SleepDetailDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SleepDetailDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SleepDetailDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SleepDetailDisplay value)  $default,){
final _that = this;
switch (_that) {
case _SleepDetailDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SleepDetailDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _SleepDetailDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( SleepData session,  List<SleepStage> sortedStages,  List<SleepStageTotal> stageTotals)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SleepDetailDisplay() when $default != null:
return $default(_that.session,_that.sortedStages,_that.stageTotals);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( SleepData session,  List<SleepStage> sortedStages,  List<SleepStageTotal> stageTotals)  $default,) {final _that = this;
switch (_that) {
case _SleepDetailDisplay():
return $default(_that.session,_that.sortedStages,_that.stageTotals);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( SleepData session,  List<SleepStage> sortedStages,  List<SleepStageTotal> stageTotals)?  $default,) {final _that = this;
switch (_that) {
case _SleepDetailDisplay() when $default != null:
return $default(_that.session,_that.sortedStages,_that.stageTotals);case _:
  return null;

}
}

}

/// @nodoc


class _SleepDetailDisplay extends SleepDetailDisplay {
  const _SleepDetailDisplay({required this.session, required final  List<SleepStage> sortedStages, required final  List<SleepStageTotal> stageTotals}): _sortedStages = sortedStages,_stageTotals = stageTotals,super._();
  

@override final  SleepData session;
/// The night's stages, earliest first.
 final  List<SleepStage> _sortedStages;
/// The night's stages, earliest first.
@override List<SleepStage> get sortedStages {
  if (_sortedStages is EqualUnmodifiableListView) return _sortedStages;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sortedStages);
}

/// Longest stage type first, as the breakdown card lists them.
 final  List<SleepStageTotal> _stageTotals;
/// Longest stage type first, as the breakdown card lists them.
@override List<SleepStageTotal> get stageTotals {
  if (_stageTotals is EqualUnmodifiableListView) return _stageTotals;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_stageTotals);
}


/// Create a copy of SleepDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SleepDetailDisplayCopyWith<_SleepDetailDisplay> get copyWith => __$SleepDetailDisplayCopyWithImpl<_SleepDetailDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SleepDetailDisplay&&(identical(other.session, session) || other.session == session)&&const DeepCollectionEquality().equals(other._sortedStages, _sortedStages)&&const DeepCollectionEquality().equals(other._stageTotals, _stageTotals));
}


@override
int get hashCode => Object.hash(runtimeType,session,const DeepCollectionEquality().hash(_sortedStages),const DeepCollectionEquality().hash(_stageTotals));

@override
String toString() {
  return 'SleepDetailDisplay(session: $session, sortedStages: $sortedStages, stageTotals: $stageTotals)';
}


}

/// @nodoc
abstract mixin class _$SleepDetailDisplayCopyWith<$Res> implements $SleepDetailDisplayCopyWith<$Res> {
  factory _$SleepDetailDisplayCopyWith(_SleepDetailDisplay value, $Res Function(_SleepDetailDisplay) _then) = __$SleepDetailDisplayCopyWithImpl;
@override @useResult
$Res call({
 SleepData session, List<SleepStage> sortedStages, List<SleepStageTotal> stageTotals
});


@override $SleepDataCopyWith<$Res> get session;

}
/// @nodoc
class __$SleepDetailDisplayCopyWithImpl<$Res>
    implements _$SleepDetailDisplayCopyWith<$Res> {
  __$SleepDetailDisplayCopyWithImpl(this._self, this._then);

  final _SleepDetailDisplay _self;
  final $Res Function(_SleepDetailDisplay) _then;

/// Create a copy of SleepDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? session = null,Object? sortedStages = null,Object? stageTotals = null,}) {
  return _then(_SleepDetailDisplay(
session: null == session ? _self.session : session // ignore: cast_nullable_to_non_nullable
as SleepData,sortedStages: null == sortedStages ? _self._sortedStages : sortedStages // ignore: cast_nullable_to_non_nullable
as List<SleepStage>,stageTotals: null == stageTotals ? _self._stageTotals : stageTotals // ignore: cast_nullable_to_non_nullable
as List<SleepStageTotal>,
  ));
}

/// Create a copy of SleepDetailDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res> get session {
  
  return $SleepDataCopyWith<$Res>(_self.session, (value) {
    return _then(_self.copyWith(session: value));
  });
}
}

// dart format on
