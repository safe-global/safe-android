package pm.gnosis.heimdall.data.repositories.impls

import android.app.Application
import android.content.res.Resources
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Predicate
import io.reactivex.observers.TestObserver
import io.reactivex.processors.PublishProcessor
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.reset
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.ERC20TokenDao
import pm.gnosis.heimdall.data.db.models.ERC20TokenDb
import pm.gnosis.heimdall.data.remote.*
import pm.gnosis.heimdall.data.remote.models.JsonRpcRequest
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ERC20
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import java.math.BigInteger
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class DefaultTokenRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var repository: DefaultTokenRepository
    private lateinit var preferencesManager: PreferencesManager

    @Mock
    private lateinit var application: Application

    @Mock
    lateinit var dbMock: ApplicationDb

    @Mock
    lateinit var erc20DaoMock: ERC20TokenDao

    @Mock
    lateinit var jsonRpcMock: EthereumJsonRpcRepository

    @Mock
    lateinit var resourcesMock: Resources

    private val preferences = TestPreferences()

    @Before
    fun setUp() {
        given(application.getSharedPreferences(anyString(), anyInt())).willReturn(preferences)
        given(dbMock.erc20TokenDao()).willReturn(erc20DaoMock)
        preferencesManager = PreferencesManager(application)
        repository = DefaultTokenRepository(dbMock, jsonRpcMock, preferencesManager, Moshi.Builder().add(HexNumberAdapter()).build(), application)
    }

    @Test
    fun setupAddTokens() {
        given(resourcesMock.openRawResource(R.raw.verified_tokens)).willReturn(javaClass.getResourceAsStream("/verified_tokens.json"))
        given(application.resources).willReturn(resourcesMock)
        val testObserver = TestObserver<Unit>()
        // Should be initial start
        preferences.clear().putBoolean(PREFS_TOKEN_SETUP, false)
        repository.setup().subscribe(testObserver)
        testObserver.assertResult()

        then(erc20DaoMock).should().insertERC20Tokens(
            listOf(
                ERC20TokenDb(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
                ),
                ERC20TokenDb(
                    "0x9d3de1be7309764824211f9e4219e01a5f223d99".hexAsBigInteger(), "Love", "<3", 6, true
                )
            )
        )
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
        assertEquals("Token setup should be marked as done", true, preferences.getBoolean(PREFS_TOKEN_SETUP, false))
    }

    @Test
    fun setupTokensAlreadyAdded() {
        val testObserver = TestObserver<Unit>()
        // Should be initial start
        preferences.clear().putBoolean(PREFS_TOKEN_SETUP, true)
        repository.setup().subscribe(testObserver)
        testObserver.assertResult()
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
        assertEquals("Token setup should be marked as done", true, preferences.getBoolean(PREFS_TOKEN_SETUP, false))
    }

    @Test
    fun setupErrorAddingTokens() {
        given(resourcesMock.openRawResource(R.raw.verified_tokens)).willReturn(javaClass.getResourceAsStream("/invalid_verified_tokens.json"))
        given(application.resources).willReturn(resourcesMock)
        val testObserver = TestObserver<Unit>()
        // Should be initial start
        preferences.clear().putBoolean(PREFS_TOKEN_SETUP, false)
        repository.setup().subscribe(testObserver)
        //testObserver.assertError(KotlinNullPointerException::class.java)
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
        assertEquals("Token setup should not be marked as done", false, preferences.getBoolean(PREFS_TOKEN_SETUP, false))
    }

    @Test
    fun observeTokens() {
        val testProcessor = PublishProcessor.create<List<ERC20TokenDb>>()
        given(erc20DaoMock.observeTokens()).willReturn(testProcessor)

        val testSubscriber = TestSubscriber<List<ERC20Token>>()
        repository.observeTokens().subscribe(testSubscriber)

        testSubscriber.assertEmpty()

        testProcessor.offer(
            listOf(
                ERC20TokenDb(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
                ),
                ERC20TokenDb(
                    "0x9d3de1be7309764824211f9e4219e01a5f223d99".hexAsBigInteger(), "Love", "<3", 6, true
                )
            )
        )

        val initialTokenList = listOf(
            ERC20Token(
                "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
            ),
            ERC20Token(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".hexAsBigInteger(), "Love", "<3", 6, true
            )
        )
        testSubscriber.assertValuesOnly(initialTokenList)

        testProcessor.offer(
            listOf(
                ERC20TokenDb(
                    "0x9d3de1be7309764824211f9e4219e01a5f223d99".hexAsBigInteger(), "Love", "<3", 6, true
                ),
                ERC20TokenDb(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
                )
            )
        )

        testSubscriber.assertValuesOnly(
            initialTokenList,
            listOf(
                ERC20Token(
                    "0x9d3de1be7309764824211f9e4219e01a5f223d99".hexAsBigInteger(), "Love", "<3", 6, true
                ),
                ERC20Token(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
                )
            )
        )

        then(erc20DaoMock).should().observeTokens()
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeToken() {
        val testProcessor = PublishProcessor.create<ERC20TokenDb>()
        given(erc20DaoMock.observeToken(MockUtils.any())).willReturn(testProcessor)

        val testSubscriber = TestSubscriber<ERC20Token>()
        repository.observeToken(BigInteger.TEN).subscribe(testSubscriber)

        testSubscriber.assertEmpty()

        testProcessor.offer(
            ERC20TokenDb(
                "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
            )
        )

        val initialToken =
            ERC20Token(
                "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
            )

        testSubscriber.assertValuesOnly(initialToken)

        testProcessor.offer(
            ERC20TokenDb(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".hexAsBigInteger(), "Love", "<3", 6, true
            )
        )

        testSubscriber.assertValuesOnly(
            initialToken,
            ERC20Token(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".hexAsBigInteger(), "Love", "<3", 6, true
            )
        )

        then(erc20DaoMock).should().observeToken(BigInteger.TEN)
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTokens() {
        given(erc20DaoMock.loadTokens()).willReturn(
            Single.just(
                listOf(
                    ERC20TokenDb(
                        "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
                    ),
                    ERC20TokenDb(
                        "0x9d3de1be7309764824211f9e4219e01a5f223d99".hexAsBigInteger(), "Love", "<3", 6, true
                    )
                )
            )
        )

        val testObserver = TestObserver<List<ERC20Token>>()
        repository.loadTokens().subscribe(testObserver)

        val initialTokenList = listOf(
            ERC20Token(
                "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
            ),
            ERC20Token(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".hexAsBigInteger(), "Love", "<3", 6, true
            )
        )
        testObserver.assertResult(initialTokenList)

        then(erc20DaoMock).should().loadTokens()
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadToken() {
        given(erc20DaoMock.loadToken(MockUtils.any())).willReturn(
            Single.just(
                ERC20TokenDb(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
                )
            )
        )

        val testObserver = TestObserver<ERC20Token>()
        repository.loadToken(BigInteger.TEN).subscribe(testObserver)

        val initialToken = ERC20Token(
            "0x826921230178969e9142acdfb9bd2f57330ede18".hexAsBigInteger(), "World Energy", "WE", 4, true
        )
        testObserver.assertResult(initialToken)

        then(erc20DaoMock).should().loadToken(BigInteger.TEN)
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    private fun Result<String>.toRpcResult(id: Int) =
        when (this) {
            is DataResult -> JsonRpcResult(id, "", null, this.data)
            is ErrorResult -> JsonRpcResult(id, "", JsonRpcError(7, this.error.message ?: ""), "0x0")
        }

    private fun testLoadTokenInfo(
        expectedResult: Result<ERC20Token>,
        nameResult: Result<String>,
        symbolResult: Result<String>,
        decimalResult: Result<String>
    ) {
        given(jsonRpcMock.bulk<BulkRequest>(MockUtils.any())).will {
            val request = it.arguments.first() as BulkRequest
            assertEquals(
                request.body(), listOf(
                    TransactionCallParams(to = BigInteger.TEN.asEthereumAddressString(), data = "0x${ERC20.NAME_METHOD_ID}").callRequest(0),
                    TransactionCallParams(to = BigInteger.TEN.asEthereumAddressString(), data = "0x${ERC20.SYMBOL_METHOD_ID}").callRequest(1),
                    TransactionCallParams(to = BigInteger.TEN.asEthereumAddressString(), data = "0x${ERC20.DECIMALS_METHOD_ID}").callRequest(2)
                )
            )
            try {
                request.parse(
                    listOf(
                        nameResult.toRpcResult(0),
                        symbolResult.toRpcResult(1),
                        decimalResult.toRpcResult(2)
                    )
                )
                Observable.just(request)
            } catch (t: Throwable) {
                Observable.error<BulkRequest>(t)
            }
        }

        val testObserver = TestObserver<ERC20Token>()
        repository.loadTokenInfo(BigInteger.TEN).subscribe(testObserver)
        expectedResult.handle({
            testObserver.assertResult(it)
        }, { error ->
            testObserver.assertFailure(Predicate { error.javaClass.isInstance(it) && it.message == error.message })
        })
        reset(jsonRpcMock)
    }

    @Test
    fun loadTokenInfo() {
        testLoadTokenInfo(
            DataResult(ERC20Token(BigInteger.TEN, "Hello Token", "HT", 10)), // Expected result
            DataResult("Hello Token".toByteArray().toHexString()),
            DataResult("HT".toByteArray().toHexString()),
            DataResult(BigInteger.TEN.toString(16))
        )
        testLoadTokenInfo(
            DataResult(ERC20Token(BigInteger.TEN, "Hello Token", "HT", 0)), // Expected result
            DataResult("Hello Token".toByteArray().toHexString()),
            DataResult("HT".toByteArray().toHexString()),
            DataResult("NotANumber")
        )
        testLoadTokenInfo(
            ErrorResult(ErrorResultException("revert")), // Expected result
            ErrorResult(Exception("revert")),
            DataResult("HT".toByteArray().toHexString()),
            DataResult(BigInteger.TEN.toString(16))
        )
        testLoadTokenInfo(
            ErrorResult(ErrorResultException("revert")), // Expected result
            DataResult("Hello Token".toByteArray().toHexString()),
            ErrorResult(Exception("revert")),
            DataResult(BigInteger.TEN.toString(16))
        )
        testLoadTokenInfo(
            ErrorResult(ErrorResultException("revert")), // Expected result
            DataResult("Hello Token".toByteArray().toHexString()),
            DataResult("HT".toByteArray().toHexString()),
            ErrorResult(Exception("revert"))
        )
    }

    private fun testLoadTokenBalance(
        input: List<ERC20Token>,
        requests: List<JsonRpcRequest>,
        results: List<DataResult<String>>,
        outputs: List<Pair<ERC20Token, BigInteger?>>
    ) {
        given(jsonRpcMock.bulk<BulkRequest>(MockUtils.any())).will {
            val request = it.arguments.first() as BulkRequest
            assertEquals(request.body(), requests)
            request.parse(results.mapIndexed { index, dataResult -> dataResult.toRpcResult(index) })
            Observable.just(request)
        }

        val testObserver = TestObserver<List<Pair<ERC20Token, BigInteger?>>>()
        repository.loadTokenBalances(BigInteger.TEN, input).subscribe(testObserver)
        testObserver.assertResult(outputs)
        reset(jsonRpcMock)
    }

    @Test
    fun loadTokenBalances() {
        testLoadTokenBalance(
            listOf(),
            listOf(),
            listOf(),
            listOf()
        )
        testLoadTokenBalance(
            listOf(ERC20Token.ETHER_TOKEN, TEST_TOKEN),
            listOf(
                JsonRpcRequest(
                    id = 0,
                    method = EthereumJsonRpcRepository.FUNCTION_GET_BALANCE,
                    params = arrayListOf(BigInteger.TEN.asEthereumAddressString(), EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)
                ),
                TransactionCallParams(
                    to = BigInteger.ONE.asEthereumAddressString(),
                    data = StandardToken.BalanceOf.encode(Solidity.Address(BigInteger.TEN))
                ).callRequest(1)
            ),
            listOf(
                DataResult("0x0f"), DataResult("0x000000000000000000000000000000000000000000000000000000000000000a")
            ),
            listOf(ERC20Token.ETHER_TOKEN to BigInteger.valueOf(15), TEST_TOKEN to BigInteger.valueOf(10))
        )
        testLoadTokenBalance(
            listOf(TEST_TOKEN, ERC20Token.ETHER_TOKEN),
            listOf(
                TransactionCallParams(
                    to = BigInteger.ONE.asEthereumAddressString(),
                    data = StandardToken.BalanceOf.encode(Solidity.Address(BigInteger.TEN))
                ).callRequest(0),
                JsonRpcRequest(
                    id = 1,
                    method = EthereumJsonRpcRepository.FUNCTION_GET_BALANCE,
                    params = arrayListOf(BigInteger.TEN.asEthereumAddressString(), EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)
                )
            ),
            listOf(
                DataResult("invalid balance"), DataResult("0x0f")
            ),
            listOf(TEST_TOKEN to null, ERC20Token.ETHER_TOKEN to BigInteger.valueOf(15))
        )

        val invalidTargetObserver = TestObserver<List<Pair<ERC20Token, BigInteger?>>>()
        repository.loadTokenBalances(BigInteger("10000000000000000000000000000000000000000", 16), emptyList()).subscribe(invalidTargetObserver)
        invalidTargetObserver.assertFailure(Predicate { it is InvalidAddressException })

        given(jsonRpcMock.bulk<BulkRequest>(MockUtils.any())).willReturn(Observable.error(UnknownHostException()))
        val networkErrorObserver = TestObserver<List<Pair<ERC20Token, BigInteger?>>>()
        repository.loadTokenBalances(BigInteger.TEN, emptyList()).subscribe(networkErrorObserver)
        networkErrorObserver.assertFailure(Predicate { it is UnknownHostException })
    }

    @Test
    fun addToken() {
        val testObserver = TestObserver<Unit>()
        repository.addToken(TEST_TOKEN).subscribe(testObserver)
        testObserver.assertResult()
        then(erc20DaoMock).should().insertERC20Token(ERC20TokenDb(BigInteger.ONE, "Hello Token", "HT", 10, false))
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removeToken() {
        val testObserver = TestObserver<Unit>()
        repository.removeToken(BigInteger.ONE).subscribe(testObserver)
        testObserver.assertResult()
        then(erc20DaoMock).should().deleteToken(BigInteger.ONE)
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    class HexNumberAdapter {
        @ToJson
        fun toJson(hexNumber: BigInteger): String {
            return StringBuilder("0x").append(hexNumber.toString(16)).toString()
        }

        @FromJson
        fun fromJson(hexNumber: String): BigInteger {
            return hexNumber.hexAsBigInteger()
        }
    }

    companion object {
        val PREFS_TOKEN_SETUP = "prefs.boolean.finished_tokens_setup"
        val TEST_TOKEN = ERC20Token(BigInteger.ONE, "Hello Token", "HT", 10)
    }
}
