// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'permission_categories_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$PermissionCategoriesState {

 List<PermissionCategory> get categories; HealthConnectAvailability? get availability; Set<String>? get granted; CommandState<void> get request;
/// Create a copy of PermissionCategoriesState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$PermissionCategoriesStateCopyWith<PermissionCategoriesState> get copyWith => _$PermissionCategoriesStateCopyWithImpl<PermissionCategoriesState>(this as PermissionCategoriesState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is PermissionCategoriesState&&const DeepCollectionEquality().equals(other.categories, categories)&&(identical(other.availability, availability) || other.availability == availability)&&const DeepCollectionEquality().equals(other.granted, granted)&&(identical(other.request, request) || other.request == request));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(categories),availability,const DeepCollectionEquality().hash(granted),request);

@override
String toString() {
  return 'PermissionCategoriesState(categories: $categories, availability: $availability, granted: $granted, request: $request)';
}


}

/// @nodoc
abstract mixin class $PermissionCategoriesStateCopyWith<$Res>  {
  factory $PermissionCategoriesStateCopyWith(PermissionCategoriesState value, $Res Function(PermissionCategoriesState) _then) = _$PermissionCategoriesStateCopyWithImpl;
@useResult
$Res call({
 List<PermissionCategory> categories, HealthConnectAvailability? availability, Set<String>? granted, CommandState<void> request
});


$CommandStateCopyWith<void, $Res> get request;

}
/// @nodoc
class _$PermissionCategoriesStateCopyWithImpl<$Res>
    implements $PermissionCategoriesStateCopyWith<$Res> {
  _$PermissionCategoriesStateCopyWithImpl(this._self, this._then);

  final PermissionCategoriesState _self;
  final $Res Function(PermissionCategoriesState) _then;

/// Create a copy of PermissionCategoriesState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? categories = null,Object? availability = freezed,Object? granted = freezed,Object? request = null,}) {
  return _then(_self.copyWith(
categories: null == categories ? _self.categories : categories // ignore: cast_nullable_to_non_nullable
as List<PermissionCategory>,availability: freezed == availability ? _self.availability : availability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability?,granted: freezed == granted ? _self.granted : granted // ignore: cast_nullable_to_non_nullable
as Set<String>?,request: null == request ? _self.request : request // ignore: cast_nullable_to_non_nullable
as CommandState<void>,
  ));
}
/// Create a copy of PermissionCategoriesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get request {
  
  return $CommandStateCopyWith<void, $Res>(_self.request, (value) {
    return _then(_self.copyWith(request: value));
  });
}
}


/// Adds pattern-matching-related methods to [PermissionCategoriesState].
extension PermissionCategoriesStatePatterns on PermissionCategoriesState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _PermissionCategoriesState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _PermissionCategoriesState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _PermissionCategoriesState value)  $default,){
final _that = this;
switch (_that) {
case _PermissionCategoriesState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _PermissionCategoriesState value)?  $default,){
final _that = this;
switch (_that) {
case _PermissionCategoriesState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<PermissionCategory> categories,  HealthConnectAvailability? availability,  Set<String>? granted,  CommandState<void> request)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _PermissionCategoriesState() when $default != null:
return $default(_that.categories,_that.availability,_that.granted,_that.request);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<PermissionCategory> categories,  HealthConnectAvailability? availability,  Set<String>? granted,  CommandState<void> request)  $default,) {final _that = this;
switch (_that) {
case _PermissionCategoriesState():
return $default(_that.categories,_that.availability,_that.granted,_that.request);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<PermissionCategory> categories,  HealthConnectAvailability? availability,  Set<String>? granted,  CommandState<void> request)?  $default,) {final _that = this;
switch (_that) {
case _PermissionCategoriesState() when $default != null:
return $default(_that.categories,_that.availability,_that.granted,_that.request);case _:
  return null;

}
}

}

/// @nodoc


class _PermissionCategoriesState implements PermissionCategoriesState {
  const _PermissionCategoriesState({final  List<PermissionCategory> categories = const <PermissionCategory>[], this.availability, final  Set<String>? granted, this.request = const CommandState<void>.idle()}): _categories = categories,_granted = granted;
  

 final  List<PermissionCategory> _categories;
@override@JsonKey() List<PermissionCategory> get categories {
  if (_categories is EqualUnmodifiableListView) return _categories;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_categories);
}

@override final  HealthConnectAvailability? availability;
 final  Set<String>? _granted;
@override Set<String>? get granted {
  final value = _granted;
  if (value == null) return null;
  if (_granted is EqualUnmodifiableSetView) return _granted;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(value);
}

@override@JsonKey() final  CommandState<void> request;

/// Create a copy of PermissionCategoriesState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$PermissionCategoriesStateCopyWith<_PermissionCategoriesState> get copyWith => __$PermissionCategoriesStateCopyWithImpl<_PermissionCategoriesState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _PermissionCategoriesState&&const DeepCollectionEquality().equals(other._categories, _categories)&&(identical(other.availability, availability) || other.availability == availability)&&const DeepCollectionEquality().equals(other._granted, _granted)&&(identical(other.request, request) || other.request == request));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_categories),availability,const DeepCollectionEquality().hash(_granted),request);

@override
String toString() {
  return 'PermissionCategoriesState(categories: $categories, availability: $availability, granted: $granted, request: $request)';
}


}

/// @nodoc
abstract mixin class _$PermissionCategoriesStateCopyWith<$Res> implements $PermissionCategoriesStateCopyWith<$Res> {
  factory _$PermissionCategoriesStateCopyWith(_PermissionCategoriesState value, $Res Function(_PermissionCategoriesState) _then) = __$PermissionCategoriesStateCopyWithImpl;
@override @useResult
$Res call({
 List<PermissionCategory> categories, HealthConnectAvailability? availability, Set<String>? granted, CommandState<void> request
});


@override $CommandStateCopyWith<void, $Res> get request;

}
/// @nodoc
class __$PermissionCategoriesStateCopyWithImpl<$Res>
    implements _$PermissionCategoriesStateCopyWith<$Res> {
  __$PermissionCategoriesStateCopyWithImpl(this._self, this._then);

  final _PermissionCategoriesState _self;
  final $Res Function(_PermissionCategoriesState) _then;

/// Create a copy of PermissionCategoriesState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? categories = null,Object? availability = freezed,Object? granted = freezed,Object? request = null,}) {
  return _then(_PermissionCategoriesState(
categories: null == categories ? _self._categories : categories // ignore: cast_nullable_to_non_nullable
as List<PermissionCategory>,availability: freezed == availability ? _self.availability : availability // ignore: cast_nullable_to_non_nullable
as HealthConnectAvailability?,granted: freezed == granted ? _self._granted : granted // ignore: cast_nullable_to_non_nullable
as Set<String>?,request: null == request ? _self.request : request // ignore: cast_nullable_to_non_nullable
as CommandState<void>,
  ));
}

/// Create a copy of PermissionCategoriesState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CommandStateCopyWith<void, $Res> get request {
  
  return $CommandStateCopyWith<void, $Res>(_self.request, (value) {
    return _then(_self.copyWith(request: value));
  });
}
}

// dart format on
