package pm.gnosis.heimdall.ui.tokens.balances

import android.content.Context
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.utils.scanToAdapterDataResult
import pm.gnosis.utils.exceptions.InvalidAddressException
import java.math.BigInteger
import javax.inject.Inject

class TokenBalancesViewModel @Inject constructor(@ApplicationContext private val context: Context,
                                                 private val tokenRepository: TokenRepository) : TokenBalancesContract() {
    private val errorHandler = LocalizedException.networkErrorHandlerBuilder(context)
            .add({ it is InvalidAddressException }, R.string.invalid_ethereum_address)
            .build()

    private lateinit var address: BigInteger
    private val loadingSubject = PublishSubject.create<Boolean>()
    private var tokensLoading = false

    override fun setup(address: BigInteger) {
        this.address = address
    }

    override fun observeTokens(refreshEvents: Observable<Unit>) =
            Observable
                    .combineLatest(refreshEvents.startWith(Unit), tokenRepository.observeTokens().toObservable(),
                            BiFunction { _: Unit, tokens: List<ERC20Token> -> tokens })
                    .flatMap { tokens -> loadTokenBalances(address, tokens).mapToResult() }
                    .scanToAdapterDataResult({ it.token.address })


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

    override fun observeLoadingStatus(): Observable<Boolean> = loadingSubject

    private fun tokensLoading(isLoading: Boolean) {
        tokensLoading = isLoading
        refreshLoadingStatus()
    }

    private fun refreshLoadingStatus() {
        loadingSubject.onNext(tokensLoading)
    }
}
