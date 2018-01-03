package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb
import pm.gnosis.heimdall.data.db.models.TransactionPublishStatusDb
import pm.gnosis.heimdall.data.remote.BulkRequest
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository.PublishStatus
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase.Vector
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GnosisSafeTransactionRepository @Inject constructor(
        authenticatorDb: GnosisAuthenticatorDb,
        private val accountsRepository: AccountsRepository,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository
) : TransactionRepository {

    private val descriptionsDao = authenticatorDb.descriptionsDao()

    override fun calculateHash(safeAddress: BigInteger, transaction: Transaction): Single<ByteArray> =
            Single.fromCallable {
                val to = transaction.address.asEthereumAddressString().removeHexPrefix()
                val value = transaction.value?.value.paddedHexString()
                val data = transaction.data?.removeHexPrefix() ?: ""
                val operation = BigInteger.ZERO.paddedHexString(2) // Call
                val nonce = (transaction.nonce ?: DEFAULT_NONCE).paddedHexString()
                hash(safeAddress, to, value, data, operation, nonce)
            }.subscribeOn(Schedulers.computation())

    private fun BigInteger?.paddedHexString(padding: Int = 64): String {
        return (this?.toString(16) ?: "").padStart(padding, '0')
    }

    private fun hash(safeAddress: BigInteger, vararg parts: String): ByteArray {
        val initial = StringBuilder().append(ERC191_BYTE).append(safeAddress.asEthereumAddressString().removeHexPrefix())
        return Sha3Utils.keccak(parts.fold(initial, { acc, part -> acc.append(part) }).toString().hexToByteArray())
    }

    override fun loadStatus(safeAddress: BigInteger): Single<TransactionRepository.TransactionStatus> =
            accountsRepository.loadActiveAccount()
                    .flatMap { account ->
                        val request = TransactionInfoRequest(
                                BulkRequest.SubRequest(TransactionCallParams(
                                        to = safeAddress.asEthereumAddressString(),
                                        data = GnosisSafe.IsOwner.encode(Solidity.Address(account.address))).callRequest(0),
                                        { GnosisSafe.IsOwner.decode(it.checkedResult()).param0.value }
                                ),
                                BulkRequest.SubRequest(TransactionCallParams(
                                        to = safeAddress.asEthereumAddressString(),
                                        data = GnosisSafe.Threshold.encode()).callRequest(1),
                                        { GnosisSafe.Threshold.decode(it.checkedResult()).param0.value.toInt() }
                                ),
                                BulkRequest.SubRequest(TransactionCallParams(
                                        to = safeAddress.asEthereumAddressString(),
                                        data = GnosisSafe.Nonce.encode()).callRequest(2),
                                        { GnosisSafe.Nonce.decode(it.checkedResult()).param0.value }
                                )
                        )
                        ethereumJsonRpcRepository.bulk(request)
                                .map {
                                    TransactionRepository.TransactionStatus(
                                            it.isOwner.value!!,
                                            it.requiredConfirmation.value!!,
                                            it.nonce.value!!
                                    )
                                }
                                .singleOrError()
                    }

    override fun estimateFees(safeAddress: BigInteger, transaction: Transaction): Single<GasEstimate> =
            buildExecuteTransaction(safeAddress, transaction)
                    .flatMap { confirmAndExecuteTransaction ->
                        accountsRepository.loadActiveAccount().map { it to confirmAndExecuteTransaction }
                    }
                    .flatMap { (account, confirmAndExecuteTransaction) ->
                        ethereumJsonRpcRepository.getTransactionParameters(account.address,
                                TransactionCallParams(
                                        to = confirmAndExecuteTransaction.address.asEthereumAddressString(),
                                        data = confirmAndExecuteTransaction.data))
                                .map { GasEstimate(it.gas, Wei(it.gasPrice)) }
                                .singleOrError()
                    }

    override fun submit(safeAddress: BigInteger, transaction: Transaction, overrideGasPrice: Wei?): Completable =
            buildExecuteTransaction(safeAddress, transaction)
                    .flatMapObservable { submitSignedTransaction(it, overrideGasPrice) }
                    .flatMapSingle { addLocalTransaction(safeAddress, transaction, it) }
                    .ignoreElements()

    private fun buildSignaturesData(safeAddress: BigInteger, transaction: Transaction): Single<Triple<Vector<Solidity.UInt8>, Vector<Solidity.Bytes32>, Vector<Solidity.Bytes32>>> =
            calculateHash(safeAddress, transaction)
                    .flatMap {
                        accountsRepository.sign(it)
                    }
                    .flatMap {
                        Single.fromCallable {
                            val vList = arrayListOf(Solidity.UInt8(BigInteger.valueOf(it.v.toLong())))
                            val rList = arrayListOf(Solidity.Bytes32(it.r.toBytes(32)))
                            val sList = arrayListOf(Solidity.Bytes32(it.s.toBytes(32)))
                            Triple(Vector(vList), Vector(rList), Vector(sList))
                        }.subscribeOn(Schedulers.computation())
                    }

    private fun buildExecuteTransaction(safeAddress: BigInteger, innerTransaction: Transaction): Single<Transaction> =
            buildSignaturesData(safeAddress, innerTransaction).flatMap { (vList, rList, sList) ->
                Single.fromCallable {
                    val to = Solidity.Address(innerTransaction.address)
                    val value = Solidity.UInt256(innerTransaction.value?.value ?: BigInteger.ZERO)
                    val data = Solidity.Bytes(innerTransaction.data?.hexStringToByteArrayOrNull() ?: ByteArray(0))
                    val operation = Solidity.UInt8(DEFAULT_OPERATION)
                    val confirmData = GnosisSafe.ExecuteTransaction.encode(to, value, data, operation, vList, rList, sList)
                    Transaction(safeAddress, data = confirmData)
                }
            }

    private fun submitSignedTransaction(transaction: Transaction, overrideGasPrice: Wei? = null): Observable<String> =
            accountsRepository.loadActiveAccount()
                    .flatMapObservable {
                        ethereumJsonRpcRepository.getTransactionParameters(it.address,
                                TransactionCallParams(
                                        to = transaction.address.asEthereumAddressString(),
                                        data = transaction.data))
                    }
                    .flatMapSingle {
                        accountsRepository.signTransaction(transaction.copy(nonce = it.nonce, gas = it.gas, gasPrice = overrideGasPrice?.value ?: it.gasPrice))
                    }
                    .flatMap { ethereumJsonRpcRepository.sendRawTransaction(it) }

    private fun addLocalTransaction(safeAddress: BigInteger, transaction: Transaction, txChainHash: String): Single<String> =
            calculateHash(safeAddress, transaction).flatMap {
                Single.fromCallable {
                    val transactionUuid = UUID.randomUUID().toString()
                    descriptionsDao.insert(
                            TransactionDescriptionDb(transactionUuid, safeAddress, transaction.address,
                                    transaction.value?.value ?: BigInteger.ZERO,
                                    transaction.data ?: "", DEFAULT_OPERATION,
                                    transaction.nonce ?: DEFAULT_NONCE, System.currentTimeMillis(), null, it.toHexString())
                    )
                    descriptionsDao.insert(
                            TransactionPublishStatusDb(transactionUuid, txChainHash, null)
                    )
                    transactionUuid
                }
            }

    override fun observePublishStatus(id: String): Observable<PublishStatus> =
            descriptionsDao.observeStatus(id)
                    .toObservable()
                    .switchMap { status ->
                        status.success?.let {
                            Observable.just(it)
                        } ?: ethereumJsonRpcRepository.getTransactionReceipt(status.transactionId)
                                .flatMap {
                                    it.status?.let {
                                        Observable.just(it == BigInteger.ONE)
                                    } ?: Observable.error<Boolean>(IllegalStateException())
                                }
                                .retryWhen {
                                    it.delay(20, TimeUnit.SECONDS)
                                }
                                .map {
                                    descriptionsDao.update(status.apply { success = it })
                                    it
                                }
                    }
                    .map { if (it) PublishStatus.SUCCESS else PublishStatus.FAILED }
                    .startWith(PublishStatus.PENDING)
                    .onErrorReturnItem(PublishStatus.UNKNOWN)

    private class TransactionInfoRequest(
            val isOwner: SubRequest<Boolean>,
            val requiredConfirmation: SubRequest<Int>,
            val nonce: SubRequest<BigInteger>
    ) : BulkRequest(isOwner, requiredConfirmation, nonce)

    companion object {
        private const val ERC191_BYTE = "19"
        private val DEFAULT_OPERATION = BigInteger.ZERO // Call
        private val DEFAULT_NONCE = BigInteger.ZERO
    }

}