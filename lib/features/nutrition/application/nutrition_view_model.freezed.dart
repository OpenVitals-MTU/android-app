// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'nutrition_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$NutritionState {

 LocalDate get selectedDate; TimeRange get selectedRange; bool get isLoading; ScreenError? get error; double get dailyGoal; List<DailyMacros> get dailyMacros; List<DailyMacros> get previousDailyMacros; List<DailyMacros> get baselineDailyMacros; List<NutritionEntry> get entries; NutritionDisplay? get display;
/// Create a copy of NutritionState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$NutritionStateCopyWith<NutritionState> get copyWith => _$NutritionStateCopyWithImpl<NutritionState>(this as NutritionState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is NutritionState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.dailyGoal, dailyGoal) || other.dailyGoal == dailyGoal)&&const DeepCollectionEquality().equals(other.dailyMacros, dailyMacros)&&const DeepCollectionEquality().equals(other.previousDailyMacros, previousDailyMacros)&&const DeepCollectionEquality().equals(other.baselineDailyMacros, baselineDailyMacros)&&const DeepCollectionEquality().equals(other.entries, entries)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,dailyGoal,const DeepCollectionEquality().hash(dailyMacros),const DeepCollectionEquality().hash(previousDailyMacros),const DeepCollectionEquality().hash(baselineDailyMacros),const DeepCollectionEquality().hash(entries),display);

@override
String toString() {
  return 'NutritionState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, dailyGoal: $dailyGoal, dailyMacros: $dailyMacros, previousDailyMacros: $previousDailyMacros, baselineDailyMacros: $baselineDailyMacros, entries: $entries, display: $display)';
}


}

/// @nodoc
abstract mixin class $NutritionStateCopyWith<$Res>  {
  factory $NutritionStateCopyWith(NutritionState value, $Res Function(NutritionState) _then) = _$NutritionStateCopyWithImpl;
@useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, double dailyGoal, List<DailyMacros> dailyMacros, List<DailyMacros> previousDailyMacros, List<DailyMacros> baselineDailyMacros, List<NutritionEntry> entries, NutritionDisplay? display
});


$NutritionDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$NutritionStateCopyWithImpl<$Res>
    implements $NutritionStateCopyWith<$Res> {
  _$NutritionStateCopyWithImpl(this._self, this._then);

  final NutritionState _self;
  final $Res Function(NutritionState) _then;

/// Create a copy of NutritionState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? dailyGoal = null,Object? dailyMacros = null,Object? previousDailyMacros = null,Object? baselineDailyMacros = null,Object? entries = null,Object? display = freezed,}) {
  return _then(_self.copyWith(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,dailyGoal: null == dailyGoal ? _self.dailyGoal : dailyGoal // ignore: cast_nullable_to_non_nullable
as double,dailyMacros: null == dailyMacros ? _self.dailyMacros : dailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,previousDailyMacros: null == previousDailyMacros ? _self.previousDailyMacros : previousDailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,baselineDailyMacros: null == baselineDailyMacros ? _self.baselineDailyMacros : baselineDailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,entries: null == entries ? _self.entries : entries // ignore: cast_nullable_to_non_nullable
as List<NutritionEntry>,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as NutritionDisplay?,
  ));
}
/// Create a copy of NutritionState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$NutritionDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $NutritionDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [NutritionState].
extension NutritionStatePatterns on NutritionState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _NutritionState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _NutritionState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _NutritionState value)  $default,){
final _that = this;
switch (_that) {
case _NutritionState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _NutritionState value)?  $default,){
final _that = this;
switch (_that) {
case _NutritionState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoal,  List<DailyMacros> dailyMacros,  List<DailyMacros> previousDailyMacros,  List<DailyMacros> baselineDailyMacros,  List<NutritionEntry> entries,  NutritionDisplay? display)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _NutritionState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoal,_that.dailyMacros,_that.previousDailyMacros,_that.baselineDailyMacros,_that.entries,_that.display);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoal,  List<DailyMacros> dailyMacros,  List<DailyMacros> previousDailyMacros,  List<DailyMacros> baselineDailyMacros,  List<NutritionEntry> entries,  NutritionDisplay? display)  $default,) {final _that = this;
switch (_that) {
case _NutritionState():
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoal,_that.dailyMacros,_that.previousDailyMacros,_that.baselineDailyMacros,_that.entries,_that.display);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate selectedDate,  TimeRange selectedRange,  bool isLoading,  ScreenError? error,  double dailyGoal,  List<DailyMacros> dailyMacros,  List<DailyMacros> previousDailyMacros,  List<DailyMacros> baselineDailyMacros,  List<NutritionEntry> entries,  NutritionDisplay? display)?  $default,) {final _that = this;
switch (_that) {
case _NutritionState() when $default != null:
return $default(_that.selectedDate,_that.selectedRange,_that.isLoading,_that.error,_that.dailyGoal,_that.dailyMacros,_that.previousDailyMacros,_that.baselineDailyMacros,_that.entries,_that.display);case _:
  return null;

}
}

}

/// @nodoc


class _NutritionState extends NutritionState {
  const _NutritionState({required this.selectedDate, this.selectedRange = TimeRange.week, this.isLoading = true, this.error, this.dailyGoal = 2000.0, final  List<DailyMacros> dailyMacros = const <DailyMacros>[], final  List<DailyMacros> previousDailyMacros = const <DailyMacros>[], final  List<DailyMacros> baselineDailyMacros = const <DailyMacros>[], final  List<NutritionEntry> entries = const <NutritionEntry>[], this.display}): _dailyMacros = dailyMacros,_previousDailyMacros = previousDailyMacros,_baselineDailyMacros = baselineDailyMacros,_entries = entries,super._();
  

@override final  LocalDate selectedDate;
@override@JsonKey() final  TimeRange selectedRange;
@override@JsonKey() final  bool isLoading;
@override final  ScreenError? error;
@override@JsonKey() final  double dailyGoal;
 final  List<DailyMacros> _dailyMacros;
@override@JsonKey() List<DailyMacros> get dailyMacros {
  if (_dailyMacros is EqualUnmodifiableListView) return _dailyMacros;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_dailyMacros);
}

 final  List<DailyMacros> _previousDailyMacros;
@override@JsonKey() List<DailyMacros> get previousDailyMacros {
  if (_previousDailyMacros is EqualUnmodifiableListView) return _previousDailyMacros;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_previousDailyMacros);
}

 final  List<DailyMacros> _baselineDailyMacros;
@override@JsonKey() List<DailyMacros> get baselineDailyMacros {
  if (_baselineDailyMacros is EqualUnmodifiableListView) return _baselineDailyMacros;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_baselineDailyMacros);
}

 final  List<NutritionEntry> _entries;
@override@JsonKey() List<NutritionEntry> get entries {
  if (_entries is EqualUnmodifiableListView) return _entries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entries);
}

@override final  NutritionDisplay? display;

/// Create a copy of NutritionState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$NutritionStateCopyWith<_NutritionState> get copyWith => __$NutritionStateCopyWithImpl<_NutritionState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _NutritionState&&(identical(other.selectedDate, selectedDate) || other.selectedDate == selectedDate)&&(identical(other.selectedRange, selectedRange) || other.selectedRange == selectedRange)&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.error, error) || other.error == error)&&(identical(other.dailyGoal, dailyGoal) || other.dailyGoal == dailyGoal)&&const DeepCollectionEquality().equals(other._dailyMacros, _dailyMacros)&&const DeepCollectionEquality().equals(other._previousDailyMacros, _previousDailyMacros)&&const DeepCollectionEquality().equals(other._baselineDailyMacros, _baselineDailyMacros)&&const DeepCollectionEquality().equals(other._entries, _entries)&&(identical(other.display, display) || other.display == display));
}


@override
int get hashCode => Object.hash(runtimeType,selectedDate,selectedRange,isLoading,error,dailyGoal,const DeepCollectionEquality().hash(_dailyMacros),const DeepCollectionEquality().hash(_previousDailyMacros),const DeepCollectionEquality().hash(_baselineDailyMacros),const DeepCollectionEquality().hash(_entries),display);

@override
String toString() {
  return 'NutritionState(selectedDate: $selectedDate, selectedRange: $selectedRange, isLoading: $isLoading, error: $error, dailyGoal: $dailyGoal, dailyMacros: $dailyMacros, previousDailyMacros: $previousDailyMacros, baselineDailyMacros: $baselineDailyMacros, entries: $entries, display: $display)';
}


}

/// @nodoc
abstract mixin class _$NutritionStateCopyWith<$Res> implements $NutritionStateCopyWith<$Res> {
  factory _$NutritionStateCopyWith(_NutritionState value, $Res Function(_NutritionState) _then) = __$NutritionStateCopyWithImpl;
@override @useResult
$Res call({
 LocalDate selectedDate, TimeRange selectedRange, bool isLoading, ScreenError? error, double dailyGoal, List<DailyMacros> dailyMacros, List<DailyMacros> previousDailyMacros, List<DailyMacros> baselineDailyMacros, List<NutritionEntry> entries, NutritionDisplay? display
});


@override $NutritionDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$NutritionStateCopyWithImpl<$Res>
    implements _$NutritionStateCopyWith<$Res> {
  __$NutritionStateCopyWithImpl(this._self, this._then);

  final _NutritionState _self;
  final $Res Function(_NutritionState) _then;

/// Create a copy of NutritionState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? selectedDate = null,Object? selectedRange = null,Object? isLoading = null,Object? error = freezed,Object? dailyGoal = null,Object? dailyMacros = null,Object? previousDailyMacros = null,Object? baselineDailyMacros = null,Object? entries = null,Object? display = freezed,}) {
  return _then(_NutritionState(
selectedDate: null == selectedDate ? _self.selectedDate : selectedDate // ignore: cast_nullable_to_non_nullable
as LocalDate,selectedRange: null == selectedRange ? _self.selectedRange : selectedRange // ignore: cast_nullable_to_non_nullable
as TimeRange,isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,dailyGoal: null == dailyGoal ? _self.dailyGoal : dailyGoal // ignore: cast_nullable_to_non_nullable
as double,dailyMacros: null == dailyMacros ? _self._dailyMacros : dailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,previousDailyMacros: null == previousDailyMacros ? _self._previousDailyMacros : previousDailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,baselineDailyMacros: null == baselineDailyMacros ? _self._baselineDailyMacros : baselineDailyMacros // ignore: cast_nullable_to_non_nullable
as List<DailyMacros>,entries: null == entries ? _self._entries : entries // ignore: cast_nullable_to_non_nullable
as List<NutritionEntry>,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as NutritionDisplay?,
  ));
}

/// Create a copy of NutritionState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$NutritionDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $NutritionDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
