package pm.gnosis.heimdall.ui.transactiondetails

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.accounts.base.models.Transaction
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.contracts.GnosisMultisigTransaction
import pm.gnosis.heimdall.data.contracts.GnosisMultisigWrapper
import pm.gnosis.heimdall.data.models.TransactionDetails
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import pm.gnosis.utils.*
import java.math.BigInteger
import javax.inject.Inject

class TransactionDetailsViewModel @Inject constructor(private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
                                                      private val accountsRepository: AccountsRepository,
                                                      private val multisigRepository: MultisigRepository,
                                                      private val gnosisMultisigWrapper: GnosisMultisigWrapper,
                                                      private val tokenRepository: TokenRepository) : TransactionDetailsContract() {
    private lateinit var transactionDetails: TransactionDetails
    private lateinit var transactionType: MultisigTransactionType
    private lateinit var transactionId: BigInteger

    override fun setTransaction(transactionDetails: TransactionDetails?): Completable =
            Completable.fromCallable {
                if (transactionDetails == null) throw IllegalStateException("Transaction is null")
                if (!transactionDetails.address.isValidEthereumAddress()) throw IllegalStateException("Invalid wallet address")
                this.transactionDetails = transactionDetails

                val data = transactionDetails.data ?: throw IllegalStateException("Transaction doesn't have any data")
                when {
                    data.isSolidityMethod(MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID) -> {
                        val argument = transactionDetails.data!!.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID)
                        transactionId = MultiSigWalletWithDailyLimit.ConfirmTransaction.decodeArguments(argument).transactionid.value
                        transactionType = ConfirmMultisigTransaction()
                    }
                    data.isSolidityMethod(MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID) -> {
                        val argument = transactionDetails.data!!.removeSolidityMethodPrefix(MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID)
                        transactionId = MultiSigWalletWithDailyLimit.RevokeConfirmation.decodeArguments(argument).transactionid.value
                        transactionType = RevokeMultisigTransaction()
                    }
                    else -> throw IllegalStateException("Transaction it's neither a Confirm or Revoke")
                }
            }.subscribeOn(Schedulers.computation())


    override fun getMultisigTransactionType() = transactionType

    override fun getMultisigTransactionId() = transactionId

    override fun getTransaction() = transactionDetails

    override fun observeMultisigWalletDetails(): Flowable<MultisigWallet> =
            multisigRepository.observeMultisigWallet(transactionDetails.address)

    override fun signTransaction(): Observable<Result<String>> =
            accountsRepository.loadActiveAccount()
                    .flatMapObservable {
                        ethereumJsonRpcRepository.getTransactionParameters(it.address,
                                TransactionCallParams(
                                        to = transactionDetails.address.asEthereumAddressString(),
                                        data = transactionDetails.data))
                    }
                    .flatMapSingle {
                        val tx = Transaction(nonce = it.nonce,
                                gasPrice = it.gasPrice,
                                startGas = it.gas,
                                to = transactionDetails.address,
                                value = transactionDetails.value?.value ?: BigInteger("0"),
                                data = transactionDetails.data?.hexToByteArray() ?: ByteArray(0))
                        accountsRepository.signTransaction(tx)
                    }
                    .flatMap { ethereumJsonRpcRepository.sendRawTransaction(it) }
                    .mapToResult()

    override fun addMultisigWallet(address: BigInteger, name: String?): Single<Result<BigInteger>> =
            multisigRepository.addMultisigWallet(address, name).andThen(Single.just(address)).mapToResult()

    override fun loadTransactionDetails(): Observable<GnosisMultisigTransaction> =
            gnosisMultisigWrapper.getTransaction(transactionDetails.address, transactionId)

    override fun loadTokenInfo(address: BigInteger) = tokenRepository.loadTokenInfo(address)
}
