package io.gnosis.safe.utils

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
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
    }
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
