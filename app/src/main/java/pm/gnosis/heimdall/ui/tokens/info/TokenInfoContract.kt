package pm.gnosis.heimdall.ui.tokens.info

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import java.math.BigInteger

abstract class TokenInfoContract : ViewModel() {
    abstract fun observeToken(): Observable<ERC20Token>
    abstract fun removeToken(): Observable<Result<Unit>>
    abstract fun setup(tokenAddress: String)
}
