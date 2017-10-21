package pm.gnosis.heimdall.ui.tokens

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.Function
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.common.util.mapToResult
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import java.math.BigInteger
import javax.inject.Inject

class TokensViewModel @Inject constructor(@ApplicationContext private val context: Context,
                                          private val tokenRepository: TokenRepository) : TokensContract() {
    private val errorHandler = LocalizedException.networkErrorHandlerBuilder(context)
            .add({ it is SQLiteConstraintException }, R.string.token_add_error)
            .build()

    override fun observeTokens(): Flowable<List<ERC20Token>> = tokenRepository.observeTokens()

    override fun loadTokenInfo(token: ERC20Token) =
            tokenRepository.loadTokenInfo(token.address)
                    .onErrorResumeNext(Function { errorHandler.observable(it) })
                    .mapToResult()

    override fun addToken(address: BigInteger, name: String?): Observable<Result<ERC20Token>> =
            tokenRepository.addToken(address, name)
                    .andThen(Observable.just(ERC20Token(address, name)))
                    .onErrorResumeNext(Function { errorHandler.observable(it) })
                    .mapToResult()

    override fun removeToken(token: ERC20Token) = tokenRepository.removeToken(token.address)
            .andThen(Observable.just(token))
            .onErrorResumeNext(Function { errorHandler.observable(it) })
            .mapToResult()
}
