package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.GnosisSafeV1
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.TransactionDescriptionDb
import pm.gnosis.heimdall.data.db.models.TransactionPublishStatusDb
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.EstimateParams
import pm.gnosis.heimdall.data.remote.models.ExecuteParams
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.PublishStatus
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.TransactionEventsCallback
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.models.SemVer
import pm.gnosis.heimdall.data.repositories.models.toSemVer
import pm.gnosis.heimdall.data.repositories.toInt
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.*
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTransactionExecutionRepository @Inject constructor(
    appDb: ApplicationDb,
    private val accountRepository: AccountsRepository,
    private val cryptoHelper: CryptoHelper,
    private val ethereumRepository: EthereumRepository,
    private val pushServiceRepository: PushServiceRepository,
    private val relayServiceApi: RelayServiceApi
) : TransactionExecutionRepository {

    private val descriptionsDao = appDb.descriptionsDao()

    private val transactionSubmittedCallbacks = CopyOnWriteArraySet<TransactionEventsCallback>()
    private val nonceCache = ConcurrentHashMap<Solidity.Address, BigInteger>()

    override fun addTransactionEventsCallback(callback: TransactionEventsCallback): Boolean =
        transactionSubmittedCallbacks.add(callback)

    override fun removeTransactionEventsCallback(callback: TransactionEventsCallback): Boolean =
        transactionSubmittedCallbacks.remove(callback)

    override fun calculateHash(
        safeAddress: Solidity.Address, transaction: SafeTransaction,
        txGas: BigInteger, dataGas: BigInteger, gasPrice: BigInteger, gasToken: Solidity.Address, version: SemVer
    ): Single<ByteArray> =
        Single.fromCallable {
            val tx = transaction.wrapped
            val to = tx.address.value.paddedHexString()
            val value = tx.value?.value.paddedHexString()
            val data = Sha3Utils.keccak(tx.data?.hexToByteArray() ?: ByteArray(0)).toHex().padStart(64, '0')
            val operationString = transaction.operation.toInt().toBigInteger().paddedHexString()
            val gasPriceString = gasPrice.paddedHexString()
            val txGasString = txGas.paddedHexString()
            val dataGasString = dataGas.paddedHexString()
            val gasTokenString = gasToken.value.paddedHexString()
            val refundReceiverString = BigInteger.ZERO.paddedHexString()
            val nonce = (tx.nonce ?: DEFAULT_NONCE).paddedHexString()
            hash(
                safeAddress,
                version,
                to,
                value,
                data,
                operationString,
                txGasString,
                dataGasString,
                gasPriceString,
                gasTokenString,
                refundReceiverString,
                nonce
            )
        }.subscribeOn(Schedulers.computation())

    private fun BigInteger?.paddedHexString(padding: Int = 64): String {
        return (this?.toString(16) ?: "").padStart(padding, '0')
    }

    private fun domainHash(safeAddress: Solidity.Address) =
        Sha3Utils.keccak(
            ("0x035aff83d86937d35b32e04f0ddc6ff469290eef2f1b692d8a815c89404d4749" +
                    safeAddress.value.paddedHexString()).hexToByteArray()
        ).toHex()

    private fun valuesHash(version: SemVer, parts: Array<out String>) =
        parts.fold(StringBuilder().append(getTypeHash(version))) { acc, part ->
            acc.append(part)
        }.toString().run {
            Sha3Utils.keccak(hexToByteArray()).toHex()
        }

    private fun getTypeHash(version: SemVer) =
        if (version >= SemVer(1, 0, 0))
            "0xbb8310d486368db6bd6f849402fdd73ad53d316b5a4b2644ad6efe0f941286d8"
        else
            "0x14d461bc7412367e924637b363c7bf29b8f47e2f84869f4426e5633d8af47b20"

    private fun hash(safeAddress: Solidity.Address, version: SemVer, vararg parts: String): ByteArray {
        val initial = StringBuilder().append(ERC191_BYTE).append(ERC191_VERSION).append(domainHash(safeAddress)).append(valuesHash(version, parts))
        return Sha3Utils.keccak(initial.toString().hexToByteArray())
    }

    private fun loadSafeState(safeAddress: Solidity.Address, paymentToken: Solidity.Address) =
        Single.fromCallable {
            TransactionInfoRequest(
                EthCall(
                    transaction = Transaction(
                        safeAddress, data = GnosisSafe.GetThreshold.encode()
                    ), id = 0
                ).toMappedRequest(),
                EthCall(
                    transaction = Transaction(
                        safeAddress, data = GnosisSafe.Nonce.encode()
                    ), id = 1
                ).toMappedRequest(),
                EthCall(
                    transaction = Transaction(
                        safeAddress, data = GnosisSafe.GetOwners.encode()
                    ), id = 2
                ).toMappedRequest(),
                balanceRequest(safeAddress, paymentToken, 3),
                EthCall(
                    transaction = Transaction(
                        safeAddress, data = GnosisSafe.VERSION.encode()
                    ), id = 4
                ).toMappedRequest()
            )

        }.subscribeOn(Schedulers.computation())
            .flatMap { ethereumRepository.request(it).singleOrError() }

    private fun balanceRequest(safe: Solidity.Address, token: Solidity.Address, index: Int) =
        if (token == ERC20Token.ETHER_TOKEN.address) {
            MappedRequest(EthBalance(safe, id = index)) {
                it?.value
            }
        } else {
            MappedRequest(
                EthCall(
                    transaction = Transaction(
                        token,
                        data = ERC20Contract.BalanceOf.encode(safe)
                    ),
                    id = index
                )
            ) { ERC20Contract.BalanceOf.decode(it!!).balance.value }
        }

    private fun checkNonce(safeAddress: Solidity.Address, contractNonce: BigInteger): BigInteger {
        val cachedNonce = nonceCache[safeAddress]
        return if (cachedNonce != null && cachedNonce >= contractNonce) cachedNonce + BigInteger.ONE else contractNonce
    }

    override fun loadSafeExecuteState(
        safeAddress: Solidity.Address,
        paymentToken: Solidity.Address
    ): Single<TransactionExecutionRepository.SafeExecuteState> =
        loadSafeState(safeAddress, paymentToken)
            .flatMap { info ->
                accountRepository.signingOwner(safeAddress)
                    .map { info to it.address }
                    .onErrorReturnItem(info to Solidity.Address(BigInteger.ZERO)) // We don't have a owner for this Safe, fallback to 0x0
            }
            .map { (info, sender) ->
                val nonce = checkNonce(safeAddress, GnosisSafe.Nonce.decode(info.nonce.mapped()!!).param0.value)
                val threshold = GnosisSafe.GetThreshold.decode(info.threshold.mapped()!!).param0.value.toInt()
                val owners = GnosisSafe.GetOwners.decode(info.owners.mapped()!!).param0.items
                val safeBalance = info.balance.mapped()!!
                val version = GnosisSafe.VERSION.decode(info.version.mapped()!!).param0.value.toSemVer()
                TransactionExecutionRepository.SafeExecuteState(
                    sender,
                    threshold,
                    owners,
                    nonce,
                    safeBalance,
                    version
                )
            }

    override fun loadExecuteInformation(
        safeAddress: Solidity.Address,
        paymentToken: Solidity.Address,
        transaction: SafeTransaction,
        safeOwner: AccountsRepository.SafeOwner?
    ): Single<TransactionExecutionRepository.ExecuteInformation> =
        loadSafeState(safeAddress, paymentToken)
            .flatMap { info ->
                relayServiceApi.estimate(
                    safeAddress.asEthereumAddressChecksumString(),
                    EstimateParams(
                        transaction.wrapped.address.asEthereumAddressChecksumString(),
                        transaction.wrapped.value?.value?.asDecimalString() ?: "0",
                        transaction.wrapped.data ?: "0x",
                        transaction.operation.toInt(),
                        GnosisSafe.GetThreshold.decode(info.threshold.mapped()!!).param0.value.toInt(),
                        paymentToken
                    )
                ).map { info to it }
            }
            .flatMap { info ->
                safeOwner?.let { Single.just(info to it.address) } ?: accountRepository.signingOwner(safeAddress).map { info to it.address }
            }
            .flatMap { (infoWithEstimate, sender) ->
                val (info, estimate) = infoWithEstimate
                assert(paymentToken == estimate.gasToken)
                // We have 3 nonce sources: RPC endpoint, Estimate endpoint, local nonce cache ... we take the maximum of all
                val rpcNonce = GnosisSafe.Nonce.decode(info.nonce.mapped()!!).param0.value
                val estimateNonce = estimate.lastUsedNonce?.decimalAsBigInteger()?.let { it + BigInteger.ONE } ?: BigInteger.ZERO
                val nonce = checkNonce(safeAddress, if (rpcNonce > estimateNonce) rpcNonce else estimateNonce)

                val threshold = GnosisSafe.GetThreshold.decode(info.threshold.mapped()!!).param0.value.toInt()
                val owners = GnosisSafe.GetOwners.decode(info.owners.mapped()!!).param0.items
                val updatedTransaction = transaction.copy(wrapped = transaction.wrapped.updateTransactionWithStatus(nonce))
                val txGas = estimate.safeTxGas.decimalAsBigInteger()
                val dataGas = estimate.dataGas.decimalAsBigInteger()
                val operationalGas = estimate.operationalGas.decimalAsBigInteger()
                val gasPrice = estimate.gasPrice.decimalAsBigInteger()
                val safeBalance = info.balance.mapped()!!
                val safeVersion = GnosisSafe.VERSION.decode(info.version.mapped()!!).param0.value.toSemVer()
                calculateHash(safeAddress, updatedTransaction, txGas, dataGas, gasPrice, paymentToken, safeVersion).map {
                    TransactionExecutionRepository.ExecuteInformation(
                        it.toHexString().addHexPrefix(),
                        updatedTransaction,
                        sender,
                        threshold,
                        owners,
                        safeVersion,
                        paymentToken,
                        gasPrice,
                        txGas,
                        dataGas,
                        operationalGas,
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
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        version: SemVer
    ): Single<Signature> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice, gasToken, version)
            .flatMap {
                accountRepository.sign(safeAddress, it)
            }

    override fun signRejection(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        version: SemVer
    ): Single<Signature> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice, gasToken, version)
            .flatMap(pushServiceRepository::calculateRejectionHash)
            .flatMap {
                accountRepository.sign(safeAddress, it)
            }

    override fun checkConfirmation(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        signature: Signature,
        version: SemVer
    ): Single<Pair<Solidity.Address, Signature>> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice, gasToken, version)
            .map { cryptoHelper.recover(it, signature) to signature }

    override fun checkRejection(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        signature: Signature,
        version: SemVer
    ): Single<Pair<Solidity.Address, Signature>> =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice, gasToken, version)
            .flatMap(pushServiceRepository::calculateRejectionHash)
            .map { cryptoHelper.recover(it, signature) to signature }

    override fun notifyReject(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        targets: Set<Solidity.Address>,
        version: SemVer
    ): Completable =
        signRejection(safeAddress, transaction, txGas, dataGas, gasPrice, gasToken, version)
            .flatMap { signature ->
                calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice, gasToken, version).map { it.toHexString() to signature }
            }
            .flatMapCompletable { (hash, signature) -> pushServiceRepository.propagateTransactionRejected(hash, signature, safeAddress, targets) }

    override fun submit(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        version: SemVer,
        addToHistory: Boolean,
        referenceId: Long?
    ): Single<String> =
        loadExecutionParams(safeAddress, transaction, signatures, senderIsOwner, txGas, dataGas, gasPrice, gasToken, version)
            .flatMap { relayServiceApi.execute(safeAddress.asEthereumAddressChecksumString(), it) }
            .flatMap {
                transaction.wrapped.nonce?.let { nonceCache[safeAddress] = it }
                broadcastTransactionSubmitted(safeAddress, transaction, it.transactionHash, referenceId)
                if (addToHistory)
                    handleSubmittedTransaction(
                        safeAddress, transaction, it.transactionHash.addHexPrefix(), txGas, dataGas, gasPrice, gasToken, version
                    )
                else
                    Single.just(it.transactionHash.addHexPrefix())
            }

    private fun broadcastTransactionSubmitted(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        chainHash: String,
        referenceId: Long?
    ) {
        transactionSubmittedCallbacks.forEach { it.onTransactionSubmitted(safeAddress, transaction, chainHash, referenceId) }
    }

    override fun reject(referenceId: Long) {
        transactionSubmittedCallbacks.forEach { it.onTransactionRejected(referenceId) }
    }

    private fun loadExecutionParams(
        safeAddress: Solidity.Address,
        innerTransaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        version: SemVer
    ): Single<ExecuteParams> =
        loadSignatures(safeAddress, innerTransaction, signatures, senderIsOwner, txGas, dataGas, gasPrice, gasToken, version)
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
                    tx.address.asEthereumAddressChecksumString(),
                    tx.value?.value?.asDecimalString() ?: "0",
                    tx.data,
                    innerTransaction.operation.toInt(),
                    serviceSignatures,
                    txGas.asDecimalString(),
                    dataGas.asDecimalString(),
                    gasPrice.asDecimalString(),
                    gasToken.asEthereumAddressChecksumString(),
                    tx.nonce?.toLong() ?: 0
                )
            }

    private fun loadSignatures(
        safeAddress: Solidity.Address,
        innerTransaction: SafeTransaction,
        signatures: Map<Solidity.Address, Signature>,
        senderIsOwner: Boolean,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        version: SemVer
    ): Single<Map<Solidity.Address, Signature>> =
        // If owner is signature we need to sign the hash and add the signature to the map
        if (senderIsOwner)
            accountRepository.signingOwner(safeAddress)
                .flatMap { signingOwner ->
                    calculateHash(safeAddress, innerTransaction, txGas, dataGas, gasPrice, gasToken, version)
                        .flatMap { accountRepository.sign(safeAddress, it) }
                        .map { signatures.plus(signingOwner.address to it) }
                }
        else Single.just(signatures)

    private fun handleSubmittedTransaction(
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txChainHash: String,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        version: SemVer
    ) =
        calculateHash(safeAddress, transaction, txGas, dataGas, gasPrice, gasToken, version)
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
                    gasToken,
                    gasPrice,
                    tx.nonce ?: DEFAULT_NONCE,
                    System.currentTimeMillis(),
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
                } ?: observeTransactionStatus(status.transactionId.hexAsBigInteger())
                    .map {
                        it.apply {
                            descriptionsDao.update(status.apply {
                                success = first
                                timestamp = second
                            })
                        }
                    }
            }
            .map { (success, timestamp) ->
                if (success) PublishStatus.Success(timestamp) else PublishStatus.Failed(timestamp)
            }
            .startWith(PublishStatus.Pending)
            .onErrorReturnItem(PublishStatus.Unknown)

    override fun observeTransactionStatus(transactionHash: BigInteger): Observable<Pair<Boolean, Long>> =
        ethereumRepository.getTransactionReceipt(transactionHash.asTransactionHash())
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
                    // If we have a failure event then the transaction failed
                    receipt.logs.none {
                        it.topics.getOrNull(0)?.removeHexPrefix() == GnosisSafe.Events.ExecutionFailure.EVENT_ID ||
                                it.topics.getOrNull(0)?.removeHexPrefix() == GnosisSafeV1.Events.ExecutionFailed.EVENT_ID
                    }
                }
                executed to time
            }

    private class TransactionInfoRequest(
        val threshold: MappedRequest<String, String?>,
        val nonce: MappedRequest<String, String?>,
        val owners: MappedRequest<String, String?>,
        val balance: MappedRequest<out Any, BigInteger?>,
        val version: MappedRequest<String, String?>
    ) : MappingBulkRequest<Any?>(threshold, nonce, owners, balance, version)

    private fun EthRequest<String>.toMappedRequest() = MappedRequest(this) { it }

    companion object {
        private const val ERC191_BYTE = "19"
        private const val ERC191_VERSION = "01"
        private val DEFAULT_NONCE = BigInteger.ZERO
    }
}
