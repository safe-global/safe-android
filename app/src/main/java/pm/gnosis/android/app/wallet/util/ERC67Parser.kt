package pm.gnosis.android.app.wallet.util

import pm.gnosis.android.app.wallet.data.model.TransactionDetails
import pm.gnosis.android.app.wallet.data.model.Wei
import java.math.BigInteger

class ERC67Parser {
    companion object {
        const val SCHEMA = "ethereum:"
        const val VALUE_KEY = "value="
        const val GAS_KEY = "gas="
        const val DATA_KEY = "data="
        const val SEPARATOR = "?"

        fun parse(string: String): TransactionDetails? {
            if (string.startsWith(SCHEMA)) {
                val noSchema = string.removePrefix(SCHEMA)
                val tokens = noSchema.split(SEPARATOR)
                var address: BigInteger? = null
                var value: Wei? = null
                var gas: BigInteger? = null
                var data: String? = null

                tokens.forEach {
                    when {
                        it.startsWith("0x") -> address = nullOnThrow { it.hexAsBigInteger() }
                        it.startsWith(VALUE_KEY) -> value = nullOnThrow { Wei(it.removePrefix(VALUE_KEY).decimalAsBigInteger()) }
                        it.startsWith(GAS_KEY) -> gas = nullOnThrow { it.removePrefix(GAS_KEY).decimalAsBigInteger() }
                        it.startsWith(DATA_KEY) -> data = it.removePrefix(DATA_KEY)
                    }
                }
                address?.let {
                    return TransactionDetails(it, value, gas, data)
                }
            }
            return null
        }
    }
}
