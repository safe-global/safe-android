package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import pm.gnosis.heimdall.data.repositories.models.MultisigWalletInfo
import java.math.BigInteger


interface MultisigRepository {
    fun observeMultisigWallets(): Flowable<List<MultisigWallet>>
    fun observeMultisigWallet(address: BigInteger): Flowable<MultisigWallet>
    fun addMultisigWallet(address: BigInteger, name: String?): Completable
    fun removeMultisigWallet(address: BigInteger): Completable
    fun updateMultisigWalletName(address: BigInteger, newName: String): Completable
    fun loadMultisigWalletInfo(address: BigInteger): Observable<MultisigWalletInfo>
}
