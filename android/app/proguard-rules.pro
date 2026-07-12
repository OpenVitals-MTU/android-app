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

# ── Health Connect (carried over from the Kotlin app's rules) ─────────────────
-keep class androidx.health.connect.** { *; }

# ── Kotlin coroutines (carried over) ──────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
