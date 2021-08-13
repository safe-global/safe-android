package io.gnosis.safe.utils

import androidx.annotation.VisibleForTesting
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*

class BalanceFormatter {

    private val formatter1k: DecimalFormat
    private val formatter10k: DecimalFormat
    private val formatter100k: DecimalFormat
    private val formatter1M: DecimalFormat
    private val formatter10M: DecimalFormat
    private val formatter100M: DecimalFormat
    private val formatterBigNumber: DecimalFormat

    @VisibleForTesting
    val decimalSeparator: Char

    @VisibleForTesting
    val groupingSeparator: Char

    init {
        val otherSymbols = DecimalFormatSymbols(Locale.getDefault())
        decimalSeparator = otherSymbols.decimalSeparator
        groupingSeparator = otherSymbols.groupingSeparator

        formatter1k = DecimalFormat("#.#####", otherSymbols).apply {
            roundingMode = RoundingMode.DOWN
        }
        formatter10k = DecimalFormat("#,###.####", otherSymbols).apply {
            roundingMode = RoundingMode.DOWN
        }
        formatter100k = DecimalFormat("##,###.###", otherSymbols).apply {
            roundingMode = RoundingMode.DOWN
        }
        formatter1M = DecimalFormat("###,###.##", otherSymbols).apply {
            roundingMode = RoundingMode.DOWN
        }
        formatter10M = DecimalFormat("#,###,###.#", otherSymbols).apply {
            roundingMode = RoundingMode.DOWN
        }
        formatter100M = DecimalFormat("#,###,###", otherSymbols).apply {
            roundingMode = RoundingMode.DOWN
        }
        formatterBigNumber = DecimalFormat("#.###", otherSymbols).apply {
            roundingMode = RoundingMode.DOWN
        }
    }

    fun fiatBalanceWithCurrency(amount: BigDecimal, currencyCode: String): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            currency = Currency.getInstance(currencyCode)
        }
        val formattedAmount = numberFormat.format(0)
        val shortAmount = shortAmount(amount)
        return formattedAmount.replace("0${decimalSeparator}00", shortAmount).replace("\u00A0", " ")
    }

    fun shortAmount(value: BigDecimal): String = when {
        value <= BigDecimal.ZERO -> {
            "0"
        }
        value < LOWEST_LIMIT -> {
            "< 0${decimalSeparator}00001"
        }
        value < THOUSAND_LIMIT -> {
            formatter1k.format(value)
        }
        value < TEN_THOUSAND_LIMIT -> {
            formatter10k.format(value)
        }
        value < HUNDRED_THOUSAND_LIMIT -> {
            formatter100k.format(value)
        }
        value < MILLION_LIMIT -> {
            formatter1M.format(value)
        }
        value < TEN_MILLION_LIMIT -> {
            formatter10M.format(value)
        }
        value < HUNDRED_MILLION_LIMIT -> {
            formatter100M.format(value)
        }
        value < BILLION_LIMIT -> {
            val formattedValue = value.divide(BigDecimal.TEN.pow(6))
            formatterBigNumber.format(formattedValue) + "M"
        }
        value < TRILLION_LIMIT -> {
            val formattedValue = value.divide(BigDecimal.TEN.pow(9))
            formatterBigNumber.format(formattedValue) + "B"
        }
        value < THOUSAND_TRILLION_LIMIT -> {
            val formattedValue = value.divide(BigDecimal.TEN.pow(12))
            formatterBigNumber.format(formattedValue) + "T"
        }
        else -> {
            "> 999T"
        }
    }

    fun formatAmount(amount: BigInteger, incoming: Boolean, decimals: Int, symbol: String): String {
        val decimalValue = amount.convertAmount(decimals)
        val shortAmount = shortAmount(decimalValue)
        val formattedAmount = when {
            shortAmount.contains("> ") -> {
                shortAmount.replace("> ", if (incoming) "> +" else "> -")
            }
            shortAmount.contains("< ") -> {
                shortAmount.replace("< ", if (incoming) "< +" else "< -")
            }
            else -> {
                val prefix = if (amount == BigInteger.ZERO) "" else if (incoming) "+" else "-"
                "$prefix$shortAmount"
            }
        }
        return "$formattedAmount $symbol"
    }

    companion object {
        private val LOWEST_LIMIT = BigDecimal.ONE.divide(BigDecimal.TEN.pow(5))
        private val THOUSAND_LIMIT = BigDecimal.TEN.pow(3)
        private val TEN_THOUSAND_LIMIT = BigDecimal.TEN.pow(4)
        private val HUNDRED_THOUSAND_LIMIT = BigDecimal.TEN.pow(5)
        private val MILLION_LIMIT = BigDecimal.TEN.pow(6)
        private val TEN_MILLION_LIMIT = BigDecimal.TEN.pow(7)
        private val HUNDRED_MILLION_LIMIT = BigDecimal.TEN.pow(8)
        private val BILLION_LIMIT = BigDecimal.TEN.pow(9)
        private val TRILLION_LIMIT = BigDecimal.TEN.pow(12)
        private val THOUSAND_TRILLION_LIMIT = BigDecimal.TEN.pow(15)
    }
}
