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
import org.junit.Assert.assertTrue
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
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.data.adapters.SolidityAddressAdapter
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.ERC20TokenDao
import pm.gnosis.heimdall.data.db.models.ERC20TokenDb
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ERC20
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.asEthereumAddress
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
    lateinit var ethereumRepositoryMock: EthereumRepository

    @Mock
    lateinit var resourcesMock: Resources

    private val preferences = TestPreferences()

    @Before
    fun setUp() {
        given(application.getSharedPreferences(anyString(), anyInt())).willReturn(preferences)
        given(dbMock.erc20TokenDao()).willReturn(erc20DaoMock)
        preferencesManager = PreferencesManager(application)
        repository = DefaultTokenRepository(
            dbMock,
            ethereumRepositoryMock,
            preferencesManager,
            Moshi.Builder().add(HexNumberAdapter()).add(SolidityAddressAdapter()).build(),
            application
        )
    }

    @Test
    fun setupAddTokens() {
        given(resourcesMock.openRawResource(R.raw.verified_tokens)).willReturn(
            javaClass.getResourceAsStream(
                "/verified_tokens.json"
            )
        )
        given(application.resources).willReturn(resourcesMock)
        val testObserver = TestObserver<Unit>()
        // Should be initial start
        preferences.clear().putBoolean(PREFS_TOKEN_SETUP, false)
        repository.setup().subscribe(testObserver)
        testObserver.assertResult()

        then(erc20DaoMock).should().insertERC20Tokens(
            listOf(
                ERC20TokenDb(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                    "World Energy",
                    "WE",
                    4,
                    true
                ),
                ERC20TokenDb(
                    "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                    "Love",
                    "<3",
                    6,
                    true
                )
            )
        )
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
        assertEquals(
            "Token setup should be marked as done",
            true,
            preferences.getBoolean(PREFS_TOKEN_SETUP, false)
        )
    }

    @Test
    fun setupTokensAlreadyAdded() {
        val testObserver = TestObserver<Unit>()
        // Should be initial start
        preferences.clear().putBoolean(PREFS_TOKEN_SETUP, true)
        repository.setup().subscribe(testObserver)
        testObserver.assertResult()
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
        assertEquals(
            "Token setup should be marked as done",
            true,
            preferences.getBoolean(PREFS_TOKEN_SETUP, false)
        )
    }

    @Test
    fun setupErrorAddingTokens() {
        given(resourcesMock.openRawResource(R.raw.verified_tokens)).willReturn(
            javaClass.getResourceAsStream(
                "/invalid_verified_tokens.json"
            )
        )
        given(application.resources).willReturn(resourcesMock)
        val testObserver = TestObserver<Unit>()
        // Should be initial start
        preferences.clear().putBoolean(PREFS_TOKEN_SETUP, false)
        repository.setup().subscribe(testObserver)
        //testObserver.assertError(KotlinNullPointerException::class.java)
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
        assertEquals(
            "Token setup should not be marked as done",
            false,
            preferences.getBoolean(PREFS_TOKEN_SETUP, false)
        )
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
                    "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                    "World Energy",
                    "WE",
                    4,
                    true
                ),
                ERC20TokenDb(
                    "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                    "Love",
                    "<3",
                    6,
                    true
                )
            )
        )

        val initialTokenList = listOf(
            ERC20Token(
                "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                "World Energy",
                "WE",
                4,
                true
            ),
            ERC20Token(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                "Love",
                "<3",
                6,
                true
            )
        )
        testSubscriber.assertValuesOnly(initialTokenList)

        testProcessor.offer(
            listOf(
                ERC20TokenDb(
                    "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                    "Love",
                    "<3",
                    6,
                    true
                ),
                ERC20TokenDb(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                    "World Energy",
                    "WE",
                    4,
                    true
                )
            )
        )

        testSubscriber.assertValuesOnly(
            initialTokenList,
            listOf(
                ERC20Token(
                    "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                    "Love",
                    "<3",
                    6,
                    true
                ),
                ERC20Token(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                    "World Energy",
                    "WE",
                    4,
                    true
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
        repository.observeToken(Solidity.Address(BigInteger.TEN)).subscribe(testSubscriber)

        testSubscriber.assertEmpty()

        testProcessor.offer(
            ERC20TokenDb(
                "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                "World Energy",
                "WE",
                4,
                true
            )
        )

        val initialToken =
            ERC20Token(
                "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                "World Energy",
                "WE",
                4,
                true
            )

        testSubscriber.assertValuesOnly(initialToken)

        testProcessor.offer(
            ERC20TokenDb(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                "Love",
                "<3",
                6,
                true
            )
        )

        testSubscriber.assertValuesOnly(
            initialToken,
            ERC20Token(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                "Love",
                "<3",
                6,
                true
            )
        )

        then(erc20DaoMock).should().observeToken(Solidity.Address(BigInteger.TEN))
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadTokens() {
        given(erc20DaoMock.loadTokens()).willReturn(
            Single.just(
                listOf(
                    ERC20TokenDb(
                        "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                        "World Energy",
                        "WE",
                        4,
                        true
                    ),
                    ERC20TokenDb(
                        "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                        "Love",
                        "<3",
                        6,
                        true
                    )
                )
            )
        )

        val testObserver = TestObserver<List<ERC20Token>>()
        repository.loadTokens().subscribe(testObserver)

        val initialTokenList = listOf(
            ERC20Token(
                "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                "World Energy",
                "WE",
                4,
                true
            ),
            ERC20Token(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                "Love",
                "<3",
                6,
                true
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
                    "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                    "World Energy",
                    "WE",
                    4,
                    true
                )
            )
        )

        val testObserver = TestObserver<ERC20Token>()
        repository.loadToken(Solidity.Address(BigInteger.TEN)).subscribe(testObserver)

        val initialToken = ERC20Token(
            "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
            "World Energy",
            "WE",
            4,
            true
        )
        testObserver.assertResult(initialToken)

        then(erc20DaoMock).should().loadToken(Solidity.Address(BigInteger.TEN))
        then(erc20DaoMock).shouldHaveNoMoreInteractions()

        val etherObserver = TestObserver<ERC20Token>()
        repository.loadToken(ERC20Token.ETHER_TOKEN.address).subscribe(etherObserver)
        etherObserver.assertResult(ERC20Token.ETHER_TOKEN)

        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    private fun testLoadTokenInfo(
        expectedResult: Result<ERC20Token>,
        nameResult: EthRequest.Response<String>,
        symbolResult: EthRequest.Response<String>,
        decimalResult: EthRequest.Response<String>
    ) {
        given(erc20DaoMock.loadToken(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).will {
            val bulk = it.arguments.first() as BulkRequest
            val requests = bulk.requests
            assertEqualRequests(
                requests,
                listOf(
                    EthCall(
                        transaction = Transaction(
                            Solidity.Address(BigInteger.TEN),
                            data = "0x${ERC20.NAME_METHOD_ID}"
                        ), id = 0
                    ),

                    EthCall(
                        transaction = Transaction(
                            Solidity.Address(BigInteger.TEN),
                            data = "0x${ERC20.SYMBOL_METHOD_ID}"
                        ), id = 1
                    ),

                    EthCall(
                        transaction = Transaction(
                            Solidity.Address(BigInteger.TEN),
                            data = "0x${ERC20.DECIMALS_METHOD_ID}"
                        ), id = 2
                    )
                )
            )
            try {
                (requests[0] as EthCall).response = nameResult
                (requests[1] as EthCall).response = symbolResult
                (requests[2] as EthCall).response = decimalResult
                Observable.just(bulk)
            } catch (t: Throwable) {
                Observable.error<List<EthRequest<*>>>(t)
            }
        }

        val testObserver = TestObserver<ERC20Token>()
        repository.loadToken(Solidity.Address(BigInteger.TEN)).subscribe(testObserver)
        expectedResult.handle({
            testObserver.assertResult(it)
        }, { error ->
            testObserver.assertFailure(Predicate { error.javaClass.isInstance(it) && it.message == error.message })
        })
        reset(ethereumRepositoryMock)
    }

    @Test
    fun loadTokenInfo() {
        testLoadTokenInfo(
            DataResult(ERC20Token(Solidity.Address(BigInteger.TEN), "Hello Token", "HT", 10)), // Expected result
            EthRequest.Response.Success("Hello Token".toByteArray().toHexString()),
            EthRequest.Response.Success("HT".toByteArray().toHexString()),
            EthRequest.Response.Success(BigInteger.TEN.toHexString())
        )
        testLoadTokenInfo(
            ErrorResult(IllegalArgumentException()), // Expected result
            EthRequest.Response.Success("Hello Token".toByteArray().toHexString()),
            EthRequest.Response.Success("HT".toByteArray().toHexString()),
            EthRequest.Response.Success("NotANumber")
        )
        testLoadTokenInfo(
            ErrorResult(IllegalArgumentException()), // Expected result
            EthRequest.Response.Failure("revert"),
            EthRequest.Response.Success("HT".toByteArray().toHexString()),
            EthRequest.Response.Success(BigInteger.TEN.toString(16))
        )
        testLoadTokenInfo(
            ErrorResult(IllegalArgumentException()), // Expected result
            EthRequest.Response.Success("Hello Token".toByteArray().toHexString()),
            EthRequest.Response.Failure("revert"),
            EthRequest.Response.Success(BigInteger.TEN.toString(16))
        )
        testLoadTokenInfo(
            ErrorResult(IllegalArgumentException()), // Expected result
            EthRequest.Response.Success("Hello Token".toByteArray().toHexString()),
            EthRequest.Response.Success("HT".toByteArray().toHexString()),
            EthRequest.Response.Failure("revert")
        )
    }

    private fun testLoadTokenBalance(
        input: List<ERC20Token>,
        expectedRequests: List<EthRequest<*>>,
        expectedResults: List<EthRequest.Response<*>>,
        outputs: List<Pair<ERC20Token, BigInteger?>>
    ) {
        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).will {
            val bulk = it.arguments.first() as BulkRequest
            val requests = bulk.requests
            assertEqualRequests(expectedRequests, requests)
            requests.forEachIndexed { index, request ->
                when (request) {
                    is EthBalance -> request.response =
                            expectedResults[index] as EthRequest.Response<Wei>
                    is EthCall -> request.response =
                            expectedResults[index] as EthRequest.Response<String>
                    else ->
                        throw UnsupportedOperationException()
                }
            }
            Observable.just(bulk)
        }

        val testObserver = TestObserver<List<Pair<ERC20Token, BigInteger?>>>()
        repository.loadTokenBalances(Solidity.Address(BigInteger.TEN), input).subscribe(testObserver)
        testObserver.assertResult(outputs)
        reset(ethereumRepositoryMock)
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
                EthBalance(Solidity.Address(BigInteger.TEN), id = 0),
                EthCall(
                    transaction = Transaction(
                        Solidity.Address(BigInteger.ONE),
                        data = ERC20Contract.BalanceOf.encode(Solidity.Address(BigInteger.TEN))
                    ), id = 1
                )
            ),
            listOf(
                EthRequest.Response.Success(Wei(BigInteger.valueOf(15))),
                EthRequest.Response.Success("0x000000000000000000000000000000000000000000000000000000000000000a")
            ),
            listOf(
                ERC20Token.ETHER_TOKEN to BigInteger.valueOf(15),
                TEST_TOKEN to BigInteger.valueOf(10)
            )
        )
        testLoadTokenBalance(
            listOf(TEST_TOKEN, ERC20Token.ETHER_TOKEN),
            listOf(
                EthCall(
                    transaction = Transaction(
                        Solidity.Address(BigInteger.ONE),
                        data = ERC20Contract.BalanceOf.encode(Solidity.Address(BigInteger.TEN))
                    ), id = 0
                ),
                EthBalance(Solidity.Address(BigInteger.TEN), id = 1)
            ),
            listOf(
                EthRequest.Response.Success("invalid balance"),
                EthRequest.Response.Success(Wei(BigInteger.valueOf(15)))
            ),
            listOf(TEST_TOKEN to null, ERC20Token.ETHER_TOKEN to BigInteger.valueOf(15))
        )

        given(ethereumRepositoryMock.request(MockUtils.any<BulkRequest>())).willReturn(
            Observable.error(
                UnknownHostException()
            )
        )
        val networkErrorObserver = TestObserver<List<Pair<ERC20Token, BigInteger?>>>()
        repository.loadTokenBalances(Solidity.Address(BigInteger.TEN), emptyList()).subscribe(networkErrorObserver)
        networkErrorObserver.assertFailure(Predicate { it is UnknownHostException })
    }

    @Test
    fun addToken() {
        val testObserver = TestObserver<Unit>()
        repository.addToken(TEST_TOKEN).subscribe(testObserver)
        testObserver.assertResult()
        then(erc20DaoMock).should()
            .insertERC20Token(ERC20TokenDb(Solidity.Address(BigInteger.ONE), "Hello Token", "HT", 10, false))
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun removeToken() {
        val testObserver = TestObserver<Unit>()
        repository.removeToken(Solidity.Address(BigInteger.ONE)).subscribe(testObserver)
        testObserver.assertResult()
        then(erc20DaoMock).should().deleteToken(Solidity.Address(BigInteger.ONE))
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

    private fun assertEqualRequests(expected: List<EthRequest<*>>, actual: List<EthRequest<*>>) {
        assertEquals("Different request count", expected.size, actual.size)
        expected.forEachIndexed { index, request ->
            actual[index].let {
                assertTrue("Different request #$index", request.check(it))
            }
        }
    }

    private fun EthRequest<*>.check(other: EthRequest<*>): Boolean {
        if (this == other) return true
        if (id != other.id) return false
        if (response != other.response) return false
        when (this) {
            is EthBalance -> {
                if (other !is EthBalance) return false
                return this.address == other.address
            }
            is EthCall -> {
                if (other !is EthCall) return false
                if (this.transaction != other.transaction) return false
                return this.from == other.from
            }
            else ->
                throw UnsupportedOperationException()
        }
    }

    companion object {
        const val PREFS_TOKEN_SETUP = "prefs.boolean.finished_tokens_setup"
        val TEST_TOKEN = ERC20Token(Solidity.Address(BigInteger.ONE), "Hello Token", "HT", 10)
    }
}
