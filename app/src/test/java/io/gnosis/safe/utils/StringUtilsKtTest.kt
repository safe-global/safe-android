package io.gnosis.safe.utils

import android.content.Context
import android.graphics.Color
import io.gnosis.safe.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pm.gnosis.svalinn.common.utils.getColorCompat

class StringUtilsKtTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(Color::class)
    }

    @Test
    fun `toColor with #RRGGBB should return right color`() {
        every { context.getColorCompat(any()) } returns 23
        every { Color.parseColor(any()) } returns Color.BLACK

        val result = "#ffffff".toColor(context)

        assertEquals(Color.BLACK, result)
    }

    @Test
    fun `toColor with unsupported format should return failsafe color`() {
        every { context.getColorCompat(any()) } returns 23
        every { Color.parseColor(any()) } throws Exception()

        val result = "#123".toColor(context, R.color.white)

        assertEquals(23, result)
    }
}
