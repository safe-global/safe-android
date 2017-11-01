package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Observable
import okio.ByteString
import pm.gnosis.crypto.utils.Base58Utils
import pm.gnosis.heimdall.DailyLimitException
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.IpfsApi
import pm.gnosis.heimdall.data.remote.models.GnosisSafeTransactionDescription
import pm.gnosis.heimdall.data.repositories.TransactionDetailRepository
import pm.gnosis.models.Wei
import pm.gnosis.utils.isSolidityMethod
import pm.gnosis.utils.removeSolidityMethodPrefix
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
                val arguments = innerData.removeSolidityMethodPrefix(GnosisSafe.ReplaceOwner.METHOD_ID)
                GnosisSafe.ReplaceOwner.decodeArguments(arguments).let { SafeReplaceOwner(it.oldowner.value, it.newowner.value) }
            }
            innerData.isSolidityMethod(GnosisSafe.AddOwner.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(GnosisSafe.AddOwner.METHOD_ID)
                GnosisSafe.AddOwner.decodeArguments(arguments).let { SafeAddOwner(it.owner.value) }
            }
            innerData.isSolidityMethod(GnosisSafe.RemoveOwner.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(GnosisSafe.RemoveOwner.METHOD_ID)
                GnosisSafe.RemoveOwner.decodeArguments(arguments).let { SafeRemoveOwner(it.owner.value) }
            }
            innerData.isSolidityMethod(GnosisSafe.ChangeRequired.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(GnosisSafe.ChangeRequired.METHOD_ID)
                GnosisSafe.ChangeRequired.decodeArguments(arguments).let { SafeChangeConfirmations(it._required.value) }
            }
            innerData.isSolidityMethod(DailyLimitException.ChangeDailyLimit.METHOD_ID) -> {
                val arguments = innerData.removeSolidityMethodPrefix(DailyLimitException.ChangeDailyLimit.METHOD_ID)
                DailyLimitException.ChangeDailyLimit.decodeArguments(arguments).let { SafeChangeDailyLimit(it.dailylimit.value) }
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
