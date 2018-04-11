package pm.gnosis.heimdall.ui.dialogs.transaction

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity

abstract class CreateTokenTransactionProgressContract : ViewModel() {
    abstract fun loadCreateTokenTransaction(tokenAddress: Solidity.Address): Single<SafeTransaction>
}
