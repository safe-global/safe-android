package pm.gnosis.heimdall.ui.multisig.overview

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Flowable
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.ui.base.Adapter
import java.math.BigInteger

abstract class MultisigOverviewContract : ViewModel() {
    abstract fun addMultisigWallet(address: BigInteger, name: String): Completable
    abstract fun removeMultisigWallet(address: BigInteger): Completable
    abstract fun updateMultisigWalletName(address: BigInteger, newName: String): Completable
    abstract fun observeMultisigWallets(): Flowable<Result<Adapter.Data<MultisigWallet>>>
}
