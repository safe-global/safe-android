package pm.gnosis.heimdall.ui.tokens.balances

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result

abstract class TokenBalancesContract : ViewModel() {
    abstract fun setup(address: Solidity.Address)
    abstract fun observeTokens(refreshEvents: Observable<Unit>): Observable<out Result<Adapter.Data<ERC20TokenWithBalance>>>
    abstract fun observeLoadingStatus(): Observable<Boolean>
}
