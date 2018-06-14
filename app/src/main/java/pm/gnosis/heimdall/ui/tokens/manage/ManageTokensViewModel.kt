package pm.gnosis.heimdall.ui.tokens.manage

import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

class ManageTokensViewModel @Inject constructor(
    private val tokenRepository: TokenRepository
) : ManageTokensContract() {
    private val errorSubject: Subject<Throwable> = PublishSubject.create()
    private var cachedData: Adapter.Data<ERC20TokenEnabled>? = null

    override fun observeErrors(): Observable<Throwable> = errorSubject

    override fun observeVerifiedTokens() = FlowableTransformer<Unit, Adapter.Data<ERC20TokenEnabled>> { inStream ->
        Flowable.combineLatest(
            inStream.switchMapSingle {
                tokenRepository.loadVerifiedTokens().map { it.map { ERC20TokenEnabled(it, enabled = false) } }.doOnError(errorSubject::onNext)
                    .onErrorReturnItem(emptyList())
            },
            tokenRepository.observeEnabledTokens().map { it.map { ERC20TokenEnabled(it, enabled = true) } },
            BiFunction { verifiedTokens: List<ERC20TokenEnabled>, enabledTokens: List<ERC20TokenEnabled> -> loadData(verifiedTokens, enabledTokens) }
        )
            .scanToAdapterData({ it.erc20Token.address }, payloadCalc = { _, _ -> Unit }, initialData = cachedData)
            .doOnNext { this.cachedData = it }
    }

    private fun loadData(verifiedTokens: List<ERC20TokenEnabled>, enabledTokens: List<ERC20TokenEnabled>): List<ERC20TokenEnabled> {
        val map = verifiedTokens.associateByTo(mutableMapOf(), { it.erc20Token.address })
        enabledTokens.forEach { map[it.erc20Token.address] = it }
        return map.values.toList()
    }

    override fun enableToken(erC20Token: ERC20Token) =
        tokenRepository.enableToken(erC20Token).mapToResult()

    override fun disableToken(tokenAddress: Solidity.Address) =
        tokenRepository.disableToken(tokenAddress).mapToResult()
}
