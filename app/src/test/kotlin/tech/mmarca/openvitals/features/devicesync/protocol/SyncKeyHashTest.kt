package tech.mmarca.openvitals.features.devicesync.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SyncKeyHashTest {

    @Test fun `equal keys collapse to equal hashes`() {
        val hasher = SyncKeyHasher()
        assertEquals(
            hasher.hash("sync_0123456789abcdef0123456789abcdef"),
            hasher.hash("sync_0123456789abcdef0123456789abcdef"),
        )
    }

    @Test fun `different keys hash apart`() {
        val hasher = SyncKeyHasher()
        val keys = listOf(
            "sync_0123456789abcdef0123456789abcdef",
            "sync_0123456789abcdef0123456789abcdee",
            "not-a-fingerprint",
            "",
        )
        val hashes = keys.map { hasher.hash(it) }
        assertEquals(keys.size, hashes.toSet().size)
        assertNotEquals(hashes[0], hashes[1])
    }
}
