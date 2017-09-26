package pm.gnosis.heimdall.ui.multisig

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.MultisigWallet
import javax.inject.Inject

@ForView
class MultisigPresenter @Inject constructor(private val gnosisAuthenticatorDb: GnosisAuthenticatorDb) {
    fun observeMultisigWallets(): Flowable<List<MultisigWallet>> {
        return gnosisAuthenticatorDb.multisigWalletDao().observeMultisigWallets().subscribeOn(Schedulers.io())
    }

    fun addMultisigWallet(name: String = "", address: String) = Completable.fromCallable {
        val multisigWallet = MultisigWallet()
        multisigWallet.name = name
        multisigWallet.address = address
        gnosisAuthenticatorDb.multisigWalletDao().insertMultisigWallet(multisigWallet)
    }.subscribeOn(Schedulers.io())

    fun removeMultisigWallet(multisigWallet: MultisigWallet) = Completable.fromCallable {
        gnosisAuthenticatorDb.multisigWalletDao().removeMultisigWallet(multisigWallet)
    }.subscribeOn(Schedulers.io())

    fun updateMultisigWalletName(address: String, newName: String) = Completable.fromCallable {
        val multisigWallet = MultisigWallet()
        multisigWallet.name = newName
        multisigWallet.address = address
        gnosisAuthenticatorDb.multisigWalletDao().updateMultisigWallet(multisigWallet)
    }.subscribeOn(Schedulers.io())
}
