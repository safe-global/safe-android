package io.gnosis.safe.utils

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.ExperimentalTime

class DateUtilsKtTest {
    @Test
    fun `formatBackendDateTime (german locale and europe timezone) should return german date string`() {
        val result = Date(0).formatBackendDateTime(ZoneId.of("CET"), Locale.GERMAN)

        assertEquals("01.01.1970, 01:00:00", result)
    }

    @Test
    fun `formatBackendDateTime (english locale and europe timezone) should return english date string`() {
        val result = Date(0).formatBackendDateTime(ZoneId.of("CET"), Locale.ENGLISH)

        assertEquals("Jan 1, 1970, 1:00:00 AM", result)
    }

    @Test
    fun `formatBackendDateTime (english locale and UTC timezone) should return english date string`() {
        val result = Date(0).formatBackendDateTime(ZoneId.of("Z"), Locale.ENGLISH)

        assertEquals("Jan 1, 1970, 12:00:00 AM", result)
    }

    @Test
    fun `formatBackendDate (german locale and europe timezone) should return german date string`() {
        val result = Date(0).formatBackendDate(ZoneId.of("CET"), Locale.GERMAN)

        assertEquals("01.01.1970", result)
    }

    @Test
    fun `formatBackendDate (english locale and european timezone (01-01-1970 23-59-59)) should return 1st of Jan`() {
        val result = Date(23 * 60 * 60 * 1000 - 1).formatBackendDate(ZoneId.of("CET"), Locale.ENGLISH)

        assertEquals("Jan 1, 1970", result)
    }

    @Test
    fun `formatBackendDate (english locale and UTC timezone plus one full day) should return 2nd of Jan`() {
        val result = Date(86400000).formatBackendDate(ZoneId.of("Z"), Locale.ENGLISH)

        assertEquals("Jan 2, 1970", result)
    }

    @ExperimentalTime
    @Test
    fun `elapsedIntervalFrom (time) should return correct unit and value`() {

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        var start: Date
        var end: Date
        var elapsedInterval: ElapsedInterval

        start = formatter.parse("2020-12-10T12:30:00")
        end = formatter.parse("2020-12-10T12:30:01")
        elapsedInterval = start.elapsedIntervalTo(end)
        assertEquals(ChronoUnit.SECONDS, elapsedInterval.unit)
        assertEquals(1, elapsedInterval.value)

        start = formatter.parse("2020-12-10T12:30:00")
        end = formatter.parse("2020-12-10T12:45:00")
        elapsedInterval = start.elapsedIntervalTo(end)
        assertEquals(ChronoUnit.MINUTES, elapsedInterval.unit)
        assertEquals(15, elapsedInterval.value)

        start = formatter.parse("2020-12-10T12:30:00")
        end = formatter.parse("2020-12-10T14:45:00")
        elapsedInterval = start.elapsedIntervalTo(end)
        assertEquals(ChronoUnit.HOURS, elapsedInterval.unit)
        assertEquals(2, elapsedInterval.value)

        start = formatter.parse("2020-12-10T12:30:00")
        end = formatter.parse("2020-12-11T14:45:00")
        elapsedInterval = start.elapsedIntervalTo(end)
        assertEquals(ChronoUnit.DAYS, elapsedInterval.unit)
        assertEquals(1, elapsedInterval.value)
    }
}
