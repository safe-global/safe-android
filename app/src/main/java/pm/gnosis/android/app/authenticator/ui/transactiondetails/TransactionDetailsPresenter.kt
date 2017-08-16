package pm.gnosis.android.app.authenticator.ui.transactiondetails

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.android.app.authenticator.data.db.GnosisAuthenticatorDb
import pm.gnosis.android.app.authenticator.data.db.MultisigWallet
import pm.gnosis.android.app.authenticator.data.geth.GethRepository
import pm.gnosis.android.app.authenticator.data.model.TransactionCallParams
import pm.gnosis.android.app.authenticator.data.model.TransactionDetails
import pm.gnosis.android.app.authenticator.data.remote.InfuraRepository
import pm.gnosis.android.app.authenticator.di.ForView
import pm.gnosis.android.app.authenticator.util.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject

@ForView
class TransactionDetailsPresenter @Inject constructor(private val infuraRepository: InfuraRepository,
                                                      private val gethRepository: GethRepository,
                                                      private val gnosisAuthenticatorDb: GnosisAuthenticatorDb) {
    fun getMultisigWalletDetails(address: BigInteger): Flowable<MultisigWallet> =
            gnosisAuthenticatorDb.multisigWalletDao().observeMultisigWallet(address.asEthereumAddressString())
                    .subscribeOn(Schedulers.io())

    fun signTransaction(transactionDetails: TransactionDetails) =
            infuraRepository.getTransactionParameters(
                    TransactionCallParams(
                            to = transactionDetails.address.asEthereumAddressString(),
                            data = transactionDetails.data))
                    .map {
                        gethRepository.signTransaction(
                                it.nonce, transactionDetails.address, transactionDetails.value?.value,
                                it.gas, it.gasPrice, transactionDetails.data)
                    }
                    .flatMap { infuraRepository.sendRawTransaction(it) }

    fun addMultisigWallet(name: String = "", address: String) = Completable.fromCallable {
        val multisigWallet = MultisigWallet()
        multisigWallet.name = name
        multisigWallet.address = address
        gnosisAuthenticatorDb.multisigWalletDao().insertMultisigWallet(multisigWallet)
    }.subscribeOn(Schedulers.io())
}
