package pm.gnosis.android.app.authenticator.data.contracts

import io.reactivex.Observable
import pm.gnosis.android.app.authenticator.data.PreferencesManager
import pm.gnosis.android.app.authenticator.data.exceptions.InvalidAddressException
import pm.gnosis.android.app.authenticator.data.geth.GethRepository
import pm.gnosis.android.app.authenticator.data.model.TransactionCallParams
import pm.gnosis.android.app.authenticator.data.model.Wei
import pm.gnosis.android.app.authenticator.data.remote.InfuraRepository
import pm.gnosis.android.app.authenticator.util.*
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GnosisMultisigWrapper @Inject constructor(private val infuraRepository: InfuraRepository,
                                                private val gethRepository: GethRepository,
                                                private val preferencesManager: PreferencesManager) {

    companion object {
        const val CONFIRM_TRANSACTION_METHOD_ID = "c01a8c84"
        const val REVOKE_TRANSACTION_METHOD_ID = "20ea8d86"
        const val TRANSACTIONS_METHOD_ID = "9ace38c2"
        const val CHANGE_DAILY_LIMIT_METHOD_ID = "cea08621"

        fun decodeTransactionResult(hex: String): Transaction? {
            var noPrefix = hex.removePrefix("0x")
            if (noPrefix.isEmpty() || noPrefix.length.rem(64) != 0) return null
            val properties = arrayListOf<String>()

            while (noPrefix.length >= 64) {
                properties.add(noPrefix.subSequence(0, 64).toString())
                noPrefix = noPrefix.removeRange(0..63)
            }

            if (properties.size == 7) { //we have inner data
                if (properties[5].isSolidityMethod(CHANGE_DAILY_LIMIT_METHOD_ID)) {
                    var innerData = properties[5] + properties[6]
                    innerData.substring(0..innerData.length - 56)
                    return decodeChangeDailyLimit(innerData)
                } else if (properties[4].isSolidityMethod(ERC20.TRANSFER_METHOD_ID)) {
                    var innerData = properties[4] + properties[5] + properties[6]
                    innerData.substring(0..innerData.length - 56)
                    ERC20.parseTransferData(innerData, BigInteger("18"))?.let {
                        return TokenTransfer(it.to, it.value)
                    }
                }
            } else if (properties.size == 5) { //normal transaction
                return Transfer(properties[0].hexAsBigInteger(),
                        Wei(properties[1].hexAsBigInteger()))
            }

            return null

        }

        fun decodeConfirm(data: String): BigInteger? {
            if (!data.isSolidityMethod(CONFIRM_TRANSACTION_METHOD_ID)) {
                return null
            }
            return decodeUint256(data.removeSolidityMethodPrefix(CONFIRM_TRANSACTION_METHOD_ID))
        }

        fun decodeRevoke(data: String): BigInteger? {
            if (!data.isSolidityMethod(REVOKE_TRANSACTION_METHOD_ID)) {
                return null
            }
            return decodeUint256(data.removeSolidityMethodPrefix(REVOKE_TRANSACTION_METHOD_ID))
        }

        fun decodeChangeDailyLimit(data: String): ChangeDailyLimit? {
            if (!data.isSolidityMethod(CHANGE_DAILY_LIMIT_METHOD_ID)) {
                return null
            }
            val args = data.removeSolidityMethodPrefix(CHANGE_DAILY_LIMIT_METHOD_ID)
            if (args.length >= 64) {
                decodeUint256(args.substring(0..63))?.let {
                    return ChangeDailyLimit(it)
                }
            }
            return null
        }

        private fun decodeUint256(data: String): BigInteger? {
            if (data.length == 64) {
                return data.hexAsBigIntegerOrNull()
            }
            return null
        }
    }

    fun getTransaction(address: String, transactionId: BigInteger): Observable<Transaction> {
        if (!address.isValidEthereumAddress()) return Observable.error(InvalidAddressException(address))
        return infuraRepository.call(TransactionCallParams(to = address,
                data = "$TRANSACTIONS_METHOD_ID${transactionId.toString(16).padStart(64, '0')}"))
                .map { decodeTransactionResult(it)!! }
    }

    fun confirmTransaction(address: String, transactionId: BigInteger): Observable<String> {
        if (!address.isValidEthereumAddress()) return Observable.error(InvalidAddressException(address))
        val data = "$CONFIRM_TRANSACTION_METHOD_ID${transactionId.toString(16).padStart(64, '0')}"
        val transactionCallParams = TransactionCallParams(to = address, data = data, from = gethRepository.getAccount().address.hex)
        return infuraRepository.getTransactionParameters(transactionCallParams)
                .map {
                    gethRepository.signTransaction(
                            nonce = it.nonce,
                            to = address.hexAsBigInteger(),
                            gasLimit = it.gas,
                            gasPrice = it.gasPrice,
                            data = data,
                            amount = BigInteger.ZERO)
                }
                .flatMap { infuraRepository.sendRawTransaction(it) }
    }

    fun revokeTransaction(address: String, transactionId: BigInteger, transactionCallParams: TransactionCallParams): Observable<String> {
        if (!address.isValidEthereumAddress()) return Observable.error(InvalidAddressException(address))
        val data = "$REVOKE_TRANSACTION_METHOD_ID${transactionId.toString(16).padStart(64, '0')}"
        return infuraRepository.getTransactionParameters(transactionCallParams)
                .map {
                    gethRepository.signTransaction(
                            nonce = it.nonce,
                            to = address.hexAsBigInteger(),
                            gasLimit = it.gas,
                            gasPrice = it.gasPrice,
                            data = data,
                            amount = BigInteger.ZERO)
                }
                .flatMap { infuraRepository.sendRawTransaction(it) }
    }

    interface Transaction
    data class Transfer(val address: BigInteger, val value: Wei) : Transaction
    data class ChangeDailyLimit(val newDailyLimit: BigInteger) : Transaction
    data class TokenTransfer(val address: BigInteger, val tokens: BigDecimal) : Transaction
}
