package tech.mmarca.openvitals.core.privacy

import com.google.common.truth.Truth.assertWithMessage

/**
 * Values that stand for health data and personal details in tests of text
 * that leaves the app. Seed them into a report, a crash or a log line, then
 * assert with [assertNoneIn] that none comes out.
 */
object PrivacySentinels {
    const val Value = "72.5"
    const val Date = "2026-03-14"
    const val Instant = "2026-03-14T07:30:00Z"
    const val Mac = "AA:BB:CC:DD:EE:FF"
    const val Uuid = "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
    const val Email = "jane.doe@example.org"
    const val Phone = "+39 333 123 4567"
    const val Uri = "content://com.android.providers.downloads.documents/document/42"
    const val Path = "/storage/emulated/0/Download/jane-export.zip"
    const val Name = "Jane Doe"
    const val Coordinates = "45.46421, 9.19003"

    val all: List<String> = listOf(Value, Date, Instant, Mac, Uuid, Email, Phone, Uri, Path, Name, Coordinates)

    /** One line carrying every sentinel, as an exception message or a log line might. */
    const val Sentence =
        "value $Value on $Date at $Instant from $Mac id $Uuid mail $Email phone $Phone " +
            "file $Uri path $Path by $Name near $Coordinates"

    /** Fails on the first sentinel [text] still holds, or on a telling part of one, unless listed in [except]. */
    fun assertNoneIn(text: String, except: Set<String> = emptySet()) {
        val fragments = mapOf(
            Value to listOf(Value),
            Date to listOf(Date),
            Instant to listOf(Instant, "07:30"),
            Mac to listOf(Mac, "DD:EE:FF"),
            Uuid to listOf(Uuid, "0305e82c3301"),
            Email to listOf(Email, "example.org"),
            Phone to listOf(Phone, "123 4567"),
            Uri to listOf(Uri, "content://"),
            Path to listOf(Path, "jane-export"),
            Name to listOf(Name),
            Coordinates to listOf(Coordinates, "45.46421"),
        )
        fragments.filterKeys { it !in except }.forEach { (sentinel, parts) ->
            parts.forEach { part ->
                assertWithMessage("sentinel %s leaked as %s in:\n%s", sentinel, part, text).that(text).doesNotContain(part)
            }
        }
    }
}
