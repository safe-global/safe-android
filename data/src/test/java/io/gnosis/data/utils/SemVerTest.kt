package io.gnosis.data.utils

import org.junit.Assert.*
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
    fun `parse (versionString with build metadata) should return SemVer`() {
        val version1 = SemVer.parse("1.3.0+L2")
        assertNotNull(version1)
        assertEquals("L2", version1.buildMetadata)

        val version2 = SemVer.parse("2.15.0-255-internal+L3", true)
        assertNotEquals(SemVer(2, 15, 0, "255-internal", "L3"), version2)
        assertEquals(SemVer(2, 15, 0), version2)

        val version3 = SemVer.parse("2.15.0-255-internal+L3")
        assertEquals(SemVer(2, 15, 0, "255-internal", "L3"), version3)
    }

    @Test
    fun `parse (versionString, ignoreExtensions) should return SemVer`() {
        val version1 = SemVer.parse("2.15.0", true)
        assertEquals(SemVer(2, 15, 0), version1)

        val version2 = SemVer.parse("2.15.0-255-internal", true)
        assertNotEquals(SemVer(2, 15, 0, "255-internal"), version2)
        assertEquals(SemVer(2, 15, 0), version2)

        val version3 = SemVer.parse("2.15.0-255rc-internal", true)
        assertNotEquals(SemVer(2, 15, 0, "255rc-internal"), version3)
        assertEquals(SemVer(2, 15, 0), version3)
    }

    @Test
    fun `parseRange (rangeString) should return pair of SemVer`() {
        val version1 = SemVer.parse("2.15.0")
        val version2 = SemVer.parse("2.17.0")
        val version3 = SemVer.parse("2.15.0-255-internal")
        val version4 = SemVer.parse("2.17.0-300-internal")

        val rangeString1 = "2.15.0...2.17.0"
        val range1 = SemVer.parseRange(rangeString1)
        assertTrue(range1.first != null)
        assertTrue(range1.second != null)
        assertEquals(version1, range1.first)
        assertEquals(version2, range1.second)

        val rangeString2 = "2.15.0"
        val range2 = SemVer.parseRange(rangeString2)
        assertTrue(range2.first != null)
        assertTrue(range2.second == null)
        assertEquals(version1, range1.first)

        val rangeString3 = "2.15.0-255-internal"
        val range3 = SemVer.parseRange(rangeString3)
        assertTrue(range3.first != null)
        assertTrue(range3.second == null)
        assertEquals(version3, range3.first)

        val rangeString4 = "2.15.0-255-internal...2.17.0-300-internal"
        val range4 = SemVer.parseRange(rangeString4)
        assertTrue(range4.first != null)
        assertTrue(range4.second != null)
        assertEquals(version3, range4.first)
        assertEquals(version4, range4.second)
    }

    @Test
    fun `parseRange (rangeString, ignoreExtensions) should return pair of SemVer`() {
        val version1 = SemVer.parse("2.15.0")
        val version2 = SemVer.parse("2.17.0")
        val version3 = SemVer.parse("2.15.0-255-internal")
        val version4 = SemVer.parse("2.17.0-300-internal")

        val rangeString1 = "2.15.0...2.17.0"
        val range1 = SemVer.parseRange(rangeString1, true)
        assertTrue(range1.first != null)
        assertTrue(range1.second != null)
        assertEquals(version1, range1.first)
        assertEquals(version2, range1.second)

        val rangeString2 = "2.15.0"
        val range2 = SemVer.parseRange(rangeString2, true)
        assertTrue(range2.first != null)
        assertTrue(range2.second == null)
        assertEquals(version1, range1.first)

        val rangeString3 = "2.15.0-255-internal"
        val range3 = SemVer.parseRange(rangeString3, true)
        assertTrue(range3.first != null)
        assertTrue(range3.second == null)
        assertNotEquals(version3, range3.first)
        assertEquals(SemVer(2, 15, 0), range3.first)

        val rangeString4 = "2.15.0-255-internal...2.17.0-300-internal"
        val range4 = SemVer.parseRange(rangeString4, true)
        assertTrue(range4.first != null)
        assertTrue(range4.second != null)
        assertNotEquals(version3, range4.first)
        assertEquals(SemVer(2, 15, 0), range4.first)
        assertNotEquals(version4, range4.second)
        assertEquals(SemVer(2, 17, 0), range4.second)
    }

    @Test
    fun `isInside (rangeList) should return if SemVer is inside the range list`() {
        val version1 = SemVer.parse("2.15.0")
        val version2 = SemVer.parse("2.17.0")
        val version3 = SemVer.parse("2.15.0-255-internal")
        val version4 = SemVer.parse("2.15.0-257-internal")

        val rangeList1 = "2.15.0"
        assertTrue(version1.isInside(rangeList1))
        assertFalse(version2.isInside(rangeList1))

        val rangeList2 = "2.15.0...2.17.0"
        assertTrue(version1.isInside(rangeList2))
        assertTrue(version2.isInside(rangeList2))

        val rangeList3 = "2.11.0,2.12...2.13.0,2.14...2.16.0"
        assertTrue(version1.isInside(rangeList3))
        assertFalse(version2.isInside(rangeList3))

        val rangeList4 = ""
        assertFalse(version1.isInside(rangeList4))

        val rangeList5 = "2.15.0-255-internal"
        assertFalse(version1.isInside(rangeList5))
        assertTrue(version3.isInside(rangeList5))

        val rangeList6 = "2.15.0-255-internal...2.15.0-258-internal"
        assertTrue(version4.isInside(rangeList6))
    }

    @Test
    fun `isInside (rangeList, ignoreExtensions) should return if SemVer is inside the range list`() {
        val version1 = SemVer.parse("2.15.0")
        val version2 = SemVer.parse("2.17.0")
        val version3 = SemVer.parse("2.15.0-255-internal")
        val version4 = SemVer.parse("2.15.0-257-internal")

        val rangeList1 = "2.15.0"
        assertTrue(version1.isInside(rangeList1, true))
        assertFalse(version2.isInside(rangeList1, true))

        val rangeList2 = "2.15.0...2.17.0"
        assertTrue(version1.isInside(rangeList2, true))
        assertTrue(version2.isInside(rangeList2, true))

        val rangeList3 = "2.11.0,2.12...2.13.0,2.14...2.16.0"
        assertTrue(version1.isInside(rangeList3, true))
        assertFalse(version2.isInside(rangeList3, true))

        val rangeList4 = ""
        assertFalse(version1.isInside(rangeList4, true))

        val rangeList5 = "2.15.0-255-internal"
        assertTrue(version1.isInside(rangeList5, true))
        assertTrue(version3.isInside(rangeList5, true))

        val rangeList6 = "2.15.0-255-internal...2.15.0-258-internal"
        assertTrue(version1.isInside(rangeList5, true))
        assertTrue(version3.isInside(rangeList5, true))
        assertTrue(version4.isInside(rangeList6, true))
    }
}
