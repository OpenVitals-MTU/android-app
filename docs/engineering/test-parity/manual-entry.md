## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/body_measurement_entry_command_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/body/BodyMeasurementEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a command at rest is idle | PORTED | BodyMeasurementEntryViewModelTest.kt `a command at rest is idle` | Idle asserted out of the constructor and after the permission probe settles |
| a successful save settles on success, and is consumed once | PORTED | BodyMeasurementEntryViewModelTest.kt `weight entry writes canonical kg value` | Write captured, input cleared, saveCompleted true, consumed via onSaveCompletedHandled |
| a failed save carries the failure to the form, not an exception | PORTED | BodyMeasurementEntryViewModelTest.kt `a failed save carries the failure to the form, not an exception` | Thrown write surfaces on the state, saving cleared, no exception escapes; Kotlin additionally sets entryError=WRITE_FAILED |
| editing a field clears the failure the last attempt left behind | PORTED | BodyMeasurementEntryViewModelTest.kt `editing a field clears the failure the last attempt left behind` | No Kotlin test that updateInput clears a prior write failure |
| validation refuses before the command ever runs | PORTED | BodyMeasurementEntryViewModelTest.kt `invalid body measurement value does not write` | INVALID_VALUE + zero writes; equivalent given Kotlin's boolean state model |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/carbs_entry_command_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/nutrition/CarbsEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a command at rest is idle | PORTED | CarbsEntryViewModelTest.kt `a command at rest is idle` | Kotlin file has no initial-state test at all |
| a successful save settles on success, and is consumed once | PORTED | CarbsEntryViewModelTest.kt `carbs entry writes grams value` | Write, cleared input, saveCompleted, consumed via onSaveCompletedHandled |
| a failed save carries the failure to the form, not an exception | PORTED | CarbsEntryViewModelTest.kt `a failed save carries the failure to the form, not an exception` | Kotlin additionally sets entryError=WRITE_FAILED by design |
| editing a field clears the failure the last attempt left behind | PORTED | CarbsEntryViewModelTest.kt `editing a field clears the failure the last attempt left behind` | No error-clearing-on-edit test |
| validation refuses before the command ever runs | PORTED | CarbsEntryViewModelTest.kt `invalid carbs value does not write` | INVALID_VALUE + zero writes |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/hydration_catalog_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationCatalogTest.kt (over the catalog helpers in HydrationEntryFormContent.kt, widened private->internal)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| uncategorized saved drinks land in the unassigned group | PORTED | HydrationCatalogTest.kt `uncategorized saved drinks land in the unassigned group` | Pure grouping logic, JVM-portable |
| a drink is filed under its category section | PORTED | HydrationCatalogTest.kt `a drink is filed under its category section` |  |
| supplement collapses into the other section, as in Kotlin | PORTED | HydrationCatalogTest.kt `supplement collapses into the other section` |  |
| a session category override beats the drink's persisted category | PORTED | HydrationCatalogTest.kt `a session category override beats the drink's persisted category` |  |
| a frequent drink is not repeated in its section | PORTED | HydrationCatalogTest.kt `a frequent drink is not repeated in its section` |  |
| a frequent drink that is no longer saved is dropped | PORTED | HydrationCatalogTest.kt `a frequent drink that is no longer saved is dropped` |  |
| the search query filters by name, case-insensitively | PORTED | HydrationCatalogTest.kt `the search query filters by name, case-insensitively` |  |
| a query matching nothing leaves the grouping empty | PORTED | HydrationCatalogTest.kt `a query matching nothing leaves the grouping empty` |  |
| a session row order reorders a section, unknown keys keep their place | PORTED | HydrationCatalogTest.kt `a session row order reorders a section, unknown keys keep their place` |  |
| row keys > round-trip a saved drink id | PORTED | HydrationCatalogTest.kt `row keys round-trip a saved drink id` |  |
| row keys > a preset key is not a saved key but still yields its id | PORTED | HydrationCatalogTest.kt `row keys a preset key is not a saved key but still yields its id` |  |
| row keys > an unprefixed key yields nothing | PORTED | HydrationCatalogTest.kt `row keys an unprefixed key yields nothing` |  |
| every category maps to a section and back | PORTED | HydrationCatalogTest.kt `every category maps to a section and back` |  |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/hydration_custom_drink_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationCustomDrinkInputTest.kt (toCustomHydrationDrink + the hydration-impact helpers) and .../HydrationEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| customHydrationDrinkFromInput > normalizes the name and sorts nutrients by enum-constant name | PORTED | HydrationCustomDrinkInputTest.kt `customHydrationDrinkFromInput normalizes the name and sorts nutrients by enum-constant name` | Kotlin `saving custom drink creates reusable drink...` saves a drink but never asserts trimming or nutrient sort order |
| customHydrationDrinkFromInput > rejects a blank name and an out-of-range volume | PORTED | HydrationCustomDrinkInputTest.kt `customHydrationDrinkFromInput rejects a blank name and an out-of-range volume` | No invalid saveCustomDrink test in Kotlin |
| customHydrationDrinkFromInput > one invalid nutrient rejects the whole drink | PORTED | HydrationCustomDrinkInputTest.kt `customHydrationDrinkFromInput one invalid nutrient rejects the whole drink, not just that nutrient` | Size-comparison rule exists in Kotlin toCustomHydrationDrink, untested |
| customHydrationDrinkFromInput > rejects a nutrient value above the maximum | PORTED | HydrationCustomDrinkInputTest.kt `customHydrationDrinkFromInput rejects a nutrient value above the maximum` |  |
| customHydrationDrinkFromInput > rejects a hydration multiplier outside [0, 1] | PORTED | HydrationCustomDrinkInputTest.kt `customHydrationDrinkFromInput rejects a hydration multiplier outside 0 to 1` |  |
| customHydrationDrinkFromInput > keeps a zero-hydration drink (nutrients only) | PORTED | HydrationEntryViewModelTest.kt `zero impact custom drink without nutrients saves reusable drink only` | Zero-multiplier drink is accepted and stored (category preservation covered by the edit test) |
| hydration impact > maps a multiplier back onto its option | PORTED | HydrationCustomDrinkInputTest.kt `hydration impact maps a multiplier back onto its option` | Private hydrationImpactOptionForMultiplier untested |
| hydration impact > partial percent parses only strictly between 0 and 100 | PORTED | HydrationCustomDrinkInputTest.kt `hydration impact partial percent parses only strictly between 0 and 100` |  |
| hydration impact > full and none ignore the percent text | PORTED | HydrationCustomDrinkInputTest.kt `hydration impact full and none ignore the percent text` |  |
| hydration impact > percent text falls back to the default outside the partial range | PORTED | HydrationCustomDrinkInputTest.kt `hydration impact percent text falls back to the default outside the partial range` |  |
| isValidCustomDrinkNutrientValue > accepts (0, max] and nothing else | PORTED | HydrationCustomDrinkInputTest.kt `isValidCustomDrinkNutrientValue accepts 0 exclusive to max inclusive and nothing else` | Kotlin internal fun isValidCustomDrinkNutrientValue untested |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/hydration_drink_usage_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationDrinkUsageTest.kt (over HydrationDrinkUsage.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| hydrationDrinkIdFromClientRecordId > extracts the id between the marker and the next underscore | PORTED | HydrationDrinkUsageTest.kt `hydrationDrinkIdFromClientRecordId extracts the id between the marker and the next underscore` | Only incidental: valid drink client-record ids get counted; no direct parse assertion |
| hydrationDrinkIdFromClientRecordId > returns null without the prefix, marker or a terminator | PORTED | HydrationDrinkUsageTest.kt `hydrationDrinkIdFromClientRecordId returns null without the prefix marker or a terminator` | Negative parse cases untested |
| pairedHydrationClientRecordId > unwraps a paired nutrition record id | PORTED | HydrationDrinkUsageTest.kt `pairedHydrationClientRecordId unwraps a paired nutrition record id` | Paired-prefix path never exercised in Kotlin tests |
| pairedHydrationClientRecordId > returns null for a standalone nutrition record | PORTED | HydrationDrinkUsageTest.kt `pairedHydrationClientRecordId returns null for a standalone nutrition record` | Incidental only: standalone ids must fall through to name matching for the test to pass |
| frequentHydrationDrinkOptions > ranks by log count, most frequent first | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions ranks by log count most frequent first` | coffee(3) > water(2) > tea(1) ordering asserted |
| frequentHydrationDrinkOptions > breaks a count tie on the most recent log | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions breaks a count tie on the most recent log` |  |
| frequentHydrationDrinkOptions > breaks a count+recency tie on the saved order | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions breaks a count and recency tie on the saved order` |  |
| frequentHydrationDrinkOptions > ignores entries for unknown or deleted drinks | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions ignores entries for unknown or deleted drinks` |  |
| frequentHydrationDrinkOptions > does not double-count a hydration record and its paired nutrition | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions does not double-count a hydration record and its paired nutrition` |  |
| frequentHydrationDrinkOptions > counts a paired nutrition record whose hydration half never wrote | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions counts a paired nutrition record whose hydration half never wrote` |  |
| frequentHydrationDrinkOptions > falls back to matching a standalone nutrition entry by drink name | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions falls back to matching a standalone nutrition entry by drink name` | "Coffee"/"coffee" nutrition entries counted case-insensitively by name |
| frequentHydrationDrinkOptions > ignores entries from other apps | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions ignores entries from other apps` |  |
| frequentHydrationDrinkOptions > caps the list at the frequent-drink limit | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions caps the list at the frequent-drink limit` | FrequentHydrationDrinkLimit=6 untested |
| frequentHydrationDrinkOptions > is empty when there are no saved drinks | PORTED | HydrationDrinkUsageTest.kt `frequentHydrationDrinkOptions is empty when there are no saved drinks` |  |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/hydration_entry_command_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a command at rest is idle | PORTED | HydrationEntryViewModelTest.kt `a command at rest is idle` | Boot state asserted only for the permission probe, not save-at-rest/no-error |
| a successful save settles on success, and is consumed once | PORTED | HydrationEntryViewModelTest.kt `custom hydration entry writes exact custom amount` + `selected hydration entry writes selected container volume` | Write, today-total update, saveCompleted; consume cycle asserted in the latter |
| a failed save carries the failure to the form, not an exception | PORTED | HydrationEntryViewModelTest.kt `write failure clears saving and exposes entry error` | Thrown write surfaces ScreenError.Message and clears isSavingEntry (Kotlin additionally sets entryError=WRITE_FAILED by design) |
| editing a field clears the failure the last attempt left behind | PORTED | HydrationEntryViewModelTest.kt `editing a field clears the failure the last attempt left behind` | A failed write then updateEntryTime clears entryError/writeError |
| validation refuses before the command ever runs | PORTED | HydrationEntryViewModelTest.kt `invalid custom hydration entry is rejected` | INVALID_AMOUNT + zero writes |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/hydration_entry_screen_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationEntryViewModelTest.kt (logic) and /home/manu/Documentos/repos/openvitals-android/app/src/androidTest/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationEntryFormTest.kt (minimal render)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| creates a custom drink and logs it from the saved list | PORTED | HydrationEntryViewModelTest.kt `saving custom drink creates reusable drink without writing entry` + `saved custom drink entry writes hydration nutrients` | Save-then-log flow incl. drinkId tagging covered; dialog composition (radio options, category dropdown) is rendering-only, uncovered |
| logging a drink keeps the catalog open for the next one | PORTED | HydrationCatalogStaysOpenTest: `loggingADrinkKeepsTheCatalogOpenForTheNextOne` | Compose instrumentation; runs on a device, not in CI |
| editing an existing entry returns after saving | PORTED | HydrationEntryViewModelTest.kt `editing an existing entry loads it and updates instead of writing` | VM half covered; the pop-back is Compose navigation |
| an imperial user types fluid ounces and stores millilitres | PORTED | HydrationEntryFormContentTest.kt `imperial hydration input converts fluid ounces to milliliters` + `imperial initial hydration amount displays fluid ounces` | Canonical-ml conversion and fl-oz display round-trip covered as unit helpers |
| rejects an out-of-range custom drink volume | PORTED | HydrationEntryViewModelTest.kt `rejects an out-of-range custom drink volume` | toCustomHydrationDrink volume validation untested in Kotlin |
| a partial-hydration drink needs a percent strictly under 100 | PORTED | HydrationCustomDrinkInputTest.kt `hydration impact partial percent parses only strictly between 0 and 100` | Private hydrationImpactMultiplier bounds logic untested |
| the logDrinkId deep link opens that drink's entry dialog | PORTED | HydrationEntryCatalogTest: `aLogDrinkIdDeepLinkOpensThatDrinksEntryDialog` | Compose instrumentation; runs on a device, not in CI |
| an unknown logDrinkId opens the plain form | PORTED | HydrationEntryCatalogTest: `anUnknownLogDrinkIdOpensThePlainCatalog` | Compose instrumentation; runs on a device, not in CI |
| shows today's hydration against the daily goal | PORTED | HydrationEntryCatalogTest: `showsTodaysHydrationAgainstTheDailyGoal` | Compose instrumentation; runs on a device, not in CI |
| category sections start collapsed and expand on tap | PORTED | HydrationEntryCatalogTest: `categorySectionsStartCollapsedAndExpandOnTap` | Compose instrumentation; runs on a device, not in CI |
| searching force-expands the sections and filters the rows | PORTED | HydrationEntryCatalogTest: `searchingForceExpandsTheSectionsAndFiltersTheRows` | Compose instrumentation; runs on a device, not in CI |
| the edit toggle swaps logging for edit/move/delete actions | PORTED | HydrationEntryCatalogTest: `theEditToggleSwapsLoggingForEditMoveAndDeleteActions` | Compose instrumentation; runs on a device, not in CI |
| a frequently-logged drink surfaces in its own section | PORTED | HydrationCatalogTest.kt `a frequent drink is not repeated in its section` | Frequent ranking and the not-repeated-under-its-category grouping both asserted |
| a drink logged only via a partial amount still hits the catalog | DIVERGED | HydrationEntryViewModelTest.kt `zero impact saved custom drink writes nutrients without hydration entry` | Zero-hydration logging covered; catalog visibility and "Does not count as hydration" labeling not |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/hydration_entry_view_model_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| saveCustomDrink persists a new drink and reloads the options | PORTED | HydrationEntryViewModelTest.kt `saving custom drink creates reusable drink without writing entry` | Persisted, options reloaded, no entry written |
| saveCustomDrink on an invalid input reports invalidCustomDrink | PORTED | HydrationEntryViewModelTest.kt `saveCustomDrink on an invalid input reports invalidCustomDrink` | No INVALID_CUSTOM_DRINK test in Kotlin |
| editing a drink keeps its id and preloaded flag | PORTED | HydrationEntryViewModelTest.kt `editing a drink keeps its id and preloaded flag` | Id retention and field updates asserted; isPreloaded survival not asserted |
| deleteCustomDrink removes it from storage and state | PORTED | HydrationEntryViewModelTest.kt `delete custom drink removes saved drink` | |
| moveCustomDrinkToTarget drops the drink onto the target slot | PORTED | HydrationEntryViewModelTest.kt `move custom drink to target reorders and persists` | Same drop-on-target semantics and persisted order |
| moveCustomDrinkToTarget is a no-op on an unknown or self target | PORTED | HydrationEntryViewModelTest.kt `moveCustomDrinkToTarget is a no-op on an unknown or self target` |  |
| moveCustomDrinkToCategory persists and reloads | PORTED | HydrationEntryViewModelTest.kt `moveCustomDrinkToCategory persists and reloads` | Kotlin fun moveCustomDrinkToCategory exists (HydrationEntryViewModel.kt:352) but has no test |
| updateContainerSize persists a default preset and selects it | PORTED | HydrationEntryViewModelTest.kt `container size update changes and selects preset option` | |
| updateContainerSize rejects an out-of-range volume | PORTED | HydrationEntryViewModelTest.kt `invalid container size is rejected` | |
| an ad-hoc container resize is session-only, never persisted | PORTED | HydrationEntryViewModelTest.kt `an ad-hoc container resize is session-only, never persisted` | Non-default-container (session-only) branch untested |
| addSavedCustomDrinkEntry honours the requested entry time | PORTED | HydrationEntryViewModelTest.kt `saved custom drink entry scales nutrients for selected portion and time` | request.time == entryTime asserted |
| addSavedCustomDrinkEntry scales nutrients to a partial amount | PORTED | HydrationEntryViewModelTest.kt `saved custom drink entry scales nutrients for selected portion and time` | 2.5x nutrient scaling + scaled volume asserted |
| refreshDailyGoal re-reads the persisted goal | PORTED | HydrationEntryViewModelTest.kt `refreshDailyGoal re-reads the persisted goal` | Initial read covered; refreshDailyGoal() re-read (HydrationEntryViewModel.kt:188) untested |
| a saved entry re-plans the reminder | PORTED | HydrationEntryViewModelTest.kt `a saved entry re-plans the reminder` | Driven through a real HydrationReminderController - a MockK'd controller cannot record applyConfig()'s defaulted argument |
| a rejected entry does not touch the reminder | PORTED | HydrationEntryViewModelTest.kt `a rejected entry does not touch the reminder` | No Kotlin test that an invalid entry leaves the reminder controller untouched |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/hydration_seeded_catalog_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/data/local/beverage/BeverageEntityTest.kt (partial)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the drink catalog is seeded from the CaffeineHealth presets | DIVERGED | BeverageEntityTest.kt `preloaded defaults include water category drinks` | Only the two waters checked; seeded count and preset names (Drip coffee, Espresso) unasserted |
| seeded drinks are marked preloaded and carry their category | DIVERGED | BeverageEntityTest.kt `preloaded defaults include water category drinks` | isPreloaded/category asserted for waters only; no catalog item (volume 240 ml, caffeine nutrient) checked |
| supplements and servingless items are excluded from the seed | PORTED | HydrationSeededCatalogTest.kt: `supplements and servingless items are excluded from the seed` | — |
| a user drink is saved to the store and read back with the seed | PORTED | HydrationSeededCatalogTest.kt: `a user drink is saved to the store and read back with the seed` | — |
| deleting and recategorizing round-trip through the store | PORTED | HydrationSeededCatalogTest.kt: `deleting and recategorizing round-trip through the store` | — |
| the entry notifier surfaces the seeded catalog | PORTED | HydrationSeededCatalogTest.kt: `the entry view model surfaces the seeded catalog` | — |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/manual_entry_forms_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/body/BodyMeasurementEntryViewModelTest.kt, .../vitals/VitalsMeasurementEntryViewModelTest.kt, /home/manu/Documentos/repos/openvitals-android/app/src/androidTest/kotlin/tech/mmarca/openvitals/features/manualentry/hydration/HydrationEntryFormTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Body weight form writes the expected request on save | PORTED | BodyMeasurementEntryViewModelTest.kt `weight entry writes canonical kg value` | Same type/value write request asserted at VM level |
| Body weight form blocks the write on invalid input | PORTED | BodyMeasurementEntryViewModelTest.kt `invalid body measurement value does not write` | Error + zero writes; the localized error copy itself is rendering-only |
| Blood pressure form writes systolic + diastolic on save | PORTED | VitalsMeasurementEntryViewModelTest.kt `blood pressure entry writes systolic and diastolic values` | |
| Blood pressure form blocks the write when systolic <= diastolic | PORTED | VitalsMeasurementEntryViewModelTest.kt `invalid vitals value does not write` | Uses 70/90 (systolic < diastolic), same guard |
| Hydration form shows the tracker card, not container presets | DIVERGED | HydrationEntryFormTest.kt `hydrationEntryForm_rendersTrackerCard` | Only asserts the tracker tag exists; absence of container chips/custom-amount field and presence of catalog search unasserted |
| re-checks the write permission when the screen resumes | PORTED | CarbsEntryResumeTest: `aPermissionGrantedWhileAwayIsPickedUpWhenTheScreenComesBack` | Compose instrumentation; runs on a device, not in CI |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/manual_entry_screen_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/ManualEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| shows every entry type the device supports | PORTED | ManualEntryViewModelTest.kt `manual entry uses default widget order when preferences are empty` | Default widget set asserted at VM level |
| hides entry types the provider cannot accept writes for | N/A-BEHAVIOR | — | The Kotlin manual-entry grid does not filter tiles by write permission - no such behavior to test |
| hides mindfulness when the feature is unavailable | N/A-BEHAVIOR | — | The Kotlin grid always shows the mindfulness tile; unavailability is reported inside the entry screen |
| edit mode removes a tile to the tray and adds it back | PORTED | ManualEntryViewModelTest.kt `removing manual entry widget persists order` + `adding manual entry widget persists order` | Persisted order on remove/add covered; tray rendering is UI |
| tiles are draggable only while editing | N/A-WIDGET | — | Drag affordance is rendering; edit-mode toggle covered by `manual entry widget edit toggles` |
| a stored order drives which tiles show and in what order | PORTED | ManualEntryViewModelTest.kt `manual entry widget order loads from preferences` | Stored order drives visible widget list; tray contents of removed tiles derived in UI |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/mindfulness_entry_command_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/mindfulness/MindfulnessEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a command at rest is idle | PORTED | MindfulnessEntryViewModelTest.kt `initial load checks write permission` | Boot state asserts not-saving, save-not-completed and entryError/writeError null |
| a successful save settles on success, and is consumed once | PORTED | MindfulnessEntryViewModelTest.kt `manual entry writes mindfulness session duration` | Write, cleared field, saveCompleted, consumed |
| a failed save carries the failure to the form, not an exception | PORTED | MindfulnessEntryViewModelTest.kt `failed manual save carries failure to the form` | WRITE_FAILED + ScreenError.Message, saving cleared, no exception escapes |
| editing a field clears the failure the last attempt left behind | PORTED | MindfulnessEntryViewModelTest.kt `editing a field clears prior save failure` |  |
| validation refuses before the command ever runs | PORTED | MindfulnessEntryViewModelTest.kt `invalid manual entry does not write` | INVALID_MANUAL_ENTRY + zero writes |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/mindfulness_entry_screen_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/mindfulness/MindfulnessEntryViewModelTest.kt (logic only; no Compose test for the mindfulness screen)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| shows the timer, its sound pickers and the manual card | PORTED | MindfulnessEntryContentTest: `showsTheTimerItsSoundPickersAndTheManualCard` | Compose instrumentation; runs on a device, not in CI |
| picking a bell previews it | PORTED | MindfulnessEntryViewModelTest.kt `changing bell sound emits short preview` | Preview event (sound + previewMillis) asserted; audio playback itself is UI glue |
| picking an ambient sound previews it; "none" stops it | PORTED | MindfulnessEntryViewModelTest.kt `changing background sound emits short preview` + `selecting no background sound clears background preview` | |
| starting the timer swaps the controls and loops the ambient | DIVERGED | MindfulnessEntryViewModelTest.kt `stopping timer pauses with resume save and discard state` | Start/stop/pause state machine covered; ambient loop start/stop calls and control swap are unasserted UI |
| discarding rewinds the countdown and restores the pickers | PORTED | MindfulnessEntryViewModelTest.kt `discard rewinds timer to configured duration` | Picker restore is UI |
| a session shorter than a minute is refused | PORTED | MindfulnessEntryViewModelTest.kt `timer session under a minute is rejected not rounded to zero` | No timer-too-short rejection test in Kotlin |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/mindfulness_timer_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/mindfulness/MindfulnessEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| formattedTimer > pads to mm:ss and clamps below zero | PORTED | FormattedTimerTest.kt `pads to mm ss and clamps below zero` | Same four assertions |
| seeds its fields from the persisted timer config | PORTED | MindfulnessEntryViewModelTest.kt `initial state seeds fields from persisted timer config` | Field texts and totalSeconds/remainingSeconds asserted |
| timer config validation > a non-positive duration cannot start the timer | PORTED | MindfulnessEntryViewModelTest.kt `non-positive duration cannot start timer` | No invalid-timer test |
| timer config validation > an interval at or above the duration is rejected | PORTED | MindfulnessEntryViewModelTest.kt `interval at or above duration is rejected` |  |
| timer config validation > an interval shorter than the duration starts and persists | PORTED | MindfulnessEntryViewModelTest.kt `starting timer persists timer config` | Valid interval config persisted, timer running, no error |
| timer config validation > the fields are frozen while the timer runs | PORTED | MindfulnessEntryViewModelTest.kt `timer fields are frozen while timer runs` | Kotlin additionally asserts a frozen bell pick emits no preview |
| sound events > picking a bell emits a preview event with a fresh id | PORTED | MindfulnessEntryViewModelTest.kt `changing bell sound emits short preview` | Re-picking the same bell yields a fresh, larger event id |
| sound events > picking "none" clears the background preview | PORTED | MindfulnessEntryViewModelTest.kt `selecting no background sound clears background preview` | |
| sound events > an interval bell rings mid-session but not at the end | PORTED | MindfulnessEntryViewModelTest.kt `interval bell rings mid-session but not at the end` | Interval-bell scheduling untested in Kotlin |
| transport > runs down to completion and banks the session | PORTED | MindfulnessEntryViewModelTest.kt `completed timer can be saved as mindfulness session` | Completion asserts not paused and remainingSeconds 0 |
| transport > stop banks the elapsed span and pauses | PORTED | MindfulnessEntryViewModelTest.kt `stopping timer pauses with resume save and discard state` | Paused with exact remaining (50s) asserted |
| transport > resume continues from where it paused | PORTED | MindfulnessEntryViewModelTest.kt `stopping timer pauses with resume save and discard state` | Remaining-seconds continuity asserted across the resume |
| transport > resume on a finished countdown is rejected | PORTED | MindfulnessEntryViewModelTest.kt `resume on finished countdown is rejected` |  |
| transport > discard rewinds to the configured duration | PORTED | MindfulnessEntryViewModelTest.kt `discard rewinds timer to configured duration` | discardTimer untested |
| saving > a completed session writes its full duration | PORTED | MindfulnessEntryViewModelTest.kt `completed timer can be saved as mindfulness session` | Also asserts the countdown rewinds to the configured duration after the save |
| saving > a session under a minute is rejected, not rounded to zero | PORTED | MindfulnessEntryViewModelTest.kt `timer session under a minute is rejected not rounded to zero` | TIMER_TOO_SHORT, no write |
| saving > saving without a banked session is a no-op | PORTED | MindfulnessEntryViewModelTest.kt `saving without banked session is a no-op` |  |
| saving > an unavailable device reports unavailable, not a permission error | PORTED | MindfulnessEntryViewModelTest.kt `unavailable device reports unavailable on timer save` | Asserted on saveTimerSession, not only at load |
| saving > a missing write permission blocks the save | PORTED | MindfulnessEntryViewModelTest.kt `missing write permission blocks timer save` | Asserted on saveTimerSession |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/vitals_measurement_entry_command_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/vitals/VitalsMeasurementEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a command at rest is idle | PORTED | VitalsMeasurementEntryViewModelTest.kt `a command at rest is idle` | Boot test asserts permission-probe state only, not save-at-rest/no-error |
| a successful save settles on success, and is consumed once | PORTED | VitalsMeasurementEntryViewModelTest.kt `blood pressure entry writes systolic and diastolic values` | Full lifecycle (write, cleared inputs, saveCompleted, consumed); Flutter uses spo2, Kotlin BP — same command path |
| a failed save carries the failure to the form, not an exception | PORTED | VitalsMeasurementEntryViewModelTest.kt `a failed save carries the failure to the form, not an exception` | Kotlin additionally sets entryError=WRITE_FAILED by design |
| editing a field clears the failure the last attempt left behind | PORTED | VitalsMeasurementEntryViewModelTest.kt `editing a field clears the failure the last attempt left behind` |  |
| validation refuses before the command ever runs | PORTED | VitalsMeasurementEntryViewModelTest.kt `invalid vitals value does not write` | INVALID_VALUE + zero writes |
## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/activity_entry_screen_test.dart
Kotlin counterpart: none (no Compose UI test for the activity entry screen; nearest logic coverage is /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/ActivityEntryViewModelTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| start hub > offers plans, record and manual logging; route import is not here | PORTED | ActivityStartHubTest: `offersPlansRecordAndManualLoggingAndKeepsFileImportOut` | Compose instrumentation; runs on a device, not in CI |
| start hub > shows the permission explainer and a Grant action when the write permission is missing | PORTED | ActivityStartHubTest: `noPlansSaysSoAndAMissingPermissionOffersTheGrant` | Compose instrumentation; runs on a device, not in CI |
| start hub > a source action requests the write permission first | N/A-WIDGET | — | UI wiring of permission request on tap; write refusal itself covered by ActivityEntryViewModelTest `missing activity write permission prevents write` |
| start hub > Grant does not itself open a form | PORTED | ActivityStartHubTest: `noPlansSaysSoAndAMissingPermissionOffersTheGrant` | Compose instrumentation; runs on a device, not in CI |
| entry card > renders the Kotlin sections in order | PORTED | ActivityEntryCardTest: `rendersEverySectionAWorkoutNeedsToBeDescribed` | Compose instrumentation; runs on a device, not in CI |
| entry card > the feeling chips are the four emoji, and toggle off | PORTED | ActivityEntryCardTest: `theFeelingChipsAreTheFourEmojiAndTapAgainClearsTheChoice` | Compose instrumentation; runs on a device, not in CI |
| entry card > distance and elevation follow the unit system | PORTED | ActivityEntryCardTest: `distanceAndElevationFollowTheUnitSystem` | Compose instrumentation; runs on a device, not in CI |
| entry card > a validation error surfaces on its own field | PORTED | ActivityEntryCardTest: `aValidationErrorLandsOnItsOwnFieldAsWellAsTheCard` | Compose instrumentation; runs on a device, not in CI |
| repetitions > are hidden for a plain GPS activity | PORTED | ActivityRepetitionInputsTest: `aPlainGpsActivityGetsNoRepetitionInputsAtAll` | Compose instrumentation; runs on a device, not in CI |
| repetitions > a step-counted type gets a single total field, no mode switch | PORTED | ActivityRepetitionInputsTest: `aStepCountedTypeGetsOneTotalAndNoModeSwitch` | Compose instrumentation; runs on a device, not in CI |
| repetitions > a rep-counted type switches between Total and Sets | PORTED | ActivityRepetitionInputsTest: `aRepetitionCountedTypeOffersTotalAndSets` | Compose instrumentation; runs on a device, not in CI |
| repetitions > typing in one set does not bleed into another | PORTED | ActivityRepetitionInputsTest: `typingInOneSetDoesNotBleedIntoAnother` | Compose instrumentation; runs on a device, not in CI |
| repetitions > Save as plan only shows for rep-counted types | PORTED | ActivityEntryCardTest: `saveAsPlanOnlyShowsForRepetitionCountedTypes` | Compose instrumentation; runs on a device, not in CI |
| start hub > an empty plan list explains where to build one | PORTED | ActivityStartHubTest: `anEmptyPlanListExplainsWhereToBuildOne` | Compose instrumentation; runs on a device, not in CI |
| the live recording dashboard > fills the body instead of a fraction of the screen | N/A-WIDGET | — | Flutter layout regression (Expanded inside Scrollable); no Kotlin analogue |
| the live recording dashboard > focus mode takes the whole screen, app bar and all | N/A-WIDGET | — | Flutter layout/app-bar behavior; Kotlin focus mode exists (ActivityRecordingControls.kt) but is untested in UI |
| the live recording dashboard > is not inside the form scroll view | N/A-WIDGET | — | Flutter widget-tree structural assertion |
| the live recording dashboard > the setup card, by contrast, stays in the scroll view | N/A-WIDGET | — | Flutter widget-tree structural assertion |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/activity_entry_view_model_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/ActivityEntryViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| buildWriteRequest converts metric distance and trims text | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest converts metric distance and trims text` | Identical assertions |
| buildWriteRequest combines selected feeling and notes | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest combines selected feeling and notes` | |
| buildWriteRequest ignores hidden unsupported metric values | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest ignores hidden unsupported metric values` | |
| buildWriteRequest rejects total calories below active calories | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest rejects total calories below active calories` | |
| validateActivityEntry returns field specific errors | PORTED | ActivityEntryViewModelTest.kt `validateActivityEntry returns field specific errors` | Same six error codes |
| buildWriteRequest uses imported route distance and adjusts end after last point | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest uses imported route distance and adjusts end after last point` | |
| buildWriteRequest retimes imported route without recorded timestamps | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest retimes imported route without recorded timestamps` | |
| buildWriteRequest includes recorded pause intervals inside session | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest includes recorded pause intervals inside session` | |
| buildWriteRequest ignores recorded GPS metadata for non GPS activity | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest ignores recorded GPS metadata for non GPS activity` | |
| buildWriteRequest keeps BLE heart rate samples for strength training | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest keeps BLE heart rate samples for strength training` | Kotlin adds an extra repetition-recording variant |
| buildWriteRequest writes total push-ups as one set segment | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest writes total push-ups as one set segment` | |
| buildWriteRequest writes repetition sets and rest segments | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest writes repetition sets and rest segments` | |
| buildWriteRequest links selected planned workout | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest links selected planned workout` | |
| buildPlannedExerciseWriteRequest maps sets and rest steps | PORTED | ActivityEntryViewModelTest.kt `buildPlannedExerciseWriteRequest maps sets and rest steps` | |
| buildWriteRequest writes treadmill steps as steps count | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest writes treadmill steps as steps count` | |
| buildWriteRequest writes walking steps as steps count | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest writes walking steps as steps count` | |
| buildWriteRequest allows walking without steps | PORTED | ActivityEntryViewModelTest.kt `buildWriteRequest allows walking without steps` | |
| activity entry exposes field errors and skips write for invalid values | PORTED | ActivityEntryViewModelTest.kt `activity entry exposes field errors and skips write for invalid values` | |
| selecting activity clears metric fields that activity does not use | PORTED | ActivityEntryViewModelTest.kt `selecting activity clears metric fields that activity does not use` | |
| missing activity write permission prevents write | PORTED | ActivityEntryViewModelTest.kt `missing activity write permission prevents write` | |
| activity entry writes request when permission is granted | PORTED | ActivityEntryViewModelTest.kt `activity entry writes request when permission is granted` | Kotlin asserts saveCompleted flag instead of CommandSuccess |
| selecting planned workout prefills editable set structure | PORTED | ActivityEntryViewModelTest.kt `selecting planned workout prefills editable set structure` | |
| start from existing plan auto-applies the only available plan | PORTED | ActivityEntryViewModelTest.kt `start from existing plan auto-applies the only available plan` | |
| start from existing plan keeps picker when multiple activity types exist | PORTED | ActivityEntryViewModelTest.kt `start from existing plan keeps picker when multiple activity types exist` | |
| startWithPlan opens the requested plan directly in manual entry | PORTED | ActivityEntryViewModelTest.kt `startWithPlan opens the requested plan directly in manual entry` | |
| selecting activity then plan opens editable manual entry | PORTED | ActivityEntryViewModelTest.kt `selecting activity then plan opens editable manual entry` | |
| edit entry loads matching planned workouts without selecting a plan | PORTED | ActivityEntryViewModelTest.kt `edit entry loads matching planned workouts without selecting a plan` | |
| missing planned read permission is surfaced when loading existing plans | PORTED | ActivityEntryViewModelTest.kt `missing planned read permission is surfaced when loading existing plans` | |
| activity entry writes selected planned workout id | PORTED | ActivityEntryViewModelTest.kt `activity entry writes selected planned workout id` | |
| saving current structure writes planned workout | PORTED | ActivityEntryViewModelTest.kt `saving current structure writes planned workout` | |
| updating selected plan clears changed highlight baseline | PORTED | ActivityEntryViewModelTest.kt `updating selected plan clears changed highlight baseline` | |
| saving current structure requires a training plan title | PORTED | ActivityEntryViewModelTest.kt `saving current structure requires a training plan title` | |
| new plan option clears selected plan and saves a new planned workout | PORTED | ActivityEntryViewModelTest.kt `new plan option clears selected plan and saves a new planned workout` | |
| missing planned workout permission is surfaced before saving plan | PORTED | ActivityEntryViewModelTest.kt `missing planned workout permission is surfaced before saving plan` | |
| activity entry defaults to latest recorded activity when no favorite is set | PORTED | ActivityEntryViewModelTest.kt `activity entry defaults to latest recorded activity when no favorite is set` | |
| favorite activity overrides latest recorded activity | PORTED | ActivityEntryViewModelTest.kt `favorite activity overrides latest recorded activity` | |
| manual activity entry does not estimate calories | PORTED | ActivityEntryViewModelTest.kt `manual activity entry does not estimate calories` | |
| recorded activity without enough route points estimates calories | PORTED | ActivityEntryViewModelTest.kt `recorded activity without enough route points estimates calories` | Same 308/343 kcal expectations |
| finished recording draft is restored by a new activity entry view model | PORTED | ActivityEntryViewModelTest.kt `finished recording draft is restored by a new activity entry view model` | |
| finished walking route recording keeps recorded steps | PORTED | ActivityEntryViewModelTest.kt `finished walking route recording keeps recorded steps` | |
| saving a restored recording draft clears it | PORTED | ActivityEntryViewModelTest.kt `saving a restored recording draft clears it` | |
| discarding a finished recording draft clears it and returns to the start hub | PORTED | ActivityEntryViewModelTest.kt `discarding a finished recording draft clears it and returns to the start hub` | |
| activity entry keeps full write permissions when optional fields change | PORTED | ActivityEntryViewModelTest.kt `activity entry keeps full write permissions when optional fields change` | |
| route import fills distance and elevation fields in current unit system | PORTED | ActivityEntryViewModelTest.kt `route import fills distance and elevation fields in current unit system` | |
| FIT import without route fills manual activity fields | PORTED | ActivityEntryViewModelTest.kt `FIT import without route fills manual activity fields` | |
| FIT workout import uses workout duration without changing selected time | PORTED | ActivityEntryViewModelTest.kt `FIT workout import uses workout duration without changing selected time` | |
| the save command runs, succeeds, and is consumed exactly once | PORTED | ActivityEntryViewModelTest.kt `the save runs, succeeds, and is consumed exactly once` | In-flight isSavingEntry asserted from inside the repository answer; one-shot consumption via onSaveCompletedHandled |
| a failed write lands on the save command with the screen error | PORTED | ActivityEntryViewModelTest.kt `a failed write lands on the save with the screen error` | Kotlin carries one detailError, which is the blocking error |
| a refused write permission is a verdict, not a command failure | PORTED | ActivityEntryViewModelTest.kt `a refused write permission is a verdict, not a failed save` | Refusal verdict plus no failure state raised |
| refreshPermission probes the repository and publishes the verdict | PORTED | ActivityEntryViewModelTest.kt `refreshPermission probes the repository and publishes the verdict` | canWrite/writePermissions asserted incidentally; no dedicated probe test, no isCheckingPermission or call-count assertion |
| a permission probe that fails surfaces the error and blocks the write | PORTED | ActivityEntryViewModelTest.kt `a permission probe that fails surfaces the error and blocks the write` | No Kotlin test makes hasActivityWritePermission throw; portable with mockk |
| the edit route prefills the form from the stored workout | PORTED | ActivityEntryViewModelTest.kt `the edit route prefills the form from the stored workout` | Kotlin prefills on loadEditEntry() rather than during construction |
| an edit prefill that cannot be read reports the failure | PORTED | ActivityEntryViewModelTest.kt `an edit prefill that cannot be read reports the failure` | ScreenError.Message rather than Flutter's NotFound - toScreenError has no typed variants |
| the route-import command runs and returns to rest | PORTED | ActivityEntryViewModelTest.kt `the route import runs and returns to rest` | In-flight isImportingRoute asserted from inside the importer answer |
| a route file that will not parse fails its own command | PORTED | ActivityEntryViewModelTest.kt `a route file that will not parse fails its own command` | ROUTE_IMPORT_FAILED, save state untouched |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/activity_route_section_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/routeimport/ActivityRouteSectionTest.kt (over ActivityRouteSection.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| routeMovingDurationMs > is the full span when nothing was paused | PORTED | ActivityRouteSectionTest.kt `routeMovingDurationMs is the full span when nothing was paused` | `routeMovingDurationMs` exists in ActivityRouteSection.kt (internal, pure) but has no Kotlin test |
| routeMovingDurationMs > subtracts every pause | PORTED | ActivityRouteSectionTest.kt `routeMovingDurationMs subtracts every pause` | Same untested Kotlin function |
| routeMovingDurationMs > never goes negative when the pauses exceed the span | PORTED | ActivityRouteSectionTest.kt `routeMovingDurationMs never goes negative when the pauses exceed the span` | Same untested Kotlin function |
| routeAverageMetrics > is null when the route has no moving time left | PORTED | ActivityRouteSectionTest.kt `routeAverageMetrics is null when the route has no moving time left` | `routeAverageMetrics` exists in ActivityRouteSection.kt, untested |
| routeAverageMetrics > reports pace and speed over the moving time only | PORTED | ActivityRouteSectionTest.kt `routeAverageMetrics reports pace and speed over the moving time only` | Same untested Kotlin function |
| ActivityEntryCard with an imported route > narrows the type selector to GPS-capable types | PORTED | ActivityRouteSectionUiTest: `anAttachedRouteNarrowsTheTypeSelectorToGpsCapableTypes` | Compose instrumentation; runs on a device, not in CI |
| ActivityEntryCard with an imported route > offers every type when no route is attached | PORTED | ActivityRouteSectionUiTest: `withNoRouteEveryTypeIsStillOnOffer` | Compose instrumentation; runs on a device, not in CI |
| ActivityEntryCard with an imported route > renders the route summary and its average metrics | PORTED | ActivityRouteSectionUiTest: `theRouteSectionNamesTheFileAndItsAverageMetrics` | Compose instrumentation; runs on a device, not in CI |
| ActivityEntryCard with an imported route > renders no route section when no route is attached | PORTED | ActivityRouteSectionUiTest: `withNoRouteThereIsNoRouteSectionAtAll` | Compose instrumentation; runs on a device, not in CI |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/heart_rate_recovery_segment_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/HeartRateRecoverySegmentTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the recovery is written as a trailing rest segment, and read back | PORTED | HeartRateRecoverySegmentTest.kt `the recovery is written as a trailing rest segment and read back` | Identical writer+reader round-trip assertions |
| pauses during the effort survive alongside the recovery mark | PORTED | HeartRateRecoverySegmentTest.kt `pauses during the effort survive alongside the recovery mark` | |
| an ordinary recording gets no recovery mark at all | PORTED | HeartRateRecoverySegmentTest.kt `an ordinary recording gets no recovery mark at all` | |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/indoor_fit_import_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/routeimport/IndoorFitImportTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an indoor run imports, keeping the calories it measured | PORTED | IndoorFitImportTest.kt `an indoor run imports keeping the calories it measured` | Same fixture and assertions |
| an indoor ride imports as a STATIONARY BIKE, not as a run | PORTED | IndoorFitImportTest.kt `an indoor ride imports as a STATIONARY BIKE not as a run` | Same fixture, incl. 8 HR samples |
| a file that measured NO calories still gets both estimated | PORTED | IndoorFitImportTest.kt `a file that measured NO calories still gets both estimated` | |
| a generic FIT sport still yields to the name | PORTED | IndoorFitImportTest.kt `a generic FIT sport still yields to the name` | |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recorded_session_range_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/RecordedSessionRangeTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the session is stretched to cover the last recorded sample | PORTED | RecordedSessionRangeTest.kt `the end is stretched to cover the last sample` | Kotlin asserts exact stretched end (lastSample+1s); Flutter additionally re-counts samples, which Kotlin checks in the untouched-session test |
| an untruncated recording is left exactly as it is | PORTED | RecordedSessionRangeTest.kt `a session already containing its samples is untouched` | |
| samples before a start the user moved forward are dropped, not clamped | PORTED | RecordedSessionRangeTest.kt `samples before the start are dropped rather than clamped onto it` | |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/repetition_recognizers_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/RepetitionRecognizersTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| push-up recognizer counts only close transitions | PORTED | RepetitionRecognizersTest.kt `push-up recognizer counts only close transitions` | Identical sequences |
| step recognizer counts each step detector event | PORTED | RepetitionRecognizersTest.kt `step recognizer counts each step detector event` | |
| jump recognizer counts jumping to falling transition | PORTED | RepetitionRecognizersTest.kt `jump recognizer counts jumping to falling transition` | |
| pull-up recognizer counts pull and relax sequence | PORTED | RepetitionRecognizersTest.kt `pull-up recognizer counts pull and relax sequence` | |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/routeimport/route_file_parser_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/routeimport/RouteFileParserTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| RouteFileParser > parse extracts timestamped GPX track points and summaries | PORTED | RouteFileParserTest.kt `parse extracts timestamped GPX track points and summaries` | |
| RouteFileParser > parseFile extracts timestamped KML gx track from KMZ | PORTED | RouteFileParserTest.kt `parseFile extracts timestamped KML gx track from KMZ` | |
| RouteFileParser > parseFile extracts timestamped FIT activity records and sport | PORTED | RouteFileParserTest.kt `parseFile extracts timestamped FIT activity records and sport` | |
| RouteFileParser > parseFile imports FIT activity without GPS route | PORTED | RouteFileParserTest.kt `parseFile imports FIT activity without GPS route` | Both assert total=220 / active=null (the calorie-swap regression) |
| RouteFileParser > a FIT activity brings its heart rate, cadence and speed | DIVERGED | IndoorFitImportTest.kt `an indoor ride imports as a STATIONARY BIKE not as a run` | Kotlin covers FIT HR extraction only via the real fixture; no synthetic-FIT test asserting cadence-kind-by-sport or speed samples (Kotlin FIT test writer emits no hr/cadence/speed fields) |
| RouteFileParser > a RUNNING FIT file doubles the cadence into steps | PORTED | RouteFileParserTest.kt: `a RUNNING FIT file doubles the cadence into steps` | the FIT fixture writer gained optional per-point HR/cadence/speed fields; existing fixtures stay byte-identical |
| RouteFileParser > an INDOOR FIT file with no GPS still brings its heart rate | DIVERGED | IndoorFitImportTest.kt `an indoor ride imports as a STATIONARY BIKE not as a run` | Same scenario via real routeless fixture (8 HR samples), but no no-fix-record synthetic case and no cadence assertion |
| RouteFileParser > parseFile imports FIT activity and ignores unusable one point route | PORTED | RouteFileParserTest.kt `parseFile imports FIT activity and ignores unusable one point route` | |
| RouteFileParser > parseFile imports FIT course as route without activity time range | PORTED | RouteFileParserTest.kt `parseFile imports FIT course as route without activity time range` | |
| RouteFileParser > parseFile imports sparse FIT course without route geometry | PORTED | RouteFileParserTest.kt `parseFile imports sparse FIT course without route geometry` | |
| RouteFileParser > parseFile imports FIT workout definition without activity session | PORTED | RouteFileParserTest.kt `parseFile imports FIT workout definition without activity session` | |
| RouteFileParser > parseFile extracts untimestamped KML line string with synthetic timing | PORTED | RouteFileParserTest.kt `parseFile extracts untimestamped KML line string with synthetic timing` | |
| RouteFileParser > parse simplifies very large route files | PORTED | RouteFileParserTest.kt `parse simplifies very large route files` | |
| RouteFileParser > parse rejects GPX without two timestamped points | PORTED | RouteFileParserTest.kt `parse rejects GPX without two timestamped points` | Kotlin asserts only that it fails (no exception-type check) — minor |
| RouteFileParser > parseFile rejects oversized raw route file before parsing | PORTED | RouteFileParserTest.kt `parseFile rejects oversized raw route file before parsing` | Same error message asserted |
| RouteFileParser > parseFile rejects oversized KMZ route entry before XML parsing | PORTED | RouteFileParserTest.kt `parseFile rejects oversized KMZ route entry before XML parsing` | Same error message asserted |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/routeless_gpx_import_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/routeimport/RoutelessGpxImportTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a strength session: 1931 heartbeats and not one location | PORTED | RoutelessGpxImportTest.kt `a strength session - 1931 heartbeats and not one location` | Same fixture and assertions |
| an indoor run: the times, and the sport the file names | PORTED | RoutelessGpxImportTest.kt `an indoor run - the times and the sport the file names` | |
| a GPX with neither places nor times is still refused | PORTED | RoutelessGpxImportTest.kt `a GPX with neither places nor times is still refused` | Kotlin asserts IllegalArgumentException vs Flutter's RouteImportException — same verdict |
| a routed GPX keeps its route AND gains its heart rate | PORTED | RoutelessGpxImportTest.kt `a routed GPX keeps its route AND gains its heart rate` | Incl. per-foot cadence doubling assertions |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/tcx_route_parser_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/routeimport/TcxRouteParserTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an indoor ride: no GPS, and a whole activity anyway > keeps the session the file recorded | PORTED | TcxRouteParserTest.kt `indoor ride keeps the session the file recorded` | |
| an indoor ride > carries the heart rate, cadence and speed beside it | PORTED | TcxRouteParserTest.kt `indoor ride carries the heart rate cadence and speed beside it` | Incl. cadence-kind-by-sport assertions |
| an indoor ride > imports — which is the whole bug | PORTED | TcxRouteParserTest.kt `indoor ride imports - which is the whole bug` | |
| an outdoor run: the route still works > reads the track, and the samples along it | PORTED | TcxRouteParserTest.kt `outdoor run reads the track and the samples along it` | |
| an outdoor run > is a run, and it saves | PORTED | TcxRouteParserTest.kt `outdoor run is a run and it saves` | |
| a TCX is recognised by its CONTENT, not its extension | PORTED | TcxRouteParserTest.kt `a TCX is recognised by its CONTENT not its extension` | |
| a routeless GPX is still refused, and must be | PORTED | RoutelessGpxImportTest.kt `a GPX with neither places nor times is still refused` | Covered in the sibling Kotlin file, not TcxRouteParserTest.kt; identical empty-GPX fixture |
## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recorded_sensor_summary_test.dart
Kotlin counterpart: none (UI lives in /home/manu/Documentos/repos/openvitals-android/app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/ActivityRecordingSensorUi.kt, untested)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the time-axis heart-rate chart card | PORTED | ActivityRecordedSensorSummaryTest: `rendersTheHeartRateCardWithItsAverageRangeAndSampleCount` | Compose instrumentation; runs on a device, not in CI |
| falls back to the sample range without a session range | PORTED | ActivityRecordedSensorSummaryTest: `withoutASessionRangeTheAxisFallsBackToTheSamples` | Compose instrumentation; runs on a device, not in CI |
| renders nothing when no sensor produced samples | PORTED | ActivityRecordedSensorSummaryTest: `withNoSamplesAtAllThereIsNoCard` | Compose instrumentation; runs on a device, not in CI |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_accepted_location_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/ActivityRecordingAcceptedLocationTest.kt (over the extracted pure ActivityRecordingState.withAcceptedLocation in ActivityRecordingLocationSupport.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the first fix opens the route but banks no distance or speed | PORTED | ActivityRecordingAcceptedLocationTest.kt `the first fix opens the route but banks no distance or speed` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| a second fix accumulates distance, climb and speed | PORTED | ActivityRecordingAcceptedLocationTest.kt `a second fix accumulates distance, climb and speed` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| a descent accumulates loss, not gain | PORTED | ActivityRecordingAcceptedLocationTest.kt `a descent accumulates loss, not gain` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| max speed is a high-water mark, not the latest speed | PORTED | ActivityRecordingAcceptedLocationTest.kt `max speed is a high-water mark, not the latest speed` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| a fix that does not advance the clock is dropped | PORTED | ActivityRecordingAcceptedLocationTest.kt `a fix that does not advance the clock is dropped` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| a fix inside the minimum sample distance is shown but not banked | PORTED | ActivityRecordingAcceptedLocationTest.kt `a fix inside the minimum sample distance is shown but not banked` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| a gap wider than routeGapMeters breaks the route and banks no distance | PORTED | ActivityRecordingAcceptedLocationTest.kt `a gap wider than routeGapMeters breaks the route and banks no distance` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| an implausible jump is dropped rather than banked | PORTED | ActivityRecordingAcceptedLocationTest.kt `an implausible jump is dropped rather than banked` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| auto-idle charges only the stretch beyond the timeout | PORTED | ActivityRecordingAcceptedLocationTest.kt `auto-idle charges only the stretch beyond the timeout` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| moving again inside the timeout accrues no idle at all | PORTED | ActivityRecordingAcceptedLocationTest.kt `moving again inside the timeout accrues no idle at all` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |
| a route break does not stop the auto-idle clock | PORTED | ActivityRecordingAcceptedLocationTest.kt `a route break does not stop the auto-idle clock` | Asserted on the extracted pure ActivityRecordingState.withAcceptedLocation seam |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_dashboard_edit_test.dart
Kotlin counterpart: none (Compose grid/editor in ActivityRecordingDashboard.kt, untested)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| outside edit mode the tiles are plain, undraggable and unremovable | PORTED | ActivityRecordingDashboardEditTest: `outsideEditModeTilesCarryNoEditControls` | Compose instrumentation; runs on a device, not in CI |
| edit mode wraps every tile in the shared reorderable tile | PORTED | ActivityRecordingDashboardEditTest: `editModeGivesEveryTileItsOwnEditControls` | Compose instrumentation; runs on a device, not in CI |
| the shared ✕ removes that field from the layout | PORTED | ActivityRecordingDashboardEditTest: `removingATileTakesThatFieldOutOfTheLayout` | Compose instrumentation; runs on a device, not in CI |
| dragging a tile onto another lands it on the target | PORTED | ActivityRecordingDashboardEditTest: `movingATileLandsItOnTheTargetRatherThanTheGapBeforeIt` | Compose instrumentation; runs on a device, not in CI |
| the add tray offers the fields not on the grid, and adds one | PORTED | ActivityRecordingDashboardEditTest: `theAddTrayOffersTheFieldsThatAreNotOnTheGrid` | Compose instrumentation; runs on a device, not in CI |
| a full grid renders no add tray at all | PORTED | ActivityRecordingDashboardEditTest: `aGridThatAlreadyHasEverythingRendersNoAddTray` | Compose instrumentation; runs on a device, not in CI |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_dashboard_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/ActivityRecordingDashboardTest.kt (over ActivityRecordingDashboard.kt and ActivityRecordingSplitsUi.formatRecordingElapsed)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| formatRecordingElapsed > drops the hour segment under an hour | PORTED | ActivityRecordingDashboardTest.kt `formatRecordingElapsed drops the hour segment under an hour` | Kotlin formatRecordingElapsed exists, untested |
| formatRecordingElapsed > shows zero-padded minutes once there are hours | PORTED | ActivityRecordingDashboardTest.kt `formatRecordingElapsed shows zero-padded minutes once there are hours` |  |
| formatRecordingElapsed > floors a negative duration at zero | PORTED | ActivityRecordingDashboardTest.kt `formatRecordingElapsed floors a negative duration at zero` |  |
| dragSteps > is zero inside the dead zone, in both directions | PORTED | ActivityRecordingDashboardTest.kt `dragSteps is zero inside the dead zone, in both directions` |  |
| dragSteps > counts whole steps only, truncating toward zero | PORTED | ActivityRecordingDashboardTest.kt `dragSteps counts whole steps only, truncating toward zero` |  |
| dragSteps > never divides by a zero step | PORTED | ActivityRecordingDashboardTest.kt `dragSteps never divides by a zero step` | fixed: `stepPx <= 0f` now short-circuits to 0 instead of dividing by zero |
| item size > grows across before it grows down, and stops at the template | PORTED | ActivityRecordingDashboardTest.kt `item size grows across before it grows down, and stops at the template` | nextSize untested |
| item size > shrinks height before width, and stops at 1x1 | PORTED | ActivityRecordingDashboardTest.kt `item size shrinks height before width, and stops at 1x1` | previousSize untested |
| item size > canGrow / canShrink bound the ends | PORTED | ActivityRecordingDashboardTest.kt `item size canGrow and canShrink bound the ends` |  |
| item size > text emphasis follows the cell shape | PORTED | ActivityRecordingDashboardTest.kt `item size text emphasis follows the cell shape` | hasCompactMetricText/hasRoomyMetricText untested |
| item size > a resize drag maps offsets onto spans | PORTED | ActivityRecordingDashboardTest.kt `item size a resize drag maps offsets onto spans` | sizeForResizeDrag untested |
| recordingDashboardLazyGridRows > a full row of single cells is one row | PORTED | ActivityRecordingDashboardTest.kt `recordingDashboardLazyGridRows a full row of single cells is one row` |  |
| recordingDashboardLazyGridRows > a tall cell makes its whole line tall | PORTED | ActivityRecordingDashboardTest.kt `recordingDashboardLazyGridRows a tall cell makes its whole line tall` |  |
| recordingDashboardLazyGridRows > an item that does not fit wraps onto the next line | PORTED | ActivityRecordingDashboardTest.kt `recordingDashboardLazyGridRows an item that does not fit wraps onto the next line` |  |
| recordingDashboardLazyGridRows > is at least one row, even with no items | PORTED | ActivityRecordingDashboardTest.kt `recordingDashboardLazyGridRows is at least one row, even with no items` |  |
| layout operations > withRemovedField refuses to empty the dashboard | PORTED | ActivityRecordingDashboardTest.kt `layout operations withRemovedField refuses to empty the dashboard` |  |
| layout operations > withAddedField is a no-op for a field already present | PORTED | ActivityRecordingDashboardTest.kt `layout operations withAddedField is a no-op for a field already present` |  |
| layout operations > withAddedField appends a new field | PORTED | ActivityRecordingDashboardTest.kt `layout operations withAddedField appends a new field` |  |
| layout operations > withMovedFieldToTarget lands the field on the target index | PORTED | ActivityRecordingDashboardTest.kt `layout operations withMovedFieldToTarget lands the field on the target index` |  |
| layout operations > withMovedFieldToTarget is a no-op for an unknown or same field | PORTED | ActivityRecordingDashboardTest.kt `layout operations withMovedFieldToTarget is a no-op for an unknown or same field` |  |
| layout operations > withAvailableFields drops what the activity cannot measure | PORTED | ActivityRecordingDashboardTest.kt `layout operations withAvailableFields drops what the activity cannot measure` |  |
| layout operations > withAvailableFields falls back to the defaults when nothing survives | PORTED | ActivityRecordingDashboardTest.kt `layout operations withAvailableFields falls back to the defaults when nothing survives` |  |
| layout operations > withAvailableFields falls back to the available list when even the defaults do not intersect | PORTED | ActivityRecordingDashboardTest.kt `layout operations withAvailableFields falls back to the available list when even the defaults do not intersect` |  |
| availableRecordingDashboardFields > a timed activity has no distance or speed | PORTED | ActivityRecordingDashboardTest.kt `availableRecordingDashboardFields a timed activity has no distance or speed` |  |
| availableRecordingDashboardFields > a GPS activity offers the full set, without steps | PORTED | ActivityRecordingDashboardTest.kt `availableRecordingDashboardFields a GPS activity offers the full set, without steps` |  |
| availableRecordingDashboardFields > a step-counted activity adds steps | PORTED | ActivityRecordingDashboardTest.kt `availableRecordingDashboardFields a step-counted activity adds steps` |  |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_location_settings_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| android requests the Kotlin service GPS sampling parameters | N/A-FRAMEWORK | — | asserts Dart geolocator config mirrors Kotlin's inline requestLocationUpdates(GPS_PROVIDER, 1000L, 0f) call (ActivityRecordingService.kt:366); Kotlin side has no separable unit to test |
| non-android falls back to plain settings | N/A-FRAMEWORK | — | Flutter-only platform branching |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_native_sensors_test.dart
Kotlin counterpart: none (Kotlin talks to SensorManager/AltitudeConverter directly; no channel bridge)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| with no native side, every query resolves to its safe default | N/A-FRAMEWORK | — | MissingPluginException path is Dart-channel-only |
| hasSensor asks the platform by sensor type name | PORTED | ActivityRecordingSensorTypeTest.kt `each hardware sensor kind maps to its Android sensor type` | The Kotlin equivalent is ActivityRecordingSensor.toAndroidSensorType(), widened private->internal |
| sensor kinds with no Android sensor type are false without asking | PORTED | ActivityRecordingSensorTypeTest.kt `sensor kinds with no Android sensor type are null without asking` | GPS/BLE/NONE map to null, so SensorManager is never asked |
| convertToMsl hands the fix through and returns the platform answer | N/A-FRAMEWORK | — | Kotlin calls android AltitudeConverter directly, no portable seam |
| a platform error degrades to null, never a crash | N/A-FRAMEWORK | — | Kotlin guards via null converter below API 34 (ActivityRecording.kt:280-284); framework-bound |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_screen_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/ActivityRecordingGpsFixTest.kt (activityGpsFixQuality, PreRecordingGpsFixState and the extracted start-enablement/start-action helpers); the Compose screens themselves are untested
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| activityGpsFixQuality > a fresh, accurate fix is precise | PORTED | ActivityRecordingGpsFixTest.kt `activityGpsFixQuality - a fresh, accurate fix is precise` | Location.activityGpsFixQuality exists in Kotlin, untested |
| activityGpsFixQuality > a fix worse than the required accuracy is not precise | PORTED | ActivityRecordingGpsFixTest.kt `activityGpsFixQuality - a fix worse than the required accuracy is not precise` |  |
| activityGpsFixQuality > a stale fix is not precise, however accurate | PORTED | ActivityRecordingGpsFixTest.kt `activityGpsFixQuality - a stale fix is not precise, however accurate` |  |
| activityGpsFixQuality > a fix from before the session started is not precise | PORTED | ActivityRecordingGpsFixTest.kt `activityGpsFixQuality - a fix from before the session started is not precise` |  |
| PreRecordingGpsFixState > withholds the fix without permission or with GPS off | PORTED | ActivityRecordingGpsFixTest.kt `PreRecordingGpsFixState - withholds the fix without permission or with GPS off` | Kotlin PreRecordingGpsFixState data class exists, untested |
| PreRecordingGpsFixState > exposes an initial fix once everything lines up | PORTED | ActivityRecordingGpsFixTest.kt `PreRecordingGpsFixState - exposes an initial fix once everything lines up` | Kotlin has no initialFix field; latestPreciseFix and fixQuality asserted instead |
| setup screen > a GPS activity cannot start until a precise fix arrives | N/A-WIDGET | — | Start-button gating is Compose; the gating predicate (latestPreciseFix) is the MISSING PreRecordingGpsFixState rows above |
| setup screen > switched to record without GPS, a run starts at once — no fix, no permission | PORTED | ActivityRecordingGpsFixTest.kt `setup screen - switched to record without GPS, a run starts at once - no fix, no permission` | Start-enablement and start action covered via extracted pure functions; the warning copy stays rendering-only |
| setup screen > without the location permission Start is enabled, to ask for it | N/A-WIDGET | — | Compose setup screen wiring, untested in Kotlin |
| setup screen > push-ups explain that the proximity sensor is unusable | PORTED | RecordingGuidancePanelTest: `aMissingSensorIsNamedAndManualEntryIsOfferedInstead` | Compose instrumentation; runs on a device, not in CI |
| setup screen > a rep activity with a usable sensor is ready and startable | PORTED | RecordingGuidancePanelTest: `aRepActivityWithItsSensorReportsReady` | Compose instrumentation; runs on a device, not in CI |
| recording screen > an idle GPS session offers Start and Cancel only | PORTED | ActivityRecordingControlsTest: `anIdleGpsSessionOffersStartAndCancelOnly` | Compose instrumentation; runs on a device, not in CI |
| recording screen > the outdoor toggle is reachable from normal recording mode | N/A-WIDGET | — | regression is about Compose layout reachability |
| recording screen > the outdoor toggle is reachable for repetition recordings | N/A-WIDGET | — | |
| recording screen > a running GPS session shows the tabs, pause, lap and marker | PORTED | ActivityRecordingControlsTest: `aRunningGpsSessionShowsItsTabsPauseLapAndMarker` | Compose instrumentation; runs on a device, not in CI |
| recording screen > the dashboard edit toggle only appears while idle or paused | PORTED | ActivityRecordingScreenTest: `theDashboardEditToggleOnlyOffersItselfWhileIdleOrPaused` | Compose instrumentation; runs on a device, not in CI |
| recording screen > editing the dashboard shows the add-field chips | PORTED | ActivityRecordingScreenTest: `editingTheDashboardSwapsTheTabsForTheAddFieldChips` | Compose instrumentation; runs on a device, not in CI |
| recording screen > a repetition session shows the counter, +/- and End set | PORTED | ActivityRecordingControlsTest: `aRepetitionSessionCountsRepsAndOffersEndSet` | Compose instrumentation; runs on a device, not in CI |
| recording screen > a repetition set cannot end before a rep is counted | PORTED | ActivityRecordingControlsTest: `aRepetitionSetCannotEndBeforeARepIsCounted` | Compose instrumentation; runs on a device, not in CI |
| recording screen > Focus enters and exits full-screen mode | PORTED | ActivityRecordingScreenTest: `focusEntersAndExitsFullScreenMode`, `aRepetitionRecordingIsNeverLeftInFocusMode` | Compose instrumentation; runs on a device, not in CI |
| recording screen > the outdoor toggle applies the high-contrast theme | PORTED | ActivityRecordingThemeTest (scheme read back through a probe) + ActivityRecordingScreenTest: `theOutdoorToggleInFocusModeHandsTheChoiceBackToTheHost` | Compose instrumentation; runs on a device, not in CI |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_serialization_test.dart
Kotlin counterpart: none (logic in /home/manu/Documentos/repos/openvitals-android/app/src/main/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/ActivityRecordingStoreSerialization.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| recording state survives a SharedPreferences round-trip | PORTED | ActivityRecordingSerializationTest.kt: `recording state survives a SharedPreferences round-trip` | — |
| idle state clears persisted recording keys | PORTED | ActivityRecordingSerializationTest.kt: `idle state clears persisted recording keys` | — |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_service_test.dart
Kotlin counterpart: none (ActivityRecordingService.kt + ActivityRecordingController in ActivityRecording.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| notification buttons drive the recording: pause, resume, discard | PORTED | ActivityRecordingControllerTest.kt: `notification buttons drive the recording - pause, resume, discard` | — |
| an unknown notification command is ignored | N/A-WIDGET | — | the action relay is a bare `when` in ActivityRecordingService.onStartCommand, with no JVM-callable dispatcher to feed an unknown string to |
| a recording restored after process death re-enters instead of going numb | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin controller re-subscribes to the BLE metrics flow on init but never re-calls startRecording, so the re-attach assertion has no counterpart (the restore-status half is covered by `a recording restored after process death comes up already recording`) |
| discard clears the persisted draft, so a restart stays idle | PORTED | ActivityRecordingControllerTest.kt: `discard clears the persisted draft, so a restart stays idle` | — |
| a denied notification permission refuses to start and says why | PORTED | ActivityRecordingControllerTest.kt: `a denied notification permission refuses to start and says why` | — |
| finishRecording snapshots the session and hands back the BLE buffer | PORTED | ActivityRecordingControllerTest.kt: `finishRecording snapshots the session and hands back the BLE buffer` | — |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_splits_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/ActivityRecordingSplitsTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| interval splits are empty for empty and single point routes | PORTED | ActivityRecordingSplitsTest.kt `interval splits are empty for empty and single point routes` | identical assertions |
| interval splits use route breaks and do not count gap distance | PORTED | ActivityRecordingSplitsTest.kt `interval splits use route breaks and do not count gap distance` | |
| time splits include active incomplete split | PORTED | ActivityRecordingSplitsTest.kt `time splits include active incomplete split` | |
| time splits do not calculate across route breaks | PORTED | ActivityRecordingSplitsTest.kt `time splits do not calculate across route breaks` | |
| distance splits create fixed distance buckets with active remainder | PORTED | ActivityRecordingSplitsTest.kt `distance splits create fixed distance buckets with active remainder` | |
| split max speed is calculated per bucket | PORTED | ActivityRecordingSplitsTest.kt `split max speed is calculated per bucket` | |
| manual lap splits do not count route break gaps | PORTED | ActivityRecordingSplitsTest.kt `manual lap splits do not count route break gaps` | |
| route distance helper avoids route break gaps | PORTED | ActivityRecordingSplitsTest.kt `route distance helper avoids route break gaps` | |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_state_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/ActivityRecordingStateTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| movingDuration excludes open auto idle time | PORTED | ActivityRecordingStateTest.kt `movingDuration excludes open auto idle time` | identical values and assertions |
| movingDuration excludes manual pauses and auto idle | PORTED | ActivityRecordingStateTest.kt `movingDuration excludes manual pauses and auto idle` | |
| repetition movingDuration excludes recorded and open rest time | PORTED | ActivityRecordingStateTest.kt `repetition movingDuration excludes recorded and open rest time` | |
| effective speed is zero while idle or gps is poor | PORTED | ActivityRecordingStateTest.kt `effective speed is zero while idle or gps is poor` | |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/activity_recording_view_model_test.dart
Kotlin counterpart: partial — /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/ActivityEntryViewModelTest.kt (Kotlin folds recording into ActivityEntryViewModel; no separate recording view model)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| start > a started recording succeeds and republishes the session | PORTED | ActivityEntryViewModelTest.kt: `a started recording succeeds and republishes the session` | — |
| start > a refused start fails with the reason the service gave | PORTED | ActivityEntryViewModelTest.kt: `a refused start fails with the reason the service gave` | — |
| start > the screen consumes the failure once, then it is gone | N/A-BEHAVIOR | — | blocked on behavior decision - Kotlin has no start-command consume seam; entryError is cleared implicitly on the next field edit |
| start > a second start while one is running is refused | N/A-BEHAVIOR | — | blocked on behavior decision - neither ActivityEntryViewModel.startGpsRecording nor ActivityRecordingController guards on an already-active recording |
| pause / resume > pause and resume reach the service and republish its status | PORTED | ActivityEntryViewModelTest.kt: `pause and resume reach the service and republish its status` | — |
| stop > stopping hands back the recorded snapshot | DIVERGED | ActivityEntryViewModelTest.kt `recorded activity without enough route points estimates calories`, `finished walking route recording keeps recorded steps` | exercises finishGpsRecording consuming the snapshot into form fields, but asserts form prefill rather than a snapshot/command result surface |
| stop > the snapshot is consumed exactly once | DIVERGED | ActivityEntryViewModelTest.kt `saving a restored recording draft clears it` | Kotlin's one-shot semantic is draft clearing on save; no command-state reset equivalent asserted |
| stop > stopping with nothing recording fails loudly, not silently | PORTED | ActivityEntryViewModelTest.kt: `stopping with nothing recording fails loudly, not silently` | — |
| discard > discarding clears the session and both commands | PORTED | ActivityEntryViewModelTest.kt: `discarding clears the session and returns to the start hub` | — |
| focus mode > focus mode needs a session that can actually use it | PORTED | ActivityEntryViewModelTest.kt: `focus mode needs a session that can actually use it` | seam: `ActivityRecordingState.canUseFocusMode` extracted |
| elapsed time > a repetition session counts its rests, a route does not | DIVERGED | ActivityRecordingStateTest.kt `repetition movingDuration excludes recorded and open rest time` | underlying moving/rest math covered at state level; the screen-surface totalTime vs movingTime split is not |

## /home/manu/Documentos/repos/mobile-app/test/features/manualentry/activity/recording/heart_rate_recovery_phase_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/manualentry/activity/recording/HeartRateRecoveryPhaseTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| phase countdown > the warmup counts down from the start of the recording | PORTED | HeartRateRecoveryPhaseTest.kt `the warmup counts down from the start of the recording` | identical assertions incl. isHeartRateRecoveryTest |
| phase countdown > the effort has no deadline — it ends when the rider does | PORTED | HeartRateRecoveryPhaseTest.kt `the effort has no deadline - it ends when the rider does` | |
| phase countdown > the recovery counts down from the instant effort stopped | PORTED | HeartRateRecoveryPhaseTest.kt `the recovery counts down from the instant effort stopped` | |
| phase countdown > a countdown never runs negative | PORTED | HeartRateRecoveryPhaseTest.kt `a countdown never runs negative` | |
| phase countdown > an ordinary recording is not a test | PORTED | HeartRateRecoveryPhaseTest.kt `an ordinary recording is not a test` | Kotlin file additionally tests zero-warmup and countdown text formatting |
| phase banner > offers a way out of the effort by hand | PORTED | HeartRateRecoveryPhaseBannerTest: `theEffortCanAlwaysBeEndedByHand` | Compose instrumentation; runs on a device, not in CI |
| phase banner > during the recovery there is nothing to press, only to keep still | PORTED | HeartRateRecoveryPhaseBannerTest: `duringTheRecoveryThereIsNothingToPressOnlyToKeepStill` | Compose instrumentation; runs on a device, not in CI |
## /home/manu/Documentos/repos/mobile-app/test/features/hydration/hydration_display_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/hydration/HydrationPresentationMapperTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an empty period derives zeroes, not nulls | PORTED | HydrationPresentationMapperTest.kt `display has no data for empty hydration` | All zero fields asserted; chartValues/cumulativeSamples/drinkBreakdown/maxDrinkLiters have no Kotlin equivalent |
| the summary folds totals, tracked days and the best day | PORTED | HydrationPresentationMapperTest.kt `summary ignores zero intake days for averages` | loggedDays, trackedDays, total, average and best day all asserted |
| the goal streak is the trailing one, the longest is the best one | PORTED | HydrationPresentationMapperTest.kt `goal statistics use configured daily goal` | asserts currentGoalStreakDays=1, longestGoalStreakDays=2 on the same broken-streak shape |
| an unmet TODAY does not break the trailing streak | PORTED | HydrationPresentationMapperTest.kt `an unfinished today does not break the current goal streak` | same in-progress-day guard, streak stays 2 |
| an unmet PAST day still breaks the trailing streak | PORTED | HydrationPresentationMapperTest.kt `a finished day short of the goal does break the streak` | equivalent finished-day-breaks assertion |
| the drink breakdown sums by name, biggest first, and scales itself | N/A-BEHAVIOR | — | No drink-breakdown feature exists in the Kotlin hydration screen |
| the day curve accumulates, in time order, skipping empty drinks | PORTED | HydrationDayCurveTest.kt: `the day curve accumulates, in time order, skipping empty drinks` | seam: cumulativeHydrationPoints private->internal |
| the entry list is newest first | PORTED | HydrationPeriodContentTest: `theEntryListIsNewestFirst` | Compose instrumentation; runs on a device, not in CI |
| a single day over the goal is one day of seven, not a full bar | PORTED | HydrationPresentationMapperTest.kt `goal progress divides by elapsed days not by tracked days` + `a goal of zero never fills the bar and never divides by zero` | 1/7 plus the zero-goal division guard |
| the goal bar measures the period... > one logged day in a seven-day week does not fill the bar | PORTED | HydrationPresentationMapperTest.kt `goal progress divides by elapsed days not by tracked days` + `elapsed days count the whole period once it is over` | goalMetDays, elapsedDays=7 and 1/7 all asserted |
| the goal bar measures the period... > meeting the goal every day of the week fills it | PORTED | HydrationPresentationMapperTest.kt `goal progress is clamped to one` | full week over goal yields 1.0 |
| the goal bar measures the period... > a goal you have not had the chance to miss yet does not count | PORTED | HydrationPresentationMapperTest.kt `a period running past today is cut at today` | elapsedDays=3 and progress over elapsed days asserted |

## /home/manu/Documentos/repos/mobile-app/test/features/hydration/hydration_entry_merge_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/hydration/HydrationViewModelTest.kt (merge is done in `HydrationViewModel.load()`)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| mergeHydrationAndNutrition > a hydration entry takes its name and nutrients from its paired nutrition record | DIVERGED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | paired-record exclusion asserted, but the hydration entry's adopted displayName/nutrients are never asserted |
| mergeHydrationAndNutrition > the paired nutrition record is not also listed on its own | PORTED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | "paired-id" absent from the expected id list |
| mergeHydrationAndNutrition > a beverage with nutrients but no volume is surfaced as a nutrition-only entry | PORTED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | NUTRITION_ONLY record type, name "Coffee", liters 0.0 asserted |
| mergeHydrationAndNutrition > another app's caffeinated drink joins the beverage history | PORTED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | external "Red Bull" included with isOpenVitalsEntry=false |
| mergeHydrationAndNutrition > a plain meal is not a beverage | PORTED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | external "Sandwich" (no caffeine nutrient) excluded |
| mergeHydrationAndNutrition > the carbs record OpenVitals writes for an activity is excluded | PORTED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | "OpenVitals carbs" entry excluded from ids |
| mergeHydrationAndNutrition > an unpaired nutrition record at the same instant as an OpenVitals hydration entry is treated as its other half | PORTED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | same-instant no-client-id record excluded, later standalone kept |
| mergeHydrationAndNutrition > a hydration entry with no paired record keeps a null name | DIVERGED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | an unpaired hydration entry is in the fixture but its null displayName is never asserted |

## /home/manu/Documentos/repos/mobile-app/test/features/hydration/hydration_intraday_chart_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt (generic helpers only; hydration wiring is in HydrationPeriodContent.kt, untested)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| the day is drawn as a LINE, with an hour axis | PORTED | HydrationPeriodContentTest: `aDayIsDrawnOverAnHourAxisRatherThanAsOneBarForTheWholeDay` | Compose instrumentation; runs on a device, not in CI |
| the line is CUMULATIVE and anchored at both ends | DIVERGED | ChartTimeAxesTest.kt `cumulative shape anchors at zero and plateaus out to the end fraction` | generic shape helper covered; hydration-entry accumulation (sum, sort) in private `cumulativeHydrationPoints()` untested |
| a drink at 06:00 sits a QUARTER of the way across the DAY | DIVERGED | ChartTimeAxesTest.kt `axisFractionOf places a moment against the whole span and clamps outside it` | whole-span fraction helper covered generically; hydration day-chart wiring untested |
| the line is the running total, plotted at each drink's real hour | DIVERGED | ChartTimeAxesTest.kt `cumulative shape anchors at zero and plateaus out to the end fraction` | running-total shape covered on the generic helper; per-drink totals from real entries untested |
| TODAY ends the chart at now, not at midnight | DIVERGED | ChartTimeAxesTest.kt `cumulative shape anchors at zero and plateaus out to the end fraction` | plateau to an endFraction below 1.0 covered; the today-vs-past endFraction decision in HydrationPeriodContent.kt untested |
| a day with nothing logged says so, and draws no line | PORTED | ChartTimeAxesTest.kt `cumulative shape of an empty day is empty` | empty input yields no plot points, same substance |

## /home/manu/Documentos/repos/mobile-app/test/features/hydration/hydration_screen_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/androidTest/kotlin/tech/mmarca/openvitals/features/hydration/HydrationScreenWeekTest.kt (partial); logic overlap in HydrationViewModelTest.kt and PeriodSelectionDriverTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Hydration screen renders summary + bar chart once loaded | PORTED | HydrationPeriodContentTest: `rendersTheSummaryOnceLoaded` | Compose instrumentation; runs on a device, not in CI |
| the drink breakdown names the drink, never its package | DIVERGED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | naming-from-paired-record covered at VM level; "package is never a name" not asserted and the breakdown card itself is absent in Kotlin |
| the Day view lists each logged beverage | DIVERGED | HydrationViewModelTest.kt `load adds OpenVitals standalone nutrition drinks to hydration entries` | merged list incl. nutrition-only entries covered at VM level; "No hydration impact" label and SourceChip rendering unasserted |
| Hydration screen shows the empty placeholder with no data | PORTED | HydrationPeriodContentTest: `showsTheEmptyPlaceholderWithNoData` | Compose instrumentation; runs on a device, not in CI |
| opens on the day the dashboard was pinned to, not today | DIVERGED | PeriodSelectionDriverTest.kt `an initial date anchors the date but keeps the given range` | anchoring of the driver covered; propagation into the load query's anchorDate not asserted |
| opens on today when the caller had no day in mind | DIVERGED | HydrationViewModelTest.kt `nextPeriod DAY is blocked when selectedDate is today` | default-to-today only implicit (that test passes only if the default date is today); no direct assertion |
| Hydration screen shows the access gate when permission missing | PORTED | HealthConnectAccessGateTest: `insufficientAccess_replacesTheContentAndOffersToGrant` | Kotlin routes every screen gate through HealthConnectScreenShell, so it is pinned once rather than per screen |

## /home/manu/Documentos/repos/mobile-app/test/features/hydration/hydration_view_model_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/hydration/HydrationViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded period lands with its display precomputed | PORTED | HydrationViewModelTest.kt `load success populates hydration and derived totals` | hasData, totals, average, daily goal and goalMetDays asserted; cumulativeSamples has no Kotlin equivalent |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | HydrationViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | toScreenError() now maps Health Connect's SecurityException to ScreenError.PermissionDenied; error type, cleared loading and empty entries asserted |
| an unexpected failure carries its message to the screen | PORTED | HydrationViewModelTest.kt `load failure sets error and clears loading` | asserts ScreenError.Message("timeout") and isLoading false |
| refresh reloads the current selection in force mode | PORTED | HydrationViewModelTest.kt `refresh reloads the current selection in force mode` | no refresh() test; RefreshMode.FORCE only verified on the delete-reload path |
| a stale load cannot overwrite the newer one it lost to | PORTED | HydrationViewModelTest.kt `a stale load cannot overwrite the newer one it lost to` | LoadCoordinator single-flight guard: the superseded week load never paints |
| deleteHydrationEntry > removes an owned entry and deletes it through the repository | PORTED | HydrationViewModelTest.kt `deleteHydrationEntry removes entry and reloads period data` | delete verified, list emptied, force reload verified |
| deleteHydrationEntry > ignores a foreign entry it does not own | PORTED | HydrationViewModelTest.kt `deleteHydrationEntry ignores entries not created by OpenVitals` | no repo delete call, entries unchanged |
| deleteHydrationEntry > rolls the row back and surfaces the error when the delete fails | PORTED | HydrationViewModelTest.kt `deleteHydrationEntry rolls the row back and surfaces the error when the delete fails` | no failing-delete rollback test for hydration (exists for caffeine) |

## /home/manu/Documentos/repos/mobile-app/test/features/hydration/reminders/hydration_reminder_card_test.dart
Kotlin counterpart: none (card UI in HydrationCards.kt/HydrationPeriodContent.kt is untested; config logic overlaps HydrationViewModelTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| off: shows only the switch and the off summary | PORTED | HydrationReminderCardTest: `offShowsOnlyTheSwitchAndTheOffSummary` | Compose instrumentation; the ask-vs-enable decision lives in the switch lambda, so the card is the only way to reach it |
| on: shows the interval, the window and the goal note | PORTED | HydrationReminderCardTest: `onShowsTheIntervalAndTheWindow` | Compose instrumentation; the ask-vs-enable decision lives in the switch lambda, so the card is the only way to reach it |
| the stepper edits the interval and persists it | PORTED | HydrationViewModelTest.kt `updating reminder config saves normalized config` + `the reminder interval cannot go below the minimum` | Increase and decrease both covered; the summary copy is rendering |
| the stepper buttons disable at the interval bounds | PORTED | HydrationViewModelTest.kt `the reminder interval is clamped to its upper bound` + `the reminder interval cannot go below the minimum` | Clamping at both bounds asserted; the disabled-button affordance is rendering |
| toggling on persists the config | PORTED | HydrationViewModelTest.kt `updating reminder config saves normalized config` | setHydrationRemindersEnabled(true) → saved config enabled asserted |
| blocked by permission: warns and offers to grant | PORTED | HydrationReminderCardTest: `blockedByPermissionWarnsAndOffersToGrant` | Compose instrumentation; the ask-vs-enable decision lives in the switch lambda, so the card is the only way to reach it |
| flipping the switch on without permission asks instead | PORTED | HydrationReminderCardTest: `flippingTheSwitchOnWithoutPermissionAsksInsteadOfEnabling` | Compose instrumentation; the ask-vs-enable decision lives in the switch lambda, so the card is the only way to reach it |
| tapping a time row opens a time picker | PORTED | HydrationReminderCardTest: `tappingATimeRowOpensATimePickerAndReportsWhatItConfirms` | Compose instrumentation; runs on a device, not in CI |
| re-reads the permission when the app resumes | N/A-WIDGET | — | resume re-check exists in HydrationScreen.kt (lifecycle-bound), untested |

## /home/manu/Documentos/repos/mobile-app/test/features/hydration/reminders/hydration_reminder_controller_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/hydration/reminders/HydrationReminderControllerTest.kt and /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/hydration/reminders/HydrationReminderScheduleTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| disabled config clears and schedules nothing | PORTED | HydrationReminderControllerTest.kt `disabled config clears alarm and notification` | cancel verified, zero schedules verified |
| enabled config schedules a batch | PORTED | HydrationReminderControllerTest.kt `enabled config schedules next reminder` | Kotlin arms a single next alarm rather than a batch — same contract |
| anchors the first reminder to the last logged drink | PORTED | HydrationReminderScheduleTest.kt `anchor measures the countdown from the last drink` | exact anchor+interval time asserted (09:30+90=11:00) |
| the anchor read spans back into yesterday, not just today | PORTED | HydrationReminderControllerTest.kt `the anchor read spans back into yesterday, not just today` | yesterday..today range asserted on loadHydrationEntries |
| a met goal schedules only tomorrow onward | PORTED | HydrationReminderScheduleTest.kt `goal met schedules tomorrow after active start interval` | tomorrow's window-start-plus-interval asserted |
| an intake read failure counts as zero and still schedules | PORTED | HydrationReminderControllerTest.kt `an intake read failure counts as zero and still schedules` | `runCatching{...}.getOrDefault(0.0)` at HydrationReminderController.kt:191-195 untested |
| logging a drink re-anchors and reschedules | PORTED | HydrationReminderControllerTest.kt `logging a drink re-anchors and reschedules` | applyConfig() over the persisted config is Kotlin's onHydrationLogged |

## /home/manu/Documentos/repos/mobile-app/test/features/hydration/reminders/hydration_reminder_quick_add_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/hydration/reminders/HydrationReminderQuickAddTest.kt (the extracted quick-add amount/label helpers) and .../HydrationReminderControllerTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| hydrationQuickAddAmountMilliliters > round-trips the volume through the action id | N/A-FRAMEWORK | — | Kotlin carries the volume as a typed Intent extra (HydrationQuickAddReceiver), no string action-id codec exists |
| hydrationQuickAddAmountMilliliters > ignores anything that is not a quick-add action | N/A-FRAMEWORK | — | Kotlin filters by Intent action constant; Dart id-parsing plumbing has no counterpart |
| hydrationQuickAddAmountMilliliters > rejects out-of-range volumes from a stale schedule | PORTED | HydrationReminderControllerTest.kt `quick add ignores invalid volumes` | Zero, NaN, negative, oversized and infinite all refused |
| hydrationQuickAddAmountsMilliliters > falls back to a glass and a bottle for a fresh install | PORTED | HydrationReminderQuickAddTest.kt `quick add amounts fall back to a glass and a bottle for a fresh install` | identical logic exists in private `quickAddAmountsMilliliters()` (HydrationReminderNotificationService.kt:98), untested |
| hydrationQuickAddAmountsMilliliters > offers the last two used sizes, newest first | PORTED | HydrationReminderQuickAddTest.kt `quick add amounts offer the last two used sizes, newest first` | same untested private function plus PreferencesRepository recents ordering |
| hydrationQuickAddAmountsMilliliters > pads a single recent with the last custom amount, then defaults | PORTED | HydrationReminderQuickAddTest.kt `quick add amounts pad a single recent with the last custom amount, then defaults` | same untested private function (recents → lastCustom → fallback) |
| hydrationQuickAddAmountsMilliliters > never offers the same size twice | PORTED | HydrationReminderQuickAddTest.kt `quick add amounts never offer the same size twice` | dedupe (`value in amounts`) in same untested private function |
| hydrationReminderQuickAddActions > builds two silent actions labelled in millilitres for metric | PORTED | HydrationReminderQuickAddTest.kt `quick add actions are labelled in millilitres for metric` | Silent/cancel/color flags are notification plumbing with no Kotlin analog |
| hydrationReminderQuickAddActions > labels in fluid ounces for imperial | PORTED | HydrationReminderQuickAddTest.kt `quick add actions are labelled in fluid ounces for imperial` | Millilitres stay the payload unit via the typed Intent extra |
| HydrationQuickAddLogger > resolves Health Connect access, then logs plain water | DIVERGED | HydrationReminderControllerTest.kt `quick add logs water and reschedules after a real write` | write volume, remembered sizes and reschedule asserted; availability refresh and null drink-id not asserted |
| HydrationQuickAddLogger > a missing write permission logs nothing and leaves the schedule | PORTED | HydrationReminderControllerTest.kt `quick add refused by missing permission does not reschedule` | no write, no reschedule asserted |
| HydrationQuickAddLogger > a failing re-anchor never fails the logged drink | PORTED | HydrationReminderControllerTest.kt `a failing re-anchor never fails the logged drink` | fixed: HydrationReminderController.handleQuickAdd wraps the re-anchor in its own runCatching and logs the failure |

## /home/manu/Documentos/repos/mobile-app/test/features/hydration/reminders/hydration_reminder_settings_view_model_test.dart
Kotlin counterpart: none as a unit (settings live in HydrationViewModel + HydrationScreen; partial overlap in HydrationViewModelTest.kt and HydrationReminderControllerTest.kt)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| starts from the persisted config | PORTED | HydrationViewModelTest.kt `reminder config can be restored` | full config restore asserted |
| enabling persists the config and arms the alarm | DIVERGED | HydrationViewModelTest.kt `updating reminder config saves normalized config` + HydrationReminderControllerTest.kt `enabled config schedules next reminder` | persist and arming asserted in separate tests; the end-to-end enable→arm flow is not |
| the switch reflects a toggle-off before the schedule work lands | N/A-FRAMEWORK | — | Flutter-notifier async-ordering regression; Kotlin updates config state synchronously with scheduling decoupled into the controller |
| disabling persists and clears the alarm | DIVERGED | HydrationReminderControllerTest.kt `disabled config clears alarm and notification` | clearing asserted; disable-persist not asserted at VM level |
| enabling without permission asks first, and enables once granted | PORTED | HydrationReminderCardTest: `flippingTheSwitchOnWithoutPermissionAsksInsteadOfEnabling` | Compose instrumentation; the ask-vs-enable decision lives in the switch lambda, so the card is the only way to reach it |
| a denied permission leaves the reminder off rather than silently dead | PORTED | HydrationReminderCardTest: `flippingTheSwitchOnWithoutPermissionAsksInsteadOfEnabling` | Compose instrumentation; the ask-vs-enable decision lives in the switch lambda, so the card is the only way to reach it |
| granting permission for an already-enabled reminder arms it | N/A-WIDGET | — | no isBlockedByPermission state exists anywhere in the Kotlin reminder feature |
| openNotificationSettings opens the OS settings — the permanently-denied escape hatch | N/A-BEHAVIOR | — | blocked on behavior decision - no notification-settings deeplink exists in the Kotlin app |
| interval > steps by 30 minutes and re-arms | DIVERGED | HydrationViewModelTest.kt `updating reminder config saves normalized config` | one step + persist asserted; re-arm count per step not |
| interval > is clamped to its bounds, and the buttons disable there | PORTED | HydrationViewModelTest.kt `the reminder interval is clamped to its upper bound` | Button disabling is rendering |
| interval > rapid taps each step, even while a reschedule is in flight | N/A-FRAMEWORK | — | Flutter-notifier in-flight-reschedule race; structurally absent in Kotlin's synchronous config update |
| interval > cannot go below the minimum | PORTED | HydrationViewModelTest.kt `the reminder interval cannot go below the minimum` | lower-bound clamp untested |
| changing the active window persists and re-arms | DIVERGED | HydrationViewModelTest.kt `updating reminder config saves normalized config` | start-time set (with seconds stripped) and persist asserted; end time and re-arm not |
| refreshPermission picks up a revoked permission | N/A-WIDGET | — | the re-check is a LifecycleEventEffect(ON_RESUME) writing Compose-local state |
| exact alarms > an enabled reminder without exact alarms surfaces inexact timing | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin app has no exact-alarm feature (alarms use setAndAllowWhileIdle) |
| exact alarms > a disabled reminder never nags about timing | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin app has no exact-alarm feature (alarms use setAndAllowWhileIdle) |
| exact alarms > granting exact alarms clears the nudge and re-arms precisely | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin app has no exact-alarm feature (alarms use setAndAllowWhileIdle) |
| exact alarms > a declined exact-alarm request leaves timing inexact | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin app has no exact-alarm feature (alarms use setAndAllowWhileIdle) |
| exact alarms > refreshPermission picks up a revoked exact-alarm permission | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin app has no exact-alarm feature (alarms use setAndAllowWhileIdle) |

## /home/manu/Documentos/repos/mobile-app/test/features/mindfulness/mindfulness_display_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/mindfulness/MindfulnessPresentationMapperTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| summary folds total, count, average and longest | PORTED | MindfulnessPresentationMapperTest.kt `display has data when sessions exist` | totalMinutes, sessionCount, averageDurationMs and longestSessionMs all asserted |
| an empty period derives zeroes, not nulls | PORTED | MindfulnessPresentationMapperTest.kt `display has no data for empty sessions` | All five summary fields asserted zero; the cumulative curve is not part of the Kotlin display |
| the bar series sums minutes per day, not per session | PORTED | MindfulnessPresentationMapperTest.kt `daily minutes aggregate sessions by date` | Per-day values asserted, not only the aggregate |
| sessions are listed newest first | PORTED | MindfulnessPeriodContentTest: `sessionsAreListedNewestFirst` | Compose instrumentation; runs on a device, not in CI |

## /home/manu/Documentos/repos/mobile-app/test/features/mindfulness/mindfulness_intraday_chart_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt (generic day-plot logic only)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| cumulativeMindfulness banks the minutes when a session ENDS | PORTED | MindfulnessIntradayChartTest.kt: `cumulativeMindfulness banks the minutes when a session ENDS` | seam: cumulativeMindfulnessPoints private->internal |
| a zero-length session never enters the curve | N/A-BEHAVIOR | — | blocked on behavior decision - Kotlin maps every session (durationMs.coerceAtLeast(0)), so a zero-length sit yields a flat point instead of being dropped |
| a day with a session draws a plot, an empty day does not (testWidgets) | DIVERGED | ChartTimeAxesTest.kt `axisFractionOf places a moment against the whole span and clamps outside it`, `cumulative shape of an empty day is empty` | day-fraction placement and empty-day emptiness covered generically; the mindfulness card wiring (plot presence, 12:00 label) is Compose-only and untested |

## /home/manu/Documentos/repos/mobile-app/test/features/mindfulness/mindfulness_screen_test.dart
Kotlin counterpart: none (no Compose UI tests for mindfulness in app/src/androidTest)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders total card + session list once loaded | PORTED | MindfulnessPeriodContentTest: `rendersTheTotalCardAndSessionListOnceLoaded` | Compose instrumentation; runs on a device, not in CI |
| shows the access gate when the permission is missing | PORTED | HealthConnectAccessGateTest: `insufficientAccess_replacesTheContentAndOffersToGrant` | Kotlin routes every screen gate through HealthConnectScreenShell, so it is pinned once rather than per screen |
| shows the empty placeholder with no sessions | PORTED | MindfulnessPeriodContentTest: `showsTheEmptyPlaceholderWithNoSessions` | Compose instrumentation; runs on a device, not in CI |
| rolling week mode names every period title "Last 30 days" | N/A-WIDGET | — | title-naming logic separately covered by PeriodTitleTest.kt `rollingPeriodTitlesUseFixedDayWindowLabels`; the "same name on all 3 surfaces" wiring is rendering-only |
| calendar week mode keeps the "This month" titles | PORTED | MindfulnessPeriodContentTest: `calendarWeekModeNamesEveryPeriodTitleTheSameWay` | Compose instrumentation; runs on a device, not in CI |

## /home/manu/Documentos/repos/mobile-app/test/features/mindfulness/mindfulness_view_model_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/mindfulness/MindfulnessViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded period lands with its display precomputed | PORTED | MindfulnessViewModelTest.kt `load success populates sessions and derived total minutes` | hasData, totals, session count, selectedRange and no error; the cumulative curve lives in the composable |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | MindfulnessViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | error type, cleared loading and empty sessions asserted |
| an unexpected failure carries its message to the screen | PORTED | MindfulnessViewModelTest.kt `load failure sets error and clears loading` | asserts ScreenError.Message("timeout") and loading cleared |
| refresh reloads the current selection in force mode | PORTED | MindfulnessViewModelTest.kt `refresh reloads the current selection in force mode` | `resumeCurrentPeriod(refreshCurrent = true)` → load(FORCE) exists (MindfulnessViewModel.kt:169) but is untested; FORCE is only verified inside the delete test |
| a stale load cannot overwrite the newer one it lost to | PORTED | MindfulnessViewModelTest.kt `a stale load cannot overwrite the newer one it lost to` | LoadCoordinator single-flight guard |
| deleteMindfulnessSession > removes an owned session and deletes it through the repository | PORTED | MindfulnessViewModelTest.kt `deleteMindfulnessSessionEntry removes OpenVitals session and reloads` | delete verified, list empties, display totals zeroed; reload-based rather than optimistic but equivalent assertions |
| deleteMindfulnessSession > ignores a foreign session it does not own | PORTED | MindfulnessViewModelTest.kt `deleteMindfulnessSessionEntry ignores sessions not created by OpenVitals` | repo delete verified never called, list unchanged |
| deleteMindfulnessSession > rolls the row back and surfaces the error when the delete fails | PORTED | MindfulnessViewModelTest.kt `deleteMindfulnessSessionEntry rolls the row back and surfaces the error when the delete fails` | no failed-delete test for mindfulness (the nutrition VM has one; mindfulness does not) |

## /home/manu/Documentos/repos/mobile-app/test/features/mindfulness/reminders/mindfulness_reminder_controller_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/mindfulness/reminders/MindfulnessReminderControllerTest.kt and .../MindfulnessReminderScheduleTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| disabled config clears and schedules nothing | PORTED | MindfulnessReminderControllerTest.kt `disabled config clears alarm and notification` | cancel verified, schedule never called (also asserts notification cancel — stronger) |
| enabled config schedules a batch at the daily time | PORTED | MindfulnessReminderControllerTest.kt `enabled config schedules next reminder` + MindfulnessReminderScheduleTest.kt `next reminder before configured time schedules today` | schedule-call and the exact today-at-18:00 trigger time asserted across the two tests |
| a met goal schedules only tomorrow onward | PORTED | MindfulnessReminderScheduleTest.kt `goal met schedules tomorrow` (+ controller `alarm trigger does not notify after goal is met`) | exact tomorrow-at-18:00 trigger asserted |

## /home/manu/Documentos/repos/mobile-app/test/features/mindfulness/reminders/mindfulness_reminder_settings_view_model_test.dart
Kotlin counterpart: partial — /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/mindfulness/MindfulnessViewModelTest.kt (no dedicated settings VM exists; permission gating lives in MindfulnessScreen.kt Compose code)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| starts from the persisted config | DIVERGED | MindfulnessViewModelTest.kt `initial reminder config uses default disabled daily reminder` / `mindfulness reminder config updates and persists` | initial config only enters via constructor param in the test; the prefs-read wiring is untested |
| enabling persists the config and arms the alarm | DIVERGED | MindfulnessViewModelTest.kt `mindfulness reminder config updates and persists` + MindfulnessReminderControllerTest.kt `enabled config schedules next reminder` | persist and arm are asserted in separate tests; the VM→controller wiring (onReminderConfigChanged) is untested end-to-end |
| disabling persists and clears the alarm | DIVERGED | MindfulnessViewModelTest.kt `mindfulness reminder config updates and persists` + MindfulnessReminderControllerTest.kt `disabled config clears alarm and notification` | same split: persist and clear asserted separately, never together |
| enabling without permission asks first, and enables once granted | N/A-WIDGET | — | Kotlin has no reminder-settings view model; the permission flow is Compose-local (remember + rememberLauncherForActivityResult) |
| a denied permission leaves the reminder off rather than silently dead | N/A-WIDGET | — | Kotlin has no reminder-settings view model; the permission flow is Compose-local (remember + rememberLauncherForActivityResult) |
| granting permission for an already-enabled reminder arms it | N/A-WIDGET | — | no isBlockedByPermission state exists anywhere in the Kotlin reminder feature |
| openNotificationSettings opens the OS settings — the permanently-denied escape hatch | N/A-BEHAVIOR | — | blocked on behavior decision - no notification-settings deeplink exists in the Kotlin app |
| changing the reminder time persists and re-arms | DIVERGED | MindfulnessViewModelTest.kt `mindfulness reminder config updates and persists` | time change + persist asserted; re-arm not asserted in the same test |
| refreshPermission picks up a revoked permission | N/A-WIDGET | — | the re-check is a LifecycleEventEffect(ON_RESUME) writing Compose-local state |
| exact alarms > an enabled reminder without exact alarms surfaces inexact timing | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin app has no exact-alarm feature (alarms use setAndAllowWhileIdle) |
| exact alarms > granting exact alarms clears the nudge and re-arms precisely | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin app has no exact-alarm feature (alarms use setAndAllowWhileIdle) |
| exact alarms > a declined exact-alarm request leaves timing inexact | N/A-BEHAVIOR | — | blocked on behavior decision - the Kotlin app has no exact-alarm feature (alarms use setAndAllowWhileIdle) |

## /home/manu/Documentos/repos/mobile-app/test/features/caffeine/caffeine_display_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/caffeine/CaffeineDisplayTest.kt (over the CaffeineScreen.kt derivations widened/extracted to internal) and CaffeineInsightCalculatorTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an empty load leaves the bars empty and the scale non-zero | PORTED | CaffeineDisplayTest.kt `an empty load leaves the bars empty and the scale non-zero` | curve-max floor `maxOf(..., 1.0)` (CaffeineScreen.kt:742) and empty-bar handling untested |
| the sleep verdict compares bedtime first, then right now | PORTED | CaffeineDisplayTest.kt `the sleep verdict compares bedtime first, then right now` | `caffeineSleepImpactStatus` is a private function in CaffeineScreen.kt:527, untested |
| the bedtime card is safe exactly at the threshold | PORTED | CaffeineDisplayTest.kt `the bedtime card is safe exactly at the threshold` | at-threshold boundary (`>` not `>=`) untested in both CaffeineScreen.kt and CaffeineInsightCalculatorTest |
| the curve maximum fits the tallest point and the threshold | PORTED | CaffeineDisplayTest.kt `the curve maximum fits the tallest point and the threshold` | `maxOf(points.maxOf{...}, thresholdMg, 1.0)` at CaffeineScreen.kt:742 untested |
| a distribution card shows six bars, scaled against their own tallest | PORTED | CaffeineDisplayTest.kt `a distribution card shows six bars, scaled against their own tallest` | Kotlin scales over all slices before take(6); identical for sorted input |

## /home/manu/Documentos/repos/mobile-app/test/features/caffeine/caffeine_drink_screen_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| renders the drink it was opened for, by name and dose | PORTED | CaffeineDrinkScreenTest: `theScreenShowsTheDrinkItWasOpenedForByNameAndDose` | Compose instrumentation; runs on a device, not in CI |
| a drink deleted while its screen was open degrades to "no data" | PORTED | CaffeineDrinkScreenTest: `aDrinkDeletedWhileItsScreenWasOpenDegradesToNoData` | Compose instrumentation; runs on a device, not in CI |

## /home/manu/Documentos/repos/mobile-app/test/features/caffeine/caffeine_screen_test.dart
Kotlin counterpart: none
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Caffeine screen renders curve + guidance cards once loaded | PORTED | CaffeineContentTest: `aLoadedDayLeadsWithTheActiveDoseAndTheDecayCurve` | Compose instrumentation; runs on a device, not in CI |
| Caffeine screen shows the access gate when permission missing | PORTED | HealthConnectAccessGateTest: `insufficientAccess_replacesTheContentAndOffersToGrant` | Kotlin routes every screen gate through HealthConnectScreenShell, so it is pinned once rather than per screen |

## /home/manu/Documentos/repos/mobile-app/test/features/caffeine/caffeine_view_model_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/caffeine/CaffeineViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded window lands with its display precomputed | DIVERGED | CaffeineViewModelTest.kt `first load shows setup when caffeine exists and profile is incomplete` | todayTotalMg asserted (plus insights via CaffeineInsightCalculatorTest `build ...`); curveEntryTimes/curveMaxMg/sourceBars/topSourceLabel derivations have no tested Kotlin unit |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | CaffeineViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | error type, cleared loading and an empty curve asserted (Kotlin's display is non-nullable) |
| an unexpected failure carries its message to the screen | PORTED | CaffeineViewModelTest.kt `an unexpected failure carries its message to the screen` | load-failure error mapping untested (only the delete path asserts ScreenError.Message, in `a failed delete restores the drink and surfaces the error`) |
| picking an analytics range reloads over the wider window | PORTED | CaffeineViewModelTest.kt `analytics range selection reloads matching caffeine window` | Includes the same-range-is-not-a-reload dedupe |
| refresh reloads in force mode | PORTED | CaffeineViewModelTest.kt `refresh reloads with force mode` | RefreshMode.FORCE verified |
| an empty load still gives the screen a display to render | PORTED | CaffeineViewModelTest.kt `an empty load still gives the screen a display to render` | homeDisplay is non-nullable in Kotlin, so the default display is what is asserted |
| deleteCaffeineEntry > removes an owned drink and deletes its nutrition record | PORTED | CaffeineViewModelTest.kt `deleting a drink removes it optimistically and force-reloads` | nutrition delete, trimmed list and force reload asserted |
| deleteCaffeineEntry > ignores a foreign drink it does not own | PORTED | CaffeineViewModelTest.kt `a foreign or unidentified drink is never deleted` | also covers blank/missing ids |
| deleteCaffeineEntry > rolls the row back and surfaces the error on failure | PORTED | CaffeineViewModelTest.kt `a failed delete restores the drink and surfaces the error` | restored list and ScreenError.Message asserted |
## /home/manu/Documentos/repos/mobile-app/test/features/nutrition/nutrition_display_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/nutrition/NutritionPresentationMapperTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| an empty period has no data, and every series is empty | DIVERGED | NutritionPresentationMapperTest.kt `display has no data for empty macros and entries` | asserts hasData false and zero totals but not empty series lists, null macroSplit or empty entries |
| a series folds total, average, best and logged days | DIVERGED | NutritionPresentationMapperTest.kt `metric display tracks goal progress and period comparison` | asserts total/average/loggedDays but never bestDayValue, and has no unlogged-day-excluded-from-average case |
| tracked nutrients split into primary and grouped additional ones | PORTED | NutritionDisplayTest.kt: `tracked nutrients split into primary and grouped additional ones` | — |
| the macro split needs macros, and the comparison needs a previous period | PORTED | NutritionPresentationMapperTest.kt `macro split is computed when macros are present` + `metric display tracks goal progress and period comparison` | macroSplit non-null and comparison current/previous values asserted across the two tests |
| the day curve accumulates in time order, skipping absent readings | PORTED | NutritionDisplayTest.kt: `the day curve accumulates in time order, skipping absent readings` | — |
| meals are listed newest first, and indexed by their day | PORTED | NutritionDisplayTest.kt: `meals are listed newest first, and indexed by their day` | seam: nutritionEntriesNewestFirst / nutritionEntriesOnDay extracted from three identical inline sites |
| goal progress counts the days that met the goal | DIVERGED | NutritionPresentationMapperTest.kt `metric display tracks goal progress and period comparison` | only asserts goalProgress non-null; goalMetDays counting is covered generically in DailyGoalsTest.kt, not for the nutrition display |

## /home/manu/Documentos/repos/mobile-app/test/features/nutrition/nutrition_intraday_chart_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/ui/charts/ChartTimeAxesTest.kt (generic day-plot logic only)
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| cumulativeNutritionPoints (Kotlin parity) > sorts by time, accumulates, and drops non-positive readings | PORTED | NutritionIntradayChartTest.kt: `cumulativeNutritionPoints sorts by time, accumulates, and drops non-positive readings` | seam: cumulativeNutritionPoints private->internal |
| cumulativeNutritionPoints (Kotlin parity) > an entry with no value for the nutrient is skipped | PORTED | NutritionIntradayChartTest.kt: `cumulativeNutritionPoints an entry with no value for the nutrient is skipped` | — |
| NutritionIntradayChartCard > plots the cumulative curve bracketed by 0 and the total | DIVERGED | ChartTimeAxesTest.kt `cumulative shape anchors at zero and plateaus out to the end fraction` + `full viewport gives the classic five hour labels` | zero anchor, plateau and 24:00 axis label covered generically; nutrition-specific totals/min-max and card wiring unasserted |
| NutritionIntradayChartCard > today plots a meal at its real hour and stops the line at now | DIVERGED | ChartTimeAxesTest.kt `axisFractionOf places a moment against the whole span and clamps outside it` + `cumulative shape anchors at zero and plateaus out to the end fraction` | the whole-day-fraction regression and stop-at-endFraction are covered generically; the isToday → endFraction=now wiring in the composable is untested |
| NutritionIntradayChartCard > renders the empty-day message and no plot without entries | PORTED | NutritionContentTest: `aDayWithNoMealsSaysSoInsteadOfDrawingALineFromNothingToNothing` | Compose instrumentation; runs on a device, not in CI |

## /home/manu/Documentos/repos/mobile-app/test/features/nutrition/nutrition_screen_test.dart
Kotlin counterpart: none for the screen cases; /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/core/presentation/MetricDetailSectionOrderViewModelTest.kt for the reorder case
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| Protein metric screen renders hero, chart, goal card, statistics and meals | PORTED | NutritionMetricScreenTest: `aTrackedProteinPeriodRendersItsHeroAndItsChart`, `…ItsGoalCardStatisticsAndMeals` | Compose instrumentation; runs on a device, not in CI |
| Protein metric screen shows placeholder with no data | PORTED | NutritionContentTest: `aMetricScreenShowsThePlaceholderWithNoData` | Compose instrumentation; runs on a device, not in CI |
| Nutrition overview renders grouped nutrient statistics | PORTED | NutritionContentTest: `theOverviewShowsThePlaceholderWithNoData` | Compose instrumentation; runs on a device, not in CI |
| Nutrition screen shows the access gate when permission missing | PORTED | HealthConnectAccessGateTest: `insufficientAccess_replacesTheContentAndOffersToGrant` | Kotlin routes every screen gate through HealthConnectScreenShell, so it is pinned once rather than per screen |
| Reordering a metric detail section persists across rebuilds | DIVERGED | MetricDetailSectionOrderViewModelTest.kt `moveSectionToTarget_reordersAndPersists` + `initialOrder_usesDefaultWhenPreferencesMissing` | reorder and persist-call verified via mocked prefs, but no round-trip re-read through a fresh instance as Flutter asserts |

## /home/manu/Documentos/repos/mobile-app/test/features/nutrition/nutrition_view_model_test.dart
Kotlin counterpart: /home/manu/Documentos/repos/openvitals-android/app/src/test/kotlin/tech/mmarca/openvitals/features/nutrition/NutritionViewModelTest.kt
| Flutter case | Status | Kotlin test | Note |
|---|---|---|---|
| a loaded period lands with its display precomputed | DIVERGED | NutritionViewModelTest.kt `load success populates macros entries and derived totals` | asserts display period totals (all four macros) but not metric average or goalProgress.target == dailyGoal |
| nudging the daily goal rebuilds the display against the new target | PORTED | NutritionViewModelTest.kt: `nudging the daily goal rebuilds the display against the new target` | — |
| a permission failure becomes ScreenErrorPermissionDenied | PORTED | NutritionViewModelTest.kt: `a permission failure becomes ScreenError PermissionDenied` | a thrown SecurityException now maps to ScreenError.PermissionDenied; error type, cleared loading and empty entries asserted |
| an unexpected failure carries its message to the screen | PORTED | NutritionViewModelTest.kt `load failure sets error and clears loading` | asserts ScreenError.Message("timeout") and loading cleared |
| refresh reloads the current selection in force mode | PORTED | NutritionViewModelTest.kt: `refresh reloads the current selection in force mode` | — |
| a stale load cannot overwrite the newer one it lost to | PORTED | NutritionViewModelTest.kt: `a stale load cannot overwrite the newer one it lost to` | — |
| deleteNutritionEntry > removes an owned meal and deletes it through the repository | PORTED | NutritionViewModelTest.kt `deleting an entry removes it optimistically and force-reloads` | delete verified, list trimmed, FORCE reload verified, no error |
| deleteNutritionEntry > ignores a foreign meal it does not own | PORTED | NutritionViewModelTest.kt `a foreign or unidentified entry is never deleted` | also covers blank and unknown ids — stronger than the Flutter case |
| deleteNutritionEntry > rolls the row back and surfaces the error when the delete fails | PORTED | NutritionViewModelTest.kt `a failed delete restores the entry and surfaces the error` | entry restored and ScreenError.Message("denied") asserted |

## Summary

Scope: test/features/manualentry (incl. activity/ and recording/), test/features/hydration, test/features/mindfulness, test/features/caffeine, test/features/nutrition — 61 Flutter test files, 537 test cases.

| Status | Count |
|---|---|
| PORTED | 381 |
| DIVERGED | 38 |
| N/A-WIDGET | 92 |
| N/A-FRAMEWORK | 9 |
| N/A-BEHAVIOR | 17 |
| **Total** | **537** |

### Portable gaps

None. Every remaining non-ported case carries N/A-WIDGET (Compose-local reminder/permission
state with no view-model seam) or N/A-BEHAVIOR (no exact-alarm feature, no notification-settings
deeplink, and the deliberate keep-the-display-through-a-load choice) with the reason in its Note
column.

### Status legend

- **PORTED** — a Kotlin test asserts the same behavior as the Flutter case.
- **DIVERGED** — a Kotlin test covers the area but asserts less, or the Kotlin behavior itself differs (see the row note).
- **MISSING** — no Kotlin coverage; JVM-portable.
- **N/A-WIDGET** — Compose-rendering-only, no JVM seam.
- **N/A-BEHAVIOR** — the behavior does not exist in the Kotlin port, so there is nothing to assert.
- **N/A-FRAMEWORK** — Flutter-framework plumbing with no Kotlin counterpart.
