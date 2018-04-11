package pm.gnosis.heimdall.ui.transactions.details.base

import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.model.Solidity
import javax.inject.Inject

class BaseTransactionDetailsViewModel @Inject constructor(
    private val safeRepository: GnosisSafeRepository
) : BaseTransactionDetailsContract() {
    override fun observeSafe(safeAddress: Solidity.Address): Observable<Safe> =
        safeRepository.observeSafe(safeAddress).toObservable()
}
