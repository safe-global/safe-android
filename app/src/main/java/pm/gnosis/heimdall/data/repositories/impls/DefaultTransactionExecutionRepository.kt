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
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.PublishStatus
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.toInt
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
class DefaultTransactionExecutionRepository @Inject constructor(
    appDb: ApplicationDb,
    private val accountsRepository: AccountsRepository,
    private val ethereumRepository: EthereumRepository,
    private val pushServiceRepository: PushServiceRepository,
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

    private fun loadSafeState(safeAddress: Solidity.Address) =
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

    override fun loadSafeExecuteState(safeAddress: Solidity.Address): Single<TransactionExecutionRepository.SafeExecuteState> =
        loadSafeState(safeAddress)
            .flatMap { info -> accountsRepository.loadActiveAccount().map { info to it.address } }
            .map { (info, sender) ->
                val nonce = GnosisSafePersonalEdition.Nonce.decode(info.nonce.result()!!).param0.value
                val threshold = GnosisSafePersonalEdition.GetThreshold.decode(info.threshold.result()!!).param0.value.toInt()
                val owners = GnosisSafePersonalEdition.GetOwners.decode(info.owners.result()!!).param0.items
                val safeBalance = info.balance.result()!!
                TransactionExecutionRepository.SafeExecuteState(
                    sender,
                    threshold,
                    owners,
                    nonce,
                    safeBalance
                )
            }

    override fun loadExecuteInformation(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction
    ): Single<TransactionExecutionRepository.ExecuteInformation> =
        loadSafeState(safeAddress)
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
                        it.toHexString().addHexPrefix(),
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

    override fun signConfirmation(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Single<Signature> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice)
            .flatMap(accountsRepository::sign)

    override fun signRejection(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Single<Signature> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice)
            .flatMap(pushServiceRepository::calculateRejectionHash)
            .flatMap(accountsRepository::sign)

    override fun checkConfirmation(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        signature: Signature
    ): Single<Pair<Solidity.Address, Signature>> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice)
            .flatMap { accountsRepository.recover(it, signature) }
            .map { it to signature }

    override fun checkRejection(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        signature: Signature
    ): Single<Pair<Solidity.Address, Signature>> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice)
            .flatMap(pushServiceRepository::calculateRejectionHash)
            .flatMap {
                accountsRepository.recover(it, signature)
            }.map { it to signature }

    override fun notifyReject(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        targets: Set<Solidity.Address>
    ): Completable =
        signRejection(safeAddress, transaction, txGas, dataGas, gasPrice)
            .flatMap { signature -> calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice).map { it.toHexString() to signature } }
            .flatMapCompletable { (hash, signature) -> pushServiceRepository.propagateTransactionRejected(hash, signature, targets) }

    override fun submit(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ): Single<String> =
        loadExecutionParams(safeAddress, transaction, signatures, senderIsOwner, txGas, dataGas, gasPrice)
            .flatMap(relayServiceApi::execute)
            .flatMap { handleSubmittedTransaction(safeAddress, transaction, it.transactionHash, txGas, dataGas, gasPrice) }

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
                        serviceSignatures += ServiceSignature(it.r, it.s, it.v.toInt())
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

    private fun handleSubmittedTransaction(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txChainHash: String,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger
    ) =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice)
            .map {
                val tx = transaction.wrapped
                val transactionUuid = UUID.randomUUID().toString()
                val transactionObject = TransactionDescriptionDb(
                    transactionUuid,
                    safeAddress,
                    tx.address,
                    tx.value?.value ?: BigInteger.ZERO,
                    tx.data ?: "",
                    transaction.operation.toInt().toBigInteger(),
                    txGas,
                    dataGas,
                    ERC20Token.ETHER_TOKEN.address,
                    gasPrice,
                    tx.nonce
                            ?: DEFAULT_NONCE, System.currentTimeMillis(),
                    it.toHexString()
                )
                descriptionsDao.insert(transactionObject, TransactionPublishStatusDb(transactionUuid, txChainHash, null, null))
                txChainHash
            }

    override fun observePublishStatus(id: String): Observable<PublishStatus> =
        descriptionsDao.observeStatus(id)
            .toObservable()
            .switchMap { status ->
                status.success?.let {
                    Observable.just(it to (status.timestamp ?: 0))
                } ?: ethereumRepository.getTransactionReceipt(status.transactionId)
                    .flatMap { receipt ->
                        ethereumRepository.getBlockByHash(receipt.blockHash)
                            .map { receipt to (it.timestamp.toLong() * 1000) }
                    }
                    .retryWhen {
                        it.delay(20, TimeUnit.SECONDS)
                    }
                    .map { (receipt, time) ->
                        val executed = if (receipt.status == BigInteger.ZERO) false
                        else {
                            // If we have a failure event than the transaction failed
                            receipt.logs.none {
                                it.topics.getOrNull(0) == GnosisSafePersonalEdition.Events.ExecutionFailed.EVENT_ID
                            }
                        }
                        descriptionsDao.update(status.apply {
                            success = executed
                            timestamp = time
                        })
                        executed to time
                    }
            }
            .map { (success, timestamp) -> if (success) PublishStatus.Success(timestamp) else PublishStatus.Failed(timestamp) }
            .startWith(PublishStatus.Pending)
            .onErrorReturnItem(PublishStatus.Unknown)

    private class TransactionInfoRequest(
        val threshold: EthRequest<String>,
        val nonce: EthRequest<String>,
        val owners: EthRequest<String>,
        val balance: EthRequest<Wei>
    ) : BulkRequest(threshold, nonce, owners, balance)

    companion object {
        private const val ERC191_BYTE = "19"
        private const val ERC191_VERSION = "00"
        private val DEFAULT_NONCE = BigInteger.ZERO
    }
}
