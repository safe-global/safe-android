package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.model.ERC20Token

interface TokenRepository {
    fun observeTokens(): Flowable<List<ERC20Token>>
    fun observeTokenInfo(token: ERC20Token): Observable<ERC20Token>
    fun addToken(address: String, name: String = ""): Completable
    fun removeToken(address: String): Completable
    fun setup(): Completable
}
