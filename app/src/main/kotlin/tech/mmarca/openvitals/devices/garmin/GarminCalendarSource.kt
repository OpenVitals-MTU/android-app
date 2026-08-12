package tech.mmarca.openvitals.devices.garmin

import android.Manifest
import android.content.Context
import android.content.ContentUris
import android.content.pm.PackageManager
import android.provider.CalendarContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the phone's calendar for the watch's calendar glance.
 *
 * Queries `CalendarContract.Instances` rather than `Events` so recurring
 * events arrive already expanded into their occurrences — the watch wants
 * "what is happening this week", not recurrence rules.
 *
 * The read happens only while answering a watch that asked, and the result
 * goes to the wrist over BLE and nowhere else. Nothing is stored: the watch
 * re-asks whenever it wants fresher events, and a copy would only be a second
 * place calendar data lives.
 */
@Singleton
class GarminCalendarSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * The event instances overlapping the window, soonest first. Empty when
     * permission is missing — the toggle asks for it, but a grant can be
     * revoked in the OS settings long after, and the watch's ask must still
     * be answered.
     */
    fun events(beginEpochSeconds: Long, endEpochSeconds: Long): List<GarminCalendarEvent> {
        if (!hasPermission()) {
            GarminLog.log("[GARMIN-CAL] calendar sync is on but READ_CALENDAR is not granted")
            return emptyList()
        }

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let { builder ->
            ContentUris.appendId(builder, beginEpochSeconds * 1000L)
            ContentUris.appendId(builder, endEpochSeconds * 1000L)
            builder.build()
        }
        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.ORGANIZER,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.STATUS,
        )

        val events = mutableListOf<GarminCalendarEvent>()
        runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    // A declined or cancelled meeting is not on the wearer's
                    // day, however firmly it sits in the calendar.
                    val status = cursor.getInt(7)
                    if (status == CalendarContract.Instances.STATUS_CANCELED) continue
                    events.add(
                        GarminCalendarEvent(
                            title = cursor.getString(2).orEmpty()
                                .ifBlank { UNTITLED_EVENT },
                            location = cursor.getString(3)?.takeIf { it.isNotBlank() },
                            description = cursor.getString(4)?.takeIf { it.isNotBlank() },
                            organizer = cursor.getString(5)?.takeIf { it.isNotBlank() },
                            startEpochSeconds = cursor.getLong(0) / 1000L,
                            endEpochSeconds = cursor.getLong(1) / 1000L,
                            allDay = cursor.getInt(6) == 1,
                        ),
                    )
                }
            }
        }.onFailure {
            GarminLog.log("[GARMIN-CAL] calendar query failed: ${it.message}")
        }
        return events.sortedBy { it.startEpochSeconds }
    }

    private companion object {
        /** The glance renders a nameless event as a blank line; name it instead. */
        const val UNTITLED_EVENT = "(untitled)"
    }
}
