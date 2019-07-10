package pm.gnosis.heimdall.ui.settings.general.payment

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.svalinn.common.utils.Result

@Deprecated("See PaymentTokensContract")
abstract class PaymentTokenContract : ViewModel() {
    abstract fun loadPaymentTokens(): Single<Adapter.Data<ERC20Token>>
    abstract fun setPaymentToken(token: ERC20Token): Single<Result<ERC20Token>>
}
