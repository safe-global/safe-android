package pm.gnosis.heimdall.ui.tokens

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import java.math.BigInteger

abstract class AddTokenContract : ViewModel() {
    abstract fun addToken(): Single<Result<Unit>>
    abstract fun loadTokenInfo(tokenAddress: String): Observable<Result<ERC20Token>>
}
