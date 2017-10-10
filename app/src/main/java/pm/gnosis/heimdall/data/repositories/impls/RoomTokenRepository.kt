package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.common.util.edit
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.model.ERC20TokenDb
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.data.repositories.model.fromDb
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.isValidEthereumAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTokenRepository @Inject constructor(
        gnosisAuthenticatorDb: GnosisAuthenticatorDb,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
        private val preferencesManager: PreferencesManager
) : TokenRepository {

    private val erc20TokenDao = gnosisAuthenticatorDb.erc20TokenDao()

    override fun observeTokens(): Flowable<List<ERC20Token>> =
            erc20TokenDao.observeTokens()
                    .subscribeOn(Schedulers.io())
                    .map { it.mapNotNull { it.fromDb() } }


    override fun observeTokenInfo(token: ERC20Token): Observable<ERC20Token> {
        val tokenAddress = token.address
        return if (tokenAddress.isValidEthereumAddress()) ethereumJsonRpcRepository.getTokenInfo(tokenAddress.hexAsBigInteger())
        else Observable.error(IllegalArgumentException("Invalid token address"))
    }

    override fun addToken(address: String, name: String): Completable = Completable.fromCallable {
        val token = ERC20TokenDb(address, name, false)
        erc20TokenDao.insertERC20Token(token)
    }.subscribeOn(Schedulers.io())

    override fun removeToken(address: String): Completable = Completable.fromCallable {
        erc20TokenDao.deleteToken(address)
    }.subscribeOn(Schedulers.io())

    override fun setup(): Completable {
        val isFirstLaunch = preferencesManager.prefs.getBoolean(PreferencesManager.FIRST_LAUNCH_KEY, true)
        return Completable.fromCallable {
            if (isFirstLaunch) {
                val tokens = ERC20.verifiedTokens.entries.map {
                    ERC20TokenDb(it.key.asEthereumAddressString(), it.value, true)
                }.toList()
                erc20TokenDao.insertERC20Tokens(tokens)
                preferencesManager.prefs.edit { putBoolean(PreferencesManager.FIRST_LAUNCH_KEY, false) }
            }
        }
    }
}
