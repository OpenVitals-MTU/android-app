package tech.mmarca.openvitals.domain.medical.shc

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import tech.mmarca.openvitals.domain.medical.FhirFile
import tech.mmarca.openvitals.domain.medical.FhirImportAnalyzer
import tech.mmarca.openvitals.domain.medical.string

/** Cards decode from every form they come in, and their records arrive tagged, linked and ready for Health Connect. */
class SmartHealthCardsTest {

    private val jws = SmartHealthCardFixtures.jws()

    @Test
    fun `the QR digits turn back into the JWS`() {
        assertThat(SmartHealthCards.numericToJws(SmartHealthCardFixtures.numeric(jws).removePrefix("shc:/"))).isEqualTo(jws)
        assertThat(SmartHealthCards.numericToJws("123")).isNull()
    }

    @Test
    fun `a card reads from its QR text, its JWS, and a smart-health-card file`() {
        val fromQr = SmartHealthCards.read(listOf(SmartHealthCardFixtures.numeric(jws)))
        val fromJws = SmartHealthCards.read(listOf(jws))
        val fromFile = SmartHealthCards.read(listOf(SmartHealthCardFixtures.file(jws, SmartHealthCardFixtures.jws(issuer = "https://other.example"))))

        assertThat(fromQr.cards.single().issuer).isEqualTo(SmartHealthCardFixtures.Issuer)
        assertThat(fromQr.cards.single().fhirVersion).isEqualTo("4.0.1")
        assertThat(fromJws.cards).isEqualTo(fromQr.cards)
        assertThat(fromFile.cards.map { it.issuer }).containsExactly(SmartHealthCardFixtures.Issuer, "https://other.example")
    }

    @Test
    fun `a card split over several codes is joined, and one with a part missing is counted`() {
        val parts = SmartHealthCardFixtures.chunks(jws, 3)

        assertThat(SmartHealthCards.read(parts.reversed()).cards).hasSize(1)
        val missing = SmartHealthCards.read(parts.drop(1))
        assertThat(missing.cards).isEmpty()
        assertThat(missing.incomplete).isEqualTo(1)
    }

    @Test
    fun `an uncompressed payload is read, and a broken one is counted as unreadable`() {
        assertThat(SmartHealthCards.read(listOf(SmartHealthCardFixtures.jws(compressed = false))).cards).hasSize(1)
        val broken = SmartHealthCards.read(listOf("eyJ6aXAiOiJERUYifQ.bm90IGRlZmxhdGU.c2ln"))
        assertThat(broken.cards).isEmpty()
        assertThat(broken.unreadable).isEqualTo(1)
    }

    @Test
    fun `records come under their issuer, tagged as not verified`() {
        val entries = SmartHealthCards.entries(SmartHealthCards.read(listOf(jws)).cards)

        assertThat(entries.map { it.origin?.baseUri }.toSet()).containsExactly(SmartHealthCardFixtures.Issuer)
        assertThat(entries.first().origin?.displayName).isEqualTo("cards.example")
        assertThat(entries.all { SmartHealthCards.isUnverified(it.resource) }).isTrue()
    }

    @Test
    fun `the card's references are rewritten to real ids, nothing is held back, and ids are stable`() {
        fun analyse() = FhirImportAnalyzer.analyze(FhirFile(SmartHealthCards.entries(SmartHealthCards.read(listOf(jws)).cards)), "card.png")
        val group = analyse().groups.single()
        val patient = group.ready.single { it.type == "Patient" }
        val vaccination = group.ready.single { it.type == "Immunization" }

        assertThat(group.rejected).isEmpty()
        assertThat(group.versionStated).isTrue()
        assertThat(vaccination.json.toString()).contains("\"reference\":\"Patient/${patient.id}\"")
        assertThat(analyse().groups.single().ready.map { it.id }).isEqualTo(group.ready.map { it.id })
        assertThat(patient.json.string("resourceType")).isEqualTo("Patient")
    }

    @Test
    fun `text that is not a card is not taken for one`() {
        assertThat(SmartHealthCards.looksLikeCard("""{"resourceType":"Bundle"}""")).isFalse()
        assertThat(SmartHealthCards.looksLikeCard("hello")).isFalse()
        assertThat(SmartHealthCards.looksLikeCard(SmartHealthCardFixtures.numeric(jws))).isTrue()
        assertThat(SmartHealthCards.looksLikeCard(SmartHealthCardFixtures.file(jws))).isTrue()
    }
}
