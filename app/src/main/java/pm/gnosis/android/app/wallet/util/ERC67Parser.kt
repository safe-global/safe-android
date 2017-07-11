package pm.gnosis.android.app.wallet.util

import pm.gnosis.android.app.wallet.data.model.TransferDetails

class ERC67Parser {
    companion object {
        const val SCHEMA = "ethereum:"
        const val VALUE_KEY = "value="
        const val GAS_KEY = "gas="
        const val DATA_KEY = "data="
        const val SEPARATOR = "?"

        fun parse(string: String): TransferDetails? {
            if (string.startsWith(SCHEMA)) {
                val noSchema = string.removePrefix(SCHEMA)
                val tokens = noSchema.split(SEPARATOR)
                var address: String? = null
                var value: String? = null
                var gas: String? = null
                var data: String? = null

                tokens.forEach {
                    when {
                        it.startsWith("0x") -> address = it
                        it.startsWith(VALUE_KEY) -> value = it.removePrefix(VALUE_KEY)
                        it.startsWith(GAS_KEY) -> gas = it.removePrefix(GAS_KEY)
                        it.startsWith(DATA_KEY) -> data = it.removePrefix(DATA_KEY)
                    }
                }
                address?.let {
                    return TransferDetails(it, value, gas, data)
                }
            }
            return null
        }
    }
}
