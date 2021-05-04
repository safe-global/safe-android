package io.gnosis.safe.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SemVerTest {

    @Test
    fun `parse (versionString) should return SemVer`() {
        val version1 = SemVer.parse("2.15.0")
        assertEquals(SemVer(2, 15, 0), version1)

        val version2 = SemVer.parse("2.15.0-255-internal")
        assertEquals(SemVer(2, 15, 0, "255-internal"), version2)

        val version3 = SemVer.parse("2.15.0-255rc-internal")
        assertEquals(SemVer(2, 15, 0, "255rc-internal"), version3)
    }

    @Test
    fun `parseSemVerRange (rangeString) should return pair of SemVer`() {
        val version1 = SemVer.parse("2.15.0")
        val version2 = SemVer.parse("2.17.0")

        val rangeString1 = "2.15.0-2.17.0"
        val range1 = parseSemVerRange(rangeString1)
        assert(range1.first != null)
        assert(range1.second != null)
        assertEquals(version1, range1.first)
        assertEquals(version2, range1.second)

        val rangeString2 = "2.15.0"
        val range2 = parseSemVerRange(rangeString2)
        assert(range2.first != null)
        assert(range2.second == null)
        assertEquals(version1, range1.first)
    }

    @Test
    fun `isInside (rangeList) should return if SemVer is inside the range list`() {
        val version1 = SemVer.parse("2.15.0")
        val version2 = SemVer.parse("2.17.0")

        val rangeList1 = "2.15.0"
        assert(version1.isInside(rangeList1))
        assertFalse(version2.isInside(rangeList1))

        val rangeList2 = "2.15.0-2.17.0"
        assert(version1.isInside(rangeList2))
        assert(version2.isInside(rangeList2))

        val rangeList3 = "2.11.0,2.12-2.13.0,2.14-2.16.0"
        assert(version1.isInside(rangeList3))
        assertFalse(version2.isInside(rangeList3))

        val rangeList4 = ""
        assertFalse(version1.isInside(rangeList4))
    }
}
