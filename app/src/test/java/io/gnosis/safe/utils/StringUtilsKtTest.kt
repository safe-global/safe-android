package io.gnosis.safe.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.TextView
import androidx.core.text.getSpans
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.gnosis.safe.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.utils.asEthereumAddress

@RunWith(AndroidJUnit4::class)
class StringUtilsKtTest {

    private val context = mockk<Context>(relaxed = true)
    private val applicationContext: Context = ApplicationProvider.getApplicationContext()

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

    @Ignore
    @Test
    fun `setLink`() {
        val textView = TextView(applicationContext)

        textView.setLink(url="url", urlText = "urlText", prefix = "prefix", underline = false)

        assertEquals(7, textView.text.length)
    }

    @Test
    fun `appendLink`() {

    }

    @Test
    fun `replaceDoubleNewlineWithParagraphLineSpacing`() {
    }

    @Test
    fun `appendTextWithSpans`() {
    }

    @Test
    fun `formatForTxList`() {
    }

    @Test
    fun `abbreviateEthAddress`() {
    }

    @Test
    fun `formatEthAddressBold`() {
    }

    @Test
    fun `fallBackHandlerLabel`() {
    }

    @Test
    fun `formatEthAddress (with middleLineBreak)`() {
        val address = "0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44".asEthereumAddress()!!

        val spannable = address.formatEthAddress(applicationContext)

        assertEquals('\n', spannable.get(21)) // check linebreak
        assertEquals(43, spannable.length)
    }

    @Test
    fun `formatEthAddress (no middleLineBreak)`() {
        val address = "0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44".asEthereumAddress()!!

        val spannable = address.formatEthAddress(context = applicationContext, addMiddleLinebreak = false)

        assertEquals('C', spannable.get(21)) // check no linebreak
        assertEquals(42, spannable.length)
    }

    @Test
    fun `formatEthAddress (not checksummed)`() {
        val address = "0xd5d82B6addc9027b22dca772Aa68d5d74cdbdf44".asEthereumAddress()!!

        val spannable = address.formatEthAddress(context = applicationContext, addMiddleLinebreak = false)

        assertEquals("0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44", spannable.toString())

        val prefixLength = 6
        val suffixLength = 4

        val foregroundColorSpans = spannable.getSpans<ForegroundColorSpan>(start = 0, end = prefixLength)
        assertEquals(foregroundColorSpans.size, 1)
        assertEquals(applicationContext.getColorCompat(R.color.address_boundaries), foregroundColorSpans[0].foregroundColor)

        val styleSpans = spannable.getSpans<StyleSpan>(start = 0, end = prefixLength)
        assertEquals(styleSpans.size, 1)
        assertEquals(Typeface.BOLD, styleSpans[0].style)

        val foregroundColorSpansEnd = spannable.getSpans<ForegroundColorSpan>(start = spannable.length - suffixLength, end = spannable.length)
        assertEquals(foregroundColorSpansEnd.size, 1)
        assertEquals(applicationContext.getColorCompat(R.color.address_boundaries), foregroundColorSpansEnd[0].foregroundColor)

        val styleSpansEnd = spannable.getSpans<StyleSpan>(start = spannable.length - suffixLength, end = spannable.length)
        assertEquals(styleSpansEnd.size, 1)
        assertEquals(Typeface.BOLD, styleSpansEnd[0].style)

        val foregroundColorSpansMiddle = spannable.getSpans<ForegroundColorSpan>(start = prefixLength, end = spannable.length - suffixLength)
        assertEquals(foregroundColorSpansMiddle.size, 1)
        assertEquals(applicationContext.getColorCompat(R.color.text_emphasis_low), foregroundColorSpansMiddle[0].foregroundColor)

        val spans = spannable.getSpans<Any>()
        assertEquals(5, spans.size)

    }

    @Test
    fun `shortChecksumString`() {
    }

    @Test
    fun `parseEthereumAddress`() {
    }

    @Test
    fun `underline`() {
    }

    @Test
    fun `asMiddleEllipsized(boundariesLength)`() {
    }

    @Test
    fun `asMiddleEllipsized(prefixLength, suffixLength)`() {
    }
}
