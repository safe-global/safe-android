package io.gnosis.safe.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BalanceFormatterTest {

    @Test
    fun `shortAmount () should remove trailing zeroes`() {

        val value = BigDecimal.valueOf(0.100000)
        val shortAmount = BalanceFormatter().shortAmount(value)
        assertEquals("0.1", shortAmount)
    }

    @Test
    fun `shortAmount () should have correct number of decimals`() {

        // 5 decimals till 1k
        val value1 = BigDecimal.valueOf(0.123456789)
        val shortAmount1 = BalanceFormatter().shortAmount(value1)
        assertEquals("0.12346", shortAmount1)

        // 4 decimals till 10k
        val value2 = BigDecimal.valueOf(1000.123456789)
        val shortAmount2 = BalanceFormatter().shortAmount(value2)
        assertEquals("1,000.1235", shortAmount2)

        //3 decimals till 100k
        val value3 = BigDecimal.valueOf(10_000.123456789)
        val shortAmount3 = BalanceFormatter().shortAmount(value3)
        assertEquals("10,000.123", shortAmount3)

        //2 decimals till 1M
        val value4 = BigDecimal.valueOf(100_000.123456789)
        val shortAmount4 = BalanceFormatter().shortAmount(value4)
        assertEquals("100,000.12", shortAmount4)

        //1 decimal till 10M
        val value5 = BigDecimal.valueOf(5_000_000.123456789)
        val shortAmount5 = BalanceFormatter().shortAmount(value5)
        assertEquals("5,000,000.1", shortAmount5)

        //no decimals after 10M
        val value6 = BigDecimal.valueOf(15_000_000.123456789)
        val shortAmount6 = BalanceFormatter().shortAmount(value6)
        assertEquals("15,000,000", shortAmount6)
    }

    @Test
    fun `shortAmount () should have correct notation for big numbers`() {

        val value1 = BigDecimal.valueOf(100_000_000.123456789)
        val shortAmount1 = BalanceFormatter().shortAmount(value1)
        assertEquals("100M", shortAmount1)

        val value2 = BigDecimal.valueOf(100_100_000.123456789)
        val shortAmount2 = BalanceFormatter().shortAmount(value2)
        assertEquals("100.1M", shortAmount2)

        val value3 = BigDecimal.valueOf(999_999_999.123456789)
        val shortAmount3 = BalanceFormatter().shortAmount(value3)
        assertEquals("999.999M", shortAmount3)

        val value4 = BigDecimal.valueOf(1_999_999_999.123456789)
        val shortAmount4 = BalanceFormatter().shortAmount(value4)
        assertEquals("1.999B", shortAmount4)

        val value5 = BigDecimal.valueOf(1_999_999_999_999.123456789)
        val shortAmount5 = BalanceFormatter().shortAmount(value5)
        assertEquals("1.999T", shortAmount5)

        val value6 = BigDecimal.valueOf(999_000_000_000_000)
        val shortAmount6 = BalanceFormatter().shortAmount(value6)
        assertEquals("999T", shortAmount6)

        val value7 = BigDecimal.valueOf(999_999_999_999_999.123456789)
        val shortAmount7 = BalanceFormatter().shortAmount(value7)
        assertEquals("999.999T", shortAmount7)

        val value8 = BigDecimal.valueOf(1_999_999_999_999_999.123456789)
        val shortAmount8 = BalanceFormatter().shortAmount(value8)
        assertEquals("> 999T", shortAmount8)
    }

    @Test
    fun `shortAmount () should have correct notation for small numbers`() {

        val value = BigDecimal.valueOf(0.000000100000)
        val shortAmount = BalanceFormatter().shortAmount(value)
        assertEquals("< 0.00001", shortAmount)
    }
}
