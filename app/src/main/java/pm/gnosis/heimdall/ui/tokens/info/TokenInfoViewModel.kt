package pm.gnosis.heimdall.ui.tokens.info

import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.exceptions.InvalidAddressException
import javax.inject.Inject

class TokenInfoViewModel @Inject constructor(
    private val tokenRepository: TokenRepository
) : TokenInfoContract() {
    private lateinit var tokenAddress: Solidity.Address

    override fun setup(tokenAddress: String) {
        this.tokenAddress = tokenAddress.asEthereumAddress() ?: throw InvalidAddressException(tokenAddress)
    }

    override fun observeToken(): Observable<ERC20Token> =
        tokenRepository.observeToken(tokenAddress)
            .toObservable()

    override fun removeToken(): Observable<Result<Unit>> =
        tokenRepository.removeToken(tokenAddress)
            .andThen(Observable.just(Unit))
            .mapToResult()
}
