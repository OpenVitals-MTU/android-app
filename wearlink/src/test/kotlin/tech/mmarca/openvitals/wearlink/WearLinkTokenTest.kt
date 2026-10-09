package tech.mmarca.openvitals.wearlink

import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearLinkTokenTest {

    @Test
    fun `a token is 44 characters of Base64 for 32 bytes, and different every time`() {
        val one = WearLinkToken.generate()
        val two = WearLinkToken.generate()

        assertEquals(WearLinkToken.LENGTH, one.length)
        assertTrue(WearLinkToken.isWellFormed(one))
        assertNotEquals(one, two)
        assertTrue(WearLinkToken.matches(one, one))
        assertFalse(WearLinkToken.matches(one, two))
    }

    @Test
    fun `a seeded source gives a reproducible token`() {
        val a = WearLinkToken.generate(SecureRandom.getInstance("SHA1PRNG").apply { setSeed(7L) })
        val b = WearLinkToken.generate(SecureRandom.getInstance("SHA1PRNG").apply { setSeed(7L) })

        assertEquals(a, b)
    }

    @Test
    fun `malformed tokens are neither well-formed nor matching`() {
        val good = WearLinkToken.generate()

        assertFalse(WearLinkToken.isWellFormed(null))
        assertFalse(WearLinkToken.isWellFormed(""))
        assertFalse(WearLinkToken.isWellFormed(good.dropLast(1)))
        assertFalse(WearLinkToken.isWellFormed(good.replace('A', ' ')))
        assertFalse(WearLinkToken.isWellFormed("*".repeat(44)))
        assertFalse(WearLinkToken.matches(good, good.dropLast(1)))
        assertFalse(WearLinkToken.matches(null, good))
    }
}
