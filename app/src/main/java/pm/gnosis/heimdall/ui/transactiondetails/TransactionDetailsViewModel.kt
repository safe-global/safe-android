package pm.gnosis.heimdall.ui.transactiondetails

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.accounts.base.models.Transaction
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.common.util.mapToResult
import pm.gnosis.heimdall.data.contracts.GnosisMultisigTransaction
import pm.gnosis.heimdall.data.contracts.GnosisMultisigWrapper
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.model.TransactionDetails
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.utils.*
import java.math.BigInteger
import javax.inject.Inject

class TransactionDetailsViewModel @Inject constructor(private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
                                                      private val accountsRepository: AccountsRepository,
                                                      private val multisigRepository: MultisigRepository,
                                                      private val gnosisMultisigWrapper: GnosisMultisigWrapper) : TransactionDetailsContract() {
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

    override fun getMultisigWalletDetails(): Flowable<MultisigWallet> =
            multisigRepository.observeMultisigWallet(transactionDetails.address.asEthereumAddressString())

    override fun signTransaction(): Observable<String> =
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

    override fun addMultisigWallet(address: String, name: String): Single<Result<String>> =
            multisigRepository.addMultisigWallet(address, name).andThen(Single.just(address)).mapToResult()

    override fun getTransactionDetails(): Observable<GnosisMultisigTransaction> =
            gnosisMultisigWrapper.getTransaction(transactionDetails.address.asEthereumAddressString(), transactionId)

    override fun getTokenInfo(address: BigInteger) = ethereumJsonRpcRepository.getTokenInfo(address)
}
