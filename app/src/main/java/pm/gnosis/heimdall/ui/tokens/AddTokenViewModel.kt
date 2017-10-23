package pm.gnosis.heimdall.ui.tokens

import android.support.annotation.VisibleForTesting
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.common.util.ErrorResult
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.common.util.mapToResult
import pm.gnosis.heimdall.data.exceptions.InvalidAddressException
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.isValidEthereumAddress
import javax.inject.Inject

class AddTokenViewModel @Inject constructor(
        private val tokenRepository: TokenRepository
) : AddTokenContract() {
    @VisibleForTesting
    var erc20Token: ERC20Token? = null

    override fun addToken(): Single<Result<Unit>> {
        val token = erc20Token ?: return Single.just(ErrorResult(IllegalStateException("No token set")))
        return tokenRepository.addToken(token).andThen(Single.just(Unit)).mapToResult()
    }

    override fun loadTokenInfo(tokenAddress: String): Observable<Result<ERC20Token>> {
        if (!tokenAddress.isValidEthereumAddress()) return Observable.just(ErrorResult(InvalidAddressException()))
        return tokenRepository.loadTokenInfo(tokenAddress.hexAsBigInteger())
                .doOnNext { this.erc20Token = it }
                .mapToResult()
    }
}
