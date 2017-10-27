package pm.gnosis.heimdall.ui.tokens.overview

import android.content.Context
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import io.reactivex.subjects.PublishSubject
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.common.util.mapToResult
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.utils.scanToAdapterDataResult
import pm.gnosis.utils.exceptions.InvalidAddressException
import java.math.BigInteger
import javax.inject.Inject

class TokensViewModel @Inject constructor(@ApplicationContext private val context: Context,
                                          private val accountsRepository: AccountsRepository,
                                          private val tokenRepository: TokenRepository) : TokensContract() {
    private val errorHandler = LocalizedException.networkErrorHandlerBuilder(context)
            .add({ it is InvalidAddressException }, R.string.invalid_ethereum_address)
            .build()

    private var address: BigInteger? = null
    private val loadingSubject = PublishSubject.create<Boolean>()
    private var tokenInfoLoading = false
    private var tokensLoading = false

    override fun setup(address: BigInteger?) {
        this.address = address
    }

    override fun observeTokens(refreshEvents: Observable<Unit>): Observable<out Result<Adapter.Data<ERC20TokenWithBalance>>> {
        val address = this.address
        return Observable
                .combineLatest(refreshEvents.startWith(Unit), tokenRepository.observeTokens().toObservable(),
                        BiFunction { _: Unit, tokens: List<ERC20Token> -> tokens })
                .flatMap { tokens ->
                    if (address != null) {
                        loadTokenBalances(address, tokens)
                    } else {
                        accountsRepository.loadActiveAccount()
                                .flatMapObservable { loadTokenBalances(it.address, tokens) }
                    }.mapToResult()
                }
                .scanToAdapterDataResult(itemCheck = { (prevToken), (newToken) -> prevToken.address == newToken.address })
    }

    private fun loadTokenBalances(ofAddress: BigInteger, tokens: List<ERC20Token>) =
            tokenRepository.loadTokenBalances(ofAddress, tokens)
                    .map {
                        it.mapNotNull { (token, balance) ->
                            if (token.verified && (balance == BigInteger.ZERO)) null
                            else ERC20TokenWithBalance(token, balance)
                        }
                    }
                    .onErrorResumeNext { throwable: Throwable -> errorHandler.observable(throwable) }
                    .doOnSubscribe { tokensLoading(true) }
                    .doOnTerminate { tokensLoading(false) }

    override fun loadTokenInfo(token: ERC20Token) =
            tokenRepository.loadTokenInfo(token.address)
                    .doOnSubscribe { tokenInfoLoading(true) }
                    .doOnTerminate { tokenInfoLoading(false) }
                    .onErrorResumeNext(Function { errorHandler.observable(it) })
                    .mapToResult()

    override fun removeToken(token: ERC20Token) = tokenRepository.removeToken(token.address)
            .andThen(Observable.just(token))
            .onErrorResumeNext(Function { errorHandler.observable(it) })
            .mapToResult()

    override fun observeLoadingStatus(): Observable<Boolean> = loadingSubject

    private fun tokenInfoLoading(isLoading: Boolean) {
        tokenInfoLoading = isLoading
        refreshLoadingStatus()
    }

    private fun tokensLoading(isLoading: Boolean) {
        tokensLoading = isLoading
        refreshLoadingStatus()
    }

    private fun refreshLoadingStatus() {
        loadingSubject.onNext(tokenInfoLoading || tokensLoading)
    }
}
