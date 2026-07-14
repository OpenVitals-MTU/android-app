// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'caffeine_view_model.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CaffeineState {

 bool get isLoading; CaffeineAnalyticsRange get analyticsRange; CaffeineInsights get homeDisplay; CaffeineInsights get analyticsDisplay;/// The drinks themselves, as Health Connect holds them.
///
/// The insights are a SUM of these, and a sum cannot be tapped. Keeping the entries
/// is what lets the screen list them and open one -- the difference between knowing
/// you had 240mg today and knowing which coffee is still keeping you awake.
 List<CaffeineEntry> get entries; CaffeineDisplay? get display; ScreenError? get error;
/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CaffeineStateCopyWith<CaffeineState> get copyWith => _$CaffeineStateCopyWithImpl<CaffeineState>(this as CaffeineState, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CaffeineState&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.analyticsRange, analyticsRange) || other.analyticsRange == analyticsRange)&&(identical(other.homeDisplay, homeDisplay) || other.homeDisplay == homeDisplay)&&(identical(other.analyticsDisplay, analyticsDisplay) || other.analyticsDisplay == analyticsDisplay)&&const DeepCollectionEquality().equals(other.entries, entries)&&(identical(other.display, display) || other.display == display)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,isLoading,analyticsRange,homeDisplay,analyticsDisplay,const DeepCollectionEquality().hash(entries),display,error);

@override
String toString() {
  return 'CaffeineState(isLoading: $isLoading, analyticsRange: $analyticsRange, homeDisplay: $homeDisplay, analyticsDisplay: $analyticsDisplay, entries: $entries, display: $display, error: $error)';
}


}

/// @nodoc
abstract mixin class $CaffeineStateCopyWith<$Res>  {
  factory $CaffeineStateCopyWith(CaffeineState value, $Res Function(CaffeineState) _then) = _$CaffeineStateCopyWithImpl;
@useResult
$Res call({
 bool isLoading, CaffeineAnalyticsRange analyticsRange, CaffeineInsights homeDisplay, CaffeineInsights analyticsDisplay, List<CaffeineEntry> entries, CaffeineDisplay? display, ScreenError? error
});


$CaffeineInsightsCopyWith<$Res> get homeDisplay;$CaffeineInsightsCopyWith<$Res> get analyticsDisplay;$CaffeineDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class _$CaffeineStateCopyWithImpl<$Res>
    implements $CaffeineStateCopyWith<$Res> {
  _$CaffeineStateCopyWithImpl(this._self, this._then);

  final CaffeineState _self;
  final $Res Function(CaffeineState) _then;

/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? isLoading = null,Object? analyticsRange = null,Object? homeDisplay = null,Object? analyticsDisplay = null,Object? entries = null,Object? display = freezed,Object? error = freezed,}) {
  return _then(_self.copyWith(
isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,analyticsRange: null == analyticsRange ? _self.analyticsRange : analyticsRange // ignore: cast_nullable_to_non_nullable
as CaffeineAnalyticsRange,homeDisplay: null == homeDisplay ? _self.homeDisplay : homeDisplay // ignore: cast_nullable_to_non_nullable
as CaffeineInsights,analyticsDisplay: null == analyticsDisplay ? _self.analyticsDisplay : analyticsDisplay // ignore: cast_nullable_to_non_nullable
as CaffeineInsights,entries: null == entries ? _self.entries : entries // ignore: cast_nullable_to_non_nullable
as List<CaffeineEntry>,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as CaffeineDisplay?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}
/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineInsightsCopyWith<$Res> get homeDisplay {
  
  return $CaffeineInsightsCopyWith<$Res>(_self.homeDisplay, (value) {
    return _then(_self.copyWith(homeDisplay: value));
  });
}/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineInsightsCopyWith<$Res> get analyticsDisplay {
  
  return $CaffeineInsightsCopyWith<$Res>(_self.analyticsDisplay, (value) {
    return _then(_self.copyWith(analyticsDisplay: value));
  });
}/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $CaffeineDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}


/// Adds pattern-matching-related methods to [CaffeineState].
extension CaffeineStatePatterns on CaffeineState {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CaffeineState value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CaffeineState() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CaffeineState value)  $default,){
final _that = this;
switch (_that) {
case _CaffeineState():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CaffeineState value)?  $default,){
final _that = this;
switch (_that) {
case _CaffeineState() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool isLoading,  CaffeineAnalyticsRange analyticsRange,  CaffeineInsights homeDisplay,  CaffeineInsights analyticsDisplay,  List<CaffeineEntry> entries,  CaffeineDisplay? display,  ScreenError? error)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CaffeineState() when $default != null:
return $default(_that.isLoading,_that.analyticsRange,_that.homeDisplay,_that.analyticsDisplay,_that.entries,_that.display,_that.error);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool isLoading,  CaffeineAnalyticsRange analyticsRange,  CaffeineInsights homeDisplay,  CaffeineInsights analyticsDisplay,  List<CaffeineEntry> entries,  CaffeineDisplay? display,  ScreenError? error)  $default,) {final _that = this;
switch (_that) {
case _CaffeineState():
return $default(_that.isLoading,_that.analyticsRange,_that.homeDisplay,_that.analyticsDisplay,_that.entries,_that.display,_that.error);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool isLoading,  CaffeineAnalyticsRange analyticsRange,  CaffeineInsights homeDisplay,  CaffeineInsights analyticsDisplay,  List<CaffeineEntry> entries,  CaffeineDisplay? display,  ScreenError? error)?  $default,) {final _that = this;
switch (_that) {
case _CaffeineState() when $default != null:
return $default(_that.isLoading,_that.analyticsRange,_that.homeDisplay,_that.analyticsDisplay,_that.entries,_that.display,_that.error);case _:
  return null;

}
}

}

/// @nodoc


class _CaffeineState implements CaffeineState {
  const _CaffeineState({this.isLoading = true, this.analyticsRange = CaffeineAnalyticsRange.last30Days, this.homeDisplay = const CaffeineInsights(), this.analyticsDisplay = const CaffeineInsights(), final  List<CaffeineEntry> entries = const <CaffeineEntry>[], this.display, this.error}): _entries = entries;
  

@override@JsonKey() final  bool isLoading;
@override@JsonKey() final  CaffeineAnalyticsRange analyticsRange;
@override@JsonKey() final  CaffeineInsights homeDisplay;
@override@JsonKey() final  CaffeineInsights analyticsDisplay;
/// The drinks themselves, as Health Connect holds them.
///
/// The insights are a SUM of these, and a sum cannot be tapped. Keeping the entries
/// is what lets the screen list them and open one -- the difference between knowing
/// you had 240mg today and knowing which coffee is still keeping you awake.
 final  List<CaffeineEntry> _entries;
/// The drinks themselves, as Health Connect holds them.
///
/// The insights are a SUM of these, and a sum cannot be tapped. Keeping the entries
/// is what lets the screen list them and open one -- the difference between knowing
/// you had 240mg today and knowing which coffee is still keeping you awake.
@override@JsonKey() List<CaffeineEntry> get entries {
  if (_entries is EqualUnmodifiableListView) return _entries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entries);
}

@override final  CaffeineDisplay? display;
@override final  ScreenError? error;

/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CaffeineStateCopyWith<_CaffeineState> get copyWith => __$CaffeineStateCopyWithImpl<_CaffeineState>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CaffeineState&&(identical(other.isLoading, isLoading) || other.isLoading == isLoading)&&(identical(other.analyticsRange, analyticsRange) || other.analyticsRange == analyticsRange)&&(identical(other.homeDisplay, homeDisplay) || other.homeDisplay == homeDisplay)&&(identical(other.analyticsDisplay, analyticsDisplay) || other.analyticsDisplay == analyticsDisplay)&&const DeepCollectionEquality().equals(other._entries, _entries)&&(identical(other.display, display) || other.display == display)&&(identical(other.error, error) || other.error == error));
}


@override
int get hashCode => Object.hash(runtimeType,isLoading,analyticsRange,homeDisplay,analyticsDisplay,const DeepCollectionEquality().hash(_entries),display,error);

@override
String toString() {
  return 'CaffeineState(isLoading: $isLoading, analyticsRange: $analyticsRange, homeDisplay: $homeDisplay, analyticsDisplay: $analyticsDisplay, entries: $entries, display: $display, error: $error)';
}


}

/// @nodoc
abstract mixin class _$CaffeineStateCopyWith<$Res> implements $CaffeineStateCopyWith<$Res> {
  factory _$CaffeineStateCopyWith(_CaffeineState value, $Res Function(_CaffeineState) _then) = __$CaffeineStateCopyWithImpl;
@override @useResult
$Res call({
 bool isLoading, CaffeineAnalyticsRange analyticsRange, CaffeineInsights homeDisplay, CaffeineInsights analyticsDisplay, List<CaffeineEntry> entries, CaffeineDisplay? display, ScreenError? error
});


@override $CaffeineInsightsCopyWith<$Res> get homeDisplay;@override $CaffeineInsightsCopyWith<$Res> get analyticsDisplay;@override $CaffeineDisplayCopyWith<$Res>? get display;

}
/// @nodoc
class __$CaffeineStateCopyWithImpl<$Res>
    implements _$CaffeineStateCopyWith<$Res> {
  __$CaffeineStateCopyWithImpl(this._self, this._then);

  final _CaffeineState _self;
  final $Res Function(_CaffeineState) _then;

/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? isLoading = null,Object? analyticsRange = null,Object? homeDisplay = null,Object? analyticsDisplay = null,Object? entries = null,Object? display = freezed,Object? error = freezed,}) {
  return _then(_CaffeineState(
isLoading: null == isLoading ? _self.isLoading : isLoading // ignore: cast_nullable_to_non_nullable
as bool,analyticsRange: null == analyticsRange ? _self.analyticsRange : analyticsRange // ignore: cast_nullable_to_non_nullable
as CaffeineAnalyticsRange,homeDisplay: null == homeDisplay ? _self.homeDisplay : homeDisplay // ignore: cast_nullable_to_non_nullable
as CaffeineInsights,analyticsDisplay: null == analyticsDisplay ? _self.analyticsDisplay : analyticsDisplay // ignore: cast_nullable_to_non_nullable
as CaffeineInsights,entries: null == entries ? _self._entries : entries // ignore: cast_nullable_to_non_nullable
as List<CaffeineEntry>,display: freezed == display ? _self.display : display // ignore: cast_nullable_to_non_nullable
as CaffeineDisplay?,error: freezed == error ? _self.error : error // ignore: cast_nullable_to_non_nullable
as ScreenError?,
  ));
}

/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineInsightsCopyWith<$Res> get homeDisplay {
  
  return $CaffeineInsightsCopyWith<$Res>(_self.homeDisplay, (value) {
    return _then(_self.copyWith(homeDisplay: value));
  });
}/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineInsightsCopyWith<$Res> get analyticsDisplay {
  
  return $CaffeineInsightsCopyWith<$Res>(_self.analyticsDisplay, (value) {
    return _then(_self.copyWith(analyticsDisplay: value));
  });
}/// Create a copy of CaffeineState
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$CaffeineDisplayCopyWith<$Res>? get display {
    if (_self.display == null) {
    return null;
  }

  return $CaffeineDisplayCopyWith<$Res>(_self.display!, (value) {
    return _then(_self.copyWith(display: value));
  });
}
}

// dart format on
