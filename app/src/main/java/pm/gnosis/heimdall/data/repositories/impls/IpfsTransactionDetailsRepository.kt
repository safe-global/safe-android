package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okio.ByteString
import pm.gnosis.crypto.utils.Base58Utils
import pm.gnosis.heimdall.DailyLimitException
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.db.models.toDb
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.IpfsApi
import pm.gnosis.heimdall.data.remote.models.GnosisSafeTransactionDescription
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.isSolidityMethod
import pm.gnosis.utils.removeSolidityMethodPrefix
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IpfsTransactionDetailsRepository @Inject constructor(
        authenticatorDb: GnosisAuthenticatorDb,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
        private val ipfsApi: IpfsApi
) : TransactionDetailsRepository {

    companion object {
        private const val IPFS_HASH_PREFIX = "1220"
    }

    private val descriptionsDao = authenticatorDb.descriptionsDao()

    private val detailRequests = ConcurrentHashMap<String, Observable<GnosisSafeTransactionDescription>>()

    override fun loadTransactionDetails(descriptionHash: String, address: BigInteger, transactionHash: String?): Observable<TransactionDetails> {
        return loadDescription(descriptionHash, address, transactionHash)
                .map { decodeTransactionResult(descriptionHash, it) }
                .onErrorReturnItem(TransactionDetails.unknown(descriptionHash))
    }

    private fun loadDescription(hash: String, address: BigInteger, transactionHash: String?): Observable<GnosisSafeTransactionDescription> {
        return descriptionsDao.loadDescription(hash)
                .subscribeOn(Schedulers.io())
                .map { it.fromDb() }
                .toObservable()
                .onErrorResumeNext { _: Throwable ->
                    loadDescriptionFromIpfs(hash, address, transactionHash)
                            .map {
                                descriptionsDao.insertDescription(it.toDb(hash))
                                it
                            }
                }
    }

    private fun loadDescriptionFromIpfs(hash: String, address: BigInteger, transactionHash: String?): Observable<GnosisSafeTransactionDescription> {
        return detailRequests.getOrPut(hash, {
            createIpfsIdentifier(hash)
                    .flatMap { ipfsApi.transactionDescription(it) }
                    .flatMap { verifyDescription(address, transactionHash, it) }
                    .doOnTerminate { detailRequests.remove(hash) }
                    .publish()
                    .autoConnect()
        })
    }

    private fun verifyDescription(address: BigInteger, transactionHash: String?, description: GnosisSafeTransactionDescription): Observable<GnosisSafeTransactionDescription> {
        return Observable.fromCallable<String> {
            if (transactionHash != null && description.transactionHash.removePrefix("0x") != transactionHash.removePrefix("0x")) {
                throw IllegalStateException("Invalid description data!")
            }
            val to = Solidity.Address(description.to)
            val value = Solidity.UInt256(description.value.value)
            val data = Solidity.Bytes(description.data.hexToByteArray())
            val operation = Solidity.UInt8(description.operation)
            val nonce = Solidity.UInt256(description.nonce)
            GnosisSafe.GetTransactionHash.encode(to, value, data, operation, nonce)
        }.flatMap {
            ethereumJsonRpcRepository.call(TransactionCallParams(to = description.safeAddress.asEthereumAddressString(), data = it))
        }.map { remoteTxHash ->
            val cleanHash = remoteTxHash.removePrefix("0x")
            if (transactionHash != null && transactionHash.removePrefix("0x") != cleanHash ||
                    description.transactionHash.removePrefix("0x") != cleanHash) {
                throw IllegalStateException("Invalid description data!")
            }
            description
        }
    }

    private fun createIpfsIdentifier(hash: String) =
            Observable.fromCallable {
                Base58Utils.encode(ByteString.decodeHex(IPFS_HASH_PREFIX + hash))
            }

    private fun decodeTransactionResult(descriptionHash: String, description: GnosisSafeTransactionDescription): TransactionDetails {
        val innerData = description.data
        val type = when {
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
            else -> SafeTransaction(description.to, description.value, description.data, description.operation, description.nonce)
        }
        return TransactionDetails(type, descriptionHash, description.transactionHash, description.subject, description.submittedAt)
    }
}
