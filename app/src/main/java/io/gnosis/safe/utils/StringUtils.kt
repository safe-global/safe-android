package io.gnosis.safe.utils

import android.text.SpannableString
import android.text.style.UnderlineSpan
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.utils.ethereum.ERC67Parser
import pm.gnosis.utils.*
import java.math.BigInteger

fun String.asMiddleEllipsized(boundariesLength: Int): String {
    return if (this.length > boundariesLength * 2)
        "${this.subSequence(0, boundariesLength)}...${this.subSequence(this.length - boundariesLength, this.length)}"
    else this
}

fun String.underline(): CharSequence =
    SpannableString(this).also {
        it.setSpan(UnderlineSpan(), 0, length, 0)
    }

fun String.parseToBigIntegerOrNull(): BigInteger? =
    nullOnThrow { parseToBigInteger() }

fun String.parseToBigInteger(): BigInteger =
    if (startsWith("0x")) hexAsBigInteger() else decimalAsBigInteger()

fun parseEthereumAddress(address: String) = address.asEthereumAddress() ?: ERC67Parser.parse(address)?.address

fun Solidity.Address.shortChecksumString() =
    asEthereumAddressChecksumString().asMiddleEllipsized(4)
