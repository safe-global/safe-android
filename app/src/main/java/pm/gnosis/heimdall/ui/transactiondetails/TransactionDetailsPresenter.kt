package pm.gnosis.heimdall.ui.transactiondetails

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.accounts.base.models.Transaction
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.data.contracts.GnosisMultisigWrapper
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.MultisigWallet
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.model.TransactionDetails
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexToByteArray
import java.math.BigInteger
import javax.inject.Inject

@ForView
class TransactionDetailsPresenter @Inject constructor(private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
                                                      private val accountsRepository: AccountsRepository,
                                                      private val gnosisAuthenticatorDb: GnosisAuthenticatorDb,
                                                      private val gnosisMultisigWrapper: GnosisMultisigWrapper) {
    fun getMultisigWalletDetails(address: BigInteger): Flowable<MultisigWallet> =
            gnosisAuthenticatorDb.multisigWalletDao().observeMultisigWallet(address.asEthereumAddressString())
                    .subscribeOn(Schedulers.io())

    fun signTransaction(transactionDetails: TransactionDetails) =
            ethereumJsonRpcRepository.getTransactionParameters(
                    TransactionCallParams(
                            to = transactionDetails.address.asEthereumAddressString(),
                            data = transactionDetails.data))
                    .flatMapSingle {
                        val tx = Transaction(it.nonce, transactionDetails.address, transactionDetails.value?.value ?: BigInteger("0"),
                                it.gas, it.gasPrice, transactionDetails.data?.hexToByteArray() ?: ByteArray(0))
                        accountsRepository.signTransaction(tx)
                    }
                    .flatMap { ethereumJsonRpcRepository.sendRawTransaction(it) }

    fun addMultisigWallet(name: String = "", address: String) = Completable.fromCallable {
        val multisigWallet = MultisigWallet()
        multisigWallet.name = name
        multisigWallet.address = address
        gnosisAuthenticatorDb.multisigWalletDao().insertMultisigWallet(multisigWallet)
    }.subscribeOn(Schedulers.io())

    fun getTransactionDetails(address: String, transactionId: BigInteger): Observable<GnosisMultisigWrapper.WrapperTransaction> =
            gnosisMultisigWrapper.getTransaction(address, transactionId)

    fun getTokenInfo(address: BigInteger) = ethereumJsonRpcRepository.getTokenInfo(address)
}
