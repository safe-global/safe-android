package io.gnosis.safe.utils

import android.content.Context
import io.gnosis.safe.R
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Test
import pm.gnosis.svalinn.common.utils.getColorCompat

class StringUtilsKtTest {

    private val context = mockk<Context>(relaxed = true)


    @Test
    fun `toColor with #RRGGBB should return right color`() {
        every { context.getColorCompat(any()) } returns 23
//        mockkStatic(Context::class)
//        mockkStatic(Color::class)

        val result = "#112233".toColor(context)

        assertEquals(-12345, result)
    }

    @Test
    fun `toColor with unsupported format should return failsafe color`() {
        every { context.getColorCompat(any()) } returns 23
//        mockkStatic(Context::class)
//        mockkStatic(Color::class)

        val result = "#123".toColor(context, R.color.white)

        assertEquals(23, result)
    }
}
