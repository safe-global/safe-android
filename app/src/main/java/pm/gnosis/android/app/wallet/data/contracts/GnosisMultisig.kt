package pm.gnosis.android.app.wallet.data.contracts

import io.reactivex.Observable
import pm.gnosis.android.app.wallet.data.geth.GethRepository
import pm.gnosis.android.app.wallet.data.model.TransactionCallParams
import pm.gnosis.android.app.wallet.data.model.Wei
import pm.gnosis.android.app.wallet.data.remote.InfuraRepository
import pm.gnosis.android.app.wallet.util.hexAsBigInteger
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GnosisMultisig @Inject constructor(private val infuraRepository: InfuraRepository,
                                         private val gethRepository: GethRepository) {
    companion object {
        const val ADDRESS = "0xc6fc5e99b5d253f6eabf53fb2eab94af4e6a1444" //test address
        const val CONFIRM_TRANSACTION_METHOD_ID = "0xc01a8c84"
        const val REVOKE_TRANSACTION_METHOD_ID = "0x20ea8d86"
        const val TRANSACTIONS_METHOD_ID = "0x9ace38c2"

        fun decodeTransactionResult(hex: String): MultiSigTransaction? {
            val noPrefix = hex.removePrefix("0x")
            if (noPrefix.isEmpty() || noPrefix.length.rem(64) != 0) return null
            val properties = arrayListOf<CharSequence>()
            (0..noPrefix.length - 1 step 64).forEach { i -> properties += noPrefix.subSequence(i, i + 64) }
            if (properties.size >= 2) {
                return MultiSigTransaction(properties[0].toString().hexAsBigInteger(),
                        Wei(properties[1].toString().hexAsBigInteger()))
            }
            return null
        }
    }

    fun getTransaction(transactionId: BigInteger): Observable<MultiSigTransaction> {
        return infuraRepository.call(TransactionCallParams(to = ADDRESS,
                data = "$TRANSACTIONS_METHOD_ID${transactionId.toString(16).padStart(64, '0')}"))
                .map { decodeTransactionResult(it)!! }
    }

    fun confirmTransaction(transactionId: BigInteger): Observable<String> {
        val data = "$CONFIRM_TRANSACTION_METHOD_ID${transactionId.toString(16).padStart(64, '0')}"
        val transactionCallParams = TransactionCallParams(to = ADDRESS, data = data, from = gethRepository.getAccount().address.hex)
        return infuraRepository.getTransactionParameters(transactionCallParams)
                .map {
                    gethRepository.signTransaction(
                            nonce = it.nonce,
                            to = ADDRESS.hexAsBigInteger(),
                            gasLimit = it.gas,
                            gasPrice = it.gasPrice,
                            data = data,
                            amount = BigInteger.ZERO)
                }
                .flatMap { infuraRepository.sendRawTransaction(it) }
    }

    fun revokeTransaction(transactionId: BigInteger, transactionCallParams: TransactionCallParams): Observable<String> {
        val data = "$REVOKE_TRANSACTION_METHOD_ID${transactionId.toString(16).padStart(64, '0')}"
        return infuraRepository.getTransactionParameters(transactionCallParams)
                .map {
                    gethRepository.signTransaction(
                            nonce = it.nonce,
                            to = ADDRESS.hexAsBigInteger(),
                            gasLimit = it.gas,
                            gasPrice = it.gasPrice,
                            data = data,
                            amount = BigInteger.ZERO)
                }
                .flatMap { infuraRepository.sendRawTransaction(it) }
    }

    data class MultiSigTransaction(val address: BigInteger, val value: Wei)
}
