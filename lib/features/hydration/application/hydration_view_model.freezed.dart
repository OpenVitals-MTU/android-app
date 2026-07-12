// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'hydration_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$HydrationState {

 LocalDate get selectedDate; TimeRange get selectedRange; bool get isLoading; ScreenError? get error; double get dailyGoalLiters; List<DailyHydration> get dailyHydration; List<HydrationEntry> get entries; HydrationDisplay? get display;
/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$HydrationStateCopyWith<HydrationState> get copyWith => _$HydrationStateCopyWithImpl<HydrationState>(this as HydrationState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is HydrationState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.dailyGoalLiters, dailyGoalLiters) || other.dailyGoalLiters == dailyGoalLiters)&&const DeepCollectionEquality().equals(other.dailyHydration, dailyHydration)&&const DeepCollectionEquality().equals(other.entries, entries)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,dailyGoalLiters,const DeepCollectionEquality().hash(dailyHydration),const DeepCollectionEquality().hash(entries),display);

@override
String toString() {
  return 'HydrationState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, dailyGoalLiters: $dailyGoalLiters, dailyHydration: $dailyHydration, entries: $entries, display: $display)';
}


}

/// @nodoc
abstract mixin class $HydrationStateCopyWith<$Res>  {
  factory $HydrationStateCopyWith(HydrationState value, $Res Function(HydrationState) _then) = _$HydrationStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, double dailyGoalLiters, List<DailyHydration> dailyHydration, List<HydrationEntry> entries, HydrationDisplay? display
});


$HydrationDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$HydrationStateCopyWithImpl<$Res>
    implements $HydrationStateCopyWith<$Res> {
  _$HydrationStateCopyWithImpl(this._self, this._then);

  final HydrationState _self;
  final $Res Function(HydrationState) _then;

/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? dailyGoalLiters = null,Object? dailyHydration = null,Object? entries = null,Object? display = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,dailyGoalLiters: null == dailyGoalLiters ? _self.dailyGoalLiters : dailyGoalLiters // ignore: cast_nullable_to_non_nullable
as double,dailyHydration: null == dailyHydration ? _self.dailyHydration : dailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,entries: null == entries ? _self.entries : entries // ignore: cast_nullable_to_non_nullable
as List<HydrationEntry>,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as HydrationDisplay?,
  ));
}
/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HydrationDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $HydrationDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [HydrationState].
extension HydrationStatePatterns on HydrationState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _HydrationState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _HydrationState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _HydrationState value)  $default,){
final _that = this;
switch (_that) {
case _HydrationState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _HydrationState value)?  $default,){
final _that = this;
switch (_that) {
case _HydrationState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoalLiters,  List<DailyHydration> dailyHydration,  List<HydrationEntry> entries,  HydrationDisplay? display)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _HydrationState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoalLiters,_that.dailyHydration,_that.entries,_that.display);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoalLiters,  List<DailyHydration> dailyHydration,  List<HydrationEntry> entries,  HydrationDisplay? display)  $default,) {final _that = this;
switch (_that) {
case _HydrationState():
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoalLiters,_that.dailyHydration,_that.entries,_that.display);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoalLiters,  List<DailyHydration> dailyHydration,  List<HydrationEntry> entries,  HydrationDisplay? display)?  $default,) {final _that = this;
switch (_that) {
case _HydrationState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoalLiters,_that.dailyHydration,_that.entries,_that.display);case _:
  return null;

}
}

}

/// @nodoc


class _HydrationState extends HydrationState {
  const _HydrationState({required this.selectedDate, this.selectedRange = TimeRange.week, this.isLoading = true, this.error, this.dailyGoalLiters = 2.0, final  List<DailyHydration> dailyHydration = const <DailyHydration>[], final  List<HydrationEntry> entries = const <HydrationEntry>[], this.display}): _dailyHydration = dailyHydration,_entries = entries,super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override@JsonKey() final  double dailyGoalLiters;
 final  List<DailyHydration> _dailyHydration;
@override@JsonKey() List<DailyHydration> get dailyHydration {
  if (_dailyHydration is EqualUnmodifiableListView) return _dailyHydration;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyHydration);
}

 final  List<HydrationEntry> _entries;
@override@JsonKey() List<HydrationEntry> get entries {
  if (_entries is EqualUnmodifiableListView) return _entries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entries);
}

@override final  HydrationDisplay? display;

/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$HydrationStateCopyWith<_HydrationState> get copyWith => __$HydrationStateCopyWithImpl<_HydrationState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _HydrationState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.dailyGoalLiters, dailyGoalLiters) || other.dailyGoalLiters == dailyGoalLiters)&&const DeepCollectionEquality().equals(other._dailyHydration, _dailyHydration)&&const DeepCollectionEquality().equals(other._entries, _entries)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,dailyGoalLiters,const DeepCollectionEquality().hash(_dailyHydration),const DeepCollectionEquality().hash(_entries),display);

@override
String toString() {
  return 'HydrationState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, dailyGoalLiters: $dailyGoalLiters, dailyHydration: $dailyHydration, entries: $entries, display: $display)';
}


}

/// @nodoc
abstract mixin class _$HydrationStateCopyWith<$Res> implements $HydrationStateCopyWith<$Res> {
  factory _$HydrationStateCopyWith(_HydrationState value, $Res Function(_HydrationState) _then) = __$HydrationStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, double dailyGoalLiters, List<DailyHydration> dailyHydration, List<HydrationEntry> entries, HydrationDisplay? display
});


@override $HydrationDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$HydrationStateCopyWithImpl<$Res>
    implements _$HydrationStateCopyWith<$Res> {
  __$HydrationStateCopyWithImpl(this._self, this._then);

  final _HydrationState _self;
  final $Res Function(_HydrationState) _then;

/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? dailyGoalLiters = null,Object? dailyHydration = null,Object? entries = null,Object? display = freezed,}) {
  return _then(_HydrationState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,dailyGoalLiters: null == dailyGoalLiters ? _self.dailyGoalLiters : dailyGoalLiters // ignore: cast_nullable_to_non_nullable
as double,dailyHydration: null == dailyHydration ? _self._dailyHydration : dailyHydration // ignore: cast_nullable_to_non_nullable
as List<DailyHydration>,entries: null == entries ? _self._entries : entries // ignore: cast_nullable_to_non_nullable
as List<HydrationEntry>,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as HydrationDisplay?,
  ));
}

/// Create a copy of HydrationState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$HydrationDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $HydrationDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
