# R8/ProGuard keep rules for the release build.
#
# The Kotlin app shipped an `app/proguard-rules.pro`; the Flutter port dropped it,
# and one of the rules it needed was never re-added. See the Glance block below —
# that omission silently broke every beverage widget in release builds.

# ── Glance ActionCallback: keep the CONSTRUCTOR, not just the class ───────────
#
# Glance instantiates an `ActionCallback` reflectively, by class name:
#
#     callbackClass.getDeclaredConstructor().newInstance()
#
# glance-appwidget ships its own consumer rule, but it is not enough:
#
#     -keep public class * extends androidx.glance.appwidget.action.ActionCallback
#
# That keeps the class *name* and nothing else, so R8 is free to strip its
# members — and it does. In the shipped v2.0.2 APK,
# `HomeQuickBeverageLogAction` had an EMPTY method table: no `<init>()`, not even
# `onAction()`. Glance therefore threw, every time a beverage tile was tapped:
#
#     E/GlanceAppWidget: java.lang.NoSuchMethodException:
#       ...HomeQuickBeverageLogAction.<init> []
#
# The tap sent no broadcast, the Dart callback never ran, no drink was ever
# logged, and — because the failure happened before any of our code — the widget
# could not even report it. Both beverage widgets were dead in release and fine in
# debug, which is exactly why it shipped.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
    *;
}

# ── Enums reflected on by name ───────────────────────────────────────────────
#
# audioplayers (`enumValueOf`) and flutter_foreground_task
# (`NotificationPermission.valueOf`) resolve enum constants from strings. Both
# survive in the shipped APK today, but only because the AGP default file happens
# to keep them. Stating it here makes that deliberate rather than accidental --
# the same "it works until it doesn't" gap that killed the Glance callback.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Health Connect (carried over from the Kotlin app's rules) ─────────────────
#
# Worth knowing: this is INSURANCE, not a fix. In the shipped v2.0.2 APK zero
# androidx.health.connect classes survived under their original names -- they were
# fully obfuscated -- and Health Connect still worked, because its record-type map
# keys off string literals ("Steps", "SleepSession", ...), not class names. The
# rule matches the Kotlin app's and costs ~330 KB of dex. Kept for parity; drop it
# if size ever matters.
-keep class androidx.health.connect.** { *; }

# ── Kotlin coroutines (carried over) ──────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
