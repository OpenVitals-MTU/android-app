// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'manual_entry_widgets_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$ManualEntryWidgetsState {

 List<ManualEntryWidgetId> get visible; bool get editing;
/// Create a copy of ManualEntryWidgetsState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$ManualEntryWidgetsStateCopyWith<ManualEntryWidgetsState> get copyWith => _$ManualEntryWidgetsStateCopyWithImpl<ManualEntryWidgetsState>(this as ManualEntryWidgetsState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is ManualEntryWidgetsState&&const DeepCollectionEquality().equals(other.visible, visible)&&(identical(other.editing, editing) || other.editing == editing));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(visible),editing);

@override
String toString() {
  return 'ManualEntryWidgetsState(visible: $visible, editing: $editing)';
}


}

/// @nodoc
abstract mixin class $ManualEntryWidgetsStateCopyWith<$Res>  {
  factory $ManualEntryWidgetsStateCopyWith(ManualEntryWidgetsState value, $Res Function(ManualEntryWidgetsState) _then) = _$ManualEntryWidgetsStateCopyWithImpl;
@useResult
$Res call({
 List<ManualEntryWidgetId> visible, bool editing
});




}
/// @nodoc
class _$ManualEntryWidgetsStateCopyWithImpl<$Res>
    implements $ManualEntryWidgetsStateCopyWith<$Res> {
  _$ManualEntryWidgetsStateCopyWithImpl(this._self, this._then);

  final ManualEntryWidgetsState _self;
  final $Res Function(ManualEntryWidgetsState) _then;

/// Create a copy of ManualEntryWidgetsState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? visible = null,Object? editing = null,}) {
  return _then(_self.copyWith(
visible: null == visible ? _self.visible : visible // ignore: cast_nullable_to_non_nullable
as List<ManualEntryWidgetId>,editing: null == editing ? _self.editing : editing // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [ManualEntryWidgetsState].
extension ManualEntryWidgetsStatePatterns on ManualEntryWidgetsState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _ManualEntryWidgetsState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _ManualEntryWidgetsState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _ManualEntryWidgetsState value)  $default,){
final _that = this;
switch (_that) {
case _ManualEntryWidgetsState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _ManualEntryWidgetsState value)?  $default,){
final _that = this;
switch (_that) {
case _ManualEntryWidgetsState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<ManualEntryWidgetId> visible,  bool editing)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _ManualEntryWidgetsState() when $default != null:
return $default(_that.visible,_that.editing);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<ManualEntryWidgetId> visible,  bool editing)  $default,) {final _that = this;
switch (_that) {
case _ManualEntryWidgetsState():
return $default(_that.visible,_that.editing);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<ManualEntryWidgetId> visible,  bool editing)?  $default,) {final _that = this;
switch (_that) {
case _ManualEntryWidgetsState() when $default != null:
return $default(_that.visible,_that.editing);case _:
  return null;

}
}

}

/// @nodoc


class _ManualEntryWidgetsState implements ManualEntryWidgetsState {
  const _ManualEntryWidgetsState({required final  List<ManualEntryWidgetId> visible, this.editing = false}): _visible = visible;
  

 final  List<ManualEntryWidgetId> _visible;
@override List<ManualEntryWidgetId> get visible {
  if (_visible is EqualUnmodifiableListView) return _visible;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_visible);
}

@override@JsonKey() final  bool editing;

/// Create a copy of ManualEntryWidgetsState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$ManualEntryWidgetsStateCopyWith<_ManualEntryWidgetsState> get copyWith => __$ManualEntryWidgetsStateCopyWithImpl<_ManualEntryWidgetsState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _ManualEntryWidgetsState&&const DeepCollectionEquality().equals(other._visible, _visible)&&(identical(other.editing, editing) || other.editing == editing));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_visible),editing);

@override
String toString() {
  return 'ManualEntryWidgetsState(visible: $visible, editing: $editing)';
}


}

/// @nodoc
abstract mixin class _$ManualEntryWidgetsStateCopyWith<$Res> implements $ManualEntryWidgetsStateCopyWith<$Res> {
  factory _$ManualEntryWidgetsStateCopyWith(_ManualEntryWidgetsState value, $Res Function(_ManualEntryWidgetsState) _then) = __$ManualEntryWidgetsStateCopyWithImpl;
@override @useResult
$Res call({
 List<ManualEntryWidgetId> visible, bool editing
});




}
/// @nodoc
class __$ManualEntryWidgetsStateCopyWithImpl<$Res>
    implements _$ManualEntryWidgetsStateCopyWith<$Res> {
  __$ManualEntryWidgetsStateCopyWithImpl(this._self, this._then);

  final _ManualEntryWidgetsState _self;
  final $Res Function(_ManualEntryWidgetsState) _then;

/// Create a copy of ManualEntryWidgetsState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? visible = null,Object? editing = null,}) {
  return _then(_ManualEntryWidgetsState(
visible: null == visible ? _self._visible : visible // ignore: cast_nullable_to_non_nullable
as List<ManualEntryWidgetId>,editing: null == editing ? _self.editing : editing // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}

// dart format on
