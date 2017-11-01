package pm.gnosis.heimdall.ui.transactiondetails

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetails
import pm.gnosis.heimdall.data.repositories.UnknownTransactionDetails
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.models.Transaction
import pm.gnosis.utils.*
import java.math.BigInteger
import javax.inject.Inject

class TransactionDetailsViewModel @Inject constructor(private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
                                                      private val accountsRepository: AccountsRepository,
                                                      private val multisigRepository: GnosisSafeRepository,
                                                      private val transactionDetailRepository: TransactionDetailRepository,
                                                      private val tokenRepository: TokenRepository) : TransactionDetailsContract() {
    private lateinit var transaction: Transaction
    private lateinit var transactionType: MultisigTransactionType
    private lateinit var transactionHash: String

    private var descriptionHash: String? = null

    override fun setTransaction(transaction: Transaction?, descriptionHash: String?): Completable =
            Completable.fromCallable {
                this.descriptionHash = descriptionHash
                if (transaction == null) throw IllegalStateException("Transaction is null")
                if (!transaction.address.isValidEthereumAddress()) throw IllegalStateException("Invalid wallet address")
                this.transaction = transaction

                val data = transaction.data ?: throw IllegalStateException("Transaction doesn't have any data")
                when {
                    data.isSolidityMethod(GnosisSafe.ConfirmTransaction.METHOD_ID) -> {
                        val argument = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.ConfirmTransaction.METHOD_ID)
                        transactionHash = GnosisSafe.ConfirmTransaction.decodeArguments(argument).transactionhash.bytes.toHexString()
                        transactionType = ConfirmMultisigTransaction()
                    }
                    data.isSolidityMethod(GnosisSafe.RevokeConfirmation.METHOD_ID) -> {
                        val argument = transaction.data!!.removeSolidityMethodPrefix(GnosisSafe.RevokeConfirmation.METHOD_ID)
                        transactionHash = GnosisSafe.RevokeConfirmation.decodeArguments(argument).transactionhash.bytes.toHexString()
                        transactionType = RevokeMultisigTransaction()
                    }
                    else -> throw IllegalStateException("Transaction it's neither a Confirm or Revoke")
                }
            }.subscribeOn(Schedulers.computation())


    override fun getTransactionType() = transactionType

    override fun getTransactionHash() = transactionHash

    override fun getTransaction() = transaction

    override fun observeSafeDetails(): Flowable<Safe> =
            multisigRepository.observeSafe(transaction.address)

    override fun signTransaction(): Observable<Result<String>> =
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
                    .mapToResult()

    override fun addSafe(address: BigInteger, name: String?): Single<Result<BigInteger>> =
            multisigRepository.add(address, name).andThen(Single.just(address)).mapToResult()

    override fun loadTransactionDetails(): Observable<TransactionDetails> {
        val descriptionHash = this.descriptionHash ?: return Observable.just(UnknownTransactionDetails(null))
        return transactionDetailRepository.loadTransactionDetails(descriptionHash, transaction.address, transactionHash)
    }

    override fun loadTokenInfo(address: BigInteger) = tokenRepository.loadTokenInfo(address)
}
