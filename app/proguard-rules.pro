# Health Connect record class names are a wire format, not just types:
# features/devicesync/store/SyncRecordCodec.kt keys SYNC_RECORD_CLASSES on
# KClass.simpleName and sends that name to the other phone. Obfuscating these
# would rename the protocol — and two app versions could rename them
# differently, so a sync would fail only between builds, only in release.
# Names must survive; members and unused classes need not, which is why this is
# -keepnames and not a blanket -keep ... { *; }. Only the top-level *Record
# classes are keyed on -- companions, aggregate-metric lambdas and helpers in the
# package can be obfuscated freely. The reflection Health Connect itself needs
# is covered by the consumer rules its AAR already ships for
# androidx.health.platform.client.**.
-keepnames class androidx.health.connect.client.records.*Record

# kotlinx-coroutines-core ships its own consumer rules (META-INF/proguard/
# coroutines.pro) covering MainDispatcherFactory and CoroutineExceptionHandler;
# nothing to add here.

# Strip low-value release logs. Warnings and errors remain for operational failures.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# Glance instantiates ActionCallback subclasses reflectively at tap time:
# RunCallbackAction does Class.forName(name).getDeclaredConstructor().newInstance().
# glance-appwidget's bundled consumer rule (-keep public class * extends
# ActionCallback, no member spec) keeps the class name but, under the strict
# full-mode keep rules this build runs with, not the no-arg constructor. R8
# stripped <init>() and every widget tap died in a silent NoSuchMethodException.
-keepclassmembers class * extends androidx.glance.appwidget.action.ActionCallback {
    <init>();
}
