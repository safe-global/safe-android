package pm.gnosis.heimdall.ui.settings.tokens

import io.reactivex.Flowable
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import javax.inject.Inject

class TokenManagementViewModel @Inject constructor(
        private val tokenRepository: TokenRepository
) : TokenManagementContract() {
    override fun observeVerifiedTokens(): Flowable<Adapter.Data<ERC20Token>> =
            tokenRepository.observeTokens()
                    .scanToAdapterData({ it.address })
}
