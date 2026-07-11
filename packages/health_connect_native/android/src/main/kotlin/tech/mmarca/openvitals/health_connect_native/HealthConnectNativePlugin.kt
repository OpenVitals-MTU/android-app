@file:Suppress("UNCHECKED_CAST")

package tech.mmarca.openvitals.health_connect_native

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import kotlin.reflect.KClass

/**
 * Flutter plugin bridging to the AndroidX Health Connect Kotlin client.
 *
 * STAGE 2: fully implements [HealthConnectHostApi]. Each `@async` method launches
 * a coroutine on a [Dispatchers.Main] + [SupervisorJob] scope, performs the
 * suspending Health Connect call on [Dispatchers.IO], and completes the Pigeon
 * callback with `Result.success`/`Result.failure`. Records cross the bridge as
 * JSON strings per the schema documented in `lib/health_connect_native.dart`.
 */
class HealthConnectNativePlugin :
  FlutterPlugin,
  ActivityAware,
  HealthConnectHostApi {

  private var applicationContext: Context? = null
  private var activityBinding: ActivityPluginBinding? = null
  private var activity: Activity? = null

  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  /**
   * Ported Health Connect infrastructure (see the `HealthConnect*` files in this
   * package). Instantiated once the application context is available in
   * [onAttachedToEngine]. [syncGate] is created eagerly because it holds no
   * context and Dart may call `setSyncEnabled` before any read.
   */
  private val syncGate = HealthConnectSyncGate()
  private var diagnostics: HealthConnectDiagnostics? = null
  private var availabilityService: HealthConnectAvailabilityService? = null
  private var readerSupport: HealthConnectReaderSupport? = null

  /** Typed domain readers (populated in [onAttachedToEngine]). */
  private var bodyReader: BodyHealthReader? = null
  private var hydrationReader: HydrationHealthReader? = null
  private var mindfulnessReader: MindfulnessHealthReader? = null
  private var vitalsReader: VitalsHealthReader? = null
  private var cycleReader: CycleHealthReader? = null
  private var heartReader: HeartHealthReader? = null
  private var nutritionReader: NutritionHealthReader? = null
  private var sleepReader: SleepHealthReader? = null
  private var activityReader: ActivityHealthReader? = null

  private fun requireBodyReader(): BodyHealthReader =
    bodyReader ?: throw IllegalStateException("Plugin not attached to an engine")

  private fun requireHydrationReader(): HydrationHealthReader =
    hydrationReader ?: throw IllegalStateException("Plugin not attached to an engine")

  private fun requireMindfulnessReader(): MindfulnessHealthReader =
    mindfulnessReader ?: throw IllegalStateException("Plugin not attached to an engine")

  private fun requireVitalsReader(): VitalsHealthReader =
    vitalsReader ?: throw IllegalStateException("Plugin not attached to an engine")

  private fun requireCycleReader(): CycleHealthReader =
    cycleReader ?: throw IllegalStateException("Plugin not attached to an engine")

  private fun requireHeartReader(): HeartHealthReader =
    heartReader ?: throw IllegalStateException("Plugin not attached to an engine")

  private fun requireNutritionReader(): NutritionHealthReader =
    nutritionReader ?: throw IllegalStateException("Plugin not attached to an engine")

  private fun requireSleepReader(): SleepHealthReader =
    sleepReader ?: throw IllegalStateException("Plugin not attached to an engine")

  private fun requireActivityReader(): ActivityHealthReader =
    activityReader ?: throw IllegalStateException("Plugin not attached to an engine")

  /** Pending Health Connect permission request state (single in-flight request). */
  private var pendingPermissionCallback: ((Result<Boolean>) -> Unit)? = null
  private var pendingPermissions: List<String> = emptyList()

  /**
   * Launcher for the Health Connect permission contract, registered against the
   * host [ComponentActivity]'s `ActivityResultRegistry` on attach.
   *
   * In modern connect-client (Android 14+), Health Connect permissions are
   * runtime permissions and `createRequestPermissionResultContract()` returns
   * the Activity-Result-API contract; its intent
   * (`androidx.activity.result.contract.action.REQUEST_PERMISSIONS`) is a
   * sentinel that MUST be dispatched through a registered launcher, not via
   * `startActivityForResult` (which throws `ActivityNotFoundException`).
   */
  private var permissionLauncher: ActivityResultLauncher<Set<String>>? = null

  // ---------------------------------------------------------------------------
  // FlutterPlugin
  // ---------------------------------------------------------------------------

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    val context = binding.applicationContext
    applicationContext = context
    val diag = HealthConnectDiagnostics(context)
    diagnostics = diag
    availabilityService = HealthConnectAvailabilityService(context, diag)
    val support = HealthConnectReaderSupport(
      clientProvider = { client() },
      diagnostics = diag,
      syncEnabled = { syncGate.isEnabled },
    )
    readerSupport = support
    bodyReader = BodyHealthReader(support, context.packageName)
    hydrationReader = HydrationHealthReader(support, context.packageName)
    mindfulnessReader = MindfulnessHealthReader(support, context.packageName)
    vitalsReader = VitalsHealthReader(support, context.packageName)
    cycleReader = CycleHealthReader(support)
    heartReader = HeartHealthReader(support)
    nutritionReader = NutritionHealthReader(support, context.packageName)
    sleepReader = SleepHealthReader(support)
    activityReader = ActivityHealthReader(support, context.packageName)
    HealthConnectHostApi.setUp(binding.binaryMessenger, this)
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    HealthConnectHostApi.setUp(binding.binaryMessenger, null)
    applicationContext = null
    diagnostics = null
    availabilityService = null
    readerSupport = null
    bodyReader = null
    hydrationReader = null
    mindfulnessReader = null
    vitalsReader = null
    cycleReader = null
    heartReader = null
    nutritionReader = null
    sleepReader = null
    activityReader = null
    scope.cancel()
  }

  // ---------------------------------------------------------------------------
  // ActivityAware — needed for the permission-request contract.
  // ---------------------------------------------------------------------------

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityBinding = binding
    activity = binding.activity
    registerPermissionLauncher(binding.activity)
    Log.i(TAG, "onAttachedToActivity: ${binding.activity}")
  }

  /**
   * Registers (or re-registers) the Health Connect permission launcher against
   * the host activity's `ActivityResultRegistry`. The no-lifecycle
   * `register(key, contract, callback)` overload may be called at any time
   * (after the activity is already RESUMED, which is our case as a plugin), and
   * returns a launcher we can `launch(...)` immediately.
   */
  private fun registerPermissionLauncher(activity: Activity) {
    val componentActivity = activity as? ComponentActivity
    if (componentActivity == null) {
      Log.w(TAG, "activity is not a ComponentActivity; permission requests unavailable")
      return
    }
    permissionLauncher?.unregister()
    permissionLauncher = componentActivity.activityResultRegistry.register(
      "tech.mmarca.openvitals.health_connect_native.permissions",
      PermissionController.createRequestPermissionResultContract(),
    ) { granted: Set<String> ->
      val callback = pendingPermissionCallback
      val requested = pendingPermissions
      pendingPermissionCallback = null
      pendingPermissions = emptyList()
      Log.i(TAG, "permission result: granted ${granted.size} of ${requested.size} requested")
      callback?.invoke(
        Result.success(requested.isNotEmpty() && granted.containsAll(requested)),
      )
    }
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onDetachedFromActivity() {
    permissionLauncher?.unregister()
    permissionLauncher = null
    activityBinding = null
    activity = null
  }

  // ---------------------------------------------------------------------------
  // HealthConnectHostApi
  // ---------------------------------------------------------------------------

  override fun getSdkStatus(): Long {
    val context = applicationContext
      ?: throw IllegalStateException("Plugin not attached to an engine")
    return HealthConnectClient.getSdkStatus(context).toLong()
  }

  override fun availabilityDetail(): HealthConnectAvailabilityDetail {
    val service = availabilityService
      ?: throw IllegalStateException("Plugin not attached to an engine")
    return HealthConnectAvailabilityDetail(
      sdkStatus = service.sdkStatus().toLong(),
      unsupportedProfile = service.isUnsupportedProfile(),
      standaloneNeedsPlayStore = service.standaloneNeedsPlayStore(),
    )
  }

  override fun setSyncEnabled(enabled: Boolean) {
    syncGate.setEnabled(enabled)
  }

  override fun getSyncEnabled(): Boolean = syncGate.isEnabled

  // ---------------------------------------------------------------------------
  // Body (Phase 1) — delegate to BodyHealthReader, returning typed *Msg classes.
  // ---------------------------------------------------------------------------

  override fun readWeightEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<WeightEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireBodyReader().readWeightEntries(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readLatestWeight(callback: (Result<WeightEntryMsg?>) -> Unit) =
    launchCatching(callback) { requireBodyReader().readLatestWeight() }

  override fun readHeightEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<HeightEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireBodyReader().readHeightEntries(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readLatestHeightEntry(callback: (Result<HeightEntryMsg?>) -> Unit) =
    launchCatching(callback) { requireBodyReader().readLatestHeightEntry() }

  override fun readBodyFatEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<BodyFatEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireBodyReader().readBodyFatEntries(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readLatestBodyFat(callback: (Result<BodyFatEntryMsg?>) -> Unit) =
    launchCatching(callback) { requireBodyReader().readLatestBodyFat() }

  override fun readLeanBodyMassEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<BodyMassEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireBodyReader().readLeanBodyMassEntries(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readLatestLeanBodyMass(callback: (Result<BodyMassEntryMsg?>) -> Unit) =
    launchCatching(callback) { requireBodyReader().readLatestLeanBodyMass() }

  override fun readBmrEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<BmrEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireBodyReader().readBmrEntries(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readLatestBmr(callback: (Result<BmrEntryMsg?>) -> Unit) =
    launchCatching(callback) { requireBodyReader().readLatestBmr() }

  override fun readBoneMassEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<BodyMassEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireBodyReader().readBoneMassEntries(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readLatestBoneMass(callback: (Result<BodyMassEntryMsg?>) -> Unit) =
    launchCatching(callback) { requireBodyReader().readLatestBoneMass() }

  override fun readBodyWaterMassEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<BodyMassEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireBodyReader().readBodyWaterMassEntries(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readLatestBodyWaterMass(callback: (Result<BodyMassEntryMsg?>) -> Unit) =
    launchCatching(callback) { requireBodyReader().readLatestBodyWaterMass() }

  override fun writeBodyMeasurementEntry(
    request: BodyMeasurementWriteRequestMsg,
    callback: (Result<String>) -> Unit,
  ) = launchCatching(callback) { requireBodyReader().writeBodyMeasurementEntry(request) }

  override fun readBodyMeasurementEntry(
    type: BodyMeasurementTypeMsg,
    id: String,
    callback: (Result<BodyMeasurementEntryMsg?>) -> Unit,
  ) = launchCatching(callback) { requireBodyReader().readBodyMeasurementEntry(type, id) }

  override fun updateBodyMeasurementEntry(
    id: String,
    request: BodyMeasurementWriteRequestMsg,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) { requireBodyReader().updateBodyMeasurementEntry(id, request) }

  override fun deleteBodyMeasurementEntry(
    type: BodyMeasurementTypeMsg,
    id: String,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) { requireBodyReader().deleteBodyMeasurementEntry(type, id) }

  // ---------------------------------------------------------------------------
  // Hydration (Phase 2)
  // ---------------------------------------------------------------------------

  override fun readHydrationLiters(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<Double?>) -> Unit,
  ) = launchCatching(callback) {
    requireHydrationReader().readHydrationLiters(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readDailyHydration(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<DailyHydrationMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireHydrationReader().readDailyHydration(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readHydrationEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<HydrationEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireHydrationReader().readHydrationEntries(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readHydrationEntry(
    id: String,
    callback: (Result<HydrationEntryMsg?>) -> Unit,
  ) = launchCatching(callback) { requireHydrationReader().readHydrationEntry(id) }

  override fun writeHydrationEntry(
    request: HydrationWriteRequestMsg,
    callback: (Result<String>) -> Unit,
  ) = launchCatching(callback) { requireHydrationReader().writeHydrationEntry(request) }

  override fun updateHydrationEntry(
    id: String,
    request: HydrationWriteRequestMsg,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) { requireHydrationReader().updateHydrationEntry(id, request) }

  override fun deleteHydrationEntry(
    id: String,
    callback: (Result<String?>) -> Unit,
  ) = launchCatching(callback) { requireHydrationReader().deleteHydrationEntry(id) }

  // ---------------------------------------------------------------------------
  // Mindfulness (Phase 2)
  // ---------------------------------------------------------------------------

  override fun readMindfulnessSessions(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<MindfulnessSessionMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireMindfulnessReader().readMindfulnessSessions(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun readMindfulnessSession(
    id: String,
    callback: (Result<MindfulnessSessionMsg?>) -> Unit,
  ) = launchCatching(callback) { requireMindfulnessReader().readMindfulnessSession(id) }

  override fun readMindfulnessMinutes(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<Long>) -> Unit,
  ) = launchCatching(callback) {
    requireMindfulnessReader().readMindfulnessMinutes(
      Instant.ofEpochMilli(startEpochMs),
      Instant.ofEpochMilli(endEpochMs),
    )
  }

  override fun writeMindfulnessSessionEntry(
    request: MindfulnessSessionWriteRequestMsg,
    callback: (Result<String>) -> Unit,
  ) = launchCatching(callback) { requireMindfulnessReader().writeMindfulnessSessionEntry(request) }

  override fun updateMindfulnessSessionEntry(
    id: String,
    request: MindfulnessSessionWriteRequestMsg,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) {
    requireMindfulnessReader().updateMindfulnessSessionEntry(id, request)
  }

  override fun deleteMindfulnessSessionEntry(
    id: String,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) { requireMindfulnessReader().deleteMindfulnessSessionEntry(id) }

  // ---------------------------------------------------------------------------
  // Vitals (Phase 3)
  // ---------------------------------------------------------------------------

  private fun instant(startEpochMs: Long) = Instant.ofEpochMilli(startEpochMs)

  override fun readBloodPressureEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<BloodPressureEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readBloodPressureEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readLatestBloodPressure(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<BloodPressureEntryMsg?>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readLatestBloodPressure(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readSpO2Entries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<SpO2EntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readSpO2Entries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readLatestSpO2(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<SpO2EntryMsg?>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readLatestSpO2(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readRespiratoryRateEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<RespiratoryRateEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readRespiratoryRateEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readBodyTemperatureEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<BodyTempEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readBodyTemperatureEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readVo2MaxEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<Vo2MaxEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readVo2MaxEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readLatestVo2Max(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<Vo2MaxEntryMsg?>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readLatestVo2Max(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readBloodGlucoseEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<BloodGlucoseEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readBloodGlucoseEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readSkinTemperatureEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<SkinTemperatureEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireVitalsReader().readSkinTemperatureEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun writeVitalsMeasurementEntry(
    request: VitalsMeasurementWriteRequestMsg,
    callback: (Result<String>) -> Unit,
  ) = launchCatching(callback) { requireVitalsReader().writeVitalsMeasurementEntry(request) }

  override fun readVitalsMeasurementEntry(
    type: VitalsMeasurementTypeMsg,
    id: String,
    callback: (Result<VitalsMeasurementEntryMsg?>) -> Unit,
  ) = launchCatching(callback) { requireVitalsReader().readVitalsMeasurementEntry(type, id) }

  override fun updateVitalsMeasurementEntry(
    id: String,
    request: VitalsMeasurementWriteRequestMsg,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) { requireVitalsReader().updateVitalsMeasurementEntry(id, request) }

  override fun deleteVitalsMeasurementEntry(
    type: VitalsMeasurementTypeMsg,
    id: String,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) { requireVitalsReader().deleteVitalsMeasurementEntry(type, id) }

  // ---------------------------------------------------------------------------
  // Cycle (Phase 4) — read-only
  // ---------------------------------------------------------------------------

  override fun readMenstruationFlowEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<MenstruationFlowEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireCycleReader().readMenstruationFlowEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readMenstruationPeriods(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<MenstruationPeriodEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireCycleReader().readMenstruationPeriods(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readOvulationTests(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<OvulationTestEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireCycleReader().readOvulationTests(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readCervicalMucusEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<CervicalMucusEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireCycleReader().readCervicalMucusEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readBasalBodyTemperatureEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<BasalBodyTemperatureEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireCycleReader().readBasalBodyTemperatureEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readIntermenstrualBleedingEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<IntermenstrualBleedingEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireCycleReader().readIntermenstrualBleedingEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readSexualActivityEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<SexualActivityEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireCycleReader().readSexualActivityEntries(instant(startEpochMs), instant(endEpochMs))
  }

  // ---------------------------------------------------------------------------
  // Heart (Phase 5)
  // ---------------------------------------------------------------------------

  override fun readAvgHeartRate(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<Long?>) -> Unit,
  ) = launchCatching(callback) {
    requireHeartReader().readAvgHeartRate(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readRawHeartRateSamples(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<HeartRateSampleMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireHeartReader().readRawHeartRateSamples(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readHeartRateAggregatedBuckets(
    startEpochMs: Long,
    endEpochMs: Long,
    bucketMs: Long,
    callback: (Result<List<HeartRateAggBucketMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireHeartReader().readHeartRateAggregatedBuckets(
      instant(startEpochMs),
      instant(endEpochMs),
      bucketMs,
    )
  }

  override fun readDailyHeartRateSummaries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<HeartRateSummaryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireHeartReader().readDailyHeartRateSummaries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readRestingHeartRate(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<Long?>) -> Unit,
  ) = launchCatching(callback) {
    requireHeartReader().readRestingHeartRate(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readRestingHeartRateSamples(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<RestingHeartRateSampleMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireHeartReader().readRestingHeartRateSamples(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readDailyRestingHR(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<DailyRestingHRMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireHeartReader().readDailyRestingHR(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readHrvSamples(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<HrvSampleMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireHeartReader().readHrvSamples(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readDailyHRV(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<DailyHrvMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireHeartReader().readDailyHRV(instant(startEpochMs), instant(endEpochMs))
  }

  // ---------------------------------------------------------------------------
  // Nutrition (Phase 6)
  // ---------------------------------------------------------------------------

  override fun readCaloriesInKcal(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<Double?>) -> Unit,
  ) = launchCatching(callback) {
    requireNutritionReader().readCaloriesInKcal(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readDailyNutrition(
    startEpochMs: Long,
    endEpochMs: Long,
    includeHydration: Boolean,
    includeCalories: Boolean,
    includeEstimatedCalories: Boolean,
    callback: (Result<List<DailyNutritionMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireNutritionReader().readDailyNutrition(
      instant(startEpochMs),
      instant(endEpochMs),
      includeHydration,
      includeCalories,
      includeEstimatedCalories,
    )
  }

  override fun readDailyMacros(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<DailyMacrosMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireNutritionReader().readDailyMacros(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readNutritionEntries(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<NutritionEntryMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireNutritionReader().readNutritionEntries(instant(startEpochMs), instant(endEpochMs))
  }

  override fun writeNutritionEntry(
    request: NutritionWriteRequestMsg,
    callback: (Result<String>) -> Unit,
  ) = launchCatching(callback) { requireNutritionReader().writeNutritionEntry(request) }

  override fun deleteNutritionEntry(
    id: String,
    callback: (Result<String?>) -> Unit,
  ) = launchCatching(callback) { requireNutritionReader().deleteNutritionEntry(id) }

  override fun deleteHydrationNutritionEntry(
    hydrationClientRecordId: String,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) {
    requireNutritionReader().deleteHydrationNutritionEntry(hydrationClientRecordId)
  }

  // ---------------------------------------------------------------------------
  // Sleep (Phase 7)
  // ---------------------------------------------------------------------------

  override fun readSleepSessionsRaw(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<SleepDataMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireSleepReader().readSleepSessionsRaw(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readSleepSessionById(
    id: String,
    callback: (Result<SleepDataMsg?>) -> Unit,
  ) = launchCatching(callback) { requireSleepReader().readSleepSessionById(id) }

  // ---------------------------------------------------------------------------
  // Activity / Exercise (Phase 8)
  // ---------------------------------------------------------------------------

  override fun readExerciseSessions(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<ExerciseDataMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireActivityReader().readExerciseSessions(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readExerciseSessionsWithMetrics(
    startEpochMs: Long,
    endEpochMs: Long,
    includeDistance: Boolean,
    includeSpeed: Boolean,
    callback: (Result<List<ExerciseDataMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireActivityReader().readExerciseSessionsWithMetrics(
      start = instant(startEpochMs),
      end = instant(endEpochMs),
      includeDistance = includeDistance,
      includeSpeed = includeSpeed,
    )
  }

  override fun readExerciseSessionById(
    id: String,
    callback: (Result<ExerciseDataMsg?>) -> Unit,
  ) = launchCatching(callback) { requireActivityReader().readExerciseSessionById(id) }

  override fun readSpeedSamples(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<SpeedSampleMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireActivityReader().readSpeedSamples(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readActivityCadenceSamples(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<ActivityCadenceSampleMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireActivityReader().readActivityCadenceSamples(instant(startEpochMs), instant(endEpochMs))
  }

  override fun readPlannedExerciseSessions(
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<List<PlannedExerciseSessionMsg>>) -> Unit,
  ) = launchCatching(callback) {
    requireActivityReader().readPlannedExerciseSessions(instant(startEpochMs), instant(endEpochMs))
  }

  override fun writePlannedExerciseSession(
    request: PlannedExerciseWriteRequestMsg,
    callback: (Result<String>) -> Unit,
  ) = launchCatching(callback) { requireActivityReader().writePlannedExerciseSession(request) }

  override fun writeActivityEntry(
    request: ActivityWriteRequestMsg,
    callback: (Result<String>) -> Unit,
  ) = launchCatching(callback) { requireActivityReader().writeActivityEntry(request) }

  override fun updateActivityEntry(
    id: String,
    request: ActivityWriteRequestMsg,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) { requireActivityReader().updateActivityEntry(id, request) }

  override fun deleteActivityEntry(
    id: String,
    callback: (Result<Unit>) -> Unit,
  ) = launchCatching(callback) { requireActivityReader().deleteActivityEntry(id) }

  // ---------------------------------------------------------------------------
  // Apple Health import (Phase 9)
  // ---------------------------------------------------------------------------

  override fun insertImportedRecords(
    records: List<ImportRecordMsg>,
    callback: (Result<List<String>>) -> Unit,
  ) = launchCatching(callback) {
    // Failures propagate so the Dart import service can classify duplicates /
    // failures and retry individually (parity with the JSON path it replaces).
    withContext(Dispatchers.IO) {
      client().insertRecords(records.map { ImportRecordsBuilder.build(it) }).recordIdsList
    }
  }

  override fun getGrantedPermissions(
    permissions: List<String>,
    callback: (Result<List<String>>) -> Unit,
  ) {
    launchCatching(callback) {
      val context = applicationContext
        ?: throw IllegalStateException("Plugin not attached to an engine")
      withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          // Android 14+: Health Connect permissions are OS runtime permissions,
          // so checkSelfPermission is the source of truth. Crucially it reports
          // the "additional access" permissions (READ_HEALTH_DATA_IN_BACKGROUND /
          // READ_HEALTH_DATA_HISTORY) as granted, which
          // permissionController.getGrantedPermissions() does not on the
          // OS-module Health Connect. Matches the reference app's
          // HealthConnectPermissionService.grantedPermissions().
          permissions.filter {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
          }
        } else {
          val granted = client().permissionController.getGrantedPermissions()
          permissions.filter { it in granted }
        }
      }
    }
  }

  override fun filterSupportedPermissions(
    permissions: List<String>,
    callback: (Result<List<String>>) -> Unit,
  ) {
    launchCatching(callback) {
      val context = applicationContext
        ?: throw IllegalStateException("Plugin not attached to an engine")
      val packageManager = context.packageManager
      withContext(Dispatchers.IO) {
        // A permission the installed Health Connect provider doesn't define is
        // not present on the device, so getPermissionInfo throws
        // NameNotFoundException — that's our signal it can never be granted
        // (e.g. newer record types the app's connect-client knows but the
        // on-device provider does not).
        permissions.filter { permission ->
          runCatching { packageManager.getPermissionInfo(permission, 0) }.isSuccess
        }
      }
    }
  }

  override fun requestPermissions(
    permissions: List<String>,
    callback: (Result<Boolean>) -> Unit,
  ) {
    val launcher = permissionLauncher
    Log.i(TAG, "requestPermissions: ${permissions.size} perms, launcher=$launcher")
    if (launcher == null) {
      Log.w(TAG, "requestPermissions: no permission launcher (activity not attached)")
      callback(Result.success(false))
      return
    }
    if (permissions.isEmpty()) {
      callback(Result.success(true))
      return
    }
    try {
      pendingPermissionCallback = callback
      pendingPermissions = permissions
      launcher.launch(permissions.toSet())
    } catch (e: Throwable) {
      Log.e(TAG, "requestPermissions: failed to launch permission contract", e)
      pendingPermissionCallback = null
      pendingPermissions = emptyList()
      callback(Result.failure(e))
    }
  }

  override fun openHealthConnectSettings(callback: (Result<Boolean>) -> Unit) {
    val launchContext: Context = activity ?: applicationContext ?: run {
      callback(Result.success(false))
      return
    }
    val packageName = launchContext.packageName
    val onAndroid14Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    // Ordered candidates, mirroring the Kotlin app's HealthConnectIntents:
    //   1. app-specific HC permission page (may be system-only -> SecurityException,
    //      which is caught and skipped),
    //   2. HC settings home (PLATFORM action on Android 14+, standalone-app action
    //      below — the platform one is what resolves on Android 14+),
    //   3. HC manage-data page,
    //   4. plain launch of the Health Connect app.
    val candidates = listOfNotNull(
      Intent(ACTION_MANAGE_HEALTH_PERMISSIONS)
        .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName),
      Intent(
        if (onAndroid14Plus) {
          ACTION_HEALTH_HOME_SETTINGS
        } else {
          HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
        },
      ),
      Intent(
        if (onAndroid14Plus) ACTION_MANAGE_HEALTH_DATA else ACTION_MANAGE_HEALTH_DATA_APK,
      ),
      launchContext.packageManager.getLaunchIntentForPackage(HEALTH_CONNECT_PACKAGE),
    )
    for (intent in candidates) {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
      if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      try {
        launchContext.startActivity(intent)
        Log.i(TAG, "openHealthConnectSettings: launched ${intent.action ?: intent.component}")
        callback(Result.success(true))
        return
      } catch (e: Throwable) {
        Log.w(TAG, "openHealthConnectSettings: ${intent.action} not launchable: ${e.message}")
      }
    }
    callback(Result.success(false))
  }

  override fun getFeatureStatus(
    feature: String,
    callback: (Result<FeatureStatusMsg>) -> Unit,
  ) {
    launchCatching(callback) {
      val featureConstant = when (feature) {
        "SKIN_TEMPERATURE" -> HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE
        "MINDFULNESS_SESSION" -> HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION
        "PLANNED_EXERCISE" -> HealthConnectFeatures.FEATURE_PLANNED_EXERCISE
        "READ_HEALTH_DATA_HISTORY" ->
          HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY
        "READ_HEALTH_DATA_IN_BACKGROUND" ->
          HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
        // Unrecognized key: the caller asked about a feature we don't map, so we
        // can't report on it — surface UNKNOWN (gating treats it as unavailable).
        else -> null
      } ?: return@launchCatching FeatureStatusMsg.UNKNOWN
      val status = withContext(Dispatchers.IO) {
        client().features.getFeatureStatus(featureConstant)
      }
      when (status) {
        HealthConnectFeatures.FEATURE_STATUS_AVAILABLE -> FeatureStatusMsg.AVAILABLE
        HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE -> FeatureStatusMsg.UNAVAILABLE
        else -> FeatureStatusMsg.UNKNOWN
      }
    }
  }

  override fun aggregate(
    aggregateMetrics: List<String>,
    startEpochMs: Long,
    endEpochMs: Long,
    callback: (Result<Map<String, Double?>>) -> Unit,
  ) {
    launchCatching(callback) {
      val specs = aggregateMetrics.mapNotNull { key ->
        HealthAggregateMetrics.specFor(key)?.let { key to it }
      }
      val out = LinkedHashMap<String, Double?>()
      // Unknown metrics resolve to null.
      for (key in aggregateMetrics) out[key] = null
      if (specs.isNotEmpty()) {
        val timeRangeFilter = TimeRangeFilter.between(
          Instant.ofEpochMilli(startEpochMs),
          Instant.ofEpochMilli(endEpochMs),
        )
        val metricSet = specs.map { it.second.metric }.toSet()
        withContext(Dispatchers.IO) {
          val combined = try {
            client().aggregate(AggregateRequest(metricSet, timeRangeFilter))
          } catch (e: Throwable) {
            null
          }
          if (combined != null) {
            for ((key, spec) in specs) {
              out[key] = runCatching { spec.extract(combined) }.getOrNull()
            }
          } else {
            // Fall back to per-metric aggregation so one failing metric
            // (e.g. a missing permission) does not null out the rest.
            for ((key, spec) in specs) {
              out[key] = try {
                spec.extract(client().aggregate(AggregateRequest(setOf(spec.metric), timeRangeFilter)))
              } catch (e: Throwable) {
                null
              }
            }
          }
        }
      }
      out
    }
  }

  override fun aggregateGroupByPeriodJson(
    aggregateMetrics: List<String>,
    startEpochMs: Long,
    endEpochMs: Long,
    bucketType: String,
    callback: (Result<List<String>>) -> Unit,
  ) {
    launchCatching(callback) {
      val specs = aggregateMetrics.mapNotNull { key ->
        HealthAggregateMetrics.specFor(key)?.let { key to it }
      }
      if (specs.isEmpty()) return@launchCatching emptyList()

      val zone = ZoneId.systemDefault()
      val period = when (bucketType) {
        "DAYS" -> Period.ofDays(1)
        "WEEKS" -> Period.ofWeeks(1)
        "MONTHS" -> Period.ofMonths(1)
        "YEARS" -> Period.ofYears(1)
        else -> Period.ofDays(1)
      }
      val timeRangeFilter = TimeRangeFilter.between(
        Instant.ofEpochMilli(startEpochMs).atZone(zone).toLocalDateTime(),
        Instant.ofEpochMilli(endEpochMs).atZone(zone).toLocalDateTime(),
      )
      val metricSet = specs.map { it.second.metric }.toSet()
      withContext(Dispatchers.IO) {
        val buckets = client().aggregateGroupByPeriod(
          AggregateGroupByPeriodRequest(
            metrics = metricSet,
            timeRangeFilter = timeRangeFilter,
            timeRangeSlicer = period,
          ),
        )
        buckets.map { bucket ->
          val values = JSONObject()
          for ((key, spec) in specs) {
            val value = runCatching { spec.extract(bucket.result) }.getOrNull()
            values.put(key, value ?: JSONObject.NULL)
          }
          JSONObject()
            .put("startEpochMs", bucket.startTime.atZone(zone).toInstant().toEpochMilli())
            .put("endEpochMs", bucket.endTime.atZone(zone).toInstant().toEpochMilli())
            .put("values", values)
            .toString()
        }
      }
    }
  }

  override fun aggregateGroupByDurationJson(
    aggregateMetrics: List<String>,
    startEpochMs: Long,
    endEpochMs: Long,
    bucketMinutes: Long,
    callback: (Result<List<String>>) -> Unit,
  ) {
    launchCatching(callback) {
      val specs = aggregateMetrics.mapNotNull { key ->
        HealthAggregateMetrics.specFor(key)?.let { key to it }
      }
      if (specs.isEmpty()) return@launchCatching emptyList()

      // A zero or negative slice would make Health Connect throw.
      val slice = Duration.ofMinutes(bucketMinutes.coerceAtLeast(1L))
      val timeRangeFilter = TimeRangeFilter.between(
        Instant.ofEpochMilli(startEpochMs),
        Instant.ofEpochMilli(endEpochMs),
      )
      val metricSet = specs.map { it.second.metric }.toSet()
      withContext(Dispatchers.IO) {
        val buckets = client().aggregateGroupByDuration(
          AggregateGroupByDurationRequest(
            metrics = metricSet,
            timeRangeFilter = timeRangeFilter,
            timeRangeSlicer = slice,
          ),
        )
        buckets.map { bucket ->
          val values = JSONObject()
          for ((key, spec) in specs) {
            val value = runCatching { spec.extract(bucket.result) }.getOrNull()
            values.put(key, value ?: JSONObject.NULL)
          }
          JSONObject()
            .put("startEpochMs", bucket.startTime.toEpochMilli())
            .put("endEpochMs", bucket.endTime.toEpochMilli())
            .put("values", values)
            .toString()
        }
      }
    }
  }

  override fun filterExistingClientIds(
    recordType: String,
    clientRecordIds: List<String>,
    callback: (Result<List<String>>) -> Unit,
  ) {
    launchCatching(callback) {
      val recordClass = recordClassFor(recordType)
        ?: throw IllegalArgumentException("Unknown record type: $recordType")
      if (clientRecordIds.isEmpty()) return@launchCatching emptyList()

      val wanted = clientRecordIds.toSet()
      // Health Connect cannot query directly by clientRecordId, so read the
      // record type over a wide window and match on metadata.clientRecordId.
      val timeRangeFilter = TimeRangeFilter.between(
        Instant.EPOCH,
        Instant.now().plus(Duration.ofDays(1)),
      )
      withContext(Dispatchers.IO) {
        val found = mutableSetOf<String>()
        var pageToken: String? = null
        do {
          val response = client().readRecords(
            ReadRecordsRequest(
              recordType = recordClass as KClass<Record>,
              timeRangeFilter = timeRangeFilter,
              pageSize = READ_PAGE_SIZE,
              pageToken = pageToken,
            ),
          )
          for (record in response.records) {
            val clientRecordId = record.metadata.clientRecordId ?: continue
            if (clientRecordId in wanted) found.add(clientRecordId)
          }
          if (found.size == wanted.size) break
          pageToken = response.pageToken
        } while (pageToken != null)
        found.toList()
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private fun client(): HealthConnectClient {
    val context = applicationContext
      ?: throw IllegalStateException("Plugin not attached to an engine")
    return HealthConnectClient.getOrCreate(context)
  }

  /**
   * Runs [block] on [scope], delivering its result via [callback] and routing
   * any thrown exception to `Result.failure`.
   */
  private fun <T> launchCatching(
    callback: (Result<T>) -> Unit,
    block: suspend () -> T,
  ) {
    scope.launch {
      try {
        callback(Result.success(block()))
      } catch (e: Throwable) {
        callback(Result.failure(e))
      }
    }
  }

  private companion object {
    private const val TAG = "HealthConnectNative"
    private const val READ_PAGE_SIZE = 1000

    /**
     * System action to open a specific app's Health Connect permission page on
     * Android 14+ (Health Connect as an OS module). Passed the target app's
     * package via [Intent.EXTRA_PACKAGE_NAME]. May be system-only on some
     * devices (third-party launch throws SecurityException).
     */
    private const val ACTION_MANAGE_HEALTH_PERMISSIONS =
      "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS"

    /** Health Connect settings home — platform action (Android 14+). */
    private const val ACTION_HEALTH_HOME_SETTINGS =
      "android.health.connect.action.HEALTH_HOME_SETTINGS"

    /** Health Connect manage-data page — platform action (Android 14+). */
    private const val ACTION_MANAGE_HEALTH_DATA =
      "android.health.connect.action.MANAGE_HEALTH_DATA"

    /** Health Connect manage-data page — standalone-app action (Android 13-). */
    private const val ACTION_MANAGE_HEALTH_DATA_APK =
      "androidx.health.ACTION_MANAGE_HEALTH_DATA"

    private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
  }
}
