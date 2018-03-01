package pm.gnosis.heimdall.ui.tokens.add

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.svalinn.common.utils.Result

abstract class AddTokenContract : ViewModel() {
    abstract fun addToken(): Single<Result<Unit>>
    abstract fun loadTokenInfo(tokenAddress: String): Observable<Result<ERC20Token>>
}
