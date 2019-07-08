package pm.gnosis.heimdall.ui.tokens.manage

import android.content.Context
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

class ManageTokensViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenRepository: TokenRepository
) : ManageTokensContract() {
    private val errorSubject: Subject<Throwable> = PublishSubject.create()
    private var cachedData: Adapter.Data<ERC20TokenEnabled>? = null

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    override fun observeErrors(): Observable<Throwable> = errorSubject.map { errorHandler.translate(it) }

    override fun observeVerifiedTokens() = ObservableTransformer<String, Adapter.Data<ERC20TokenEnabled>> { inStream ->
        Observable.combineLatest(
            inStream.switchMapSingle { search ->
                tokenRepository.loadVerifiedTokens(search)
                    .doOnError(errorSubject::onNext)
                    .onErrorReturnItem(emptyList())
                    .map { tokens -> tokens to search }
            },
            tokenRepository.observeEnabledTokens().toObservable(),
            BiFunction { (tokens, search): Pair<List<ERC20Token>, String>, enabledTokens: List<ERC20Token> ->
                combineData(tokens, enabledTokens, search)
            }
        )
            .scanToAdapterData({ it.erc20Token.address }, payloadCalc = { _, _ -> Unit }, initialData = cachedData)
            .doOnNext { this.cachedData = it }
    }

    private fun combineData(verifiedTokens: List<ERC20Token>, enabledTokens: List<ERC20Token>, search: String): List<ERC20TokenEnabled> {
        val map = verifiedTokens.associateTo(mutableMapOf()) { it.address to ERC20TokenEnabled(it, false) }
        enabledTokens.forEach { if (it.applies(search)) map[it.address] = ERC20TokenEnabled(it, true) }
        return map.values.toList()
    }

    private fun ERC20Token.applies(filter: String) =
        name.contains(filter, true) or symbol.contains(filter, true)

    override fun enableToken(erC20Token: ERC20Token) = tokenRepository.enableToken(erC20Token).mapToResult()

    override fun disableToken(tokenAddress: Solidity.Address) = tokenRepository.disableToken(tokenAddress).mapToResult()
}
