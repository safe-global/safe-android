package pm.gnosis.heimdall.ui.tokens

import io.reactivex.Flowable
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.data.db.ERC20Token
import pm.gnosis.heimdall.data.repositories.TokenRepository
import javax.inject.Inject

@ForView
class TokensPresenter @Inject constructor(private val tokenRepository: TokenRepository) {
    fun observeTokens(): Flowable<List<ERC20Token>> = tokenRepository.observeTokens()

    fun observeTokenInfo(token: ERC20Token) = tokenRepository.observeTokenInfo(token)

    fun addToken(address: String, name: String = "") = tokenRepository.addToken(address, name)

    fun removeToken(token: ERC20Token) = tokenRepository.removeToken(token)
}
