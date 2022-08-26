package io.gnosis.safe.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import io.gnosis.data.repositories.SafeRepository.Companion.DEFAULT_FALLBACK_HANDLER
import io.gnosis.safe.R
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.utils.ethereum.ERC67Parser
import pm.gnosis.utils.asEthereumAddress
import timber.log.Timber
import java.util.regex.Pattern

fun String.asMiddleEllipsized(prefixLength: Int, suffixLength: Int): String {
    return if (this.length > (prefixLength + suffixLength))
        "${this.subSequence(0, prefixLength)}...${this.subSequence(this.length - suffixLength, this.length)}"
    else this
}

fun parseEthereumAddress(address: String) = address.asEthereumAddress() ?: ERC67Parser.parse(address)?.address

fun Solidity.Address.shortChecksumString() =
    asEthereumAddressChecksumString().abbreviateEthAddress()

fun Solidity.Address.formatEthAddress(context: Context, prefixLength: Int = 6, suffixLength: Int = 4, addMiddleLinebreak: Boolean = true): Spannable {
    return SpannableStringBuilder(this.asEthereumAddressChecksumString()).apply {
        if (addMiddleLinebreak) insert(21, "\n")
        setSpan(
            ForegroundColorSpan(context.getColorCompat(R.color.address_boundaries)),
            0,
            prefixLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            prefixLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setSpan(
            ForegroundColorSpan(context.getColorCompat(R.color.address_boundaries)),
            length - suffixLength,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setSpan(
            StyleSpan(Typeface.BOLD),
            length - suffixLength,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setSpan(
            ForegroundColorSpan(context.getColorCompat(R.color.label_tertiary)),
            prefixLength,
            length - suffixLength,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}

@StringRes
fun Solidity.Address?.fallBackHandlerLabel(): Int =
    if (this == DEFAULT_FALLBACK_HANDLER) {
        R.string.default_fallback_handler
    } else {
        R.string.unknown_fallback_handler
    }

fun String.formatEthAddressBold(prefixLength: Int = 6, suffixLength: Int = 4): Spannable {
    return SpannableStringBuilder(this.substring(0, this.length / 2) + "\n" + this.substring(this.length / 2, this.length)).apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, prefixLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(StyleSpan(Typeface.BOLD), this.length - suffixLength, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

fun String.abbreviateEthAddress(): String =
    asMiddleEllipsized(6, 4)

fun Solidity.Address.formatForTxList(): String = asEthereumAddressChecksumString().abbreviateEthAddress()

// Append a CharSequence with several spans applied
fun SpannableStringBuilder.appendTextWithSpans(text: CharSequence, spans: List<Any>, flags: Int = 0): SpannableStringBuilder {
    val start = length
    append(text)
    spans.forEach { span ->
        setSpan(span, start, length, flags)
    }
    return this
}

fun Resources.replaceDoubleNewlineWithParagraphLineSpacing(@StringRes stringResource: Int): SpannableString {
    val spannableString = SpannableString(getString(stringResource))
    val matcher = Pattern.compile("\n\n").matcher(getString(stringResource))
    while (matcher.find()) {
        spannableString.setSpan(
            AbsoluteSizeSpan(getDimension(R.dimen.default_paragraph_line_spacing).toInt()),
            matcher.start() + 1,
            matcher.end(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }
    return spannableString
}

fun TextView.appendLink(
    url: String,
    urlText: String,
    @DrawableRes linkIcon: Int? = null,
    @ColorRes textColor: Int = R.color.primary,
    prefix: String = "",
    underline: Boolean = false
) {
    movementMethod = LinkMovementMethod.getInstance()

    append(
        SpannableStringBuilder()
            .append(prefix)
            .appendTextWithSpans(
                urlText,
                if (underline) {
                    listOf(URLSpan(url), ForegroundColorSpan(ContextCompat.getColor(context, textColor)), UnderlineSpan())
                } else {
                    listOf(URLSpan(url), ForegroundColorSpan(ContextCompat.getColor(context, textColor)))
                }
            )
    )

    linkIcon?.let {
        val linkDrawable = ContextCompat.getDrawable(context, linkIcon)!!
        linkDrawable.setBounds(0, 0, linkDrawable.intrinsicWidth, linkDrawable.intrinsicHeight)
        append(
            SpannableStringBuilder()
                .append(" ")
                .appendTextWithSpans(" ", listOf(ImageSpan(linkDrawable, ImageSpan.ALIGN_BASELINE), URLSpan(url)))
        )
    }
}

fun TextView.setLink(
    url: String,
    urlText: String,
    @DrawableRes linkIcon: Int? = null,
    @ColorRes textColor: Int = R.color.primary,
    prefix: String = "",
    underline: Boolean = false
) {
    text = null
    appendLink(url, urlText, linkIcon, textColor, prefix, underline)
}

fun String.toColor(context: Context, failsafeColor: Int = R.color.white): Int {
    return try {
        Color.parseColor(this)
    } catch (e: Exception) {
        Timber.e(e)
        context.getColorCompat(failsafeColor)
    }
}
