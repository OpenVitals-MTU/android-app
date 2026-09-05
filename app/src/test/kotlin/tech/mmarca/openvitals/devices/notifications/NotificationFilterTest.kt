package tech.mmarca.openvitals.devices.notifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The filter runs on every notification the phone posts; a bug here means a watch that buzzes constantly. */
class NotificationFilterTest {

    private val ownPackage = "tech.mmarca.openvitals"

    private val enabled = NotificationFilter.Config(
        enabled = true,
        blockedPackages = emptySet(),
        watchAddress = "AA:BB:CC:DD:EE:FF",
    )

    private fun candidate(
        packageName: String = "com.example.chat",
        title: String? = "Ada",
        body: String? = "On my way",
        ongoing: Boolean = false,
        foregroundService: Boolean = false,
        groupSummary: Boolean = false,
        localOnly: Boolean = false,
        channelImportance: Int = 3,
    ) = NotificationFilter.Candidate(
        packageName = packageName,
        title = title,
        body = body,
        ongoing = ongoing,
        foregroundService = foregroundService,
        groupSummary = groupSummary,
        localOnly = localOnly,
        channelImportance = channelImportance,
    )

    private fun verdict(
        candidate: NotificationFilter.Candidate = candidate(),
        config: NotificationFilter.Config = enabled,
        interruptionFilter: Int = NotificationFilter.INTERRUPTION_FILTER_ALL,
    ) = NotificationFilter.verdict(candidate, config, ownPackage, interruptionFilter)

    @Test
    fun `an ordinary message is kept`() {
        assertThat(verdict()).isEqualTo(NotificationFilter.Verdict.KEEP)
    }

    @Test
    fun `nothing is captured while forwarding is switched off`() {
        assertThat(verdict(config = NotificationFilter.Config.disabled))
            .isEqualTo(NotificationFilter.Verdict.DISABLED)
    }

    @Test
    fun `nothing is captured when no watch is paired, because there is nowhere to send it`() {
        assertThat(verdict(config = enabled.copy(watchAddress = null)))
            .isEqualTo(NotificationFilter.Verdict.NO_WATCH)
    }

    @Test
    fun `our own notifications are never mirrored, or a reminder would loop back`() {
        assertThat(verdict(candidate(packageName = ownPackage)))
            .isEqualTo(NotificationFilter.Verdict.OWN_PACKAGE)
    }

    @Test
    fun `a blocked app is dropped`() {
        assertThat(
            verdict(config = enabled.copy(blockedPackages = setOf("com.example.chat"))),
        ).isEqualTo(NotificationFilter.Verdict.BLOCKED)
    }

    @Test
    fun `an ongoing notification is dropped, so a media player cannot pump the watch`() {
        assertThat(verdict(candidate(ongoing = true)))
            .isEqualTo(NotificationFilter.Verdict.ONGOING)
    }

    @Test
    fun `a foreground-service notification is dropped for the same reason`() {
        assertThat(verdict(candidate(foregroundService = true)))
            .isEqualTo(NotificationFilter.Verdict.ONGOING)
    }

    @Test
    fun `a group summary is dropped, or every chat thread would arrive twice`() {
        assertThat(verdict(candidate(groupSummary = true)))
            .isEqualTo(NotificationFilter.Verdict.GROUP_SUMMARY)
    }

    @Test
    fun `a local-only notification is dropped because the posting app asked`() {
        assertThat(verdict(candidate(localOnly = true)))
            .isEqualTo(NotificationFilter.Verdict.LOCAL_ONLY)
    }

    @Test
    fun `a minimum-importance channel is dropped`() {
        assertThat(verdict(candidate(channelImportance = NotificationFilter.IMPORTANCE_MIN)))
            .isEqualTo(NotificationFilter.Verdict.LOW_IMPORTANCE)
    }

    @Test
    fun `an unreadable importance is allowed rather than swallowed`() {
        assertThat(
            verdict(candidate(channelImportance = NotificationFilter.IMPORTANCE_UNSPECIFIED)),
        ).isEqualTo(NotificationFilter.Verdict.KEEP)
    }

    @Test
    fun `a notification with neither title nor body has nothing to render`() {
        assertThat(verdict(candidate(title = "", body = null)))
            .isEqualTo(NotificationFilter.Verdict.EMPTY)
    }

    @Test
    fun `a body with no title is still kept`() {
        assertThat(verdict(candidate(title = null))).isEqualTo(NotificationFilter.Verdict.KEEP)
    }

    @Test
    fun `Do Not Disturb silences the watch too, reusing the phone's own setting`() {
        assertThat(
            verdict(interruptionFilter = NotificationFilter.INTERRUPTION_FILTER_NONE),
        ).isEqualTo(NotificationFilter.Verdict.DO_NOT_DISTURB)
        assertThat(
            verdict(interruptionFilter = NotificationFilter.INTERRUPTION_FILTER_ALARMS),
        ).isEqualTo(NotificationFilter.Verdict.DO_NOT_DISTURB)
    }

    @Test
    fun `priority-only Do Not Disturb still forwards, because the phone let it through`() {
        assertThat(
            verdict(interruptionFilter = NotificationFilter.INTERRUPTION_FILTER_PRIORITY),
        ).isEqualTo(NotificationFilter.Verdict.KEEP)
    }

    @Test
    fun `an unknown interruption filter forwards rather than silently swallowing`() {
        assertThat(
            verdict(interruptionFilter = NotificationFilter.INTERRUPTION_FILTER_UNKNOWN),
        ).isEqualTo(NotificationFilter.Verdict.KEEP)
    }

    @Test
    fun `Android categories map onto the GNCS wire values`() {
        assertThat(NotificationFilter.categoryOrdinal("call"))
            .isEqualTo(NotificationFilter.Category.INCOMING_CALL)
        assertThat(NotificationFilter.categoryOrdinal("msg"))
            .isEqualTo(NotificationFilter.Category.SMS)
        assertThat(NotificationFilter.categoryOrdinal("email"))
            .isEqualTo(NotificationFilter.Category.EMAIL)
        assertThat(NotificationFilter.categoryOrdinal("social"))
            .isEqualTo(NotificationFilter.Category.SOCIAL)
        assertThat(NotificationFilter.categoryOrdinal("alarm"))
            .isEqualTo(NotificationFilter.Category.SCHEDULE)
    }

    @Test
    fun `an unrecognised category is OTHER rather than a guess`() {
        assertThat(NotificationFilter.categoryOrdinal("stopwatch"))
            .isEqualTo(NotificationFilter.Category.OTHER)
        assertThat(NotificationFilter.categoryOrdinal(null))
            .isEqualTo(NotificationFilter.Category.OTHER)
    }

    @Test
    fun `the category duplicates track the real Garmin enum's ordinals`() {
        // The duplication policy stayed after the Pigeon boundary went; this pin keeps the two from drifting.
        val garmin = tech.mmarca.openvitals.devices.garmin.GarminNotificationCategory.entries
        assertThat(NotificationFilter.Category.INCOMING_CALL)
            .isEqualTo(garmin.indexOfFirst { it.name == "INCOMING_CALL" })
        assertThat(NotificationFilter.Category.SMS)
            .isEqualTo(garmin.indexOfFirst { it.name == "SMS" })
        assertThat(NotificationFilter.Category.HEALTH_AND_FITNESS)
            .isEqualTo(garmin.indexOfFirst { it.name == "HEALTH_AND_FITNESS" })
    }
}
