package pm.gnosis.heimdall.ui.tokens.info

import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import java.math.BigInteger
import javax.inject.Inject

class TokenInfoViewModel @Inject constructor(
    private val tokenRepository: TokenRepository
) : TokenInfoContract() {
    private lateinit var tokenAddress: BigInteger

    override fun setup(tokenAddress: String) {
        this.tokenAddress = tokenAddress.hexAsEthereumAddressOrNull() ?: throw InvalidAddressException(tokenAddress)
    }

    override fun observeToken(): Observable<ERC20Token> =
        tokenRepository.observeToken(tokenAddress)
            .toObservable()

    override fun removeToken(): Observable<Result<Unit>> =
        tokenRepository.removeToken(tokenAddress)
            .andThen(Observable.just(Unit))
            .mapToResult()
}
