package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.data.repositories.model.MultisigWalletInfo


interface MultisigRepository {
    fun observeMultisigWallets(): Flowable<List<MultisigWallet>>
    fun observeMultisigWallet(address: String): Flowable<MultisigWallet>
    fun addMultisigWallet(address: String, name: String): Completable
    fun removeMultisigWallet(address: String): Completable
    fun updateMultisigWalletName(address: String, newName: String): Completable
    fun loadMultisigWalletInfo(address: String): Observable<MultisigWalletInfo>
}
