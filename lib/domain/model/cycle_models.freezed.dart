// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'cycle_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$CycleData {

 List<MenstruationFlowEntry> get menstruationFlows; List<MenstruationPeriodEntry> get menstruationPeriods; List<OvulationTestEntry> get ovulationTests; List<CervicalMucusEntry> get cervicalMucus; List<BasalBodyTemperatureEntry> get basalBodyTemperature; List<IntermenstrualBleedingEntry> get intermenstrualBleeding; List<SexualActivityEntry> get sexualActivity;
/// Create a copy of CycleData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CycleDataCopyWith<CycleData> get copyWith => _$CycleDataCopyWithImpl<CycleData>(this as CycleData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CycleData&&const DeepCollectionEquality().equals(other.menstruationFlows, menstruationFlows)&&const DeepCollectionEquality().equals(other.menstruationPeriods, menstruationPeriods)&&const DeepCollectionEquality().equals(other.ovulationTests, ovulationTests)&&const DeepCollectionEquality().equals(other.cervicalMucus, cervicalMucus)&&const DeepCollectionEquality().equals(other.basalBodyTemperature, basalBodyTemperature)&&const DeepCollectionEquality().equals(other.intermenstrualBleeding, intermenstrualBleeding)&&const DeepCollectionEquality().equals(other.sexualActivity, sexualActivity));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(menstruationFlows),const DeepCollectionEquality().hash(menstruationPeriods),const DeepCollectionEquality().hash(ovulationTests),const DeepCollectionEquality().hash(cervicalMucus),const DeepCollectionEquality().hash(basalBodyTemperature),const DeepCollectionEquality().hash(intermenstrualBleeding),const DeepCollectionEquality().hash(sexualActivity));

@override
String toString() {
  return 'CycleData(menstruationFlows: $menstruationFlows, menstruationPeriods: $menstruationPeriods, ovulationTests: $ovulationTests, cervicalMucus: $cervicalMucus, basalBodyTemperature: $basalBodyTemperature, intermenstrualBleeding: $intermenstrualBleeding, sexualActivity: $sexualActivity)';
}


}

/// @nodoc
abstract mixin class $CycleDataCopyWith<$Res>  {
  factory $CycleDataCopyWith(CycleData value, $Res Function(CycleData) _then) = _$CycleDataCopyWithImpl;
@useResult
$Res call({
 List<MenstruationFlowEntry> menstruationFlows, List<MenstruationPeriodEntry> menstruationPeriods, List<OvulationTestEntry> ovulationTests, List<CervicalMucusEntry> cervicalMucus, List<BasalBodyTemperatureEntry> basalBodyTemperature, List<IntermenstrualBleedingEntry> intermenstrualBleeding, List<SexualActivityEntry> sexualActivity
});




}
/// @nodoc
class _$CycleDataCopyWithImpl<$Res>
    implements $CycleDataCopyWith<$Res> {
  _$CycleDataCopyWithImpl(this._self, this._then);

  final CycleData _self;
  final $Res Function(CycleData) _then;

/// Create a copy of CycleData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? menstruationFlows = null,Object? menstruationPeriods = null,Object? ovulationTests = null,Object? cervicalMucus = null,Object? basalBodyTemperature = null,Object? intermenstrualBleeding = null,Object? sexualActivity = null,}) {
  return _then(_self.copyWith(
menstruationFlows: null == menstruationFlows ? _self.menstruationFlows : menstruationFlows // ignore: cast_nullable_to_non_nullable
as List<MenstruationFlowEntry>,menstruationPeriods: null == menstruationPeriods ? _self.menstruationPeriods : menstruationPeriods // ignore: cast_nullable_to_non_nullable
as List<MenstruationPeriodEntry>,ovulationTests: null == ovulationTests ? _self.ovulationTests : ovulationTests // ignore: cast_nullable_to_non_nullable
as List<OvulationTestEntry>,cervicalMucus: null == cervicalMucus ? _self.cervicalMucus : cervicalMucus // ignore: cast_nullable_to_non_nullable
as List<CervicalMucusEntry>,basalBodyTemperature: null == basalBodyTemperature ? _self.basalBodyTemperature : basalBodyTemperature // ignore: cast_nullable_to_non_nullable
as List<BasalBodyTemperatureEntry>,intermenstrualBleeding: null == intermenstrualBleeding ? _self.intermenstrualBleeding : intermenstrualBleeding // ignore: cast_nullable_to_non_nullable
as List<IntermenstrualBleedingEntry>,sexualActivity: null == sexualActivity ? _self.sexualActivity : sexualActivity // ignore: cast_nullable_to_non_nullable
as List<SexualActivityEntry>,
  ));
}

}


/// Adds pattern-matching-related methods to [CycleData].
extension CycleDataPatterns on CycleData {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CycleData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CycleData() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CycleData value)  $default,){
final _that = this;
switch (_that) {
case _CycleData():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CycleData value)?  $default,){
final _that = this;
switch (_that) {
case _CycleData() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<MenstruationFlowEntry> menstruationFlows,  List<MenstruationPeriodEntry> menstruationPeriods,  List<OvulationTestEntry> ovulationTests,  List<CervicalMucusEntry> cervicalMucus,  List<BasalBodyTemperatureEntry> basalBodyTemperature,  List<IntermenstrualBleedingEntry> intermenstrualBleeding,  List<SexualActivityEntry> sexualActivity)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CycleData() when $default != null:
return $default(_that.menstruationFlows,_that.menstruationPeriods,_that.ovulationTests,_that.cervicalMucus,_that.basalBodyTemperature,_that.intermenstrualBleeding,_that.sexualActivity);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<MenstruationFlowEntry> menstruationFlows,  List<MenstruationPeriodEntry> menstruationPeriods,  List<OvulationTestEntry> ovulationTests,  List<CervicalMucusEntry> cervicalMucus,  List<BasalBodyTemperatureEntry> basalBodyTemperature,  List<IntermenstrualBleedingEntry> intermenstrualBleeding,  List<SexualActivityEntry> sexualActivity)  $default,) {final _that = this;
switch (_that) {
case _CycleData():
return $default(_that.menstruationFlows,_that.menstruationPeriods,_that.ovulationTests,_that.cervicalMucus,_that.basalBodyTemperature,_that.intermenstrualBleeding,_that.sexualActivity);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<MenstruationFlowEntry> menstruationFlows,  List<MenstruationPeriodEntry> menstruationPeriods,  List<OvulationTestEntry> ovulationTests,  List<CervicalMucusEntry> cervicalMucus,  List<BasalBodyTemperatureEntry> basalBodyTemperature,  List<IntermenstrualBleedingEntry> intermenstrualBleeding,  List<SexualActivityEntry> sexualActivity)?  $default,) {final _that = this;
switch (_that) {
case _CycleData() when $default != null:
return $default(_that.menstruationFlows,_that.menstruationPeriods,_that.ovulationTests,_that.cervicalMucus,_that.basalBodyTemperature,_that.intermenstrualBleeding,_that.sexualActivity);case _:
  return null;

}
}

}

/// @nodoc


class _CycleData extends CycleData {
  const _CycleData({final  List<MenstruationFlowEntry> menstruationFlows = const <MenstruationFlowEntry>[], final  List<MenstruationPeriodEntry> menstruationPeriods = const <MenstruationPeriodEntry>[], final  List<OvulationTestEntry> ovulationTests = const <OvulationTestEntry>[], final  List<CervicalMucusEntry> cervicalMucus = const <CervicalMucusEntry>[], final  List<BasalBodyTemperatureEntry> basalBodyTemperature = const <BasalBodyTemperatureEntry>[], final  List<IntermenstrualBleedingEntry> intermenstrualBleeding = const <IntermenstrualBleedingEntry>[], final  List<SexualActivityEntry> sexualActivity = const <SexualActivityEntry>[]}): _menstruationFlows = menstruationFlows,_menstruationPeriods = menstruationPeriods,_ovulationTests = ovulationTests,_cervicalMucus = cervicalMucus,_basalBodyTemperature = basalBodyTemperature,_intermenstrualBleeding = intermenstrualBleeding,_sexualActivity = sexualActivity,super._();
  

 final  List<MenstruationFlowEntry> _menstruationFlows;
@override@JsonKey() List<MenstruationFlowEntry> get menstruationFlows {
  if (_menstruationFlows is EqualUnmodifiableListView) return _menstruationFlows;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_menstruationFlows);
}

 final  List<MenstruationPeriodEntry> _menstruationPeriods;
@override@JsonKey() List<MenstruationPeriodEntry> get menstruationPeriods {
  if (_menstruationPeriods is EqualUnmodifiableListView) return _menstruationPeriods;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_menstruationPeriods);
}

 final  List<OvulationTestEntry> _ovulationTests;
@override@JsonKey() List<OvulationTestEntry> get ovulationTests {
  if (_ovulationTests is EqualUnmodifiableListView) return _ovulationTests;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_ovulationTests);
}

 final  List<CervicalMucusEntry> _cervicalMucus;
@override@JsonKey() List<CervicalMucusEntry> get cervicalMucus {
  if (_cervicalMucus is EqualUnmodifiableListView) return _cervicalMucus;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_cervicalMucus);
}

 final  List<BasalBodyTemperatureEntry> _basalBodyTemperature;
@override@JsonKey() List<BasalBodyTemperatureEntry> get basalBodyTemperature {
  if (_basalBodyTemperature is EqualUnmodifiableListView) return _basalBodyTemperature;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_basalBodyTemperature);
}

 final  List<IntermenstrualBleedingEntry> _intermenstrualBleeding;
@override@JsonKey() List<IntermenstrualBleedingEntry> get intermenstrualBleeding {
  if (_intermenstrualBleeding is EqualUnmodifiableListView) return _intermenstrualBleeding;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_intermenstrualBleeding);
}

 final  List<SexualActivityEntry> _sexualActivity;
@override@JsonKey() List<SexualActivityEntry> get sexualActivity {
  if (_sexualActivity is EqualUnmodifiableListView) return _sexualActivity;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_sexualActivity);
}


/// Create a copy of CycleData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CycleDataCopyWith<_CycleData> get copyWith => __$CycleDataCopyWithImpl<_CycleData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CycleData&&const DeepCollectionEquality().equals(other._menstruationFlows, _menstruationFlows)&&const DeepCollectionEquality().equals(other._menstruationPeriods, _menstruationPeriods)&&const DeepCollectionEquality().equals(other._ovulationTests, _ovulationTests)&&const DeepCollectionEquality().equals(other._cervicalMucus, _cervicalMucus)&&const DeepCollectionEquality().equals(other._basalBodyTemperature, _basalBodyTemperature)&&const DeepCollectionEquality().equals(other._intermenstrualBleeding, _intermenstrualBleeding)&&const DeepCollectionEquality().equals(other._sexualActivity, _sexualActivity));
}


@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_menstruationFlows),const DeepCollectionEquality().hash(_menstruationPeriods),const DeepCollectionEquality().hash(_ovulationTests),const DeepCollectionEquality().hash(_cervicalMucus),const DeepCollectionEquality().hash(_basalBodyTemperature),const DeepCollectionEquality().hash(_intermenstrualBleeding),const DeepCollectionEquality().hash(_sexualActivity));

@override
String toString() {
  return 'CycleData(menstruationFlows: $menstruationFlows, menstruationPeriods: $menstruationPeriods, ovulationTests: $ovulationTests, cervicalMucus: $cervicalMucus, basalBodyTemperature: $basalBodyTemperature, intermenstrualBleeding: $intermenstrualBleeding, sexualActivity: $sexualActivity)';
}


}

/// @nodoc
abstract mixin class _$CycleDataCopyWith<$Res> implements $CycleDataCopyWith<$Res> {
  factory _$CycleDataCopyWith(_CycleData value, $Res Function(_CycleData) _then) = __$CycleDataCopyWithImpl;
@override @useResult
$Res call({
 List<MenstruationFlowEntry> menstruationFlows, List<MenstruationPeriodEntry> menstruationPeriods, List<OvulationTestEntry> ovulationTests, List<CervicalMucusEntry> cervicalMucus, List<BasalBodyTemperatureEntry> basalBodyTemperature, List<IntermenstrualBleedingEntry> intermenstrualBleeding, List<SexualActivityEntry> sexualActivity
});




}
/// @nodoc
class __$CycleDataCopyWithImpl<$Res>
    implements _$CycleDataCopyWith<$Res> {
  __$CycleDataCopyWithImpl(this._self, this._then);

  final _CycleData _self;
  final $Res Function(_CycleData) _then;

/// Create a copy of CycleData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? menstruationFlows = null,Object? menstruationPeriods = null,Object? ovulationTests = null,Object? cervicalMucus = null,Object? basalBodyTemperature = null,Object? intermenstrualBleeding = null,Object? sexualActivity = null,}) {
  return _then(_CycleData(
menstruationFlows: null == menstruationFlows ? _self._menstruationFlows : menstruationFlows // ignore: cast_nullable_to_non_nullable
as List<MenstruationFlowEntry>,menstruationPeriods: null == menstruationPeriods ? _self._menstruationPeriods : menstruationPeriods // ignore: cast_nullable_to_non_nullable
as List<MenstruationPeriodEntry>,ovulationTests: null == ovulationTests ? _self._ovulationTests : ovulationTests // ignore: cast_nullable_to_non_nullable
as List<OvulationTestEntry>,cervicalMucus: null == cervicalMucus ? _self._cervicalMucus : cervicalMucus // ignore: cast_nullable_to_non_nullable
as List<CervicalMucusEntry>,basalBodyTemperature: null == basalBodyTemperature ? _self._basalBodyTemperature : basalBodyTemperature // ignore: cast_nullable_to_non_nullable
as List<BasalBodyTemperatureEntry>,intermenstrualBleeding: null == intermenstrualBleeding ? _self._intermenstrualBleeding : intermenstrualBleeding // ignore: cast_nullable_to_non_nullable
as List<IntermenstrualBleedingEntry>,sexualActivity: null == sexualActivity ? _self._sexualActivity : sexualActivity // ignore: cast_nullable_to_non_nullable
as List<SexualActivityEntry>,
  ));
}


}

/// @nodoc
mixin _$MenstruationFlowEntry {

 DateTime get time; int get flow; String get source;
/// Create a copy of MenstruationFlowEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MenstruationFlowEntryCopyWith<MenstruationFlowEntry> get copyWith => _$MenstruationFlowEntryCopyWithImpl<MenstruationFlowEntry>(this as MenstruationFlowEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MenstruationFlowEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.flow, flow) || other.flow == flow)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,flow,source);

@override
String toString() {
  return 'MenstruationFlowEntry(time: $time, flow: $flow, source: $source)';
}


}

/// @nodoc
abstract mixin class $MenstruationFlowEntryCopyWith<$Res>  {
  factory $MenstruationFlowEntryCopyWith(MenstruationFlowEntry value, $Res Function(MenstruationFlowEntry) _then) = _$MenstruationFlowEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, int flow, String source
});




}
/// @nodoc
class _$MenstruationFlowEntryCopyWithImpl<$Res>
    implements $MenstruationFlowEntryCopyWith<$Res> {
  _$MenstruationFlowEntryCopyWithImpl(this._self, this._then);

  final MenstruationFlowEntry _self;
  final $Res Function(MenstruationFlowEntry) _then;

/// Create a copy of MenstruationFlowEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? flow = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,flow: null == flow ? _self.flow : flow // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [MenstruationFlowEntry].
extension MenstruationFlowEntryPatterns on MenstruationFlowEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MenstruationFlowEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MenstruationFlowEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MenstruationFlowEntry value)  $default,){
final _that = this;
switch (_that) {
case _MenstruationFlowEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MenstruationFlowEntry value)?  $default,){
final _that = this;
switch (_that) {
case _MenstruationFlowEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int flow,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MenstruationFlowEntry() when $default != null:
return $default(_that.time,_that.flow,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int flow,  String source)  $default,) {final _that = this;
switch (_that) {
case _MenstruationFlowEntry():
return $default(_that.time,_that.flow,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int flow,  String source)?  $default,) {final _that = this;
switch (_that) {
case _MenstruationFlowEntry() when $default != null:
return $default(_that.time,_that.flow,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _MenstruationFlowEntry implements MenstruationFlowEntry {
  const _MenstruationFlowEntry({required this.time, required this.flow, required this.source});
  

@override final  DateTime time;
@override final  int flow;
@override final  String source;

/// Create a copy of MenstruationFlowEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MenstruationFlowEntryCopyWith<_MenstruationFlowEntry> get copyWith => __$MenstruationFlowEntryCopyWithImpl<_MenstruationFlowEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MenstruationFlowEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.flow, flow) || other.flow == flow)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,flow,source);

@override
String toString() {
  return 'MenstruationFlowEntry(time: $time, flow: $flow, source: $source)';
}


}

/// @nodoc
abstract mixin class _$MenstruationFlowEntryCopyWith<$Res> implements $MenstruationFlowEntryCopyWith<$Res> {
  factory _$MenstruationFlowEntryCopyWith(_MenstruationFlowEntry value, $Res Function(_MenstruationFlowEntry) _then) = __$MenstruationFlowEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int flow, String source
});




}
/// @nodoc
class __$MenstruationFlowEntryCopyWithImpl<$Res>
    implements _$MenstruationFlowEntryCopyWith<$Res> {
  __$MenstruationFlowEntryCopyWithImpl(this._self, this._then);

  final _MenstruationFlowEntry _self;
  final $Res Function(_MenstruationFlowEntry) _then;

/// Create a copy of MenstruationFlowEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? flow = null,Object? source = null,}) {
  return _then(_MenstruationFlowEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,flow: null == flow ? _self.flow : flow // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$MenstruationPeriodEntry {

 DateTime get startTime; DateTime get endTime; String get source;
/// Create a copy of MenstruationPeriodEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$MenstruationPeriodEntryCopyWith<MenstruationPeriodEntry> get copyWith => _$MenstruationPeriodEntryCopyWithImpl<MenstruationPeriodEntry>(this as MenstruationPeriodEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is MenstruationPeriodEntry&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,source);

@override
String toString() {
  return 'MenstruationPeriodEntry(startTime: $startTime, endTime: $endTime, source: $source)';
}


}

/// @nodoc
abstract mixin class $MenstruationPeriodEntryCopyWith<$Res>  {
  factory $MenstruationPeriodEntryCopyWith(MenstruationPeriodEntry value, $Res Function(MenstruationPeriodEntry) _then) = _$MenstruationPeriodEntryCopyWithImpl;
@useResult
$Res call({
 DateTime startTime, DateTime endTime, String source
});




}
/// @nodoc
class _$MenstruationPeriodEntryCopyWithImpl<$Res>
    implements $MenstruationPeriodEntryCopyWith<$Res> {
  _$MenstruationPeriodEntryCopyWithImpl(this._self, this._then);

  final MenstruationPeriodEntry _self;
  final $Res Function(MenstruationPeriodEntry) _then;

/// Create a copy of MenstruationPeriodEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? startTime = null,Object? endTime = null,Object? source = null,}) {
  return _then(_self.copyWith(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [MenstruationPeriodEntry].
extension MenstruationPeriodEntryPatterns on MenstruationPeriodEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _MenstruationPeriodEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _MenstruationPeriodEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _MenstruationPeriodEntry value)  $default,){
final _that = this;
switch (_that) {
case _MenstruationPeriodEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _MenstruationPeriodEntry value)?  $default,){
final _that = this;
switch (_that) {
case _MenstruationPeriodEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _MenstruationPeriodEntry() when $default != null:
return $default(_that.startTime,_that.endTime,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime startTime,  DateTime endTime,  String source)  $default,) {final _that = this;
switch (_that) {
case _MenstruationPeriodEntry():
return $default(_that.startTime,_that.endTime,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime startTime,  DateTime endTime,  String source)?  $default,) {final _that = this;
switch (_that) {
case _MenstruationPeriodEntry() when $default != null:
return $default(_that.startTime,_that.endTime,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _MenstruationPeriodEntry extends MenstruationPeriodEntry {
  const _MenstruationPeriodEntry({required this.startTime, required this.endTime, required this.source}): super._();
  

@override final  DateTime startTime;
@override final  DateTime endTime;
@override final  String source;

/// Create a copy of MenstruationPeriodEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$MenstruationPeriodEntryCopyWith<_MenstruationPeriodEntry> get copyWith => __$MenstruationPeriodEntryCopyWithImpl<_MenstruationPeriodEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _MenstruationPeriodEntry&&(identical(other.startTime, startTime) || other.startTime == startTime)&&(identical(other.endTime, endTime) || other.endTime == endTime)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,startTime,endTime,source);

@override
String toString() {
  return 'MenstruationPeriodEntry(startTime: $startTime, endTime: $endTime, source: $source)';
}


}

/// @nodoc
abstract mixin class _$MenstruationPeriodEntryCopyWith<$Res> implements $MenstruationPeriodEntryCopyWith<$Res> {
  factory _$MenstruationPeriodEntryCopyWith(_MenstruationPeriodEntry value, $Res Function(_MenstruationPeriodEntry) _then) = __$MenstruationPeriodEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime startTime, DateTime endTime, String source
});




}
/// @nodoc
class __$MenstruationPeriodEntryCopyWithImpl<$Res>
    implements _$MenstruationPeriodEntryCopyWith<$Res> {
  __$MenstruationPeriodEntryCopyWithImpl(this._self, this._then);

  final _MenstruationPeriodEntry _self;
  final $Res Function(_MenstruationPeriodEntry) _then;

/// Create a copy of MenstruationPeriodEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? startTime = null,Object? endTime = null,Object? source = null,}) {
  return _then(_MenstruationPeriodEntry(
startTime: null == startTime ? _self.startTime : startTime // ignore: cast_nullable_to_non_nullable
as DateTime,endTime: null == endTime ? _self.endTime : endTime // ignore: cast_nullable_to_non_nullable
as DateTime,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$OvulationTestEntry {

 DateTime get time; int get result; String get source;
/// Create a copy of OvulationTestEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$OvulationTestEntryCopyWith<OvulationTestEntry> get copyWith => _$OvulationTestEntryCopyWithImpl<OvulationTestEntry>(this as OvulationTestEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is OvulationTestEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.result, result) || other.result == result)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,result,source);

@override
String toString() {
  return 'OvulationTestEntry(time: $time, result: $result, source: $source)';
}


}

/// @nodoc
abstract mixin class $OvulationTestEntryCopyWith<$Res>  {
  factory $OvulationTestEntryCopyWith(OvulationTestEntry value, $Res Function(OvulationTestEntry) _then) = _$OvulationTestEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, int result, String source
});




}
/// @nodoc
class _$OvulationTestEntryCopyWithImpl<$Res>
    implements $OvulationTestEntryCopyWith<$Res> {
  _$OvulationTestEntryCopyWithImpl(this._self, this._then);

  final OvulationTestEntry _self;
  final $Res Function(OvulationTestEntry) _then;

/// Create a copy of OvulationTestEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? result = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,result: null == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [OvulationTestEntry].
extension OvulationTestEntryPatterns on OvulationTestEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _OvulationTestEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _OvulationTestEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _OvulationTestEntry value)  $default,){
final _that = this;
switch (_that) {
case _OvulationTestEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _OvulationTestEntry value)?  $default,){
final _that = this;
switch (_that) {
case _OvulationTestEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int result,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _OvulationTestEntry() when $default != null:
return $default(_that.time,_that.result,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int result,  String source)  $default,) {final _that = this;
switch (_that) {
case _OvulationTestEntry():
return $default(_that.time,_that.result,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int result,  String source)?  $default,) {final _that = this;
switch (_that) {
case _OvulationTestEntry() when $default != null:
return $default(_that.time,_that.result,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _OvulationTestEntry implements OvulationTestEntry {
  const _OvulationTestEntry({required this.time, required this.result, required this.source});
  

@override final  DateTime time;
@override final  int result;
@override final  String source;

/// Create a copy of OvulationTestEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$OvulationTestEntryCopyWith<_OvulationTestEntry> get copyWith => __$OvulationTestEntryCopyWithImpl<_OvulationTestEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _OvulationTestEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.result, result) || other.result == result)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,result,source);

@override
String toString() {
  return 'OvulationTestEntry(time: $time, result: $result, source: $source)';
}


}

/// @nodoc
abstract mixin class _$OvulationTestEntryCopyWith<$Res> implements $OvulationTestEntryCopyWith<$Res> {
  factory _$OvulationTestEntryCopyWith(_OvulationTestEntry value, $Res Function(_OvulationTestEntry) _then) = __$OvulationTestEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int result, String source
});




}
/// @nodoc
class __$OvulationTestEntryCopyWithImpl<$Res>
    implements _$OvulationTestEntryCopyWith<$Res> {
  __$OvulationTestEntryCopyWithImpl(this._self, this._then);

  final _OvulationTestEntry _self;
  final $Res Function(_OvulationTestEntry) _then;

/// Create a copy of OvulationTestEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? result = null,Object? source = null,}) {
  return _then(_OvulationTestEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,result: null == result ? _self.result : result // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$CervicalMucusEntry {

 DateTime get time; int get appearance; int get sensation; String get source;
/// Create a copy of CervicalMucusEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$CervicalMucusEntryCopyWith<CervicalMucusEntry> get copyWith => _$CervicalMucusEntryCopyWithImpl<CervicalMucusEntry>(this as CervicalMucusEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is CervicalMucusEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.appearance, appearance) || other.appearance == appearance)&&(identical(other.sensation, sensation) || other.sensation == sensation)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,appearance,sensation,source);

@override
String toString() {
  return 'CervicalMucusEntry(time: $time, appearance: $appearance, sensation: $sensation, source: $source)';
}


}

/// @nodoc
abstract mixin class $CervicalMucusEntryCopyWith<$Res>  {
  factory $CervicalMucusEntryCopyWith(CervicalMucusEntry value, $Res Function(CervicalMucusEntry) _then) = _$CervicalMucusEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, int appearance, int sensation, String source
});




}
/// @nodoc
class _$CervicalMucusEntryCopyWithImpl<$Res>
    implements $CervicalMucusEntryCopyWith<$Res> {
  _$CervicalMucusEntryCopyWithImpl(this._self, this._then);

  final CervicalMucusEntry _self;
  final $Res Function(CervicalMucusEntry) _then;

/// Create a copy of CervicalMucusEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? appearance = null,Object? sensation = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,appearance: null == appearance ? _self.appearance : appearance // ignore: cast_nullable_to_non_nullable
as int,sensation: null == sensation ? _self.sensation : sensation // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [CervicalMucusEntry].
extension CervicalMucusEntryPatterns on CervicalMucusEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _CervicalMucusEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _CervicalMucusEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _CervicalMucusEntry value)  $default,){
final _that = this;
switch (_that) {
case _CervicalMucusEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _CervicalMucusEntry value)?  $default,){
final _that = this;
switch (_that) {
case _CervicalMucusEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int appearance,  int sensation,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _CervicalMucusEntry() when $default != null:
return $default(_that.time,_that.appearance,_that.sensation,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int appearance,  int sensation,  String source)  $default,) {final _that = this;
switch (_that) {
case _CervicalMucusEntry():
return $default(_that.time,_that.appearance,_that.sensation,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int appearance,  int sensation,  String source)?  $default,) {final _that = this;
switch (_that) {
case _CervicalMucusEntry() when $default != null:
return $default(_that.time,_that.appearance,_that.sensation,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _CervicalMucusEntry implements CervicalMucusEntry {
  const _CervicalMucusEntry({required this.time, required this.appearance, required this.sensation, required this.source});
  

@override final  DateTime time;
@override final  int appearance;
@override final  int sensation;
@override final  String source;

/// Create a copy of CervicalMucusEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$CervicalMucusEntryCopyWith<_CervicalMucusEntry> get copyWith => __$CervicalMucusEntryCopyWithImpl<_CervicalMucusEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _CervicalMucusEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.appearance, appearance) || other.appearance == appearance)&&(identical(other.sensation, sensation) || other.sensation == sensation)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,appearance,sensation,source);

@override
String toString() {
  return 'CervicalMucusEntry(time: $time, appearance: $appearance, sensation: $sensation, source: $source)';
}


}

/// @nodoc
abstract mixin class _$CervicalMucusEntryCopyWith<$Res> implements $CervicalMucusEntryCopyWith<$Res> {
  factory _$CervicalMucusEntryCopyWith(_CervicalMucusEntry value, $Res Function(_CervicalMucusEntry) _then) = __$CervicalMucusEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int appearance, int sensation, String source
});




}
/// @nodoc
class __$CervicalMucusEntryCopyWithImpl<$Res>
    implements _$CervicalMucusEntryCopyWith<$Res> {
  __$CervicalMucusEntryCopyWithImpl(this._self, this._then);

  final _CervicalMucusEntry _self;
  final $Res Function(_CervicalMucusEntry) _then;

/// Create a copy of CervicalMucusEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? appearance = null,Object? sensation = null,Object? source = null,}) {
  return _then(_CervicalMucusEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,appearance: null == appearance ? _self.appearance : appearance // ignore: cast_nullable_to_non_nullable
as int,sensation: null == sensation ? _self.sensation : sensation // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$BasalBodyTemperatureEntry {

 DateTime get time; double get temperatureCelsius; int get measurementLocation; String get source;
/// Create a copy of BasalBodyTemperatureEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$BasalBodyTemperatureEntryCopyWith<BasalBodyTemperatureEntry> get copyWith => _$BasalBodyTemperatureEntryCopyWithImpl<BasalBodyTemperatureEntry>(this as BasalBodyTemperatureEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is BasalBodyTemperatureEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.temperatureCelsius, temperatureCelsius) || other.temperatureCelsius == temperatureCelsius)&&(identical(other.measurementLocation, measurementLocation) || other.measurementLocation == measurementLocation)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,temperatureCelsius,measurementLocation,source);

@override
String toString() {
  return 'BasalBodyTemperatureEntry(time: $time, temperatureCelsius: $temperatureCelsius, measurementLocation: $measurementLocation, source: $source)';
}


}

/// @nodoc
abstract mixin class $BasalBodyTemperatureEntryCopyWith<$Res>  {
  factory $BasalBodyTemperatureEntryCopyWith(BasalBodyTemperatureEntry value, $Res Function(BasalBodyTemperatureEntry) _then) = _$BasalBodyTemperatureEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, double temperatureCelsius, int measurementLocation, String source
});




}
/// @nodoc
class _$BasalBodyTemperatureEntryCopyWithImpl<$Res>
    implements $BasalBodyTemperatureEntryCopyWith<$Res> {
  _$BasalBodyTemperatureEntryCopyWithImpl(this._self, this._then);

  final BasalBodyTemperatureEntry _self;
  final $Res Function(BasalBodyTemperatureEntry) _then;

/// Create a copy of BasalBodyTemperatureEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? temperatureCelsius = null,Object? measurementLocation = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,temperatureCelsius: null == temperatureCelsius ? _self.temperatureCelsius : temperatureCelsius // ignore: cast_nullable_to_non_nullable
as double,measurementLocation: null == measurementLocation ? _self.measurementLocation : measurementLocation // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [BasalBodyTemperatureEntry].
extension BasalBodyTemperatureEntryPatterns on BasalBodyTemperatureEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _BasalBodyTemperatureEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _BasalBodyTemperatureEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _BasalBodyTemperatureEntry value)  $default,){
final _that = this;
switch (_that) {
case _BasalBodyTemperatureEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _BasalBodyTemperatureEntry value)?  $default,){
final _that = this;
switch (_that) {
case _BasalBodyTemperatureEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  double temperatureCelsius,  int measurementLocation,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _BasalBodyTemperatureEntry() when $default != null:
return $default(_that.time,_that.temperatureCelsius,_that.measurementLocation,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  double temperatureCelsius,  int measurementLocation,  String source)  $default,) {final _that = this;
switch (_that) {
case _BasalBodyTemperatureEntry():
return $default(_that.time,_that.temperatureCelsius,_that.measurementLocation,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  double temperatureCelsius,  int measurementLocation,  String source)?  $default,) {final _that = this;
switch (_that) {
case _BasalBodyTemperatureEntry() when $default != null:
return $default(_that.time,_that.temperatureCelsius,_that.measurementLocation,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _BasalBodyTemperatureEntry implements BasalBodyTemperatureEntry {
  const _BasalBodyTemperatureEntry({required this.time, required this.temperatureCelsius, required this.measurementLocation, required this.source});
  

@override final  DateTime time;
@override final  double temperatureCelsius;
@override final  int measurementLocation;
@override final  String source;

/// Create a copy of BasalBodyTemperatureEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$BasalBodyTemperatureEntryCopyWith<_BasalBodyTemperatureEntry> get copyWith => __$BasalBodyTemperatureEntryCopyWithImpl<_BasalBodyTemperatureEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _BasalBodyTemperatureEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.temperatureCelsius, temperatureCelsius) || other.temperatureCelsius == temperatureCelsius)&&(identical(other.measurementLocation, measurementLocation) || other.measurementLocation == measurementLocation)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,temperatureCelsius,measurementLocation,source);

@override
String toString() {
  return 'BasalBodyTemperatureEntry(time: $time, temperatureCelsius: $temperatureCelsius, measurementLocation: $measurementLocation, source: $source)';
}


}

/// @nodoc
abstract mixin class _$BasalBodyTemperatureEntryCopyWith<$Res> implements $BasalBodyTemperatureEntryCopyWith<$Res> {
  factory _$BasalBodyTemperatureEntryCopyWith(_BasalBodyTemperatureEntry value, $Res Function(_BasalBodyTemperatureEntry) _then) = __$BasalBodyTemperatureEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, double temperatureCelsius, int measurementLocation, String source
});




}
/// @nodoc
class __$BasalBodyTemperatureEntryCopyWithImpl<$Res>
    implements _$BasalBodyTemperatureEntryCopyWith<$Res> {
  __$BasalBodyTemperatureEntryCopyWithImpl(this._self, this._then);

  final _BasalBodyTemperatureEntry _self;
  final $Res Function(_BasalBodyTemperatureEntry) _then;

/// Create a copy of BasalBodyTemperatureEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? temperatureCelsius = null,Object? measurementLocation = null,Object? source = null,}) {
  return _then(_BasalBodyTemperatureEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,temperatureCelsius: null == temperatureCelsius ? _self.temperatureCelsius : temperatureCelsius // ignore: cast_nullable_to_non_nullable
as double,measurementLocation: null == measurementLocation ? _self.measurementLocation : measurementLocation // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$IntermenstrualBleedingEntry {

 DateTime get time; String get source;
/// Create a copy of IntermenstrualBleedingEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$IntermenstrualBleedingEntryCopyWith<IntermenstrualBleedingEntry> get copyWith => _$IntermenstrualBleedingEntryCopyWithImpl<IntermenstrualBleedingEntry>(this as IntermenstrualBleedingEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is IntermenstrualBleedingEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,source);

@override
String toString() {
  return 'IntermenstrualBleedingEntry(time: $time, source: $source)';
}


}

/// @nodoc
abstract mixin class $IntermenstrualBleedingEntryCopyWith<$Res>  {
  factory $IntermenstrualBleedingEntryCopyWith(IntermenstrualBleedingEntry value, $Res Function(IntermenstrualBleedingEntry) _then) = _$IntermenstrualBleedingEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, String source
});




}
/// @nodoc
class _$IntermenstrualBleedingEntryCopyWithImpl<$Res>
    implements $IntermenstrualBleedingEntryCopyWith<$Res> {
  _$IntermenstrualBleedingEntryCopyWithImpl(this._self, this._then);

  final IntermenstrualBleedingEntry _self;
  final $Res Function(IntermenstrualBleedingEntry) _then;

/// Create a copy of IntermenstrualBleedingEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [IntermenstrualBleedingEntry].
extension IntermenstrualBleedingEntryPatterns on IntermenstrualBleedingEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _IntermenstrualBleedingEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _IntermenstrualBleedingEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _IntermenstrualBleedingEntry value)  $default,){
final _that = this;
switch (_that) {
case _IntermenstrualBleedingEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _IntermenstrualBleedingEntry value)?  $default,){
final _that = this;
switch (_that) {
case _IntermenstrualBleedingEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _IntermenstrualBleedingEntry() when $default != null:
return $default(_that.time,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  String source)  $default,) {final _that = this;
switch (_that) {
case _IntermenstrualBleedingEntry():
return $default(_that.time,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  String source)?  $default,) {final _that = this;
switch (_that) {
case _IntermenstrualBleedingEntry() when $default != null:
return $default(_that.time,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _IntermenstrualBleedingEntry implements IntermenstrualBleedingEntry {
  const _IntermenstrualBleedingEntry({required this.time, required this.source});
  

@override final  DateTime time;
@override final  String source;

/// Create a copy of IntermenstrualBleedingEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$IntermenstrualBleedingEntryCopyWith<_IntermenstrualBleedingEntry> get copyWith => __$IntermenstrualBleedingEntryCopyWithImpl<_IntermenstrualBleedingEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _IntermenstrualBleedingEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,source);

@override
String toString() {
  return 'IntermenstrualBleedingEntry(time: $time, source: $source)';
}


}

/// @nodoc
abstract mixin class _$IntermenstrualBleedingEntryCopyWith<$Res> implements $IntermenstrualBleedingEntryCopyWith<$Res> {
  factory _$IntermenstrualBleedingEntryCopyWith(_IntermenstrualBleedingEntry value, $Res Function(_IntermenstrualBleedingEntry) _then) = __$IntermenstrualBleedingEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, String source
});




}
/// @nodoc
class __$IntermenstrualBleedingEntryCopyWithImpl<$Res>
    implements _$IntermenstrualBleedingEntryCopyWith<$Res> {
  __$IntermenstrualBleedingEntryCopyWithImpl(this._self, this._then);

  final _IntermenstrualBleedingEntry _self;
  final $Res Function(_IntermenstrualBleedingEntry) _then;

/// Create a copy of IntermenstrualBleedingEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? source = null,}) {
  return _then(_IntermenstrualBleedingEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

/// @nodoc
mixin _$SexualActivityEntry {

 DateTime get time; int get protectionUsed; String get source;
/// Create a copy of SexualActivityEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$SexualActivityEntryCopyWith<SexualActivityEntry> get copyWith => _$SexualActivityEntryCopyWithImpl<SexualActivityEntry>(this as SexualActivityEntry, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is SexualActivityEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.protectionUsed, protectionUsed) || other.protectionUsed == protectionUsed)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,protectionUsed,source);

@override
String toString() {
  return 'SexualActivityEntry(time: $time, protectionUsed: $protectionUsed, source: $source)';
}


}

/// @nodoc
abstract mixin class $SexualActivityEntryCopyWith<$Res>  {
  factory $SexualActivityEntryCopyWith(SexualActivityEntry value, $Res Function(SexualActivityEntry) _then) = _$SexualActivityEntryCopyWithImpl;
@useResult
$Res call({
 DateTime time, int protectionUsed, String source
});




}
/// @nodoc
class _$SexualActivityEntryCopyWithImpl<$Res>
    implements $SexualActivityEntryCopyWith<$Res> {
  _$SexualActivityEntryCopyWithImpl(this._self, this._then);

  final SexualActivityEntry _self;
  final $Res Function(SexualActivityEntry) _then;

/// Create a copy of SexualActivityEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? time = null,Object? protectionUsed = null,Object? source = null,}) {
  return _then(_self.copyWith(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,protectionUsed: null == protectionUsed ? _self.protectionUsed : protectionUsed // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}

}


/// Adds pattern-matching-related methods to [SexualActivityEntry].
extension SexualActivityEntryPatterns on SexualActivityEntry {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _SexualActivityEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _SexualActivityEntry() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _SexualActivityEntry value)  $default,){
final _that = this;
switch (_that) {
case _SexualActivityEntry():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _SexualActivityEntry value)?  $default,){
final _that = this;
switch (_that) {
case _SexualActivityEntry() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( DateTime time,  int protectionUsed,  String source)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _SexualActivityEntry() when $default != null:
return $default(_that.time,_that.protectionUsed,_that.source);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( DateTime time,  int protectionUsed,  String source)  $default,) {final _that = this;
switch (_that) {
case _SexualActivityEntry():
return $default(_that.time,_that.protectionUsed,_that.source);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( DateTime time,  int protectionUsed,  String source)?  $default,) {final _that = this;
switch (_that) {
case _SexualActivityEntry() when $default != null:
return $default(_that.time,_that.protectionUsed,_that.source);case _:
  return null;

}
}

}

/// @nodoc


class _SexualActivityEntry implements SexualActivityEntry {
  const _SexualActivityEntry({required this.time, required this.protectionUsed, required this.source});
  

@override final  DateTime time;
@override final  int protectionUsed;
@override final  String source;

/// Create a copy of SexualActivityEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$SexualActivityEntryCopyWith<_SexualActivityEntry> get copyWith => __$SexualActivityEntryCopyWithImpl<_SexualActivityEntry>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _SexualActivityEntry&&(identical(other.time, time) || other.time == time)&&(identical(other.protectionUsed, protectionUsed) || other.protectionUsed == protectionUsed)&&(identical(other.source, source) || other.source == source));
}


@override
int get hashCode => Object.hash(runtimeType,time,protectionUsed,source);

@override
String toString() {
  return 'SexualActivityEntry(time: $time, protectionUsed: $protectionUsed, source: $source)';
}


}

/// @nodoc
abstract mixin class _$SexualActivityEntryCopyWith<$Res> implements $SexualActivityEntryCopyWith<$Res> {
  factory _$SexualActivityEntryCopyWith(_SexualActivityEntry value, $Res Function(_SexualActivityEntry) _then) = __$SexualActivityEntryCopyWithImpl;
@override @useResult
$Res call({
 DateTime time, int protectionUsed, String source
});




}
/// @nodoc
class __$SexualActivityEntryCopyWithImpl<$Res>
    implements _$SexualActivityEntryCopyWith<$Res> {
  __$SexualActivityEntryCopyWithImpl(this._self, this._then);

  final _SexualActivityEntry _self;
  final $Res Function(_SexualActivityEntry) _then;

/// Create a copy of SexualActivityEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? time = null,Object? protectionUsed = null,Object? source = null,}) {
  return _then(_SexualActivityEntry(
time: null == time ? _self.time : time // ignore: cast_nullable_to_non_nullable
as DateTime,protectionUsed: null == protectionUsed ? _self.protectionUsed : protectionUsed // ignore: cast_nullable_to_non_nullable
as int,source: null == source ? _self.source : source // ignore: cast_nullable_to_non_nullable
as String,
  ));
}


}

// dart format on
