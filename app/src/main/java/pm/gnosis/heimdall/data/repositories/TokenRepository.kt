package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.data.db.ERC20Token

interface TokenRepository {
    fun observeTokens(): Flowable<List<ERC20Token>>
    fun observeTokenInfo(token: ERC20Token): Observable<ERC20.Token>
    fun addToken(address: String, name: String = ""): Completable
    fun removeToken(token: ERC20Token): Completable
}
