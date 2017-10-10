package pm.gnosis.heimdall.ui.tokens

import io.reactivex.Flowable
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import javax.inject.Inject

class TokensViewModel @Inject constructor(private val tokenRepository: TokenRepository) : TokensContract() {
    override fun observeTokens(): Flowable<List<ERC20Token>> = tokenRepository.observeTokens()

    override fun observeTokenInfo(token: ERC20Token) = tokenRepository.observeTokenInfo(token)

    override fun addToken(address: String, name: String) = tokenRepository.addToken(address, name)

    override fun removeToken(token: ERC20Token) = tokenRepository.removeToken(token.address)
}
