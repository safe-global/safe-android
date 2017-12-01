package pm.gnosis.heimdall.ui.tokens.balances

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import java.math.BigInteger

abstract class TokenBalancesContract : ViewModel() {
    abstract fun setup(address: BigInteger)
    abstract fun observeTokens(refreshEvents: Observable<Unit>): Observable<out Result<Adapter.Data<ERC20TokenWithBalance>>>
    abstract fun observeLoadingStatus(): Observable<Boolean>

    data class ERC20TokenWithBalance(val token: ERC20Token, var balance: BigInteger? = null)
}
