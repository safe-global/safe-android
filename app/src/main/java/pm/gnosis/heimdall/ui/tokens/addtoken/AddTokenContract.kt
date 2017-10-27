package pm.gnosis.heimdall.ui.tokens.addtoken

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.ERC20Token

abstract class AddTokenContract : ViewModel() {
    abstract fun addToken(): Single<Result<Unit>>
    abstract fun loadTokenInfo(tokenAddress: String): Observable<Result<ERC20Token>>
}
