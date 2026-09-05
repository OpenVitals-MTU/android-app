package tech.mmarca.openvitals.devices.garmin

/**
 * The capabilities a watch declares in the CONFIGURATION handshake: a
 * 15-byte bitmap, bit `n` in byte `n / 8`. Declaration order is the wire
 * format and the name is persisted, so never reorder, remove or rename.
 * From Gadgetbridge (AGPLv3).
 */
enum class GarminCapability {
    CONNECT_MOBILE_FIT_LINK,
    GOLF_FIT_LINK,
    VIVOKID_JR_FIT_LINK,
    SYNC,
    DEVICE_INITIATES_SYNC,
    HOST_INITIATED_SYNC_REQUESTS,
    GNCS,
    ADVANCED_MUSIC_CONTROLS,
    FIND_MY_PHONE,
    FIND_MY_WATCH,
    CONNECTIQ_HTTP,
    CONNECTIQ_SETTINGS,
    CONNECTIQ_WATCH_APP_DOWNLOAD,
    CONNECTIQ_WIDGET_DOWNLOAD,
    CONNECTIQ_WATCH_FACE_DOWNLOAD,
    CONNECTIQ_DATA_FIELD_DOWNLOAD,
    CONNECTIQ_APP_MANAGEMENT,
    COURSE_DOWNLOAD,
    WORKOUT_DOWNLOAD,
    GOLF_COURSE_DOWNLOAD,
    DELTA_SOFTWARE_UPDATE_FILES,
    FITPAY,
    LIVETRACK,
    LIVETRACK_AUTO_START,
    LIVETRACK_MESSAGING,
    GROUP_LIVETRACK,
    WEATHER_CONDITIONS,
    WEATHER_ALERTS,
    GPS_EPHEMERIS_DOWNLOAD,
    EXPLICIT_ARCHIVE,
    SWING_SENSOR,
    SWING_SENSOR_REMOTE,
    INCIDENT_DETECTION,
    TRUEUP,
    INSTANT_INPUT,
    SEGMENTS,
    AUDIO_PROMPT_LAP,
    AUDIO_PROMPT_PACE_SPEED,
    AUDIO_PROMPT_HEART_RATE,
    AUDIO_PROMPT_POWER,
    AUDIO_PROMPT_NAVIGATION,
    AUDIO_PROMPT_CADENCE,
    SPORT_GENERIC,
    SPORT_RUNNING,
    SPORT_CYCLING,
    SPORT_TRANSITION,
    SPORT_FITNESS_EQUIPMENT,
    SPORT_SWIMMING,
    STOP_SYNC_AFTER_SOFTWARE_UPDATE,
    CALENDAR,
    WIFI_SETUP,
    SMS_NOTIFICATIONS,
    BASIC_MUSIC_CONTROLS,
    AUDIO_PROMPTS_SPEECH,
    DELTA_SOFTWARE_UPDATES,
    GARMIN_DEVICE_INFO_FILE_TYPE,
    SPORT_PROFILE_SETUP,
    HSA_SUPPORT,
    SPORT_STRENGTH,
    SPORT_CARDIO,
    UNION_PAY,
    IPASS,
    CIQ_AUDIO_CONTENT_PROVIDER,
    UNION_PAY_INTERNATIONAL,
    REQUEST_PAIR_FLOW,
    LOCATION_UPDATE,
    LTE_SUPPORT,
    DEVICE_DRIVEN_LIVETRACK_SUPPORT,
    CUSTOM_CANNED_TEXT_LIST_SUPPORT,
    EXPLORE_SYNC,
    INCIDENT_DETECT_AND_ASSISTANCE,
    CURRENT_TIME_REQUEST_SUPPORT,
    CONTACTS_SUPPORT,
    LAUNCH_REMOTE_CIQ_APP_SUPPORT,
    DEVICE_MESSAGES,
    WAYPOINT_TRANSFER,
    MULTI_LINK_SERVICE,
    OAUTH_CREDENTIALS,
    GOLF_9_PLUS_9,
    ANTI_THEFT_ALARM,
    INREACH,
    EVENT_SHARING,
    UNK_82,
    UNK_83,
    UNK_84,
    UNK_85,
    UNK_86,
    UNK_87,
    UNK_88,
    UNK_89,
    UNK_90,
    UNK_91,
    REALTIME_SETTINGS,
    UNK_93,
    UNK_94,
    UNK_95,
    UNK_96,
    UNK_97,
    UNK_98,
    UNK_99,
    UNK_100,
    UNK_101,
    UNK_102,
    UNK_103,
    UNK_104,
    UNK_105,
    UNK_106,
    UNK_107,
    UNK_108,
    UNK_109,
    UNK_110,
    UNK_111,
    UNK_112,
    UNK_113,
    UNK_114,
    UNK_115,
    UNK_116,
    UNK_117,
    UNK_118,
    UNK_119,
    ;

    /** Garmin's own name, for logs and persistence. */
    val wireName: String
        get() = name

    /** Index in the bitmap, which is simply declaration order. */
    val bit: Int
        get() = ordinal

    companion object {
        /** Decodes the bitmap. A short buffer is not an error; missing bytes are absent flags. */
        fun decode(bits: ByteArray): Set<GarminCapability> {
            val out = mutableSetOf<GarminCapability>()
            for (capability in entries) {
                val byte = capability.bit / 8
                if (byte >= bits.size) break
                if (bits[byte].toInt() and (1 shl (capability.bit % 8)) != 0) out.add(capability)
            }
            return out
        }
    }
}
