package io.gnosis.safe.utils

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import io.gnosis.data.repositories.SafeRepository.Companion.DEFAULT_FALLBACK_HANDLER
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_IMPLEMENTATION_0_0_2
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_IMPLEMENTATION_0_1_0
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_IMPLEMENTATION_1_0_0
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_IMPLEMENTATION_1_1_1
import io.gnosis.data.repositories.SafeRepository.Companion.SAFE_IMPLEMENTATION_1_2_0
import io.gnosis.safe.R
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.utils.ethereum.ERC67Parser
import pm.gnosis.utils.asEthereumAddress

fun String.asMiddleEllipsized(boundariesLength: Int): String {
    return if (this.length > boundariesLength * 2)
        "${this.subSequence(0, boundariesLength)}...${this.subSequence(this.length - boundariesLength, this.length)}"
    else this
}

fun String.asMiddleEllipsized(prefixLength: Int, suffixLength: Int): String {
    return if (this.length > (prefixLength + suffixLength))
        "${this.subSequence(0, prefixLength)}...${this.subSequence(this.length - suffixLength, this.length)}"
    else this
}

fun String.underline(): CharSequence =
    SpannableString(this).also {
        it.setSpan(UnderlineSpan(), 0, length, 0)
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
            ForegroundColorSpan(context.getColorCompat(R.color.text_emphasis_low)),
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

@StringRes
fun Solidity.Address.implementationVersion(): Int {
    val supportedContracts = mapOf(
        SAFE_IMPLEMENTATION_0_0_2 to R.string.implementation_version_0_0_2,
        SAFE_IMPLEMENTATION_0_1_0 to R.string.implementation_version_0_1_0,
        SAFE_IMPLEMENTATION_1_0_0 to R.string.implementation_version_1_0_0,
        SAFE_IMPLEMENTATION_1_1_1 to R.string.implementation_version_1_1_1,
        SAFE_IMPLEMENTATION_1_2_0 to R.string.implementation_version_1_2_0
    )
    return supportedContracts[this] ?: R.string.unknown_implementation_version
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

fun TextView.appendLink(url: String, urlText: String, @DrawableRes linkIcon: Int? = null, @ColorRes textColor: Int = R.color.primary) {
    movementMethod = LinkMovementMethod.getInstance()

    append(
        SpannableStringBuilder()
            .append(" ")
            .appendTextWithSpans(urlText, listOf(URLSpan(url), ForegroundColorSpan(ContextCompat.getColor(context, textColor))))
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
