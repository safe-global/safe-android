package pm.gnosis.heimdall.ui.tokens.balances

import android.content.Context
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.scanToAdapterDataResult
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.exceptions.InvalidAddressException
import javax.inject.Inject

class TokenBalancesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenRepository: TokenRepository
) : TokenBalancesContract() {
    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
        .add({ it is InvalidAddressException }, R.string.invalid_ethereum_address)
        .build()

    private lateinit var address: Solidity.Address
    private val loadingSubject = PublishSubject.create<Boolean>()
    private var tokensLoading = false

    override fun setup(address: Solidity.Address) {
        this.address = address
    }

    override fun observeTokens(refreshEvents: Observable<Unit>) =
        Observable
            .combineLatest(refreshEvents.map { false }.startWith(true), tokenRepository.observeEnabledTokens().toObservable(),
                BiFunction { initialLoad: Boolean, tokens: List<ERC20Token> -> initialLoad to tokens })
            .flatMap { (initialLoad, tokens) ->
                val tokensWithEther = listOf(ETHER_TOKEN) + tokens
                loadTokenBalances(address, tokensWithEther, initialLoad).mapToResult()
            }
            .scanToAdapterDataResult({ it.token.address to it.balance })

    private fun loadTokenBalances(ofAddress: Solidity.Address, tokens: List<ERC20Token>, initialLoad: Boolean) =
        tokenRepository.loadTokenBalances(ofAddress, tokens)
            .map { it.map { ERC20TokenWithBalance(it.first, it.second) } }
            .onErrorResumeNext { throwable: Throwable ->
                val mappedError = errorHandler.observable<List<ERC20TokenWithBalance>>(throwable)
                if (initialLoad) {
                    // If we have an error on the initial load we want to show all tokens without balance
                    mappedError.startWith(tokens.map { ERC20TokenWithBalance(it, null) })
                } else {
                    mappedError
                }
            }
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
