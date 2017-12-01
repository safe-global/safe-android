package pm.gnosis.heimdall.ui.tokens.add

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.hexAsEthereumAddress
import javax.inject.Inject

class AddTokenViewModel @Inject constructor(
        @ApplicationContext context: Context,
        private val tokenRepository: TokenRepository
) : AddTokenContract() {
    private val errorHandler = LocalizedException.networkErrorHandlerBuilder(context)
            .add({ it is InvalidAddressException }, R.string.invalid_ethereum_address)
            .add({ it is NoTokenSetException }, R.string.no_token_loaded)
            .build()
    private var erc20Token: ERC20Token? = null

    override fun addToken(): Single<Result<Unit>> {
        val token = erc20Token
        return if (token == null) {
            Single.error<Unit>(NoTokenSetException())
        } else {
            tokenRepository.addToken(token).andThen(Single.just(Unit))
        }
                .onErrorResumeNext { throwable: Throwable -> errorHandler.single(throwable) }
                .mapToResult()
    }

    override fun loadTokenInfo(tokenAddress: String): Observable<Result<ERC20Token>> =
            Observable
                    .fromCallable { tokenAddress.hexAsEthereumAddress() }
                    .flatMap { tokenRepository.loadTokenInfo(it) }
                    .doOnNext { this.erc20Token = it }
                    .onErrorResumeNext { throwable: Throwable -> errorHandler.observable(throwable) }
                    .mapToResult()

    private class NoTokenSetException : Exception()
}
