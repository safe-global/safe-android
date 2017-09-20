package pm.gnosis.heimdall.util

import pm.gnosis.heimdall.data.model.TransactionDetails
import pm.gnosis.heimdall.data.model.Wei
import java.math.BigInteger

class ERC67Parser {
    companion object {
        const val SCHEMA = "ethereum:"
        const val VALUE_KEY = "value="
        const val GAS_KEY = "gas="
        const val GAS_PRICE_KEY = "gasPrice="
        const val DATA_KEY = "data="
        const val NONCE_KEY = "nonce="
        const val SEPARATOR = "?"

        fun parse(string: String): TransactionDetails? {
            if (string.startsWith(SCHEMA)) {
                val noSchema = string.removePrefix(SCHEMA)
                val tokens = noSchema.split(SEPARATOR)
                var address: BigInteger? = null
                var value: Wei? = null
                var gas: BigInteger? = null
                var gasPrice: BigInteger? = null
                var data: String? = null
                var nonce: BigInteger? = null

                tokens.forEach {
                    when {
                        it.startsWith("0x") -> address = nullOnThrow { it.hexAsBigInteger() }
                        it.startsWith(VALUE_KEY) -> value = nullOnThrow { Wei(it.removePrefix(VALUE_KEY).decimalAsBigInteger()) }
                        it.startsWith(GAS_KEY) -> gas = nullOnThrow { it.removePrefix(GAS_KEY).decimalAsBigInteger() }
                        it.startsWith(GAS_PRICE_KEY) -> gasPrice = nullOnThrow { it.removePrefix(GAS_PRICE_KEY).decimalAsBigInteger() }
                        it.startsWith(DATA_KEY) -> data = it.removePrefix(DATA_KEY)
                        it.startsWith(NONCE_KEY) -> nonce = nullOnThrow { it.removePrefix(NONCE_KEY).decimalAsBigInteger() }
                    }
                }
                address?.let {
                    return TransactionDetails(it, value, gas, gasPrice, data, nonce)
                }
            }
            return null
        }
    }
}
