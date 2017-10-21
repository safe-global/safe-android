package pm.gnosis.heimdall.ui.tokens

import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import java.math.BigInteger

abstract class TokensContract : ViewModel() {
    abstract fun observeTokens(): Flowable<List<ERC20Token>>
    abstract fun loadTokenInfo(token: ERC20Token): Observable<Result<ERC20Token>>
    abstract fun addToken(address: BigInteger, name: String? = null): Observable<Result<ERC20Token>>
    abstract fun removeToken(token: ERC20Token): Observable<Result<ERC20Token>>
}
