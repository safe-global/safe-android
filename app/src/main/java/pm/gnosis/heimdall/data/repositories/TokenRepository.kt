package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.model.Solidity
import java.math.BigInteger

interface TokenRepository {
    fun observeEnabledTokens(): Flowable<List<ERC20Token>>
    fun observeToken(address: Solidity.Address): Flowable<ERC20Token>
    fun loadTokens(): Single<List<ERC20Token>>
    fun loadToken(address: Solidity.Address): Single<ERC20Token>
    fun loadTokenBalances(ofAddress: Solidity.Address, erC20Tokens: List<ERC20Token>): Observable<List<Pair<ERC20Token, BigInteger?>>>

    fun enableToken(token: ERC20Token): Completable
    fun disableToken(address: Solidity.Address): Completable
    fun loadVerifiedTokens(): Single<List<ERC20Token>>
}
