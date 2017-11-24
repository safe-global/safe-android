package pm.gnosis.heimdall.ui.tokens.info

import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import java.math.BigInteger
import javax.inject.Inject

class TokenInfoViewModel @Inject constructor(
        private val tokenRepository: TokenRepository
) : TokenInfoContract() {
    private lateinit var token: ERC20Token

    override fun observeToken(tokenAddress: BigInteger): Observable<ERC20Token> =
            tokenRepository.observeToken(tokenAddress)
                    .doOnNext { this.token = it }
                    .toObservable()

    override fun removeToken(): Observable<Result<ERC20Token>> =
            tokenRepository.removeToken(token.address)
                    .andThen(Observable.just(token))
                    .mapToResult()
}
