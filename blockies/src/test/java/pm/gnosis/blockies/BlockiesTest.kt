package pm.gnosis.blockies

import junit.framework.Assert.*
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import pm.gnosis.utils.hexAsBigInteger

class BlockiesTest {
    @Test
    fun blockiesWithInvalidAddress() {
        val invalidAddress = "0x1FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".hexAsBigInteger()
        assertNull(Blockies.fromAddress(invalidAddress))
    }

    @Test
    fun blockies0() {
        val address = "0x0".hexAsBigInteger()

        val blockie = Blockies.fromAddress(address)

        val expectedData = listOf(
            0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
            0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0,
            1.0, 0.0, 1.0, 2.0, 2.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0,
            2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, 1.0, 1.0, 2.0, 0.0, 0.0
        ).toDoubleArray()
        val expectedPrimaryColor = -2538413 // A=FF || R=D9 || G=44 || B=53
        val expectedBackgroundColor = -13863216 // A=FF || R=2C || G=76 || B=D0
        val expectedSpotColor = -3638619 // A=FF || R=C8 || G=7A || B=A5

        assertNotNull(blockie)
        assertEquals(expectedPrimaryColor, blockie!!.primaryColor)
        assertEquals(expectedBackgroundColor, blockie.backgroundColor)
        assertEquals(expectedSpotColor, blockie.spotColor)
        assertArrayEquals(expectedData, blockie.data, 0.toDouble())
    }

    @Test
    fun blockies1() {
        val address = "0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".hexAsBigInteger()

        val blockie = Blockies.fromAddress(address)

        val expectedData = listOf(
            0.0, 0.0, 1.0, 2.0, 2.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0,
            1.0, 2.0, 2.0, 0.0, 0.0, 2.0, 2.0, 1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, 0.0, 0.0,
            1.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0,
            1.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0
        ).toDoubleArray()
        val expectedPrimaryColor = -15519534 // A=FF || R=13 || G=30 || B=D2
        val expectedBackgroundColor = -4453413 // A=FF || R=BC || G=0B || B=DB
        val expectedSpotColor = -11193479 // A=FF || R=55 || G=33 || B=79

        assertNotNull(blockie)
        assertEquals(expectedPrimaryColor, blockie!!.primaryColor)
        assertEquals(expectedBackgroundColor, blockie.backgroundColor)
        assertEquals(expectedSpotColor, blockie.spotColor)
        assertArrayEquals(expectedData, blockie.data, 0.toDouble())
    }
}
