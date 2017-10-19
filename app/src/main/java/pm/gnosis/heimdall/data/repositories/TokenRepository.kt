package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import java.math.BigInteger

interface TokenRepository {
    fun observeTokens(): Flowable<List<ERC20Token>>
    fun loadTokenInfo(contractAddress: BigInteger): Observable<ERC20Token>
    fun loadTokenBalances(ofAddress: BigInteger, erC20Tokens: List<ERC20Token>): Observable<List<Pair<ERC20Token, BigInteger?>>>
    fun addToken(erC20Token: ERC20Token): Completable
    fun removeToken(address: BigInteger): Completable
    fun setup(): Completable
}
