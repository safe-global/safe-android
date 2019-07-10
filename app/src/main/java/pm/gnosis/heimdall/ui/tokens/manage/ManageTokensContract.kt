package pm.gnosis.heimdall.ui.tokens.manage

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result

abstract class ManageTokensContract : ViewModel() {
    abstract fun observeVerifiedTokens(): ObservableTransformer<String, Adapter.Data<ERC20TokenEnabled>>
    abstract fun enableToken(erC20Token: ERC20Token): Single<Result<Unit>>
    abstract fun disableToken(tokenAddress: Solidity.Address): Single<Result<Unit>>
    abstract fun observeErrors(): Observable<Throwable>

    data class ERC20TokenEnabled(val erc20Token: ERC20Token, val enabled: Boolean = false)
}
