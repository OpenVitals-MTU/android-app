package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.util.Locale

/**
 * The machine-stable names of Health Connect's exercise types: the constants in
 * snake_case. The workout CSV export writes them and the CSV importer reads them
 * back, so a file the app wrote imports with the type it had. Do not rename an
 * entry: files in the wild carry these words.
 */
object ExerciseTypeNames {
    /** The name of a type this table does not know. Reads back as an "other" workout. */
    const val Fallback = "workout"

    val byType: Map<Int, String> = mapOf(
        ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON to "badminton",
        ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL to "baseball",
        ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL to "basketball",
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING to "biking",
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to "biking_stationary",
        ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP to "boot_camp",
        ExerciseSessionRecord.EXERCISE_TYPE_BOXING to "boxing",
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS to "calisthenics",
        ExerciseSessionRecord.EXERCISE_TYPE_CRICKET to "cricket",
        ExerciseSessionRecord.EXERCISE_TYPE_DANCING to "dancing",
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL to "elliptical",
        ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS to "exercise_class",
        ExerciseSessionRecord.EXERCISE_TYPE_FENCING to "fencing",
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN to "football_american",
        ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AUSTRALIAN to "football_australian",
        ExerciseSessionRecord.EXERCISE_TYPE_FRISBEE_DISC to "frisbee_disc",
        ExerciseSessionRecord.EXERCISE_TYPE_GOLF to "golf",
        ExerciseSessionRecord.EXERCISE_TYPE_GUIDED_BREATHING to "guided_breathing",
        ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS to "gymnastics",
        ExerciseSessionRecord.EXERCISE_TYPE_HANDBALL to "handball",
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING to "high_intensity_interval_training",
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING to "hiking",
        ExerciseSessionRecord.EXERCISE_TYPE_ICE_HOCKEY to "ice_hockey",
        ExerciseSessionRecord.EXERCISE_TYPE_ICE_SKATING to "ice_skating",
        ExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS to "martial_arts",
        ExerciseSessionRecord.EXERCISE_TYPE_PADDLING to "paddling",
        ExerciseSessionRecord.EXERCISE_TYPE_PARAGLIDING to "paragliding",
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES to "pilates",
        ExerciseSessionRecord.EXERCISE_TYPE_RACQUETBALL to "racquetball",
        ExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING to "rock_climbing",
        ExerciseSessionRecord.EXERCISE_TYPE_ROLLER_HOCKEY to "roller_hockey",
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING to "rowing",
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE to "rowing_machine",
        ExerciseSessionRecord.EXERCISE_TYPE_RUGBY to "rugby",
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to "running",
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to "running_treadmill",
        ExerciseSessionRecord.EXERCISE_TYPE_SAILING to "sailing",
        ExerciseSessionRecord.EXERCISE_TYPE_SCUBA_DIVING to "scuba_diving",
        ExerciseSessionRecord.EXERCISE_TYPE_SKATING to "skating",
        ExerciseSessionRecord.EXERCISE_TYPE_SKIING to "skiing",
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING to "snowboarding",
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING to "snowshoeing",
        ExerciseSessionRecord.EXERCISE_TYPE_SOCCER to "soccer",
        ExerciseSessionRecord.EXERCISE_TYPE_SOFTBALL to "softball",
        ExerciseSessionRecord.EXERCISE_TYPE_SQUASH to "squash",
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to "stair_climbing",
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE to "stair_climbing_machine",
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to "strength_training",
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING to "stretching",
        ExerciseSessionRecord.EXERCISE_TYPE_SURFING to "surfing",
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to "swimming_open_water",
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to "swimming_pool",
        ExerciseSessionRecord.EXERCISE_TYPE_TABLE_TENNIS to "table_tennis",
        ExerciseSessionRecord.EXERCISE_TYPE_TENNIS to "tennis",
        ExerciseSessionRecord.EXERCISE_TYPE_VOLLEYBALL to "volleyball",
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING to "walking",
        ExerciseSessionRecord.EXERCISE_TYPE_WATER_POLO to "water_polo",
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING to "weightlifting",
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR to "wheelchair",
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA to "yoga",
    )

    /** The inverse, plus [Fallback], which the export writes for a type it cannot name. */
    val byName: Map<String, Int> =
        byType.entries.associate { (type, name) -> name to type } +
            (Fallback to ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT)

    fun exportName(type: Int): String = byType[type] ?: Fallback

    /** The type [name] stands for, ignoring case and surrounding space, or null. */
    fun typeForExportName(name: String): Int? = byName[name.trim().lowercase(Locale.US)]
}
