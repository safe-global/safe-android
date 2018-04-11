package pm.gnosis.heimdall.ui.dialogs.transaction

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction

abstract class CreateTokenTransactionProgressContract : ViewModel() {
    abstract fun loadCreateTokenTransaction(tokenAddress: Solidity.Address): Single<Transaction>
}
