package pm.gnosis.heimdall.ui.transactions.details.base

import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import java.math.BigInteger
import javax.inject.Inject

class BaseEditableTransactionDetailsViewModel @Inject constructor(
        private val safeRepository: GnosisSafeRepository
) : BaseEditableTransactionDetailsContract() {
    override fun observeSafe(safeAddress: BigInteger): Observable<Safe> =
            safeRepository.observeSafe(safeAddress).toObservable()
}
