package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Observable
import okio.ByteString
import pm.gnosis.crypto.utils.Base58Utils
import pm.gnosis.heimdall.DailyLimitException
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.IpfsApi
import pm.gnosis.heimdall.data.remote.models.GnosisSafeTransactionDescription
import pm.gnosis.heimdall.data.repositories.TransactionDetailRepository
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IpfsTransactionDetailRepository @Inject constructor(
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
        private val ipfsApi: IpfsApi
) : TransactionDetailRepository {
    override fun loadTransactionDetails(address: BigInteger, transactionHash: String, descriptionHash: String): Observable<TransactionDetails> {
        return Observable.fromCallable {
            Base58Utils.encode(ByteString.decodeHex("1220" + descriptionHash))
        }.flatMap {
            ipfsApi.transactionDescription(it)
                    .map { decodeTransactionResult(it) }
                    .onErrorReturnItem(UnknownTransactionDetails(it))
        }
    }

    private fun decodeTransactionResult(description: GnosisSafeTransactionDescription): TransactionDetails? {
        val innerData = description.data
        return when {
            innerData.isSolidityMethod(GnosisSafe.ReplaceOwner.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.ReplaceOwner.METHOD_ID)
                MultiSigWalletWithDailyLimit.ReplaceOwner.decodeArguments(arguments).let { SafeReplaceOwner(it.owner.value, it.newowner.value) }
            }
            innerData.isSolidityMethod(GnosisSafe.AddOwner.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.AddOwner.METHOD_ID)
                MultiSigWalletWithDailyLimit.AddOwner.decodeArguments(arguments).let { SafeAddOwner(it.owner.value) }
            }
            innerData.isSolidityMethod(GnosisSafe.RemoveOwner.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.RemoveOwner.METHOD_ID)
                MultiSigWalletWithDailyLimit.RemoveOwner.decodeArguments(arguments).let { SafeRemoveOwner(it.owner.value) }
            }
            innerData.isSolidityMethod(GnosisSafe.ChangeRequired.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.ChangeRequirement.METHOD_ID)
                MultiSigWalletWithDailyLimit.ChangeRequirement.decodeArguments(arguments).let { SafeChangeConfirmations(it._required.value) }
            }
            innerData.isSolidityMethod(DailyLimitException.ChangeDailyLimit.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.ChangeDailyLimit.METHOD_ID)
                MultiSigWalletWithDailyLimit.ChangeDailyLimit.decodeArguments(arguments).let { SafeChangeDailyLimit(it._dailylimit.value) }
            }
            innerData.isSolidityMethod(StandardToken.Transfer.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(StandardToken.Transfer.METHOD_ID)
                StandardToken.Transfer.decodeArguments(arguments).let { TokenTransfer(description.to, it.to.value, it.value.value) }
            }
            description.value.value != BigInteger.ZERO -> {
                EtherTransfer(description.to, description.value)
            }
            else -> GenericTransactionDetails(description.to, description.value, description.data, description.operation, description.nonce)
        }
    }
}

sealed class TransactionDetails
data class UnknownTransactionDetails(val data: String?): TransactionDetails()
data class GenericTransactionDetails(val to: BigInteger,  val value: Wei, val data: String, val operation: BigInteger, val nonce: BigInteger): TransactionDetails()
data class EtherTransfer(val address: BigInteger, val value: Wei) : TransactionDetails()
data class TokenTransfer(val tokenAddress: BigInteger, val recipient: BigInteger, val tokens: BigInteger) : TransactionDetails()
data class SafeChangeDailyLimit(val newDailyLimit: BigInteger) : TransactionDetails()
data class SafeReplaceOwner(val owner: BigInteger, val newOwner: BigInteger) : TransactionDetails()
data class SafeAddOwner(val owner: BigInteger) : TransactionDetails()
data class SafeRemoveOwner(val owner: BigInteger) : TransactionDetails()
data class SafeChangeConfirmations(val newConfirmations: BigInteger) : TransactionDetails()
