package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet


interface MultisigRepository {
    fun observeMultisigWallets(): Flowable<List<MultisigWallet>>
    fun addMultisigWallet(address: String, name: String): Completable
    fun removeMultisigWallet(address: String): Completable
    fun updateMultisigWalletName(address: String, newName: String): Completable
}