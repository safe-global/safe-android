package pm.gnosis.android.app.wallet.ui.transactiondetails

import io.reactivex.Flowable
import pm.gnosis.android.app.wallet.data.db.GnosisAuthenticatorDb
import pm.gnosis.android.app.wallet.data.db.MultisigWallet
import pm.gnosis.android.app.wallet.data.geth.GethRepository
import pm.gnosis.android.app.wallet.data.model.TransactionCallParams
import pm.gnosis.android.app.wallet.data.model.TransactionDetails
import pm.gnosis.android.app.wallet.data.remote.InfuraRepository
import pm.gnosis.android.app.wallet.di.ForView
import pm.gnosis.android.app.wallet.util.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject

@ForView
class TransactionDetailsPresenter @Inject constructor(private val infuraRepository: InfuraRepository,
                                                      private val gethRepository: GethRepository,
                                                      private val gnosisAuthenticatorDb: GnosisAuthenticatorDb) {
    fun getMultisigWalletDetails(address: BigInteger): Flowable<MultisigWallet> =
            gnosisAuthenticatorDb.multisigWalletDao().observeMultisigWallet(address.asEthereumAddressString())

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
}
