package pm.gnosis.heimdall.ui.settings.tokens

import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter

abstract class TokenManagementContract : ViewModel() {
    abstract fun observeVerifiedTokens(): Flowable<Adapter.Data<ERC20Token>>
}
