package pm.gnosis.heimdall.data.repositories.impls

import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.remote.TokenServiceApi
import pm.gnosis.heimdall.data.remote.VerifiedTokensServiceApi
import pm.gnosis.heimdall.data.remote.models.tokens.fromNetwork
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.data.repositories.models.fromDb
import pm.gnosis.heimdall.data.repositories.models.toDb
import pm.gnosis.heimdall.helpers.AppPreferencesManager
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.ERC20
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.*
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTokenRepository @Inject constructor(
    appDb: ApplicationDb,
    private val ethereumRepository: EthereumRepository,
    private val preferencesManager: AppPreferencesManager,
    private val tokenServiceApi: TokenServiceApi,
    private val verifiedTokensServiceApi: VerifiedTokensServiceApi
) : TokenRepository {

    private val hardcodedTokens = mapOf(
        ETHER_TOKEN.address to ETHER_TOKEN
    )

    private val erc20TokenDao = appDb.erc20TokenDao()

    override fun observeEnabledTokens(): Flowable<List<ERC20Token>> =
        erc20TokenDao.observeTokens()
            .subscribeOn(Schedulers.io())
            .map { it.map { it.fromDb() } }

    override fun observeToken(address: Solidity.Address): Flowable<ERC20Token> =
        erc20TokenDao.observeToken(address)
            .map { it.fromDb() }
            .subscribeOn(Schedulers.io())

    override fun loadTokens(): Single<List<ERC20Token>> =
        erc20TokenDao.loadTokens()
            .subscribeOn(Schedulers.io())
            .map { it.map { it.fromDb() } }

    override fun loadToken(address: Solidity.Address): Single<ERC20Token> =
        localToken(address)
            .flatMap { localToken ->
                localToken.toNullable()?.let {
                    Single.just(it)
                } ?: erc20TokenDao.loadToken(address)
                    .map { it.fromDb() }
                    .subscribeOn(Schedulers.io())
                    .onErrorResumeNext { loadTokenFromChain(address).firstOrError() }
            }

    override fun enableToken(token: ERC20Token): Completable =
        Completable.fromCallable {
            erc20TokenDao.insertERC20Token(token.toDb())
        }.subscribeOn(Schedulers.io())

    override fun disableToken(address: Solidity.Address): Completable =
        Completable.fromCallable {
            erc20TokenDao.deleteToken(address)
        }.subscribeOn(Schedulers.io())

    override fun loadVerifiedTokens(): Single<List<ERC20Token>> =
        verifiedTokensServiceApi.loadVerifiedTokenList().map { it.results.map { it.fromNetwork() } }

    private fun localToken(contractAddress: Solidity.Address): Single<Optional<ERC20Token>> =
        hardcodedTokens[contractAddress]?.let { Single.just(it.toOptional()) }
            ?: loadPaymentToken().map {
                if (it.address == contractAddress) it.toOptional()
                else None
            }

    private fun loadTokenFromChain(contractAddress: Solidity.Address): Observable<ERC20Token> {
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
        return ethereumRepository.request(bulk)
            .map {
                val name =
                    it.name.result()?.hexStringToByteArrayOrNull()?.utf8String()?.trim()
                        ?: throw IllegalArgumentException()
                val symbol =
                    it.symbol.result()?.hexStringToByteArrayOrNull()?.utf8String()?.trim()
                        ?: throw IllegalArgumentException()
                val decimals =
                    it.decimals.result()?.hexAsBigIntegerOrNull()?.toInt()
                        ?: throw IllegalArgumentException()
                ERC20Token(contractAddress, name, symbol, decimals, "")
            }
    }

    override fun loadTokenBalances(
        ofAddress: Solidity.Address,
        erC20Tokens: List<ERC20Token>
    ): Observable<List<Pair<ERC20Token, BigInteger?>>> {
        val requests =
            erC20Tokens.mapIndexed { index, token ->
                if (token.address == ETHER_TOKEN.address) {
                    MappedRequest(EthBalance(ofAddress, id = index)) {
                        token to it?.value
                    }
                } else {
                    MappedRequest(
                        EthCall(
                            transaction = Transaction(
                                token.address,
                                data = ERC20Contract.BalanceOf.encode(ofAddress)
                            ),
                            id = index
                        )
                    ) {
                        token to nullOnThrow {
                            ERC20Contract.BalanceOf.decode(it!!).balance.value
                        }
                    }
                }
            }.toList()

        return ethereumRepository.request(MappingBulkRequest(requests)).map { it.mapped() }
    }

    override fun loadPaymentToken(): Single<ERC20Token> =
        Single.fromCallable {
            preferencesManager.get(PREF_KEY).run {
                getString(KEY_CURRENT_PAYMENT_TOKEN_ADDRESS, null)?.let { tokenAddress ->
                    ERC20Token(
                        tokenAddress.asEthereumAddress()!!,
                        getString(KEY_CURRENT_PAYMENT_TOKEN_NAME, null)!!,
                        getString(KEY_CURRENT_PAYMENT_TOKEN_SYMBOL, null)!!,
                        getInt(KEY_CURRENT_PAYMENT_TOKEN_DECIMALS, 0),
                        getString(KEY_CURRENT_PAYMENT_TOKEN_LOGO, null) ?: ""
                    )
                } ?: ETHER_TOKEN
            }
        }
            .subscribeOn(Schedulers.io())

    override fun setPaymentToken(token: ERC20Token): Completable =
        Completable.fromAction {
            preferencesManager.get(PREF_KEY).run {
                edit {
                    putString(KEY_CURRENT_PAYMENT_TOKEN_ADDRESS, token.address.asEthereumAddressChecksumString())
                    putString(KEY_CURRENT_PAYMENT_TOKEN_NAME, token.name)
                    putString(KEY_CURRENT_PAYMENT_TOKEN_SYMBOL, token.symbol)
                    putInt(KEY_CURRENT_PAYMENT_TOKEN_DECIMALS, token.decimals)
                    putString(KEY_CURRENT_PAYMENT_TOKEN_LOGO, token.logoUrl)
                }
            }
        }
            .subscribeOn(Schedulers.io())

    override fun loadPaymentTokens(): Single<List<ERC20Token>> =
        tokenServiceApi.paymentTokens()
            .map { data ->
                data.results.mapTo(mutableListOf(ERC20Token.ETHER_TOKEN)) { it.fromNetwork() }
            }

    private class TokenInfoRequest(
        val name: EthRequest<String>,
        val symbol: EthRequest<String>,
        val decimals: EthRequest<String>
    ) : BulkRequest(name, symbol, decimals)

    companion object {
        private const val PREF_KEY: String = "TokenRepoPreferences"
        private const val KEY_CURRENT_PAYMENT_TOKEN_ADDRESS: String = "default_token_repo.string.current_payment_token_address"
        private const val KEY_CURRENT_PAYMENT_TOKEN_NAME: String = "default_token_repo.string.current_payment_token_name"
        private const val KEY_CURRENT_PAYMENT_TOKEN_SYMBOL: String = "default_token_repo.string.current_payment_token_symbol"
        private const val KEY_CURRENT_PAYMENT_TOKEN_DECIMALS: String = "default_token_repo.int.current_payment_token_decimals"
        private const val KEY_CURRENT_PAYMENT_TOKEN_LOGO: String = "default_token_repo.string.current_payment_token_logo"
    }
}
