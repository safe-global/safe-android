package pm.gnosis.heimdall.ui.multisig

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Flowable
import pm.gnosis.heimdall.data.db.MultisigWallet


abstract class MultisigContract: ViewModel() {
    abstract fun observeMultisigWallets(): Flowable<List<MultisigWallet>>
    abstract fun removeMultisigWallet(multisigWallet: MultisigWallet): Completable
    abstract fun updateMultisigWalletName(address: String, newName: String): Completable
    abstract fun addMultisigWallet(name: String, address: String): Completable
}