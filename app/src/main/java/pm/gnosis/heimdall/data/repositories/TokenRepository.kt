package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import java.math.BigInteger

interface TokenRepository {
    fun observeTokens(): Flowable<List<ERC20Token>>
    fun loadTokenInfo(contractAddress: BigInteger): Observable<ERC20Token>
    fun addToken(address: BigInteger, name: String? = null): Completable
    fun removeToken(address: BigInteger): Completable
    fun setup(): Completable
}
