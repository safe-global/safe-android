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
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.remote.BulkRequest
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.data.repositories.model.fromDb
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.toAlfaNumericAscii
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTokenRepository @Inject constructor(
        gnosisAuthenticatorDb: GnosisAuthenticatorDb,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
        private val preferencesManager: PreferencesManager
) : TokenRepository {
    private val erc20TokenDao = gnosisAuthenticatorDb.erc20TokenDao()

    override fun observeTokens(): Flowable<List<ERC20Token>> =
            erc20TokenDao.observeTokens()
                    .subscribeOn(Schedulers.io())
                    .map { it.map { it.fromDb() } }

    override fun loadTokenInfo(contractAddress: BigInteger): Observable<ERC20Token> {
        val request = TokenInfoRequest(
                BulkRequest.SubRequest(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.NAME_METHOD_ID}").callRequest(0),
                        { it.result.hexAsBigIntegerOrNull()?.toAlfaNumericAscii()?.trim() }),
                BulkRequest.SubRequest(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.SYMBOL_METHOD_ID}").callRequest(1),
                        { it.result.hexAsBigIntegerOrNull()?.toAlfaNumericAscii()?.trim() }),
                BulkRequest.SubRequest(TransactionCallParams(to = contractAddress.asEthereumAddressString(), data = "0x${ERC20.DECIMALS_METHOD_ID}").callRequest(2),
                        { it.result.hexAsBigIntegerOrNull() }))
        return ethereumJsonRpcRepository.bulk(request).map { ERC20Token(contractAddress, it.name.value, it.symbol.value, it.decimals.value) }
    }

    override fun addToken(address: BigInteger, name: String?): Completable = Completable.fromCallable {
        val token = ERC20TokenDb(address, name, false)
        erc20TokenDao.insertERC20Token(token)
    }.subscribeOn(Schedulers.io())

    override fun removeToken(address: BigInteger): Completable = Completable.fromCallable {
        erc20TokenDao.deleteToken(address)
    }.subscribeOn(Schedulers.io())

    override fun setup(): Completable = Completable.fromCallable {
        val finishedTokensSetup = preferencesManager.prefs.getBoolean(PreferencesManager.FINISHED_TOKENS_SETUP, false)
        if (!finishedTokensSetup) {
            val tokens = ERC20.verifiedTokens.entries.map {
                ERC20TokenDb(it.key, it.value, true)
            }.toList()
            erc20TokenDao.insertERC20Tokens(tokens)
            preferencesManager.prefs.edit { putBoolean(PreferencesManager.FINISHED_TOKENS_SETUP, true) }
        }
    }

    class TokenInfoRequest(val name: SubRequest<String?>, val symbol: SubRequest<String?>, val decimals: SubRequest<BigInteger?>) :
            BulkRequest(name, symbol, decimals)
}
