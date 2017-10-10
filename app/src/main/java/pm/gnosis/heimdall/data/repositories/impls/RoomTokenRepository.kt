package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.data.db.ERC20Token
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.isValidEthereumAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTokenRepository @Inject constructor(gnosisAuthenticatorDb: GnosisAuthenticatorDb,
                                              private val ethereumJsonRpcRepository: EthereumJsonRpcRepository) : TokenRepository {
    private val erc20TokenDao = gnosisAuthenticatorDb.erc20TokenDao()

    override fun observeTokens(): Flowable<List<ERC20Token>> = erc20TokenDao.observeTokens().subscribeOn(Schedulers.io())

    override fun observeTokenInfo(token: ERC20Token): Observable<ERC20.Token> {
        val tokenAddress = token.address
        return if (tokenAddress?.isValidEthereumAddress() == true) ethereumJsonRpcRepository.getTokenInfo(tokenAddress.hexAsBigInteger())
        else Observable.error(IllegalArgumentException("Invalid token address"))
    }

    override fun addToken(address: String, name: String): Completable = Completable.fromCallable {
        val token = ERC20Token()
        token.address = address
        token.name = name
        erc20TokenDao.insertERC20Token(token)
    }.subscribeOn(Schedulers.io())

    override fun removeToken(token: ERC20Token): Completable = Completable.fromCallable {
        erc20TokenDao.deleteToken(token)
    }.subscribeOn(Schedulers.io())
}
