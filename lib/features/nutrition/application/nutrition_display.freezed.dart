// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'nutrition_display.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$NutritionSeries {

 NutritionNutrient get nutrient; List<PeriodChartValue> get values; double get total; double get average; double get best; int get loggedDays; bool get hasTrackedValues;/// The DAY range's cumulative intake curve for this nutrient.
 List<DaySample> get cumulativeSamples;
/// Create a copy of NutritionSeries
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$NutritionSeriesCopyWith<NutritionSeries> get copyWith => _$NutritionSeriesCopyWithImpl<NutritionSeries>(this as NutritionSeries, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is NutritionSeries&&(identical(other.nutrient, nutrient) || other.nutrient == nutrient)&&const DeepCollectionEquality().equals(other.values, values)&&(identical(other.total, total) || other.total == total)&&(identical(other.average, average) || other.average == average)&&(identical(other.best, best) || other.best == best)&&(identical(other.loggedDays, loggedDays) || other.loggedDays == loggedDays)&&(identical(other.hasTrackedValues, hasTrackedValues) || other.hasTrackedValues == hasTrackedValues)&&const DeepCollectionEquality().equals(other.cumulativeSamples, cumulativeSamples));
}


@override
int get hashCode => Object.hash(runtimeType,nutrient,const DeepCollectionEquality().hash(values),total,average,best,loggedDays,hasTrackedValues,const DeepCollectionEquality().hash(cumulativeSamples));

@override
String toString() {
  return 'NutritionSeries(nutrient: $nutrient, values: $values, total: $total, average: $average, best: $best, loggedDays: $loggedDays, hasTrackedValues: $hasTrackedValues, cumulativeSamples: $cumulativeSamples)';
}


}

/// @nodoc
abstract mixin class $NutritionSeriesCopyWith<$Res>  {
  factory $NutritionSeriesCopyWith(NutritionSeries value, $Res Function(NutritionSeries) _then) = _$NutritionSeriesCopyWithImpl;
@useResult
$Res call({
 NutritionNutrient nutrient, List<PeriodChartValue> values, double total, double average, double best, int loggedDays, bool hasTrackedValues, List<DaySample> cumulativeSamples
});




}
/// @nodoc
class _$NutritionSeriesCopyWithImpl<$Res>
    implements $NutritionSeriesCopyWith<$Res> {
  _$NutritionSeriesCopyWithImpl(this._self, this._then);

  final NutritionSeries _self;
  final $Res Function(NutritionSeries) _then;

/// Create a copy of NutritionSeries
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? nutrient = null,Object? values = null,Object? total = null,Object? average = null,Object? best = null,Object? loggedDays = null,Object? hasTrackedValues = null,Object? cumulativeSamples = null,}) {
  return _then(_self.copyWith(
nutrient: null == nutrient ? _self.nutrient : nutrient // ignore: cast_nullable_to_non_nullable
as NutritionNutrient,values: null == values ? _self.values : values // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,total: null == total ? _self.total : total // ignore: cast_nullable_to_non_nullable
as double,average: null == average ? _self.average : average // ignore: cast_nullable_to_non_nullable
as double,best: null == best ? _self.best : best // ignore: cast_nullable_to_non_nullable
as double,loggedDays: null == loggedDays ? _self.loggedDays : loggedDays // ignore: cast_nullable_to_non_nullable
as int,hasTrackedValues: null == hasTrackedValues ? _self.hasTrackedValues : hasTrackedValues // ignore: cast_nullable_to_non_nullable
as bool,cumulativeSamples: null == cumulativeSamples ? _self.cumulativeSamples : cumulativeSamples // ignore: cast_nullable_to_non_nullable
as List<DaySample>,
  ));
}

}


/// Adds pattern-matching-related methods to [NutritionSeries].
extension NutritionSeriesPatterns on NutritionSeries {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _NutritionSeries value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _NutritionSeries() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _NutritionSeries value)  $default,){
final _that = this;
switch (_that) {
case _NutritionSeries():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _NutritionSeries value)?  $default,){
final _that = this;
switch (_that) {
case _NutritionSeries() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( NutritionNutrient nutrient,  List<PeriodChartValue> values,  double total,  double average,  double best,  int loggedDays,  bool hasTrackedValues,  List<DaySample> cumulativeSamples)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _NutritionSeries() when $default != null:
return $default(_that.nutrient,_that.values,_that.total,_that.average,_that.best,_that.loggedDays,_that.hasTrackedValues,_that.cumulativeSamples);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( NutritionNutrient nutrient,  List<PeriodChartValue> values,  double total,  double average,  double best,  int loggedDays,  bool hasTrackedValues,  List<DaySample> cumulativeSamples)  $default,) {final _that = this;
switch (_that) {
case _NutritionSeries():
return $default(_that.nutrient,_that.values,_that.total,_that.average,_that.best,_that.loggedDays,_that.hasTrackedValues,_that.cumulativeSamples);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( NutritionNutrient nutrient,  List<PeriodChartValue> values,  double total,  double average,  double best,  int loggedDays,  bool hasTrackedValues,  List<DaySample> cumulativeSamples)?  $default,) {final _that = this;
switch (_that) {
case _NutritionSeries() when $default != null:
return $default(_that.nutrient,_that.values,_that.total,_that.average,_that.best,_that.loggedDays,_that.hasTrackedValues,_that.cumulativeSamples);case _:
  return null;

}
}

}

/// @nodoc


class _NutritionSeries implements NutritionSeries {
  const _NutritionSeries({required this.nutrient, required final  List<PeriodChartValue> values, required this.total, required this.average, required this.best, required this.loggedDays, required this.hasTrackedValues, required final  List<DaySample> cumulativeSamples}): _values = values,_cumulativeSamples = cumulativeSamples;
  

@override final  NutritionNutrient nutrient;
 final  List<PeriodChartValue> _values;
@override List<PeriodChartValue> get values {
  if (_values is EqualUnmodifiableListView) return _values;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_values);
}

@override final  double total;
@override final  double average;
@override final  double best;
@override final  int loggedDays;
@override final  bool hasTrackedValues;
/// The DAY range's cumulative intake curve for this nutrient.
 final  List<DaySample> _cumulativeSamples;
/// The DAY range's cumulative intake curve for this nutrient.
@override List<DaySample> get cumulativeSamples {
  if (_cumulativeSamples is EqualUnmodifiableListView) return _cumulativeSamples;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_cumulativeSamples);
}


/// Create a copy of NutritionSeries
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$NutritionSeriesCopyWith<_NutritionSeries> get copyWith => __$NutritionSeriesCopyWithImpl<_NutritionSeries>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _NutritionSeries&&(identical(other.nutrient, nutrient) || other.nutrient == nutrient)&&const DeepCollectionEquality().equals(other._values, _values)&&(identical(other.total, total) || other.total == total)&&(identical(other.average, average) || other.average == average)&&(identical(other.best, best) || other.best == best)&&(identical(other.loggedDays, loggedDays) || other.loggedDays == loggedDays)&&(identical(other.hasTrackedValues, hasTrackedValues) || other.hasTrackedValues == hasTrackedValues)&&const DeepCollectionEquality().equals(other._cumulativeSamples, _cumulativeSamples));
}


@override
int get hashCode => Object.hash(runtimeType,nutrient,const DeepCollectionEquality().hash(_values),total,average,best,loggedDays,hasTrackedValues,const DeepCollectionEquality().hash(_cumulativeSamples));

@override
String toString() {
  return 'NutritionSeries(nutrient: $nutrient, values: $values, total: $total, average: $average, best: $best, loggedDays: $loggedDays, hasTrackedValues: $hasTrackedValues, cumulativeSamples: $cumulativeSamples)';
}


}

/// @nodoc
abstract mixin class _$NutritionSeriesCopyWith<$Res> implements $NutritionSeriesCopyWith<$Res> {
  factory _$NutritionSeriesCopyWith(_NutritionSeries value, $Res Function(_NutritionSeries) _then) = __$NutritionSeriesCopyWithImpl;
@override @useResult
$Res call({
 NutritionNutrient nutrient, List<PeriodChartValue> values, double total, double average, double best, int loggedDays, bool hasTrackedValues, List<DaySample> cumulativeSamples
});




}
/// @nodoc
class __$NutritionSeriesCopyWithImpl<$Res>
    implements _$NutritionSeriesCopyWith<$Res> {
  __$NutritionSeriesCopyWithImpl(this._self, this._then);

  final _NutritionSeries _self;
  final $Res Function(_NutritionSeries) _then;

/// Create a copy of NutritionSeries
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? nutrient = null,Object? values = null,Object? total = null,Object? average = null,Object? best = null,Object? loggedDays = null,Object? hasTrackedValues = null,Object? cumulativeSamples = null,}) {
  return _then(_NutritionSeries(
nutrient: null == nutrient ? _self.nutrient : nutrient // ignore: cast_nullable_to_non_nullable
as NutritionNutrient,values: null == values ? _self._values : values // ignore: cast_nullable_to_non_nullable
as List<PeriodChartValue>,total: null == total ? _self.total : total // ignore: cast_nullable_to_non_nullable
as double,average: null == average ? _self.average : average // ignore: cast_nullable_to_non_nullable
as double,best: null == best ? _self.best : best // ignore: cast_nullable_to_non_nullable
as double,loggedDays: null == loggedDays ? _self.loggedDays : loggedDays // ignore: cast_nullable_to_non_nullable
as int,hasTrackedValues: null == hasTrackedValues ? _self.hasTrackedValues : hasTrackedValues // ignore: cast_nullable_to_non_nullable
as bool,cumulativeSamples: null == cumulativeSamples ? _self._cumulativeSamples : cumulativeSamples // ignore: cast_nullable_to_non_nullable
as List<DaySample>,
  ));
}


}

/// @nodoc
mixin _$NutritionDisplay {

 bool get hasData;/// Kotlin gates the whole macro-derived block on `dailyMacros.isNotEmpty()`;
/// only the ENTRIES section renders for an entries-only period.
 bool get hasMacros;/// The keyed metric's own series (the one the metric screen renders).
 NutritionSeries get metricSeries; List<NutritionSeries> get allSeries; List<NutritionSeries> get primarySeries; List<NutritionSeries> get trackedSeries;/// The tracked non-primary nutrients, which the overview groups by family.
 List<NutritionSeries> get additionalSeries;/// The same nutrients, already bucketed into the group headers the overview
/// prints (vitamins, minerals, …) — the screen looks its group up.
 Map<NutritionNutrientGroup, List<NutritionSeries>> get additionalSeriesByGroup; DailyGoalProgress get goalProgress; PeriodComparison get comparison; PersonalBaselineInsight? get baselineInsight; MacroSplitInterpretation? get macroSplit;/// Data confidence over the keyed metric's tracked days…
 DataConfidence get metricConfidence;/// …and over every day with any nutrition at all (the overview's).
 DataConfidence get overviewConfidence; List<NutritionEntry> get entriesNewestFirst;/// Meals by day, each list newest first — the chart's pinned-day section
/// looks its day up rather than scanning for it.
 Map<LocalDate, List<NutritionEntry>> get entriesByDay;
/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$NutritionDisplayCopyWith<NutritionDisplay> get copyWith => _$NutritionDisplayCopyWithImpl<NutritionDisplay>(this as NutritionDisplay, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is NutritionDisplay&&(identical(other.hasData, hasData) || other.hasData == hasData)&&(identical(other.hasMacros, hasMacros) || other.hasMacros == hasMacros)&&(identical(other.metricSeries, metricSeries) || other.metricSeries == metricSeries)&&const DeepCollectionEquality().equals(other.allSeries, allSeries)&&const DeepCollectionEquality().equals(other.primarySeries, primarySeries)&&const DeepCollectionEquality().equals(other.trackedSeries, trackedSeries)&&const DeepCollectionEquality().equals(other.additionalSeries, additionalSeries)&&const DeepCollectionEquality().equals(other.additionalSeriesByGroup, additionalSeriesByGroup)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&(identical(other.comparison, comparison) || other.comparison == comparison)&&(identical(other.baselineInsight, baselineInsight) || other.baselineInsight == baselineInsight)&&(identical(other.macroSplit, macroSplit) || other.macroSplit == macroSplit)&&(identical(other.metricConfidence, metricConfidence) || other.metricConfidence == metricConfidence)&&(identical(other.overviewConfidence, overviewConfidence) || other.overviewConfidence == overviewConfidence)&&const DeepCollectionEquality().equals(other.entriesNewestFirst, entriesNewestFirst)&&const DeepCollectionEquality().equals(other.entriesByDay, entriesByDay));
}


@override
int get hashCode => Object.hash(runtimeType,hasData,hasMacros,metricSeries,const DeepCollectionEquality().hash(allSeries),const DeepCollectionEquality().hash(primarySeries),const DeepCollectionEquality().hash(trackedSeries),const DeepCollectionEquality().hash(additionalSeries),const DeepCollectionEquality().hash(additionalSeriesByGroup),goalProgress,comparison,baselineInsight,macroSplit,metricConfidence,overviewConfidence,const DeepCollectionEquality().hash(entriesNewestFirst),const DeepCollectionEquality().hash(entriesByDay));

@override
String toString() {
  return 'NutritionDisplay(hasData: $hasData, hasMacros: $hasMacros, metricSeries: $metricSeries, allSeries: $allSeries, primarySeries: $primarySeries, trackedSeries: $trackedSeries, additionalSeries: $additionalSeries, additionalSeriesByGroup: $additionalSeriesByGroup, goalProgress: $goalProgress, comparison: $comparison, baselineInsight: $baselineInsight, macroSplit: $macroSplit, metricConfidence: $metricConfidence, overviewConfidence: $overviewConfidence, entriesNewestFirst: $entriesNewestFirst, entriesByDay: $entriesByDay)';
}


}

/// @nodoc
abstract mixin class $NutritionDisplayCopyWith<$Res>  {
  factory $NutritionDisplayCopyWith(NutritionDisplay value, $Res Function(NutritionDisplay) _then) = _$NutritionDisplayCopyWithImpl;
@useResult
$Res call({
 bool hasData, bool hasMacros, NutritionSeries metricSeries, List<NutritionSeries> allSeries, List<NutritionSeries> primarySeries, List<NutritionSeries> trackedSeries, List<NutritionSeries> additionalSeries, Map<NutritionNutrientGroup, List<NutritionSeries>> additionalSeriesByGroup, DailyGoalProgress goalProgress, PeriodComparison comparison, PersonalBaselineInsight? baselineInsight, MacroSplitInterpretation? macroSplit, DataConfidence metricConfidence, DataConfidence overviewConfidence, List<NutritionEntry> entriesNewestFirst, Map<LocalDate, List<NutritionEntry>> entriesByDay
});


$NutritionSeriesCopyWith<$Res> get metricSeries;$DailyGoalProgressCopyWith<$Res> get goalProgress;$PeriodComparisonCopyWith<$Res> get comparison;$PersonalBaselineInsightCopyWith<$Res>? get baselineInsight;$MacroSplitInterpretationCopyWith<$Res>? get macroSplit;$DataConfidenceCopyWith<$Res> get metricConfidence;$DataConfidenceCopyWith<$Res> get overviewConfidence;

}
/// @nodoc
class _$NutritionDisplayCopyWithImpl<$Res>
    implements $NutritionDisplayCopyWith<$Res> {
  _$NutritionDisplayCopyWithImpl(this._self, this._then);

  final NutritionDisplay _self;
  final $Res Function(NutritionDisplay) _then;

/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? hasData = null,Object? hasMacros = null,Object? metricSeries = null,Object? allSeries = null,Object? primarySeries = null,Object? trackedSeries = null,Object? additionalSeries = null,Object? additionalSeriesByGroup = null,Object? goalProgress = null,Object? comparison = null,Object? baselineInsight = freezed,Object? macroSplit = freezed,Object? metricConfidence = null,Object? overviewConfidence = null,Object? entriesNewestFirst = null,Object? entriesByDay = null,}) {
  return _then(_self.copyWith(
hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,hasMacros: null == hasMacros ? _self.hasMacros : hasMacros // ignore: cast_nullable_to_non_nullable
as bool,metricSeries: null == metricSeries ? _self.metricSeries : metricSeries // ignore: cast_nullable_to_non_nullable
as NutritionSeries,allSeries: null == allSeries ? _self.allSeries : allSeries // ignore: cast_nullable_to_non_nullable
as List<NutritionSeries>,primarySeries: null == primarySeries ? _self.primarySeries : primarySeries // ignore: cast_nullable_to_non_nullable
as List<NutritionSeries>,trackedSeries: null == trackedSeries ? _self.trackedSeries : trackedSeries // ignore: cast_nullable_to_non_nullable
as List<NutritionSeries>,additionalSeries: null == additionalSeries ? _self.additionalSeries : additionalSeries // ignore: cast_nullable_to_non_nullable
as List<NutritionSeries>,additionalSeriesByGroup: null == additionalSeriesByGroup ? _self.additionalSeriesByGroup : additionalSeriesByGroup // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrientGroup, List<NutritionSeries>>,goalProgress: null == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as DailyGoalProgress,comparison: null == comparison ? _self.comparison : comparison // ignore: cast_nullable_to_non_nullable
as PeriodComparison,baselineInsight: freezed == baselineInsight ? _self.baselineInsight : baselineInsight // ignore: cast_nullable_to_non_nullable
as PersonalBaselineInsight?,macroSplit: freezed == macroSplit ? _self.macroSplit : macroSplit // ignore: cast_nullable_to_non_nullable
as MacroSplitInterpretation?,metricConfidence: null == metricConfidence ? _self.metricConfidence : metricConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence,overviewConfidence: null == overviewConfidence ? _self.overviewConfidence : overviewConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence,entriesNewestFirst: null == entriesNewestFirst ? _self.entriesNewestFirst : entriesNewestFirst // ignore: cast_nullable_to_non_nullable
as List<NutritionEntry>,entriesByDay: null == entriesByDay ? _self.entriesByDay : entriesByDay // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<NutritionEntry>>,
  ));
}
/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$NutritionSeriesCopyWith<$Res> get metricSeries {
  
  return $NutritionSeriesCopyWith<$Res>(_self.metricSeries, (value) {
    return _then(_self.copyWith(metricSeries: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyGoalProgressCopyWith<$Res> get goalProgress {
  
  return $DailyGoalProgressCopyWith<$Res>(_self.goalProgress, (value) {
    return _then(_self.copyWith(goalProgress: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PeriodComparisonCopyWith<$Res> get comparison {
  
  return $PeriodComparisonCopyWith<$Res>(_self.comparison, (value) {
    return _then(_self.copyWith(comparison: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PersonalBaselineInsightCopyWith<$Res>? get baselineInsight {
    if (_self.baselineInsight == null) {
    return null;
  }

  return $PersonalBaselineInsightCopyWith<$Res>(_self.baselineInsight!, (value) {
    return _then(_self.copyWith(baselineInsight: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$MacroSplitInterpretationCopyWith<$Res>? get macroSplit {
    if (_self.macroSplit == null) {
    return null;
  }

  return $MacroSplitInterpretationCopyWith<$Res>(_self.macroSplit!, (value) {
    return _then(_self.copyWith(macroSplit: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<$Res> get metricConfidence {
  
  return $DataConfidenceCopyWith<$Res>(_self.metricConfidence, (value) {
    return _then(_self.copyWith(metricConfidence: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<$Res> get overviewConfidence {
  
  return $DataConfidenceCopyWith<$Res>(_self.overviewConfidence, (value) {
    return _then(_self.copyWith(overviewConfidence: value));
  });
}
}


/// Adds pattern-matching-related methods to [NutritionDisplay].
extension NutritionDisplayPatterns on NutritionDisplay {
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

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _NutritionDisplay value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _NutritionDisplay() when $default != null:
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

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _NutritionDisplay value)  $default,){
final _that = this;
switch (_that) {
case _NutritionDisplay():
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

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _NutritionDisplay value)?  $default,){
final _that = this;
switch (_that) {
case _NutritionDisplay() when $default != null:
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

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( bool hasData,  bool hasMacros,  NutritionSeries metricSeries,  List<NutritionSeries> allSeries,  List<NutritionSeries> primarySeries,  List<NutritionSeries> trackedSeries,  List<NutritionSeries> additionalSeries,  Map<NutritionNutrientGroup, List<NutritionSeries>> additionalSeriesByGroup,  DailyGoalProgress goalProgress,  PeriodComparison comparison,  PersonalBaselineInsight? baselineInsight,  MacroSplitInterpretation? macroSplit,  DataConfidence metricConfidence,  DataConfidence overviewConfidence,  List<NutritionEntry> entriesNewestFirst,  Map<LocalDate, List<NutritionEntry>> entriesByDay)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _NutritionDisplay() when $default != null:
return $default(_that.hasData,_that.hasMacros,_that.metricSeries,_that.allSeries,_that.primarySeries,_that.trackedSeries,_that.additionalSeries,_that.additionalSeriesByGroup,_that.goalProgress,_that.comparison,_that.baselineInsight,_that.macroSplit,_that.metricConfidence,_that.overviewConfidence,_that.entriesNewestFirst,_that.entriesByDay);case _:
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

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( bool hasData,  bool hasMacros,  NutritionSeries metricSeries,  List<NutritionSeries> allSeries,  List<NutritionSeries> primarySeries,  List<NutritionSeries> trackedSeries,  List<NutritionSeries> additionalSeries,  Map<NutritionNutrientGroup, List<NutritionSeries>> additionalSeriesByGroup,  DailyGoalProgress goalProgress,  PeriodComparison comparison,  PersonalBaselineInsight? baselineInsight,  MacroSplitInterpretation? macroSplit,  DataConfidence metricConfidence,  DataConfidence overviewConfidence,  List<NutritionEntry> entriesNewestFirst,  Map<LocalDate, List<NutritionEntry>> entriesByDay)  $default,) {final _that = this;
switch (_that) {
case _NutritionDisplay():
return $default(_that.hasData,_that.hasMacros,_that.metricSeries,_that.allSeries,_that.primarySeries,_that.trackedSeries,_that.additionalSeries,_that.additionalSeriesByGroup,_that.goalProgress,_that.comparison,_that.baselineInsight,_that.macroSplit,_that.metricConfidence,_that.overviewConfidence,_that.entriesNewestFirst,_that.entriesByDay);case _:
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

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( bool hasData,  bool hasMacros,  NutritionSeries metricSeries,  List<NutritionSeries> allSeries,  List<NutritionSeries> primarySeries,  List<NutritionSeries> trackedSeries,  List<NutritionSeries> additionalSeries,  Map<NutritionNutrientGroup, List<NutritionSeries>> additionalSeriesByGroup,  DailyGoalProgress goalProgress,  PeriodComparison comparison,  PersonalBaselineInsight? baselineInsight,  MacroSplitInterpretation? macroSplit,  DataConfidence metricConfidence,  DataConfidence overviewConfidence,  List<NutritionEntry> entriesNewestFirst,  Map<LocalDate, List<NutritionEntry>> entriesByDay)?  $default,) {final _that = this;
switch (_that) {
case _NutritionDisplay() when $default != null:
return $default(_that.hasData,_that.hasMacros,_that.metricSeries,_that.allSeries,_that.primarySeries,_that.trackedSeries,_that.additionalSeries,_that.additionalSeriesByGroup,_that.goalProgress,_that.comparison,_that.baselineInsight,_that.macroSplit,_that.metricConfidence,_that.overviewConfidence,_that.entriesNewestFirst,_that.entriesByDay);case _:
  return null;

}
}

}

/// @nodoc


class _NutritionDisplay implements NutritionDisplay {
  const _NutritionDisplay({required this.hasData, required this.hasMacros, required this.metricSeries, required final  List<NutritionSeries> allSeries, required final  List<NutritionSeries> primarySeries, required final  List<NutritionSeries> trackedSeries, required final  List<NutritionSeries> additionalSeries, required final  Map<NutritionNutrientGroup, List<NutritionSeries>> additionalSeriesByGroup, required this.goalProgress, required this.comparison, this.baselineInsight, this.macroSplit, required this.metricConfidence, required this.overviewConfidence, required final  List<NutritionEntry> entriesNewestFirst, required final  Map<LocalDate, List<NutritionEntry>> entriesByDay}): _allSeries = allSeries,_primarySeries = primarySeries,_trackedSeries = trackedSeries,_additionalSeries = additionalSeries,_additionalSeriesByGroup = additionalSeriesByGroup,_entriesNewestFirst = entriesNewestFirst,_entriesByDay = entriesByDay;
  

@override final  bool hasData;
/// Kotlin gates the whole macro-derived block on `dailyMacros.isNotEmpty()`;
/// only the ENTRIES section renders for an entries-only period.
@override final  bool hasMacros;
/// The keyed metric's own series (the one the metric screen renders).
@override final  NutritionSeries metricSeries;
 final  List<NutritionSeries> _allSeries;
@override List<NutritionSeries> get allSeries {
  if (_allSeries is EqualUnmodifiableListView) return _allSeries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_allSeries);
}

 final  List<NutritionSeries> _primarySeries;
@override List<NutritionSeries> get primarySeries {
  if (_primarySeries is EqualUnmodifiableListView) return _primarySeries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_primarySeries);
}

 final  List<NutritionSeries> _trackedSeries;
@override List<NutritionSeries> get trackedSeries {
  if (_trackedSeries is EqualUnmodifiableListView) return _trackedSeries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_trackedSeries);
}

/// The tracked non-primary nutrients, which the overview groups by family.
 final  List<NutritionSeries> _additionalSeries;
/// The tracked non-primary nutrients, which the overview groups by family.
@override List<NutritionSeries> get additionalSeries {
  if (_additionalSeries is EqualUnmodifiableListView) return _additionalSeries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_additionalSeries);
}

/// The same nutrients, already bucketed into the group headers the overview
/// prints (vitamins, minerals, …) — the screen looks its group up.
 final  Map<NutritionNutrientGroup, List<NutritionSeries>> _additionalSeriesByGroup;
/// The same nutrients, already bucketed into the group headers the overview
/// prints (vitamins, minerals, …) — the screen looks its group up.
@override Map<NutritionNutrientGroup, List<NutritionSeries>> get additionalSeriesByGroup {
  if (_additionalSeriesByGroup is EqualUnmodifiableMapView) return _additionalSeriesByGroup;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_additionalSeriesByGroup);
}

@override final  DailyGoalProgress goalProgress;
@override final  PeriodComparison comparison;
@override final  PersonalBaselineInsight? baselineInsight;
@override final  MacroSplitInterpretation? macroSplit;
/// Data confidence over the keyed metric's tracked days…
@override final  DataConfidence metricConfidence;
/// …and over every day with any nutrition at all (the overview's).
@override final  DataConfidence overviewConfidence;
 final  List<NutritionEntry> _entriesNewestFirst;
@override List<NutritionEntry> get entriesNewestFirst {
  if (_entriesNewestFirst is EqualUnmodifiableListView) return _entriesNewestFirst;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entriesNewestFirst);
}

/// Meals by day, each list newest first — the chart's pinned-day section
/// looks its day up rather than scanning for it.
 final  Map<LocalDate, List<NutritionEntry>> _entriesByDay;
/// Meals by day, each list newest first — the chart's pinned-day section
/// looks its day up rather than scanning for it.
@override Map<LocalDate, List<NutritionEntry>> get entriesByDay {
  if (_entriesByDay is EqualUnmodifiableMapView) return _entriesByDay;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_entriesByDay);
}


/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$NutritionDisplayCopyWith<_NutritionDisplay> get copyWith => __$NutritionDisplayCopyWithImpl<_NutritionDisplay>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _NutritionDisplay&&(identical(other.hasData, hasData) || other.hasData == hasData)&&(identical(other.hasMacros, hasMacros) || other.hasMacros == hasMacros)&&(identical(other.metricSeries, metricSeries) || other.metricSeries == metricSeries)&&const DeepCollectionEquality().equals(other._allSeries, _allSeries)&&const DeepCollectionEquality().equals(other._primarySeries, _primarySeries)&&const DeepCollectionEquality().equals(other._trackedSeries, _trackedSeries)&&const DeepCollectionEquality().equals(other._additionalSeries, _additionalSeries)&&const DeepCollectionEquality().equals(other._additionalSeriesByGroup, _additionalSeriesByGroup)&&(identical(other.goalProgress, goalProgress) || other.goalProgress == goalProgress)&&(identical(other.comparison, comparison) || other.comparison == comparison)&&(identical(other.baselineInsight, baselineInsight) || other.baselineInsight == baselineInsight)&&(identical(other.macroSplit, macroSplit) || other.macroSplit == macroSplit)&&(identical(other.metricConfidence, metricConfidence) || other.metricConfidence == metricConfidence)&&(identical(other.overviewConfidence, overviewConfidence) || other.overviewConfidence == overviewConfidence)&&const DeepCollectionEquality().equals(other._entriesNewestFirst, _entriesNewestFirst)&&const DeepCollectionEquality().equals(other._entriesByDay, _entriesByDay));
}


@override
int get hashCode => Object.hash(runtimeType,hasData,hasMacros,metricSeries,const DeepCollectionEquality().hash(_allSeries),const DeepCollectionEquality().hash(_primarySeries),const DeepCollectionEquality().hash(_trackedSeries),const DeepCollectionEquality().hash(_additionalSeries),const DeepCollectionEquality().hash(_additionalSeriesByGroup),goalProgress,comparison,baselineInsight,macroSplit,metricConfidence,overviewConfidence,const DeepCollectionEquality().hash(_entriesNewestFirst),const DeepCollectionEquality().hash(_entriesByDay));

@override
String toString() {
  return 'NutritionDisplay(hasData: $hasData, hasMacros: $hasMacros, metricSeries: $metricSeries, allSeries: $allSeries, primarySeries: $primarySeries, trackedSeries: $trackedSeries, additionalSeries: $additionalSeries, additionalSeriesByGroup: $additionalSeriesByGroup, goalProgress: $goalProgress, comparison: $comparison, baselineInsight: $baselineInsight, macroSplit: $macroSplit, metricConfidence: $metricConfidence, overviewConfidence: $overviewConfidence, entriesNewestFirst: $entriesNewestFirst, entriesByDay: $entriesByDay)';
}


}

/// @nodoc
abstract mixin class _$NutritionDisplayCopyWith<$Res> implements $NutritionDisplayCopyWith<$Res> {
  factory _$NutritionDisplayCopyWith(_NutritionDisplay value, $Res Function(_NutritionDisplay) _then) = __$NutritionDisplayCopyWithImpl;
@override @useResult
$Res call({
 bool hasData, bool hasMacros, NutritionSeries metricSeries, List<NutritionSeries> allSeries, List<NutritionSeries> primarySeries, List<NutritionSeries> trackedSeries, List<NutritionSeries> additionalSeries, Map<NutritionNutrientGroup, List<NutritionSeries>> additionalSeriesByGroup, DailyGoalProgress goalProgress, PeriodComparison comparison, PersonalBaselineInsight? baselineInsight, MacroSplitInterpretation? macroSplit, DataConfidence metricConfidence, DataConfidence overviewConfidence, List<NutritionEntry> entriesNewestFirst, Map<LocalDate, List<NutritionEntry>> entriesByDay
});


@override $NutritionSeriesCopyWith<$Res> get metricSeries;@override $DailyGoalProgressCopyWith<$Res> get goalProgress;@override $PeriodComparisonCopyWith<$Res> get comparison;@override $PersonalBaselineInsightCopyWith<$Res>? get baselineInsight;@override $MacroSplitInterpretationCopyWith<$Res>? get macroSplit;@override $DataConfidenceCopyWith<$Res> get metricConfidence;@override $DataConfidenceCopyWith<$Res> get overviewConfidence;

}
/// @nodoc
class __$NutritionDisplayCopyWithImpl<$Res>
    implements _$NutritionDisplayCopyWith<$Res> {
  __$NutritionDisplayCopyWithImpl(this._self, this._then);

  final _NutritionDisplay _self;
  final $Res Function(_NutritionDisplay) _then;

/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? hasData = null,Object? hasMacros = null,Object? metricSeries = null,Object? allSeries = null,Object? primarySeries = null,Object? trackedSeries = null,Object? additionalSeries = null,Object? additionalSeriesByGroup = null,Object? goalProgress = null,Object? comparison = null,Object? baselineInsight = freezed,Object? macroSplit = freezed,Object? metricConfidence = null,Object? overviewConfidence = null,Object? entriesNewestFirst = null,Object? entriesByDay = null,}) {
  return _then(_NutritionDisplay(
hasData: null == hasData ? _self.hasData : hasData // ignore: cast_nullable_to_non_nullable
as bool,hasMacros: null == hasMacros ? _self.hasMacros : hasMacros // ignore: cast_nullable_to_non_nullable
as bool,metricSeries: null == metricSeries ? _self.metricSeries : metricSeries // ignore: cast_nullable_to_non_nullable
as NutritionSeries,allSeries: null == allSeries ? _self._allSeries : allSeries // ignore: cast_nullable_to_non_nullable
as List<NutritionSeries>,primarySeries: null == primarySeries ? _self._primarySeries : primarySeries // ignore: cast_nullable_to_non_nullable
as List<NutritionSeries>,trackedSeries: null == trackedSeries ? _self._trackedSeries : trackedSeries // ignore: cast_nullable_to_non_nullable
as List<NutritionSeries>,additionalSeries: null == additionalSeries ? _self._additionalSeries : additionalSeries // ignore: cast_nullable_to_non_nullable
as List<NutritionSeries>,additionalSeriesByGroup: null == additionalSeriesByGroup ? _self._additionalSeriesByGroup : additionalSeriesByGroup // ignore: cast_nullable_to_non_nullable
as Map<NutritionNutrientGroup, List<NutritionSeries>>,goalProgress: null == goalProgress ? _self.goalProgress : goalProgress // ignore: cast_nullable_to_non_nullable
as DailyGoalProgress,comparison: null == comparison ? _self.comparison : comparison // ignore: cast_nullable_to_non_nullable
as PeriodComparison,baselineInsight: freezed == baselineInsight ? _self.baselineInsight : baselineInsight // ignore: cast_nullable_to_non_nullable
as PersonalBaselineInsight?,macroSplit: freezed == macroSplit ? _self.macroSplit : macroSplit // ignore: cast_nullable_to_non_nullable
as MacroSplitInterpretation?,metricConfidence: null == metricConfidence ? _self.metricConfidence : metricConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence,overviewConfidence: null == overviewConfidence ? _self.overviewConfidence : overviewConfidence // ignore: cast_nullable_to_non_nullable
as DataConfidence,entriesNewestFirst: null == entriesNewestFirst ? _self._entriesNewestFirst : entriesNewestFirst // ignore: cast_nullable_to_non_nullable
as List<NutritionEntry>,entriesByDay: null == entriesByDay ? _self._entriesByDay : entriesByDay // ignore: cast_nullable_to_non_nullable
as Map<LocalDate, List<NutritionEntry>>,
  ));
}

/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$NutritionSeriesCopyWith<$Res> get metricSeries {
  
  return $NutritionSeriesCopyWith<$Res>(_self.metricSeries, (value) {
    return _then(_self.copyWith(metricSeries: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DailyGoalProgressCopyWith<$Res> get goalProgress {
  
  return $DailyGoalProgressCopyWith<$Res>(_self.goalProgress, (value) {
    return _then(_self.copyWith(goalProgress: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PeriodComparisonCopyWith<$Res> get comparison {
  
  return $PeriodComparisonCopyWith<$Res>(_self.comparison, (value) {
    return _then(_self.copyWith(comparison: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$PersonalBaselineInsightCopyWith<$Res>? get baselineInsight {
    if (_self.baselineInsight == null) {
    return null;
  }

  return $PersonalBaselineInsightCopyWith<$Res>(_self.baselineInsight!, (value) {
    return _then(_self.copyWith(baselineInsight: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$MacroSplitInterpretationCopyWith<$Res>? get macroSplit {
    if (_self.macroSplit == null) {
    return null;
  }

  return $MacroSplitInterpretationCopyWith<$Res>(_self.macroSplit!, (value) {
    return _then(_self.copyWith(macroSplit: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<$Res> get metricConfidence {
  
  return $DataConfidenceCopyWith<$Res>(_self.metricConfidence, (value) {
    return _then(_self.copyWith(metricConfidence: value));
  });
}/// Create a copy of NutritionDisplay
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DataConfidenceCopyWith<$Res> get overviewConfidence {
  
  return $DataConfidenceCopyWith<$Res>(_self.overviewConfidence, (value) {
    return _then(_self.copyWith(overviewConfidence: value));
  });
}
}

// dart format on
