# Keep Health Connect classes
-keep class androidx.health.connect.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Strip low-value release logs. Warnings and errors remain for operational failures.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
