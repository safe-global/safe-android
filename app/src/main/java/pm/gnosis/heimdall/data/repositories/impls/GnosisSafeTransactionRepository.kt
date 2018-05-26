package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb
import pm.gnosis.heimdall.data.db.models.TransactionPublishStatusDb
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.PublishStatus
import pm.gnosis.heimdall.data.repositories.TxExecutorRepository
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.utils.*
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GnosisSafeTransactionRepository @Inject constructor(
    appDb: ApplicationDb,
    private val accountsRepository: AccountsRepository,
    private val ethereumRepository: EthereumRepository,
    private val txExecutorRepository: TxExecutorRepository
) : TransactionExecutionRepository {

    private val descriptionsDao = appDb.descriptionsDao()

    override fun calculateHash(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<ByteArray> =
        Single.fromCallable {
            val tx = transaction.wrapped
            val to = tx.address.asEthereumAddressString().removeHexPrefix()
            val value = tx.value?.value.paddedHexString()
            val data = tx.data?.removeHexPrefix() ?: ""
            val operationString = transaction.operation.toSolidity().value.paddedHexString(2) // Call
            val nonce = (tx.nonce ?: DEFAULT_NONCE).paddedHexString()
            hash(safeAddress, to, value, data, operationString, nonce)
        }.subscribeOn(Schedulers.computation())

    private fun BigInteger?.paddedHexString(padding: Int = 64): String {
        return (this?.toString(16) ?: "").padStart(padding, '0')
    }

    private fun hash(safeAddress: Solidity.Address, vararg parts: String): ByteArray {
        val initial = StringBuilder().append(ERC191_BYTE).append(safeAddress.asEthereumAddressString().removeHexPrefix())
        return Sha3Utils.keccak(
            parts.fold(
                initial,
                { acc, part -> acc.append(part) }).toString().hexToByteArray()
        )
    }

    override fun loadExecuteInformation(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction
    ): Single<TransactionExecutionRepository.ExecuteInformation> =
        Single.fromCallable {
            TransactionInfoRequest(
                EthCall(
                    transaction = Transaction(
                        safeAddress, data = GnosisSafe.Threshold.encode()
                    ), id = 0
                ),
                EthCall(
                    transaction = Transaction(
                        safeAddress, data = GnosisSafe.Nonce.encode()
                    ), id = 1
                ),
                EthCall(
                    transaction = Transaction(
                        safeAddress, data = GnosisSafe.GetOwners.encode()
                    ), id = 2
                ),
                EthEstimateGas(
                    from = safeAddress,
                    transaction = transaction.wrapped,
                    id = 3
                ),
                EthBalance(
                    address = safeAddress,
                    id = 4
                )
            )

        }.subscribeOn(Schedulers.computation())
            .flatMap { ethereumRepository.request(it).singleOrError() }
            .flatMap { info -> accountsRepository.loadActiveAccount().map { info to it.address } }
            .flatMap { (info, sender) ->
                val nonce = GnosisSafe.Nonce.decode(info.nonce.result()!!).param0.value
                val threshold = GnosisSafe.Threshold.decode(info.threshold.result()!!).param0.value.toInt()
                val owners = GnosisSafe.GetOwners.decode(info.owners.result()!!).param0.items
                val updatedTransaction = transaction.copy(wrapped = transaction.wrapped.updateTransactionWithStatus(nonce))
                val estimatedFees = info.estimate.result()!!
                val safeBalance = info.balance.result()!!
                calculateHash(safeAddress, updatedTransaction).map {
                    TransactionExecutionRepository.ExecuteInformation(
                        it.toHexString(),
                        updatedTransaction,
                        sender,
                        threshold,
                        owners,
                        BigInteger.valueOf(20000000000),
                        estimatedFees,
                        safeBalance
                    )
                }
            }

    private fun Transaction.updateTransactionWithStatus(safeNonce: BigInteger) = nonce?.let { this } ?: copy(nonce = safeNonce)

    override fun sign(safeAddress: Solidity.Address, transaction: SafeTransaction): Single<Signature> =
        calculateHash(safeAddress, transaction).flatMap {
            accountsRepository.sign(it)
        }

    override fun checkSignature(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signature: Signature
    ): Single<Pair<Solidity.Address, Signature>> =
        calculateHash(safeAddress, transaction).flatMap {
            accountsRepository.recover(it, signature).map { it to signature }
        }

    override fun estimateFees(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean
    ): Single<FeeEstimate> =
        loadExecutableTransaction(safeAddress, transaction, signatures, senderIsOwner)
            .flatMapObservable { txExecutorRepository.estimate(it) }
            .map { FeeEstimate(it.first, it.second) }
            .firstOrError()

    override fun submit(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean
    ): Completable =
        loadExecutableTransaction(safeAddress, transaction, signatures, senderIsOwner)
            .flatMapObservable { submitSignedTransaction(it) }
            .flatMapSingle { addLocalTransaction(safeAddress, transaction, it) }
            .ignoreElements()

    override fun loadExecutableTransaction(
        safeAddress: Solidity.Address,
        innerTransaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean
    ): Single<Transaction> =
        accountsRepository.loadActiveAccount()
            .flatMap { account ->
                if (senderIsOwner)
                    calculateHash(safeAddress, innerTransaction)
                        .flatMap { accountsRepository.sign(it) }
                        .map { signatures.plus(account.address to it) }
                else
                    Single.just(signatures)
            }
            .flatMap { finalSignatures ->
                val sortedAddresses = finalSignatures.keys.map { it.value }.sorted()
                val vList = mutableListOf<Solidity.UInt8>()
                val rList = mutableListOf<Solidity.Bytes32>()
                val sList = mutableListOf<Solidity.Bytes32>()
                sortedAddresses.forEach {
                    finalSignatures[Solidity.Address(it)]?.let {
                        vList.add(Solidity.UInt8(BigInteger.valueOf(it.v.toLong())))
                        rList.add(Solidity.Bytes32(it.r.toBytes(32)))
                        sList.add(Solidity.Bytes32(it.s.toBytes(32)))
                    }
                }

                val confirmations = mutableListOf<Solidity.Address>()
                val confirmationsIndexes = mutableListOf<Solidity.UInt256>()

                Single.fromCallable {
                    val tx = innerTransaction.wrapped
                    val to = tx.address
                    val value = Solidity.UInt256(tx.value?.value ?: BigInteger.ZERO)
                    val data = Solidity.Bytes(tx.data?.hexStringToByteArrayOrNull() ?: ByteArray(0))
                    val operationInt = innerTransaction.operation.toSolidity()
                    val confirmData = GnosisSafe.ExecuteTransaction.encode(
                        to, value, data, operationInt,
                        SolidityBase.Vector(vList), SolidityBase.Vector(rList), SolidityBase.Vector(sList),
                        SolidityBase.Vector(confirmations), SolidityBase.Vector(confirmationsIndexes)
                    )
                    Transaction(safeAddress, data = confirmData)
                }
            }

    private fun submitSignedTransaction(transaction: Transaction): Observable<String> =
        txExecutorRepository.execute(transaction)

    override fun addLocalTransaction(safeAddress: Solidity.Address, transaction: SafeTransaction, txChainHash: String): Single<String> =
        calculateHash(safeAddress, transaction).flatMap {
            Single.fromCallable {
                val tx = transaction.wrapped
                val transactionUuid = UUID.randomUUID().toString()
                descriptionsDao.insert(
                    TransactionDescriptionDb(
                        transactionUuid, safeAddress, tx.address,
                        tx.value?.value ?: BigInteger.ZERO,
                        tx.data ?: "", transaction.operation.toSolidity().value,
                        tx.nonce
                                ?: DEFAULT_NONCE, System.currentTimeMillis(), null, it.toHexString()
                    )
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
                } ?: ethereumRepository.getTransactionReceipt(status.transactionId)
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

    override fun loadChainHash(id: String): Single<String> =
        descriptionsDao.observeStatus(id)
            .firstOrError()
            .map { it.transactionId }

    private class TransactionInfoRequest(
        val threshold: EthRequest<String>,
        val nonce: EthRequest<String>,
        val owners: EthRequest<String>,
        val estimate: EthRequest<BigInteger>,
        val balance: EthRequest<Wei>
    ) : BulkRequest(threshold, nonce, owners, estimate, balance)

    private fun TransactionExecutionRepository.Operation.toSolidity() =
        Solidity.UInt8(
            when (this) {
                TransactionExecutionRepository.Operation.CALL -> OPERATION_CALL
                TransactionExecutionRepository.Operation.DELEGATE_CALL -> OPERATION_DELEGATE_CALL
            }
        )

    companion object {
        private const val ERC191_BYTE = "19"
        private val OPERATION_CALL = BigInteger.ZERO
        private val OPERATION_DELEGATE_CALL = BigInteger.ONE
        private val DEFAULT_NONCE = BigInteger.ZERO
    }
}
