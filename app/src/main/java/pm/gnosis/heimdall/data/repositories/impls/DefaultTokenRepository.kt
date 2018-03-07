package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.ethereum.EthBalance
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthRequest
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.ERC20TokenDb
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.data.repositories.models.fromDb
import pm.gnosis.heimdall.data.repositories.models.toDb
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.utils.ERC20
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.*
import pm.gnosis.utils.exceptions.InvalidAddressException
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTokenRepository @Inject constructor(
    appDb: ApplicationDb,
    private val ethereumRepository: EthereumRepository,
    private val preferencesManager: PreferencesManager,
    private val moshi: Moshi,
    @ApplicationContext private val context: Context
) : TokenRepository {
    private val erc20TokenDao = appDb.erc20TokenDao()

    override fun observeTokens(): Flowable<List<ERC20Token>> =
        erc20TokenDao.observeTokens()
            .subscribeOn(Schedulers.io())
            .map { it.map { it.fromDb() } }

    override fun observeToken(address: BigInteger): Flowable<ERC20Token> =
        erc20TokenDao.observeToken(address)
            .map { it.fromDb() }
            .subscribeOn(Schedulers.io())

    override fun loadTokens(): Single<List<ERC20Token>> =
        erc20TokenDao.loadTokens()
            .subscribeOn(Schedulers.io())
            .map { it.map { it.fromDb() } }

    override fun loadToken(address: BigInteger): Single<ERC20Token> =
        erc20TokenDao.loadToken(address)
            .map { it.fromDb() }
            .subscribeOn(Schedulers.io())

    override fun loadTokenInfo(contractAddress: BigInteger): Observable<ERC20Token> {
        if (!contractAddress.isValidEthereumAddress()) return Observable.error(
            InvalidAddressException(contractAddress)
        )
        val bulk = TokenInfoRequest(
            EthCall(
                transaction = Transaction(
                    address = contractAddress,
                    data = "0x${ERC20.NAME_METHOD_ID}"
                ), id = 0
            ),
            EthCall(
                transaction = Transaction(
                    address = contractAddress,
                    data = "0x${ERC20.SYMBOL_METHOD_ID}"
                ), id = 1
            ),
            EthCall(
                transaction = Transaction(
                    address = contractAddress,
                    data = "0x${ERC20.DECIMALS_METHOD_ID}"
                ), id = 2
            )
        )
        return ethereumRepository.bulk(bulk.requests)
            .map {
                val name =
                    bulk.name.result()?.hexStringToByteArrayOrNull()?.utf8String()?.trim()
                            ?: throw IllegalArgumentException()
                val symbol =
                    bulk.symbol.result()?.hexStringToByteArrayOrNull()?.utf8String()?.trim()
                            ?: throw IllegalArgumentException()
                val decimals =
                    bulk.decimals.result()?.hexAsBigIntegerOrNull()?.toInt()
                            ?: throw IllegalArgumentException()
                ERC20Token(contractAddress, name, symbol, decimals)
            }
    }

    override fun loadTokenBalances(
        ofAddress: BigInteger,
        erC20Tokens: List<ERC20Token>
    ): Observable<List<Pair<ERC20Token, BigInteger?>>> {
        if (!ofAddress.isValidEthereumAddress()) return Observable.error(
            InvalidAddressException(ofAddress)
        )
        val requests =
            erC20Tokens.mapIndexed { index, token ->
                if (token == ETHER_TOKEN) {
                    EthBalance(ofAddress, id = index)
                } else {
                    EthCall(
                        transaction = Transaction(
                            token.address,
                            data = StandardToken.BalanceOf.encode(Solidity.Address(ofAddress))
                        ),
                        id = index
                    )
                }
            }.toList()

        return ethereumRepository.bulk(requests).map {
            it.mapIndexed { index, subRequest ->
                val value = when (subRequest) {
                    is EthBalance ->
                        subRequest.result()?.value
                    is EthCall ->
                        subRequest.result()?.let {
                            nullOnThrow { StandardToken.BalanceOf.decode(it).param0.value }
                        }
                    else -> null
                }
                erC20Tokens[index] to value
            }.toList()
        }
    }

    override fun addToken(erC20Token: ERC20Token): Completable = Completable.fromCallable {
        erc20TokenDao.insertERC20Token(erC20Token.toDb())
    }.subscribeOn(Schedulers.io())

    override fun removeToken(address: BigInteger): Completable = Completable.fromCallable {
        erc20TokenDao.deleteToken(address)
    }.subscribeOn(Schedulers.io())

    override fun setup(): Completable = Completable.fromCallable {
        val finishedTokensSetup =
            preferencesManager.prefs.getBoolean(PreferencesManager.FINISHED_TOKENS_SETUP, false)
        if (!finishedTokensSetup) {
            val verifiedTokensType =
                Types.newParameterizedType(List::class.java, VerifiedTokenJson::class.java)
            val adapter = moshi.adapter<List<VerifiedTokenJson>>(verifiedTokensType)
            val json = context.resources.openRawResource(R.raw.verified_tokens).bufferedReader()
                .use { it.readText() }
            val verifiedTokens = adapter.fromJson(json)

            verifiedTokens.map {
                ERC20TokenDb(
                    address = it.address, name = it.name, symbol = it.symbol,
                    decimals = it.decimals, verified = true
                )
            }.let { erc20TokenDao.insertERC20Tokens(it) }

            preferencesManager.prefs.edit {
                putBoolean(
                    PreferencesManager.FINISHED_TOKENS_SETUP,
                    true
                )
            }
        }
    }

    private data class VerifiedTokenJson(
        @Json(name = "address") val address: BigInteger,
        @Json(name = "name") val name: String,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "decimals") val decimals: Int
    )

    private class TokenInfoRequest(
        val name: EthRequest<String>,
        val symbol: EthRequest<String>,
        val decimals: EthRequest<String>
    ) {
        val requests = listOf(name, symbol, decimals)
    }
}
