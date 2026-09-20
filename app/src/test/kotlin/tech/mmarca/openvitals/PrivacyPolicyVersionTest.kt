package tech.mmarca.openvitals

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository

/**
 * The re-consent dialog fires only when the accepted version differs from the current one.
 * The policy was rewritten while the version stayed "1.0", so nobody was ever asked again.
 */
class PrivacyPolicyVersionTest {

    @Test
    fun `the version users accept is the date on the policy`() {
        val line = File("../PRIVACY.md").readLines().firstOrNull { it.startsWith("Last updated:") }
        assertNotNull("PRIVACY.md has no Last updated line", line)
        val date = LocalDate.parse(
            line!!.removePrefix("Last updated:").trim(),
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
        )

        assertEquals(
            "PRIVACY.md changed its date. Set CURRENT_PRIVACY_POLICY_VERSION to it, so users are asked again.",
            date.toString(),
            PreferencesRepository.CURRENT_PRIVACY_POLICY_VERSION,
        )
    }
}
