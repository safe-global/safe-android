package pm.gnosis.heimdall.data.repositories.impls

import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.preferences.PreferencesToken
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.CreationEstimate
import pm.gnosis.heimdall.data.remote.models.CreationEstimatesParams
import pm.gnosis.heimdall.data.remote.models.EstimatesParams
import pm.gnosis.heimdall.data.remote.models.PaginatedResults
import pm.gnosis.heimdall.data.remote.models.tokens.TokenInfo
import pm.gnosis.heimdall.data.remote.models.tokens.fromNetwork
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.models.fromDb
import pm.gnosis.heimdall.data.repositories.models.toDb
import pm.gnosis.heimdall.data.repositories.toInt
import pm.gnosis.heimdall.helpers.TimeProvider
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.ERC20
import pm.gnosis.utils.*
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

typealias CacheEntry = Pair<Long, List<Pair<ERC20Token, BigInteger>>>

@Singleton
class DefaultTokenRepository @Inject constructor(
    appDb: ApplicationDb,
    private val ethereumRepository: EthereumRepository,
    private val prefs: PreferencesToken,
    private val relayServiceApi: RelayServiceApi,
    private val timeProvider: TimeProvider
) : TokenRepository {

    private val hardcodedTokens = mapOf(
        ETHER_TOKEN.address to ETHER_TOKEN
    )

    private val erc20TokenDao = appDb.erc20TokenDao()
    private val safeDao = appDb.gnosisSafeDao()

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

    override fun loadVerifiedTokens(filter: String): Single<List<ERC20Token>> =
        relayServiceApi.tokens(filter).map { resp -> resp.results.map { it.fromNetwork() } }

    private fun localToken(contractAddress: Solidity.Address): Single<Optional<ERC20Token>> =
        hardcodedTokens[contractAddress]?.let { Single.just(it.toOptional()) }
            ?: loadPaymentToken().map {
                if (it.address == contractAddress) it.toOptional()
                // TODO: query from safe info
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

    override fun loadPaymentToken(safe: Solidity.Address?): Single<ERC20Token> =
        (safe?.let { safeDao.loadSafeInfo(safe).subscribeOn(Schedulers.io()) } ?: Single.error(NoSuchElementException()))
            .map {
                ERC20Token(it.paymentTokenAddress, it.paymentTokenName, it.paymentTokenSymbol, it.paymentTokenDecimals, it.paymentTokenIcon ?: "")
            }
            .onErrorResumeNext { loadDefaultPaymentToken() }

    private fun loadDefaultPaymentToken() =
        Single.fromCallable {
            prefs.paymendToken
        }
            .subscribeOn(Schedulers.io())

    override fun setPaymentToken(safe: Solidity.Address?, token: ERC20Token): Completable =
        (safe?.let { safeDao.loadSafeInfo(safe).subscribeOn(Schedulers.io()) } ?: Single.error(NoSuchElementException()))
            .map {
                safeDao.insertSafeInfo(
                    it.copy(
                        paymentTokenAddress = token.address,
                        paymentTokenSymbol = token.symbol,
                        paymentTokenDecimals = token.decimals,
                        paymentTokenName = token.name,
                        paymentTokenIcon = token.logoUrl
                    )
                )
            }
            .ignoreElement()
            .onErrorResumeNext { setDefaultPaymentToken(token) }

    private fun setDefaultPaymentToken(token: ERC20Token) =
        Completable.fromAction {
            prefs.paymendToken = token
        }
            .subscribeOn(Schedulers.io())

    override fun loadPaymentTokens(): Single<List<ERC20Token>> =
        relayServiceApi.paymentTokens()
            .map { data ->
                data.results.mapTo(mutableListOf(ETHER_TOKEN)) { it.fromNetwork() }
            }

    private val creationFeeCache: MutableMap<Long, CacheEntry> = ConcurrentHashMap()

    private fun checkCreationFeeCache(numbersOwners: Long): Single<List<Pair<ERC20Token, BigInteger>>> =
        Single.fromCallable {
            creationFeeCache[numbersOwners]?.let { entry ->
                if (entry.first >= timeProvider.currentTimeMs())
                    entry.second
                else {
                    creationFeeCache.remove(numbersOwners)
                    null
                }
            } ?: throw NoSuchElementException()
        }
            .subscribeOn(Schedulers.io())

    private fun loadCreationFeeFromNetwork(numbersOwners: Long): Single<List<Pair<ERC20Token, BigInteger>>> =
        Single.zip<List<CreationEstimate>, PaginatedResults<TokenInfo>, List<Pair<ERC20Token, BigInteger>>>(
            relayServiceApi.creationEstimates(CreationEstimatesParams(numbersOwners)),
            relayServiceApi.paymentTokens(),
            BiFunction { estimates, paymentTokens ->
                val mappedTokens = paymentTokens.results.associate { it.address to it.fromNetwork() } + (ETHER_TOKEN.address to ETHER_TOKEN)
                estimates.mapNotNull {
                    val token = mappedTokens[it.paymentToken] ?: return@mapNotNull null
                    val payment = it.payment.decimalAsBigInteger()
                    token to payment
                }
            }
        )
            .doOnSuccess {
                creationFeeCache[numbersOwners] = (timeProvider.currentTimeMs() + CACHE_TIMEOUT_MS) to it
            }

    override fun loadPaymentTokensWithCreationFees(numbersOwners: Long): Single<List<Pair<ERC20Token, BigInteger>>> =
        checkCreationFeeCache(numbersOwners)
            .onErrorResumeNext { loadCreationFeeFromNetwork(numbersOwners) }


    override fun loadPaymentTokensWithTransactionFees(
        safe: Solidity.Address,
        transaction: SafeTransaction
    ): Single<List<Pair<ERC20Token, BigInteger>>> =
        Single.zip(
            relayServiceApi.transactionEstimates(
                safe.asEthereumAddressChecksumString(),
                EstimatesParams(
                    transaction.wrapped.address.asEthereumAddressChecksumString(),
                    transaction.wrapped.value?.value?.asDecimalString() ?: "0",
                    transaction.wrapped.data ?: "0x",
                    transaction.operation.toInt()
                )
            ),
            relayServiceApi.paymentTokens(),
            BiFunction { estimatesData, paymentTokens ->
                val mappedTokens = paymentTokens.results.associate { it.address to it.fromNetwork() } + (ETHER_TOKEN.address to ETHER_TOKEN)
                estimatesData.estimations.mapNotNull {
                    val token = mappedTokens[it.gasToken] ?: return@mapNotNull null
                    val txGas = estimatesData.safeTxGas.decimalAsBigInteger()
                    val operationalGas = estimatesData.operationalGas.decimalAsBigInteger()
                    val baseGas = it.baseGas.decimalAsBigInteger()
                    val gasPrice = it.gasPrice.decimalAsBigInteger()
                    token to (txGas + operationalGas + baseGas) * gasPrice
                }
            }
        )

    private class TokenInfoRequest(
        val name: EthRequest<String>,
        val symbol: EthRequest<String>,
        val decimals: EthRequest<String>
    ) : BulkRequest(name, symbol, decimals)

    companion object {
        private const val CACHE_TIMEOUT_MS = 5 * 60 * 1000 // 5 Minutes
    }
}
