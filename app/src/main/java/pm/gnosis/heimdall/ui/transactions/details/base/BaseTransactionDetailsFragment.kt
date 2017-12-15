package pm.gnosis.heimdall.ui.transactions.details.base

import com.gojuno.koptional.Optional
import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.transactions.BaseTransactionActivity
import pm.gnosis.models.Transaction
import java.math.BigInteger


abstract class BaseTransactionDetailsFragment : BaseFragment() {

    abstract fun observeTransaction(): Observable<Result<Transaction>>
    abstract fun observeSafe(): Observable<Optional<BigInteger>>
    abstract fun inputEnabled(enabled: Boolean)

    override fun onStart() {
        super.onStart()
        (activity as? BaseTransactionActivity)?.registerFragmentObservables(this)
    }
}