// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'onboarding_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$OnboardingCategoryRow {

 OnboardingPermissionCategory get category; int get total; int get grantedCount; bool get fullyGranted; bool get partial;/// The still-missing permissions the runtime dialog CAN ask for.
 Set<String> get missingRequestable;/// The still-missing permissions it cannot (exercise routes, background and
/// history access): only a trip to the Health Connect page grants those.
 Set<String> get missingManual;/// A category whose remaining permissions are all manual-only — it shows
/// "Open settings" and an "Open" action instead of "Grant" (Kotlin
/// `isManualGrant`).
 bool get isManualGrant;
/// Create a copy of OnboardingCategoryRow
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$OnboardingCategoryRowCopyWith<OnboardingCategoryRow> get copyWith => _$OnboardingCategoryRowCopyWithImpl<OnboardingCategoryRow>(this as OnboardingCategoryRow, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is OnboardingCategoryRow&&(identical(other.category, category) || other.category == category)&&(identical(other.total, total) || other.total == total)&&(identical(other.grantedCount, grantedCount) || other.grantedCount == grantedCount)&&(identical(other.fullyGranted, fullyGranted) || other.fullyGranted == fullyGranted)&&(identical(other.partial, partial) || other.partial == partial)&&const DeepCollectionEquality().equals(other.missingRequestable, missingRequestable)&&const DeepCollectionEquality().equals(other.missingManual, missingManual)&&(identical(other.isManualGrant, isManualGrant) || other.isManualGrant == isManualGrant));
}


@override
int get hashCode => Object.hash(runtimeType,category,total,grantedCount,fullyGranted,partial,const DeepCollectionEquality().hash(missingRequestable),const DeepCollectionEquality().hash(missingManual),isManualGrant);

@override
String toString() {
  return 'OnboardingCategoryRow(category: $category, total: $total, grantedCount: $grantedCount, fullyGranted: $fullyGranted, partial: $partial, missingRequestable: $missingRequestable, missingManual: $missingManual, isManualGrant: $isManualGrant)';
}


}

/// @nodoc
abstract mixin class $OnboardingCategoryRowCopyWith<$Res>  {
  factory $OnboardingCategoryRowCopyWith(OnboardingCategoryRow value, $Res Function(OnboardingCategoryRow) _then) = _$OnboardingCategoryRowCopyWithImpl;
@useResult
$Res call({
 OnboardingPermissionCategory category, int total, int grantedCount, bool fullyGranted, bool partial, Set<String> missingRequestable, Set<String> missingManual, bool isManualGrant
});




}
/// @nodoc
class _$OnboardingCategoryRowCopyWithImpl<$Res>
    implements $OnboardingCategoryRowCopyWith<$Res> {
  _$OnboardingCategoryRowCopyWithImpl(this._self, this._then);

  final OnboardingCategoryRow _self;
  final $Res Function(OnboardingCategoryRow) _then;

/// Create a copy of OnboardingCategoryRow
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? category = null,Object? total = null,Object? grantedCount = null,Object? fullyGranted = null,Object? partial = null,Object? missingRequestable = null,Object? missingManual = null,Object? isManualGrant = null,}) {
  return _then(_self.copyWith(
category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as OnboardingPermissionCategory,total: null == total ? _self.total : total // ignore: cast_nullable_to_non_nullable
as int,grantedCount: null == grantedCount ? _self.grantedCount : grantedCount // ignore: cast_nullable_to_non_nullable
as int,fullyGranted: null == fullyGranted ? _self.fullyGranted : fullyGranted // ignore: cast_nullable_to_non_nullable
as bool,partial: null == partial ? _self.partial : partial // ignore: cast_nullable_to_non_nullable
as bool,missingRequestable: null == missingRequestable ? _self.missingRequestable : missingRequestable // ignore: cast_nullable_to_non_nullable
as Set<String>,missingManual: null == missingManual ? _self.missingManual : missingManual // ignore: cast_nullable_to_non_nullable
as Set<String>,isManualGrant: null == isManualGrant ? _self.isManualGrant : isManualGrant // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [OnboardingCategoryRow].
extension OnboardingCategoryRowPatterns on OnboardingCategoryRow {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _OnboardingCategoryRow value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _OnboardingCategoryRow() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _OnboardingCategoryRow value)  $default,){
final _that = this;
switch (_that) {
case _OnboardingCategoryRow():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _OnboardingCategoryRow value)?  $default,){
final _that = this;
switch (_that) {
case _OnboardingCategoryRow() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( OnboardingPermissionCategory category,  int total,  int grantedCount,  bool fullyGranted,  bool partial,  Set<String> missingRequestable,  Set<String> missingManual,  bool isManualGrant)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _OnboardingCategoryRow() when $default != null:
return $default(_that.category,_that.total,_that.grantedCount,_that.fullyGranted,_that.partial,_that.missingRequestable,_that.missingManual,_that.isManualGrant);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( OnboardingPermissionCategory category,  int total,  int grantedCount,  bool fullyGranted,  bool partial,  Set<String> missingRequestable,  Set<String> missingManual,  bool isManualGrant)  $default,) {final _that = this;
switch (_that) {
case _OnboardingCategoryRow():
return $default(_that.category,_that.total,_that.grantedCount,_that.fullyGranted,_that.partial,_that.missingRequestable,_that.missingManual,_that.isManualGrant);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( OnboardingPermissionCategory category,  int total,  int grantedCount,  bool fullyGranted,  bool partial,  Set<String> missingRequestable,  Set<String> missingManual,  bool isManualGrant)?  $default,) {final _that = this;
switch (_that) {
case _OnboardingCategoryRow() when $default != null:
return $default(_that.category,_that.total,_that.grantedCount,_that.fullyGranted,_that.partial,_that.missingRequestable,_that.missingManual,_that.isManualGrant);case _:
  return null;

}
}

}

/// @nodoc


class _OnboardingCategoryRow implements OnboardingCategoryRow {
  const _OnboardingCategoryRow({required this.category, required this.total, required this.grantedCount, required this.fullyGranted, required this.partial, required final  Set<String> missingRequestable, required final  Set<String> missingManual, required this.isManualGrant}): _missingRequestable = missingRequestable,_missingManual = missingManual;
  

@override final  OnboardingPermissionCategory category;
@override final  int total;
@override final  int grantedCount;
@override final  bool fullyGranted;
@override final  bool partial;
/// The still-missing permissions the runtime dialog CAN ask for.
 final  Set<String> _missingRequestable;
/// The still-missing permissions the runtime dialog CAN ask for.
@override Set<String> get missingRequestable {
  if (_missingRequestable is EqualUnmodifiableSetView) return _missingRequestable;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_missingRequestable);
}

/// The still-missing permissions it cannot (exercise routes, background and
/// history access): only a trip to the Health Connect page grants those.
 final  Set<String> _missingManual;
/// The still-missing permissions it cannot (exercise routes, background and
/// history access): only a trip to the Health Connect page grants those.
@override Set<String> get missingManual {
  if (_missingManual is EqualUnmodifiableSetView) return _missingManual;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_missingManual);
}

/// A category whose remaining permissions are all manual-only — it shows
/// "Open settings" and an "Open" action instead of "Grant" (Kotlin
/// `isManualGrant`).
@override final  bool isManualGrant;

/// Create a copy of OnboardingCategoryRow
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$OnboardingCategoryRowCopyWith<_OnboardingCategoryRow> get copyWith => __$OnboardingCategoryRowCopyWithImpl<_OnboardingCategoryRow>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _OnboardingCategoryRow&&(identical(other.category, category) || other.category == category)&&(identical(other.total, total) || other.total == total)&&(identical(other.grantedCount, grantedCount) || other.grantedCount == grantedCount)&&(identical(other.fullyGranted, fullyGranted) || other.fullyGranted == fullyGranted)&&(identical(other.partial, partial) || other.partial == partial)&&const DeepCollectionEquality().equals(other._missingRequestable, _missingRequestable)&&const DeepCollectionEquality().equals(other._missingManual, _missingManual)&&(identical(other.isManualGrant, isManualGrant) || other.isManualGrant == isManualGrant));
}


@override
int get hashCode => Object.hash(runtimeType,category,total,grantedCount,fullyGranted,partial,const DeepCollectionEquality().hash(_missingRequestable),const DeepCollectionEquality().hash(_missingManual),isManualGrant);

@override
String toString() {
  return 'OnboardingCategoryRow(category: $category, total: $total, grantedCount: $grantedCount, fullyGranted: $fullyGranted, partial: $partial, missingRequestable: $missingRequestable, missingManual: $missingManual, isManualGrant: $isManualGrant)';
}


}

/// @nodoc
abstract mixin class _$OnboardingCategoryRowCopyWith<$Res> implements $OnboardingCategoryRowCopyWith<$Res> {
  factory _$OnboardingCategoryRowCopyWith(_OnboardingCategoryRow value, $Res Function(_OnboardingCategoryRow) _then) = __$OnboardingCategoryRowCopyWithImpl;
@override @useResult
$Res call({
 OnboardingPermissionCategory category, int total, int grantedCount, bool fullyGranted, bool partial, Set<String> missingRequestable, Set<String> missingManual, bool isManualGrant
});




}
/// @nodoc
class __$OnboardingCategoryRowCopyWithImpl<$Res>
    implements _$OnboardingCategoryRowCopyWith<$Res> {
  __$OnboardingCategoryRowCopyWithImpl(this._self, this._then);

  final _OnboardingCategoryRow _self;
  final $Res Function(_OnboardingCategoryRow) _then;

/// Create a copy of OnboardingCategoryRow
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? category = null,Object? total = null,Object? grantedCount = null,Object? fullyGranted = null,Object? partial = null,Object? missingRequestable = null,Object? missingManual = null,Object? isManualGrant = null,}) {
  return _then(_OnboardingCategoryRow(
category: null == category ? _self.category : category // ignore: cast_nullable_to_non_nullable
as OnboardingPermissionCategory,total: null == total ? _self.total : total // ignore: cast_nullable_to_non_nullable
as int,grantedCount: null == grantedCount ? _self.grantedCount : grantedCount // ignore: cast_nullable_to_non_nullable
as int,fullyGranted: null == fullyGranted ? _self.fullyGranted : fullyGranted // ignore: cast_nullable_to_non_nullable
as bool,partial: null == partial ? _self.partial : partial // ignore: cast_nullable_to_non_nullable
as bool,missingRequestable: null == missingRequestable ? _self._missingRequestable : missingRequestable // ignore: cast_nullable_to_non_nullable
as Set<String>,missingManual: null == missingManual ? _self._missingManual : missingManual // ignore: cast_nullable_to_non_nullable
as Set<String>,isManualGrant: null == isManualGrant ? _self.isManualGrant : isManualGrant // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

/// @nodoc
mixin _$OnboardingDisplay {

 List<OnboardingCategoryRow> get rows;/// The required permissions that are still missing — what the primary
/// "Grant required Health Connect permissions" button requests.
 Set<String> get missingMinimum;/// Nothing required is outstanding: the primary action becomes "Continue".
 bool get minimumGranted;/// Everything else onboarding offers that is still missing — the optional
/// "Grant remaining" button's request.
 Set<String> get missingOptional;
/// Create a copy of OnboardingDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$OnboardingDisplayCopyWith<OnboardingDisplay> get copyWith => _$OnboardingDisplayCopyWithImpl<OnboardingDisplay>(this as OnboardingDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is OnboardingDisplay&&const DeepCollectionEquality().equals(other.rows, rows)&&const DeepCollectionEquality().equals(other.missingMinimum, missingMinimum)&&(identical(other.minimumGranted, minimumGranted) || other.minimumGranted == minimumGranted)&&const DeepCollectionEquality().equals(other.missingOptional, missingOptional));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(rows),const DeepCollectionEquality().hash(missingMinimum),minimumGranted,const DeepCollectionEquality().hash(missingOptional));

@override
String toString() {
  return 'OnboardingDisplay(rows: $rows, missingMinimum: $missingMinimum, minimumGranted: $minimumGranted, missingOptional: $missingOptional)';
}


}

/// @nodoc
abstract mixin class $OnboardingDisplayCopyWith<$Res>  {
  factory $OnboardingDisplayCopyWith(OnboardingDisplay value, $Res Function(OnboardingDisplay) _then) = _$OnboardingDisplayCopyWithImpl;
@useResult
$Res call({
 List<OnboardingCategoryRow> rows, Set<String> missingMinimum, bool minimumGranted, Set<String> missingOptional
});




}
/// @nodoc
class _$OnboardingDisplayCopyWithImpl<$Res>
    implements $OnboardingDisplayCopyWith<$Res> {
  _$OnboardingDisplayCopyWithImpl(this._self, this._then);

  final OnboardingDisplay _self;
  final $Res Function(OnboardingDisplay) _then;

/// Create a copy of OnboardingDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? rows = null,Object? missingMinimum = null,Object? minimumGranted = null,Object? missingOptional = null,}) {
  return _then(_self.copyWith(
rows: null == rows ? _self.rows : rows // ignore: cast_nullable_to_non_nullable
as List<OnboardingCategoryRow>,missingMinimum: null == missingMinimum ? _self.missingMinimum : missingMinimum // ignore: cast_nullable_to_non_nullable
as Set<String>,minimumGranted: null == minimumGranted ? _self.minimumGranted : minimumGranted // ignore: cast_nullable_to_non_nullable
as bool,missingOptional: null == missingOptional ? _self.missingOptional : missingOptional // ignore: cast_nullable_to_non_nullable
as Set<String>,
  ));
}

}


/// Adds pattern-matching-related methods to [OnboardingDisplay].
extension OnboardingDisplayPatterns on OnboardingDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _OnboardingDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _OnboardingDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _OnboardingDisplay value)  $default,){
final _that = this;
switch (_that) {
case _OnboardingDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _OnboardingDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _OnboardingDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<OnboardingCategoryRow> rows,  Set<String> missingMinimum,  bool minimumGranted,  Set<String> missingOptional)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _OnboardingDisplay() when $default != null:
return $default(_that.rows,_that.missingMinimum,_that.minimumGranted,_that.missingOptional);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<OnboardingCategoryRow> rows,  Set<String> missingMinimum,  bool minimumGranted,  Set<String> missingOptional)  $default,) {final _that = this;
switch (_that) {
case _OnboardingDisplay():
return $default(_that.rows,_that.missingMinimum,_that.minimumGranted,_that.missingOptional);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<OnboardingCategoryRow> rows,  Set<String> missingMinimum,  bool minimumGranted,  Set<String> missingOptional)?  $default,) {final _that = this;
switch (_that) {
case _OnboardingDisplay() when $default != null:
return $default(_that.rows,_that.missingMinimum,_that.minimumGranted,_that.missingOptional);case _:
  return null;

}
}

}

/// @nodoc


class _OnboardingDisplay implements OnboardingDisplay {
  const _OnboardingDisplay({required final  List<OnboardingCategoryRow> rows, required final  Set<String> missingMinimum, required this.minimumGranted, required final  Set<String> missingOptional}): _rows = rows,_missingMinimum = missingMinimum,_missingOptional = missingOptional;
  

 final  List<OnboardingCategoryRow> _rows;
@override List<OnboardingCategoryRow> get rows {
  if (_rows is EqualUnmodifiableListView) return _rows;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_rows);
}

/// The required permissions that are still missing — what the primary
/// "Grant required Health Connect permissions" button requests.
 final  Set<String> _missingMinimum;
/// The required permissions that are still missing — what the primary
/// "Grant required Health Connect permissions" button requests.
@override Set<String> get missingMinimum {
  if (_missingMinimum is EqualUnmodifiableSetView) return _missingMinimum;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_missingMinimum);
}

/// Nothing required is outstanding: the primary action becomes "Continue".
@override final  bool minimumGranted;
/// Everything else onboarding offers that is still missing — the optional
/// "Grant remaining" button's request.
 final  Set<String> _missingOptional;
/// Everything else onboarding offers that is still missing — the optional
/// "Grant remaining" button's request.
@override Set<String> get missingOptional {
  if (_missingOptional is EqualUnmodifiableSetView) return _missingOptional;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_missingOptional);
}


/// Create a copy of OnboardingDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$OnboardingDisplayCopyWith<_OnboardingDisplay> get copyWith => __$OnboardingDisplayCopyWithImpl<_OnboardingDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _OnboardingDisplay&&const DeepCollectionEquality().equals(other._rows, _rows)&&const DeepCollectionEquality().equals(other._missingMinimum, _missingMinimum)&&(identical(other.minimumGranted, minimumGranted) || other.minimumGranted == minimumGranted)&&const DeepCollectionEquality().equals(other._missingOptional, _missingOptional));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_rows),const DeepCollectionEquality().hash(_missingMinimum),minimumGranted,const DeepCollectionEquality().hash(_missingOptional));

@override
String toString() {
  return 'OnboardingDisplay(rows: $rows, missingMinimum: $missingMinimum, minimumGranted: $minimumGranted, missingOptional: $missingOptional)';
}


}

/// @nodoc
abstract mixin class _$OnboardingDisplayCopyWith<$Res> implements $OnboardingDisplayCopyWith<$Res> {
  factory _$OnboardingDisplayCopyWith(_OnboardingDisplay value, $Res Function(_OnboardingDisplay) _then) = __$OnboardingDisplayCopyWithImpl;
@override @useResult
$Res call({
 List<OnboardingCategoryRow> rows, Set<String> missingMinimum, bool minimumGranted, Set<String> missingOptional
});




}
/// @nodoc
class __$OnboardingDisplayCopyWithImpl<$Res>
    implements _$OnboardingDisplayCopyWith<$Res> {
  __$OnboardingDisplayCopyWithImpl(this._self, this._then);

  final _OnboardingDisplay _self;
  final $Res Function(_OnboardingDisplay) _then;

/// Create a copy of OnboardingDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? rows = null,Object? missingMinimum = null,Object? minimumGranted = null,Object? missingOptional = null,}) {
  return _then(_OnboardingDisplay(
rows: null == rows ? _self._rows : rows // ignore: cast_nullable_to_non_nullable
as List<OnboardingCategoryRow>,missingMinimum: null == missingMinimum ? _self._missingMinimum : missingMinimum // ignore: cast_nullable_to_non_nullable
as Set<String>,minimumGranted: null == minimumGranted ? _self.minimumGranted : minimumGranted // ignore: cast_nullable_to_non_nullable
as bool,missingOptional: null == missingOptional ? _self._missingOptional : missingOptional // ignore: cast_nullable_to_non_nullable
as Set<String>,
  ));
}


}

// dart format on
