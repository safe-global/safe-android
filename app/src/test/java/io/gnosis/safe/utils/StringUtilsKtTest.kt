package io.gnosis.safe.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import androidx.core.text.getSpans
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.gnosis.data.repositories.SafeRepository.Companion.DEFAULT_FALLBACK_HANDLER
import io.gnosis.safe.R
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

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

    @Test
    fun `appendTextWithSpans (no spans)`() {
        val builder = SpannableStringBuilder()

        builder.appendTextWithSpans("text", emptyList())

        assertEquals(builder.toString(), "text")
        val spans = builder.getSpans<Any>()
        assertEquals(0, spans.size)
    }

    @Test
    fun `appendTextWithSpans (with span)`() {
        val builder = SpannableStringBuilder()

        builder.appendTextWithSpans("text", listOf(ForegroundColorSpan(applicationContext.getColorCompat(R.color.address_boundaries))))

        assertEquals(builder.toString(), "text")
        val spans = builder.getSpans<Any>()
        assertEquals(1, spans.size)
        assertTrue(spans[0] is ForegroundColorSpan)
    }

    @Test
    fun `appendTextWithSpans (with spans)`() {
        val builder = SpannableStringBuilder()

        builder.appendTextWithSpans("text", listOf(ForegroundColorSpan(applicationContext.getColorCompat(R.color.address_boundaries)), UnderlineSpan()))

        assertEquals(builder.toString(), "text")
        val spans = builder.getSpans<Any>()
        assertEquals(2, spans.size)
        assertTrue(spans[0] is ForegroundColorSpan)
        assertTrue(spans[1] is UnderlineSpan)
    }

    @Test
    fun `setLink`() {
        mockkStatic(TextView::appendLink)
        val textView = mockk<TextView>(relaxed = true)
        every { any<TextView>().appendLink(any(), any(), any(), any(), any(), any()) } just Runs

        textView.setLink(url = "url", urlText = "urlText", prefix = "prefix", underline = true)

        verify { textView.text = null }
        verify { any<TextView>().appendLink("url", "urlText", null, R.color.primary, "prefix", true) }
    }

    @Test
    fun `setLink(no prefix)`() {
        mockkStatic(TextView::appendLink)
        val textView = mockk<TextView>(relaxed = true)
        every { textView.appendLink(any(), any(), any(), any(), any(), any()) } just Runs

        textView.setLink(url = "url", urlText = "urlText")

        verify { textView.text = null }
        verify { any<TextView>().appendLink("url", "urlText", null, R.color.primary, "", false) }
    }

    @Test
    fun `replaceDoubleNewlineWithParagraphLineSpacing (with double newline)`() {
        val resources = mockk<Resources>(relaxed = true)
        every { resources.getString(any()) } returns "foo\n\nbar"

        val result = resources.replaceDoubleNewlineWithParagraphLineSpacing(1)

        verify { resources.getString(1) }

        val spans = result.getSpans<Any>()
        assertEquals(1, spans.size)
        assertTrue(spans[0] is AbsoluteSizeSpan)

        val absoluteSizeSpans = result.getSpans<AbsoluteSizeSpan>(4, 5)
        assertEquals(1, absoluteSizeSpans.size)
    }

    @Test
    fun `replaceDoubleNewlineWithParagraphLineSpacing (without double newline)`() {
        val resources = mockk<Resources>(relaxed = true)
        every { resources.getString(any()) } returns "foo\nbar"

        val result = resources.replaceDoubleNewlineWithParagraphLineSpacing(1)

        verify { resources.getString(1) }
        val spans = result.getSpans<Any>()
        assertEquals(0, spans.size)
    }

    @Test
    fun `formatForTxList`() {

        val address = Solidity.Address(BigInteger.TEN)

        val result = address.formatForTxList()

        assertEquals(result, "0x0000...000A")
    }

    @Test
    fun `abbreviateEthAddress`() {

        val addressString = "0x000000000000000000000000000000A"

        val result = addressString.abbreviateEthAddress()

        assertEquals(result, "0x0000...000A")
    }

    @Test
    fun `formatEthAddressBold`() {
        val addressString = "0x000000000000000000000000000000A"

        val result = addressString.formatEthAddressBold()

        assertEquals(
            "0x00000000000000\n0000000000000000A", result.toString()
        )
    }

    @Test
    fun `fallBackHandlerLabel (Unknown)`() {
        val address = Solidity.Address(BigInteger.TEN)

        val result = address.fallBackHandlerLabel()

        assertEquals(R.string.unknown_fallback_handler, result)
    }

    @Test
    fun `fallBackHandlerLabel (default)`() {
        val address = DEFAULT_FALLBACK_HANDLER

        val result = address.fallBackHandlerLabel()

        assertEquals(R.string.default_fallback_handler, result)
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
        assertEquals(applicationContext.getColorCompat(R.color.label_tertiary), foregroundColorSpansMiddle[0].foregroundColor)

        val spans = spannable.getSpans<Any>()
        assertEquals(5, spans.size)
    }

    @Test
    fun `shortChecksumString`() {
        val address = "0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44".asEthereumAddress()!!

        val result = address.shortChecksumString()

        assertEquals(13, result.length)
        assertEquals("0xd5D8...dF44", result)
    }

    @Test
    fun `parseEthereumAddress`() {
        val address = parseEthereumAddress("0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44")!!

        assertEquals("0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44", address.asEthereumAddressChecksumString())
    }

    @Test
    fun `asMiddleEllipsized(prefixLength, suffixLength)`() {

        val result = "0xd5D82B6aDDc9027B22dCA772Aa68D5d74cdBdF44".asMiddleEllipsized(5, 5)

        assertEquals(result, "0xd5D...BdF44")
    }

    @Test
    fun `asMiddleEllipsized(prefixLength, suffixLength) shorter than prefix+suffix`() {

        val result = "0xd5DBdF44".asMiddleEllipsized(5, 5)

        assertEquals(result, "0xd5DBdF44")
    }
}
