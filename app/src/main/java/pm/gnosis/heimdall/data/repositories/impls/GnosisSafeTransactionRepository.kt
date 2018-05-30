package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.GnosisSafePersonalEdition
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb
import pm.gnosis.heimdall.data.db.models.TransactionPublishStatusDb
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.EstimateParams
import pm.gnosis.heimdall.data.remote.models.ExecuteParams
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.PublishStatus
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
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
    private val relayServiceApi: RelayServiceApi
) : TransactionExecutionRepository {

    private val descriptionsDao = appDb.descriptionsDao()

    override fun calculateHash(
        safeAddress: Solidity.Address, transaction: SafeTransaction,
        txGas: BigInteger, dataGas: BigInteger, gasPrice: BigInteger, gasToken: Solidity.Address
    ): Single<ByteArray> =
        Single.fromCallable {
            val tx = transaction.wrapped
            val to = tx.address.asEthereumAddressString().removeHexPrefix()
            val value = tx.value?.value.paddedHexString()
            val data = tx.data?.removeHexPrefix() ?: ""
            val operationString = transaction.operation.toInt().toBigInteger().paddedHexString(2)
            val gasPriceString = gasPrice.paddedHexString()
            val txGasString = txGas.paddedHexString()
            val dataGasString = dataGas.paddedHexString()
            val gasTokenString = gasToken.asEthereumAddressString().removeHexPrefix()
            val nonce = (tx.nonce ?: DEFAULT_NONCE).paddedHexString()
            hash(safeAddress, to, value, data, operationString, txGasString, dataGasString, gasPriceString, gasTokenString, nonce)
        }.subscribeOn(Schedulers.computation())

    private fun BigInteger?.paddedHexString(padding: Int = 64): String {
        return (this?.toString(16) ?: "").padStart(padding, '0')
    }

    private fun hash(safeAddress: Solidity.Address, vararg parts: String): ByteArray {
        val initial = StringBuilder().append(ERC191_BYTE).append(ERC191_VERSION).append(safeAddress.asEthereumAddressString().removeHexPrefix())
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
                        safeAddress, data = GnosisSafePersonalEdition.GetThreshold.encode()
                    ), id = 0
                ),
                EthCall(
                    transaction = Transaction(
                        safeAddress, data = GnosisSafePersonalEdition.Nonce.encode()
                    ), id = 1
                ),
                EthCall(
                    transaction = Transaction(
                        safeAddress, data = GnosisSafePersonalEdition.GetOwners.encode()
                    ), id = 2
                ),
                EthBalance(
                    address = safeAddress,
                    id = 3
                )
            )

        }.subscribeOn(Schedulers.computation())
            .flatMap { ethereumRepository.request(it).singleOrError() }
            .flatMap { info ->
                relayServiceApi.estimate(
                    EstimateParams(
                        safeAddress.asEthereumAddressString(),
                        transaction.wrapped.address.asEthereumAddressString(),
                        transaction.wrapped.value?.value?.asDecimalString() ?: "0",
                        transaction.wrapped.data ?: "0x",
                        transaction.operation.toInt(),
                        GnosisSafePersonalEdition.GetThreshold.decode(info.threshold.result()!!).param0.value.toInt()
                    )
                ).map { info to it }
            }
            .flatMap { info -> accountsRepository.loadActiveAccount().map { info to it.address } }
            .flatMap { (infoWithEstimate, sender) ->
                val (info, estimate) = infoWithEstimate
                val nonce = GnosisSafePersonalEdition.Nonce.decode(info.nonce.result()!!).param0.value
                val threshold = GnosisSafePersonalEdition.GetThreshold.decode(info.threshold.result()!!).param0.value.toInt()
                val owners = GnosisSafePersonalEdition.GetOwners.decode(info.owners.result()!!).param0.items
                val updatedTransaction = transaction.copy(wrapped = transaction.wrapped.updateTransactionWithStatus(nonce))
                val txGas = estimate.safeTxGas.decimalAsBigInteger()
                val dataGas = estimate.dataGas.decimalAsBigInteger()
                val gasPrice = estimate.gasPrice.decimalAsBigInteger()
                val safeBalance = info.balance.result()!!
                calculateHash(safeAddress, updatedTransaction, txGas, dataGas, gasPrice).map {
                    TransactionExecutionRepository.ExecuteInformation(
                        it.toHexString(),
                        updatedTransaction,
                        sender,
                        threshold,
                        owners,
                        gasPrice,
                        txGas,
                        dataGas,
                        safeBalance
                    )
                }
            }

    private fun Transaction.updateTransactionWithStatus(safeNonce: BigInteger) = nonce?.let { this } ?: copy(nonce = safeNonce)

    override fun sign(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Single<Signature> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice).flatMap {
            accountsRepository.sign(it)
        }

    override fun checkSignature(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signature: Signature,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Single<Pair<Solidity.Address, Signature>> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice).flatMap {
            accountsRepository.recover(it, signature).map { it to signature }
        }

    override fun submit(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Completable =
        loadExecutionParams(safeAddress, transaction, signatures, senderIsOwner, txGas, dataGas, gasPrice)
            .flatMap(relayServiceApi::execute)
            .flatMap { addLocalTransaction(safeAddress, transaction, it.transactionHash, txGas, dataGas, gasPrice) }
            .toCompletable()

    private fun loadExecutionParams(
        safeAddress: Solidity.Address,
        innerTransaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Single<ExecuteParams> =
        accountsRepository.loadActiveAccount()
            .flatMap { account ->
                if (senderIsOwner)
                    calculateHash(safeAddress, innerTransaction, txGas, dataGas, gasPrice)
                        .flatMap { accountsRepository.sign(it) }
                        .map { signatures.plus(account.address to it) }
                else
                    Single.just(signatures)
            }
            .map { finalSignatures ->
                val sortedAddresses = finalSignatures.keys.map { it.value }.sorted()
                val serviceSignatures = mutableListOf<ServiceSignature>()
                sortedAddresses.forEach {
                    finalSignatures[Solidity.Address(it)]?.let {
                        serviceSignatures += ServiceSignature(
                            it.v.toInt(),
                            it.r.asDecimalString(),
                            it.s.asDecimalString()
                        )
                    }
                }

                val tx = innerTransaction.wrapped
                ExecuteParams(
                    safeAddress.asEthereumAddressChecksumString(),
                    tx.address.asEthereumAddressChecksumString(),
                    tx.value?.value?.asDecimalString() ?: "0",
                    tx.data ?: "0x",
                    innerTransaction.operation.toInt(),
                    serviceSignatures,
                    txGas.asDecimalString(),
                    dataGas.asDecimalString(),
                    gasPrice.asDecimalString()
                )
            }

    private fun addLocalTransaction(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txChainHash: String,
        gasPrice: BigInteger,
        txGas: BigInteger,
        dataGas: BigInteger
    ): Single<String> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice)
            .map {
                val tx = transaction.wrapped
                val transactionUuid = UUID.randomUUID().toString()
                descriptionsDao.insert(
                    TransactionDescriptionDb(
                        transactionUuid, safeAddress, tx.address,
                        tx.value?.value ?: BigInteger.ZERO,
                        tx.data ?: "", transaction.operation.toInt().toBigInteger(),
                        tx.nonce
                                ?: DEFAULT_NONCE, System.currentTimeMillis(), null, it.toHexString()
                    )
                )
                descriptionsDao.insert(
                    TransactionPublishStatusDb(transactionUuid, txChainHash, null)
                )
                transactionUuid
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

    private class TransactionInfoRequest(
        val threshold: EthRequest<String>,
        val nonce: EthRequest<String>,
        val owners: EthRequest<String>,
        val balance: EthRequest<Wei>
    ) : BulkRequest(threshold, nonce, owners, balance)

    private fun TransactionExecutionRepository.Operation.toInt() =
        when (this) {
            TransactionExecutionRepository.Operation.CALL -> OPERATION_CALL
            TransactionExecutionRepository.Operation.DELEGATE_CALL -> OPERATION_DELEGATE_CALL
        }

    companion object {
        private const val ERC191_BYTE = "19"
        private const val ERC191_VERSION = "00"
        private const val OPERATION_CALL = 0
        private const val OPERATION_DELEGATE_CALL = 1
        private val DEFAULT_NONCE = BigInteger.ZERO
    }
}
