package pm.gnosis.heimdall.ui.tokens

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.data.db.ERC20Token

abstract class TokensContract : ViewModel() {
    abstract fun observeTokens(): Flowable<List<ERC20Token>>

    abstract fun observeTokenInfo(token: ERC20Token): Observable<ERC20.Token>

    abstract fun addToken(address: String, name: String = ""): Completable

    abstract fun removeToken(token: ERC20Token): Completable
}
