# Health Connect record class names are a wire format, not just types:
# features/devicesync/store/SyncRecordCodec.kt keys SYNC_RECORD_CLASSES on
# KClass.simpleName and sends that name to the other phone. Obfuscating these
# would rename the protocol — and two app versions could rename them
# differently, so a sync would fail only between builds, only in release.
# Names must survive; members and unused classes need not, which is why this is
# -keepnames and not a blanket -keep ... { *; }. The reflection Health Connect
# itself needs is covered by the consumer rules its AAR already ships for
# androidx.health.platform.client.**.
-keepnames class androidx.health.connect.client.records.**

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Strip low-value release logs. Warnings and errors remain for operational failures.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
