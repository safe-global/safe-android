package pm.gnosis.heimdall.ui.transactions.details.base

import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import java.math.BigInteger
import javax.inject.Inject

class BaseEditableTransactionDetailsViewModel @Inject constructor(
        private val safeRepository: GnosisSafeRepository
) : BaseEditableTransactionDetailsContract() {
    override fun loadSafeInfo(safe: BigInteger): Observable<Safe> =
            safeRepository.observeSafe(safe).toObservable()
}
