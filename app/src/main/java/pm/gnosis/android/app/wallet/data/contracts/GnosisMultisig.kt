package pm.gnosis.android.app.wallet.data.contracts

import pm.gnosis.android.app.wallet.data.model.Wei
import pm.gnosis.android.app.wallet.data.remote.InfuraRepository
import pm.gnosis.android.app.wallet.util.hexAsBigInteger
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GnosisMultisig @Inject constructor(private val infuraRepository: InfuraRepository) {
    companion object {
        const val ADDRESS = "0xe71d2ddb3ca69a94b557695a9614d3262845ea11" //test address
        const val CONFIRM_TRANSACTION_METHOD_ID = "0xc01a8c84"
        const val REVOKE_TRANSACTION_METHOD_ID = "0x20ea8d86"
        const val TRANSACTIONS_METHOD_ID = "0x9ace38c2"

        fun decodeTransactionResult(hex: String): MultiSigTransaction? {
            val noPrefix = hex.removePrefix("0x")
            if (noPrefix.isEmpty() || noPrefix.length.rem(64) != 0) return null
            val properties = arrayListOf<CharSequence>()
            (0..noPrefix.length step 64).forEach { i -> properties += noPrefix.subSequence(i, i + 64 - 1) }
            if (properties.size >= 2) {
                return MultiSigTransaction(properties[0].toString().hexAsBigInteger(),
                        Wei(properties[1].toString().hexAsBigInteger()))
            }
            return null
        }
    }

    fun confirmTransaction(transactionId: BigInteger) {

    }

    fun revokeTransaction(transactionId: BigInteger) {

    }

    data class MultiSigTransaction(val address: BigInteger, val value: Wei)
}
