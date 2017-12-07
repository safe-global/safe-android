package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okio.ByteString
import pm.gnosis.crypto.utils.Base58Utils
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.common.utils.getSharedObservable
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.db.models.toDb
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.IpfsApi
import pm.gnosis.heimdall.data.remote.models.GnosisSafeTransactionDescription
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.TokenTransferData
import pm.gnosis.heimdall.data.repositories.TransactionDetails
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
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

    override fun loadTransactionDetails(transaction: Transaction): Single<TransactionDetails> {
        return Single.fromCallable {
            decodeTransactionResult(null, transaction)
        }
    }

    override fun loadTransactionDetails(id: String, address: BigInteger, transactionHash: String?): Single<TransactionDetails> {
        return loadDescription(id, address, transactionHash)
                .map { decodeDescription(id, it) }
    }

    private fun loadDescription(id: String, address: BigInteger, transactionHash: String?): Single<GnosisSafeTransactionDescription> {
        return descriptionsDao.loadDescription(id)
                .subscribeOn(Schedulers.io())
                .map { it.fromDb() }
                .toObservable()
                .onErrorResumeNext { _: Throwable ->
                    loadDescriptionFromIpfs(id, address, transactionHash)
                            .map {
                                descriptionsDao.insertDescription(it.toDb(id))
                                it
                            }
                }
                .singleOrError()
    }

    private fun loadDescriptionFromIpfs(id: String, address: BigInteger, transactionHash: String?): Observable<GnosisSafeTransactionDescription> {
        return detailRequests.getSharedObservable(id,
                createIpfsIdentifier(id)
                        .flatMap { ipfsApi.transactionDescription(it) }
                        .flatMap { verifyDescription(address, transactionHash, it) }
        )
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

    private fun parseTransactionType(value: BigInteger?, data: String?): TransactionType =
            when {
                data?.isSolidityMethod(StandardToken.Transfer.METHOD_ID) == true -> TransactionType.TOKEN_TRANSFER
                data.isNullOrBlank() && value != null && value > BigInteger.ZERO -> TransactionType.ETHER_TRANSFER
                else -> TransactionType.GENERIC
            }

    override fun loadTransactionType(transaction: Transaction): Single<TransactionType> =
            Single.fromCallable {
                parseTransactionType(transaction.value?.value, transaction.data)
            }

    private fun createIpfsIdentifier(hash: String) =
            Observable.fromCallable {
                Base58Utils.encode(ByteString.decodeHex(IPFS_HASH_PREFIX + hash))
            }

    private fun decodeDescription(transactionId: String, description: GnosisSafeTransactionDescription) =
            decodeTransactionResult(transactionId, description.toTransaction(), description.subject, description.submittedAt)

    private fun decodeTransactionResult(transactionId: String?, transaction: Transaction, subject: String? = null, submittedAt: Long? = null): TransactionDetails {
        val type = parseTransactionType(transaction.value?.value ?: BigInteger.ZERO, transaction.data)
        val transactionData = when (type) {
            TransactionType.TOKEN_TRANSFER -> {
                val arguments = transaction.data!!.removeSolidityMethodPrefix(StandardToken.Transfer.METHOD_ID)
                StandardToken.Transfer.decodeArguments(arguments).let { TokenTransferData(it.to.value, it.value.value) }
            }
            else -> null
        }
        return TransactionDetails(transactionId, type, transactionData, transaction, subject, submittedAt)
    }

    private fun GnosisSafeTransactionDescription.toTransaction(): Transaction =
            Transaction(to, value = value, data = data, nonce = nonce)
}
