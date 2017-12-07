package pm.gnosis.heimdall.ui.safe.details.transactions

import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.ui.base.Adapter
import java.math.BigInteger


abstract class SafeTransactionsContract: ViewModel() {
    abstract fun setup(address: BigInteger)

    abstract fun observeTransactions(): Flowable<out Result<Adapter.Data<String>>>

}