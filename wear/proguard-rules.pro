# R8 keep rules for the watch app. Compose and Wear Compose ship their own
# consumer rules; add project rules here only when R8 strips something.

# Debug and verbose logs are for a development build: heart rate, sleep minutes
# and their times go into them. Release and nightly builds drop the calls. Info
# lines stay, since testers follow the step counter with `adb logcat -s StepRecorder`.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
