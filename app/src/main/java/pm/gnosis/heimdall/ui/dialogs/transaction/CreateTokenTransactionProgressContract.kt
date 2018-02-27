package pm.gnosis.heimdall.ui.dialogs.transaction

import android.arch.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.models.Transaction
import java.math.BigInteger

abstract class CreateTokenTransactionProgressContract : ViewModel() {
    abstract fun loadCreateTokenTransaction(tokenAddress: BigInteger?): Single<Transaction>
}
