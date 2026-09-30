package tech.mmarca.openvitals.data.repository

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import tech.mmarca.openvitals.data.local.medical.FakeMedicalDocumentDao
import tech.mmarca.openvitals.domain.model.MedicalRecordRef
import tech.mmarca.openvitals.util.MainDispatcherRule

/** Kept files stay in the private folder, one copy per file, and never take their records with them. */
class MedicalDocumentsRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val dao = FakeMedicalDocumentDao()
    private var clock = Instant.parse("2026-09-29T10:00:00Z")
    private val repository by lazy {
        MedicalDocumentsRepositoryImpl(folder.root, dao, mainDispatcherRule.dispatcherProvider) { clock }
    }
    private val vaccine = MedicalRecordRef("clinic", "Immunization", "i1")
    private val allergy = MedicalRecordRef("clinic", "AllergyIntolerance", "a1")

    @Test
    fun `a kept file is written privately and listed with its record count`() = runTest {
        val kept = repository.keep("summary.json", "application/json", "json", "{}".toByteArray(), "Clinic", listOf(vaccine, allergy))

        val file = repository.file(kept.id)!!
        assertThat(file.parentFile).isEqualTo(folder.root)
        assertThat(file.readText()).isEqualTo("{}")
        assertThat(repository.documents().single().recordCount).isEqualTo(2)
        assertThat(repository.totalBytes()).isEqualTo(2)
        assertThat(repository.documentFor(vaccine)?.fileName).isEqualTo("summary.json")
    }

    @Test
    fun `the same file kept twice is one copy that gains the new links`() = runTest {
        val first = repository.keep("a.json", "application/json", "json", "{}".toByteArray(), null, listOf(vaccine))
        clock = clock.plusSeconds(60)

        val second = repository.keep("a (1).json", "application/json", "json", "{}".toByteArray(), null, listOf(vaccine, allergy))

        assertThat(second.id).isEqualTo(first.id)
        assertThat(folder.root.listFiles()!!.toList()).hasSize(1)
        assertThat(second.recordCount).isEqualTo(2)
    }

    @Test
    fun `dropping links to deleted records keeps the document`() = runTest {
        val kept = repository.keep("a.json", "application/json", "json", "{}".toByteArray(), null, listOf(vaccine, allergy))

        repository.dropLinks(listOf(allergy))

        assertThat(repository.linkedRefs()).containsExactly(vaccine)
        assertThat(repository.documents().single().id).isEqualTo(kept.id)
    }

    @Test
    fun `deleting removes the file and its links, and a row whose file is gone is dropped`() = runTest {
        val one = repository.keep("a.json", "application/json", "json", "{\"a\":1}".toByteArray(), null, listOf(vaccine))
        val two = repository.keep("b.zip", "application/zip", "zip", byteArrayOf(1, 2, 3), null, listOf(allergy))

        repository.delete(one.id)
        repository.file(two.id)!!.delete()

        assertThat(repository.documents()).isEmpty()
        assertThat(dao.documents).isEmpty()
        assertThat(repository.linkedRefs()).isEmpty()
    }
}
