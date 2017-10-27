package pm.gnosis.heimdall.ui.transactiondetails

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailRepository
import pm.gnosis.heimdall.data.repositories.impls.GnosisMultisigTransaction
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import pm.gnosis.models.Transaction
import pm.gnosis.utils.*
import java.math.BigInteger
import javax.inject.Inject

class TransactionDetailsViewModel @Inject constructor(private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
                                                      private val accountsRepository: AccountsRepository,
                                                      private val multisigRepository: MultisigRepository,
                                                      private val gnosisMultisigWrapper: TransactionDetailRepository,
                                                      private val tokenRepository: TokenRepository) : TransactionDetailsContract() {
    private lateinit var transaction: Transaction
    private lateinit var transactionType: MultisigTransactionType
    private lateinit var transactionId: BigInteger

    override fun setTransaction(transaction: Transaction?): Completable =
            Completable.fromCallable {
                if (transaction == null) throw IllegalStateException("Transaction is null")
                if (!transaction.address.isValidEthereumAddress()) throw IllegalStateException("Invalid wallet address")
                this.transaction = transaction

                val data = transaction.data ?: throw IllegalStateException("Transaction doesn't have any data")
                when {
                    data.isSolidityMethod(MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID) -> {
                        val argument = transaction.data!!.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID)
                        transactionId = MultiSigWalletWithDailyLimit.ConfirmTransaction.decodeArguments(argument).transactionid.value
                        transactionType = ConfirmMultisigTransaction()
                    }
                    data.isSolidityMethod(MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID) -> {
                        val argument = transaction.data!!.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID)
                        transactionId = MultiSigWalletWithDailyLimit.RevokeConfirmation.decodeArguments(argument).transactionid.value
                        transactionType = RevokeMultisigTransaction()
                    }
                    else -> throw IllegalStateException("Transaction it's neither a Confirm or Revoke")
                }
            }.subscribeOn(Schedulers.computation())


    override fun getMultisigTransactionType() = transactionType

    override fun getMultisigTransactionId() = transactionId

    override fun getTransaction() = transaction

    override fun observeMultisigWalletDetails(): Flowable<MultisigWallet> =
            multisigRepository.observeMultisigWallet(transaction.address)

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

    override fun addMultisigWallet(address: BigInteger, name: String?): Single<Result<BigInteger>> =
            multisigRepository.addMultisigWallet(address, name).andThen(Single.just(address)).mapToResult()

    override fun loadTransactionDetails(): Observable<GnosisMultisigTransaction> =
            gnosisMultisigWrapper.loadTransactionDetails(transaction.address, transactionId)

    override fun loadTokenInfo(address: BigInteger) = tokenRepository.loadTokenInfo(address)
}
