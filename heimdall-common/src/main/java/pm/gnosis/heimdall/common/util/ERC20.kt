package pm.gnosis.heimdall.common.util

import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.isSolidityMethod
import pm.gnosis.utils.removeSolidityMethodPrefix
import java.math.BigInteger

object ERC20 {
    const val DECIMALS_METHOD_ID = "313ce567"
    const val TRANSFER_METHOD_ID = "a9059cbb"
    const val SYMBOL_METHOD_ID = "95d89b41"
    const val NAME_METHOD_ID = "06fdde03"

    fun parseTransferData(data: String): TokenTransfer? {
        if (data.isSolidityMethod(TRANSFER_METHOD_ID)) {
            val arguments = data.removeSolidityMethodPrefix(TRANSFER_METHOD_ID)
            if (arguments.length == 128) {
                val to = arguments.substring(0, 64)
                val value = arguments.substring(64, 128)
                return TokenTransfer(to.hexAsBigInteger(), value.hexAsBigInteger())
            }
        }
        return null
    }

    data class Token(val address: String, val name: String?, val symbol: String?, val decimals: BigInteger?)
    data class TokenTransfer(val to: BigInteger, val value: BigInteger)

    val verifiedTokens = mapOf(
            "0x9a642d6b3368ddc662CA244bAdf32cDA716005BC".hexAsBigInteger() to "Qtum",
            "0x888666CA69E0f178DED6D75b5726Cee99A87D698".hexAsBigInteger() to "Iconomi",
            "0x6810e776880c02933d47db1b9fc05908e5386b96".hexAsBigInteger() to "Gnosis",
            "0xa74476443119A942dE498590Fe1f2454d7D4aC0d".hexAsBigInteger() to "Golem",
            "0x744d70fdbe2ba4cf95131626614a1763df805b9e".hexAsBigInteger() to "StatusNetwork",
            "0x48c80F1f4D53D5951e5D5438B54Cba84f29F32a5".hexAsBigInteger() to "Augur",
            "0xc66ea802717bfb9833400264dd12c2bceaa34a6d".hexAsBigInteger() to "Maker",
            "0xe0b7927c4af23765cb51314a0e0521a9645f0e2a".hexAsBigInteger() to "DigixGlobal",
            "0xd26114cd6EE289AccF82350c8d8487fedB8A0C07".hexAsBigInteger() to "OmiseGO",
            "0x0d8775f648430679a709e98d2b0cb6250d2887ef".hexAsBigInteger() to "BasicAttentionToken"
    )
}
