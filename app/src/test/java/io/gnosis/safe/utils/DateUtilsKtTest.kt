package io.gnosis.safe.utils

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.util.*

class DateUtilsKtTest {
    @Test
    fun `formatBackendDate (german locale and europe timezone) should return german date string`() {
        val result = Date(0).formatBackendDate(ZoneId.of("CET"), Locale.GERMAN)

        assertEquals("01.01.1970 01:00:00", result)
    }

    @Test
    fun `formatBackendDate (german locale and europe timezone) should return english date string`() {
        val result = Date(0).formatBackendDate(ZoneId.of("CET"), Locale.ENGLISH)

        assertEquals("Jan 1, 1970 1:00:00 AM", result)
    }

    @Test
    fun `formatBackendDate (english locale and UTC timezone) should return english date string`() {
        val result = Date(0).formatBackendDate(ZoneId.of("Z"), Locale.ENGLISH)

        assertEquals("Jan 1, 1970 12:00:00 AM", result)
    }
}
