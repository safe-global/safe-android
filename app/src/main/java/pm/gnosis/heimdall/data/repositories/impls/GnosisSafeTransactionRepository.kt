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
import pm.gnosis.heimdall.data.remote.BulkRequest
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.*
import java.math.BigInteger
import java.util.*
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

    override fun loadInformation(safeAddress: BigInteger, transaction: Transaction): Single<TransactionRepository.TransactionInfo> =
            accountsRepository.loadActiveAccount()
                    .flatMap { account ->
                        calculateHash(safeAddress, transaction).map { account to it }
                    }
                    .flatMap { (account, txHash) ->
                        val request = TransactionInfoRequest(
                                BulkRequest.SubRequest(TransactionCallParams(
                                        to = safeAddress.asEthereumAddressString(),
                                        data = GnosisSafe.IsOwner.encode(Solidity.Address(account.address))).callRequest(0),
                                        { GnosisSafe.IsOwner.decode(it.checkedResult()).param0.value }
                                ),
                                BulkRequest.SubRequest(TransactionCallParams(
                                        to = safeAddress.asEthereumAddressString(),
                                        data = GnosisSafe.Required.encode()).callRequest(2),
                                        { GnosisSafe.Required.decode(it.checkedResult()).param0.value.toInt() }
                                ),
                                BulkRequest.SubRequest(TransactionCallParams(
                                        to = safeAddress.asEthereumAddressString(),
                                        data = GnosisSafe.GetConfirmationCount.encode(Solidity.Bytes32(txHash))).callRequest(3),
                                        { GnosisSafe.GetConfirmationCount.decode(it.checkedResult()).confirmationcount.value.toInt() }
                                ),
                                BulkRequest.SubRequest(TransactionCallParams(
                                        to = safeAddress.asEthereumAddressString(),
                                        data = GnosisSafe.IsExecuted.encode(Solidity.Bytes32(txHash))).callRequest(4),
                                        { GnosisSafe.IsExecuted.decode(it.checkedResult()).param0.value }
                                ),
                                BulkRequest.SubRequest(TransactionCallParams(
                                        to = safeAddress.asEthereumAddressString(),
                                        data = GnosisSafe.IsConfirmed.encode(Solidity.Bytes32(txHash), Solidity.Address(account.address))).callRequest(5),
                                        { GnosisSafe.IsExecuted.decode(it.checkedResult()).param0.value }
                                )
                        )
                        ethereumJsonRpcRepository.bulk(request)
                                .map {
                                    TransactionRepository.TransactionInfo(
                                            it.isOwner.value!!,
                                            it.requiredConfirmation.value!!,
                                            it.confirmation.value!!,
                                            it.isExecuted.value!!,
                                            it.isConfirmed.value!!
                                    )
                                }
                                .singleOrError()
                    }

    override fun estimateFees(safeAddress: BigInteger, transaction: Transaction, type: TransactionRepository.SubmitType): Single<Wei> =
            when (type) {
                TransactionRepository.SubmitType.CONFIRM -> buildConfirmTransaction(safeAddress, transaction)
                TransactionRepository.SubmitType.CONFIRM_AND_EXECUTE -> buildConfirmAndExecuteTransaction(safeAddress, transaction)
                TransactionRepository.SubmitType.EXECUTE -> buildExecuteTransaction(safeAddress, transaction)
            }
                    .flatMap { confirmAndExecuteTransaction ->
                        accountsRepository.loadActiveAccount().map { it to confirmAndExecuteTransaction }
                    }
                    .flatMap { (account, confirmAndExecuteTransaction) ->
                        ethereumJsonRpcRepository.getTransactionParameters(account.address,
                                TransactionCallParams(
                                        to = confirmAndExecuteTransaction.address.asEthereumAddressString(),
                                        data = confirmAndExecuteTransaction.data))
                                .map { Wei(it.gas * it.gasPrice) }
                                .singleOrError()
                    }

    override fun submit(safeAddress: BigInteger, transaction: Transaction, type: TransactionRepository.SubmitType): Completable =
            when (type) {
                TransactionRepository.SubmitType.CONFIRM -> buildConfirmTransaction(safeAddress, transaction)
                TransactionRepository.SubmitType.CONFIRM_AND_EXECUTE -> buildConfirmAndExecuteTransaction(safeAddress, transaction)
                TransactionRepository.SubmitType.EXECUTE -> buildExecuteTransaction(safeAddress, transaction)
            }
                    .flatMapObservable { submitSignedTransaction(it) }
                    .flatMapSingle { addLocalTransaction(safeAddress, transaction, it) }
                    .ignoreElements()

    private fun buildConfirmTransaction(safeAddress: BigInteger, transaction: Transaction): Single<Transaction> =
            calculateHash(safeAddress, transaction)
                    .map {
                        Transaction(
                                safeAddress,
                                data = GnosisSafe.ConfirmTransaction.encode(Solidity.Bytes32(it))
                        )
                    }

    private fun buildExecuteTransaction(safeAddress: BigInteger, innerTransaction: Transaction): Single<Transaction> =
            Single.fromCallable {
                val to = Solidity.Address(innerTransaction.address)
                val value = Solidity.UInt256(innerTransaction.value?.value ?: BigInteger.ZERO)
                val data = Solidity.Bytes(innerTransaction.data?.hexStringToByteArrayOrNull() ?: ByteArray(0))
                val operation = Solidity.UInt8(DEFAULT_OPERATION)
                val nonce = Solidity.UInt256(innerTransaction.nonce ?: DEFAULT_NONCE)
                val confirmData = GnosisSafe.ExecuteTransaction.encode(to, value, data, operation, nonce)
                Transaction(safeAddress, data = confirmData)
            }

    private fun buildConfirmAndExecuteTransaction(safeAddress: BigInteger, innerTransaction: Transaction): Single<Transaction> =
            Single.fromCallable {
                val to = Solidity.Address(innerTransaction.address)
                val value = Solidity.UInt256(innerTransaction.value?.value ?: BigInteger.ZERO)
                val data = Solidity.Bytes(innerTransaction.data?.hexStringToByteArrayOrNull() ?: ByteArray(0))
                val operation = Solidity.UInt8(DEFAULT_OPERATION)
                val nonce = Solidity.UInt256(innerTransaction.nonce ?: DEFAULT_NONCE)
                val confirmData = GnosisSafe.ConfirmAndExecuteTransaction.encode(to, value, data, operation, nonce)
                Transaction(safeAddress, data = confirmData)
            }.subscribeOn(Schedulers.computation())

    private fun submitSignedTransaction(transaction: Transaction): Observable<String> =
            accountsRepository.loadActiveAccount()
                    .flatMapObservable {
                        ethereumJsonRpcRepository.getTransactionParameters(it.address,
                                TransactionCallParams(
                                        to = transaction.address.asEthereumAddressString(),
                                        data = transaction.data))
                    }
                    .flatMapSingle {
                        accountsRepository.signTransaction(transaction.copy(nonce = it.nonce, gas = it.gas, gasPrice = it.gasPrice))
                    }
                    .flatMap { ethereumJsonRpcRepository.sendRawTransaction(it) }

    private fun addLocalTransaction(safeAddress: BigInteger, transaction: Transaction, txChainHash: String): Single<String> =
            calculateHash(safeAddress, transaction).flatMap {
                Single.fromCallable {
                    val transactionUuid = UUID.randomUUID().toString()
                    descriptionsDao.insertDescription(
                            TransactionDescriptionDb(transactionUuid, safeAddress, transaction.address,
                                    transaction.value?.value ?: BigInteger.ZERO,
                                    transaction.data ?: "", DEFAULT_OPERATION,
                                    transaction.nonce ?: DEFAULT_NONCE, System.currentTimeMillis(), null, it.toHexString())

                    )
                    transactionUuid
                }
            }

    private class TransactionInfoRequest(
            val isOwner: SubRequest<Boolean>,
            val requiredConfirmation: SubRequest<Int>,
            val confirmation: SubRequest<Int>,
            val isExecuted: SubRequest<Boolean>,
            val isConfirmed: SubRequest<Boolean>
    ) : BulkRequest(isOwner, requiredConfirmation, confirmation, isExecuted, isConfirmed)

    companion object {
        private const val ERC191_BYTE = "19"
        private val DEFAULT_OPERATION = BigInteger.ZERO // Call
        private val DEFAULT_NONCE = BigInteger.ZERO
    }

}