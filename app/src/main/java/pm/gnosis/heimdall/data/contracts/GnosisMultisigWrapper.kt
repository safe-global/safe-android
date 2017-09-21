package pm.gnosis.heimdall.data.contracts

import io.reactivex.Observable
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.exceptions.InvalidAddressException
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.model.Wei
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.utils.isValidEthereumAddress
import pm.gnosis.utils.isSolidityMethod
import pm.gnosis.utils.removeSolidityMethodPrefix
import pm.gnosis.utils.toHex
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GnosisMultisigWrapper @Inject constructor(private val ethereumJsonRpcRepository: EthereumJsonRpcRepository) {

    companion object {
        fun decodeTransactionResult(hex: String): WrapperTransaction? {
            val noPrefix = hex.removePrefix("0x")
            if (noPrefix.isEmpty() || noPrefix.length.rem(64) != 0) return null

            val transaction = MultiSigWalletWithDailyLimit.Transactions.decode(noPrefix)
            val innerData = transaction.data.items.toHex()
            return when {
                innerData.isSolidityMethod(MultiSigWalletWithDailyLimit.ReplaceOwner.METHOD_ID) -> {
                    val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.ReplaceOwner.METHOD_ID)
                    MultiSigWalletWithDailyLimit.ReplaceOwner.decodeArguments(arguments).let { ReplaceOwner(it.owner.value, it.newowner.value) }
                }
                innerData.isSolidityMethod(MultiSigWalletWithDailyLimit.ChangeDailyLimit.METHOD_ID) -> {
                    val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.ChangeDailyLimit.METHOD_ID)
                    MultiSigWalletWithDailyLimit.ChangeDailyLimit.decodeArguments(arguments).let { ChangeDailyLimit(it._dailylimit.value) }
                }
                innerData.isSolidityMethod(MultiSigWalletWithDailyLimit.AddOwner.METHOD_ID) -> {
                    val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.AddOwner.METHOD_ID)
                    MultiSigWalletWithDailyLimit.AddOwner.decodeArguments(arguments).let { AddOwner(it.owner.value) }
                }
                innerData.isSolidityMethod(MultiSigWalletWithDailyLimit.RemoveOwner.METHOD_ID) -> {
                    val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.RemoveOwner.METHOD_ID)
                    MultiSigWalletWithDailyLimit.RemoveOwner.decodeArguments(arguments).let { RemoveOwner(it.owner.value) }
                }
                innerData.isSolidityMethod(MultiSigWalletWithDailyLimit.ChangeRequirement.METHOD_ID) -> {
                    val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.ChangeRequirement.METHOD_ID)
                    MultiSigWalletWithDailyLimit.ChangeRequirement.decodeArguments(arguments).let { ChangeConfirmations(it._required.value) }
                }
                innerData.isSolidityMethod(StandardToken.Transfer.METHOD_ID) -> {
                    val arguments = innerData.removeSolidityMethodPrefix(StandardToken.Transfer.METHOD_ID)
                    StandardToken.Transfer.decodeArguments(arguments).let { TokenTransfer(transaction.destination.value, it.to.value, it.value.value) }
                }
                transaction.value.value != BigInteger.ZERO -> {
                    Transfer(transaction.destination.value, Wei(transaction.value.value))
                }
                else -> null
            }
        }
    }

    fun getTransaction(address: String, transactionId: BigInteger): Observable<WrapperTransaction> {
        if (!address.isValidEthereumAddress()) return Observable.error(InvalidAddressException(address))
        return ethereumJsonRpcRepository.call(TransactionCallParams(to = address,
                data = "${MultiSigWalletWithDailyLimit.Transactions.METHOD_ID}${transactionId.toString(16).padStart(64, '0')}"))
                .map { decodeTransactionResult(it)!! }
    }

    interface WrapperTransaction
    data class Transfer(val address: BigInteger, val value: Wei) : WrapperTransaction
    data class ChangeDailyLimit(val newDailyLimit: BigInteger) : WrapperTransaction
    data class TokenTransfer(val tokenAddress: BigInteger, val recipient: BigInteger, val tokens: BigInteger) : WrapperTransaction
    data class ReplaceOwner(val owner: BigInteger, val newOwner: BigInteger) : WrapperTransaction
    data class AddOwner(val owner: BigInteger) : WrapperTransaction
    data class RemoveOwner(val owner: BigInteger) : WrapperTransaction
    data class ChangeConfirmations(val newConfirmations: BigInteger) : WrapperTransaction
}