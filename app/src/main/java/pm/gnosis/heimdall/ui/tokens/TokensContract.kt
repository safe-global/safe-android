package pm.gnosis.heimdall.ui.tokens

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import java.math.BigInteger

abstract class TokensContract : ViewModel() {
    // If the address is not provided the current active account will be used
    abstract fun observeTokens(refreshEvents: Observable<Unit>, ofAddress: BigInteger? = null): Observable<out Result<Adapter.Data<ERC20TokenWithBalance>>>

    abstract fun loadTokenInfo(token: ERC20Token): Observable<Result<ERC20Token>>
    abstract fun removeToken(token: ERC20Token): Observable<Result<ERC20Token>>
    abstract fun observeLoadingStatus(): Observable<Boolean>

    data class ERC20TokenWithBalance(val token: ERC20Token, var balance: BigInteger? = null)
}
