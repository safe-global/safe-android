package pm.gnosis.heimdall.data.repositories.impls

import android.app.Application
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
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.reset
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.ERC20TokenDao
import pm.gnosis.heimdall.data.db.models.ERC20TokenDb
import pm.gnosis.heimdall.data.preferences.PreferencesToken
import pm.gnosis.heimdall.data.remote.TokenServiceApi
import pm.gnosis.heimdall.data.remote.models.PaginatedResults
import pm.gnosis.heimdall.data.remote.models.tokens.TokenInfo
import pm.gnosis.heimdall.data.remote.models.tokens.fromNetwork
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ERC20
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestPreferences
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHexString
import java.math.BigInteger
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class DefaultTokenRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var applicationMock: Application

    @Mock
    private lateinit var dbMock: ApplicationDb

    @Mock
    private lateinit var erc20DaoMock: ERC20TokenDao

    @Mock
    private lateinit var ethereumRepositoryMock: EthereumRepository

    @Mock
    private lateinit var tokenServiceApiMock: TokenServiceApi

    private val testPreferences = TestPreferences()

    private lateinit var repository: DefaultTokenRepository

    private lateinit var tokenPrefs: PreferencesToken


    @Before
    fun setUp() {
        BDDMockito.given(applicationMock.getSharedPreferences(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt())).willReturn(testPreferences)
        tokenPrefs = PreferencesToken(applicationMock)

        given(dbMock.erc20TokenDao()).willReturn(erc20DaoMock)
        repository = DefaultTokenRepository(
            dbMock,
            ethereumRepositoryMock,
            tokenPrefs,
            tokenServiceApiMock
        )
    }

    @Test
    fun observeTokens() {
        val testProcessor = PublishProcessor.create<List<ERC20TokenDb>>()
        given(erc20DaoMock.observeTokens()).willReturn(testProcessor)

        val testSubscriber = TestSubscriber<List<ERC20Token>>()
        repository.observeEnabledTokens().subscribe(testSubscriber)

        testSubscriber.assertEmpty()

        testProcessor.offer(
            listOf(
                ERC20TokenDb(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                    "World Energy",
                    "WE",
                    4,
                    ""
                ),
                ERC20TokenDb(
                    "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                    "Love",
                    "<3",
                    6,
                    ""
                )
            )
        )

        val initialTokenList = listOf(
            ERC20Token(
                "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                "World Energy",
                "WE",
                4,
                ""
            ),
            ERC20Token(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                "Love",
                "<3",
                6,
                ""
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
                    ""
                ),
                ERC20TokenDb(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                    "World Energy",
                    "WE",
                    4,
                    ""
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
                    ""
                ),
                ERC20Token(
                    "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                    "World Energy",
                    "WE",
                    4,
                    ""
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
                ""
            )
        )

        val initialToken =
            ERC20Token(
                "0x826921230178969e9142acdfb9bd2f57330ede18".asEthereumAddress()!!,
                "World Energy",
                "WE",
                4,
                ""
            )

        testSubscriber.assertValuesOnly(initialToken)

        testProcessor.offer(
            ERC20TokenDb(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                "Love",
                "<3",
                6,
                ""
            )
        )

        testSubscriber.assertValuesOnly(
            initialToken,
            ERC20Token(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                "Love",
                "<3",
                6,
                ""
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
                        ""
                    ),
                    ERC20TokenDb(
                        "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                        "Love",
                        "<3",
                        6,
                        ""
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
                ""
            ),
            ERC20Token(
                "0x9d3de1be7309764824211f9e4219e01a5f223d99".asEthereumAddress()!!,
                "Love",
                "<3",
                6,
                ""
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
                    ""
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
            ""
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
    fun enableToken() {
        val testObserver = TestObserver<Unit>()
        repository.enableToken(TEST_TOKEN).subscribe(testObserver)
        testObserver.assertResult()
        then(erc20DaoMock).should()
            .insertERC20Token(ERC20TokenDb(Solidity.Address(BigInteger.ONE), "Hello Token", "HT", 10, ""))
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun disableToken() {
        val testObserver = TestObserver<Unit>()
        repository.disableToken(Solidity.Address(BigInteger.ONE)).subscribe(testObserver)
        testObserver.assertResult()
        then(erc20DaoMock).should().deleteToken(Solidity.Address(BigInteger.ONE))
        then(erc20DaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadVerifiedTokens() {
        val testObserver = TestObserver<List<ERC20Token>>()
        val verifiedToken = TokenInfo(
            address = Solidity.Address(BigInteger.ZERO),
            name = "Test Token",
            symbol = "TST",
            decimals = 18,
            logoUri = ""
        )
        val verifiedTokensList = PaginatedResults(listOf(verifiedToken))
        given(tokenServiceApiMock.tokens()).willReturn(Single.just(verifiedTokensList))

        repository.loadVerifiedTokens().subscribe(testObserver)

        testObserver.assertResult(listOf(verifiedToken.fromNetwork()))
        then(tokenServiceApiMock).should().tokens()
        then(tokenServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadVerifiedTokensError() {
        val testObserver = TestObserver<List<ERC20Token>>()
        val exception = Exception()
        given(tokenServiceApiMock.tokens()).willReturn(Single.error(exception))

        repository.loadVerifiedTokens().subscribe(testObserver)

        testObserver.assertError(exception)
        then(tokenServiceApiMock).should().tokens()
        then(tokenServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadPaymentTokenDefault() {
        val testObserver = TestObserver<ERC20Token>()

        repository.loadPaymentToken().subscribe(testObserver)

        testObserver.assertResult(ERC20Token.ETHER_TOKEN)
    }

    @Test
    fun setAndloadPaymentToken() {
        val setObserver = TestObserver<Unit>()
        repository.setPaymentToken(TEST_TOKEN).subscribe(setObserver)
        setObserver.assertResult()

        val token = tokenPrefs.paymendToken

        assertEquals(
            "Should store token address",
            TEST_TOKEN.address.asEthereumAddressChecksumString(),
            token.address.asEthereumAddressChecksumString()
        )

        assertEquals(
            "Should store token name",
            TEST_TOKEN.name,
            token.name
        )

        assertEquals(
            "Should store token symbol",
            TEST_TOKEN.symbol,
            token.symbol
        )

        assertEquals(
            "Should store token decimals",
            TEST_TOKEN.decimals,
            token.decimals
        )

        assertEquals(
            "Should store token logo url",
            TEST_TOKEN.logoUrl,
            token.logoUrl
        )

        val loadObserver = TestObserver<ERC20Token>()
        repository.loadPaymentToken().subscribe(loadObserver)
        loadObserver.assertResult(TEST_TOKEN)
    }

    @Test
    fun loadPaymentTokens() {
        val testObserver = TestObserver<List<ERC20Token>>()
        val tokenInfo = TokenInfo(TEST_TOKEN.address, TEST_TOKEN.name, TEST_TOKEN.symbol, TEST_TOKEN.decimals, TEST_TOKEN.logoUrl)
        val result = PaginatedResults(listOf(tokenInfo))
        given(tokenServiceApiMock.paymentTokens()).willReturn(Single.just(result))

        repository.loadPaymentTokens().subscribe(testObserver)

        testObserver.assertResult(listOf(ERC20Token.ETHER_TOKEN, TEST_TOKEN))
        then(tokenServiceApiMock).should().paymentTokens()
        then(tokenServiceApiMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadPaymentTokensError() {
        val testObserver = TestObserver<List<ERC20Token>>()
        val exception = Exception()
        given(tokenServiceApiMock.paymentTokens()).willReturn(Single.error(exception))

        repository.loadPaymentTokens().subscribe(testObserver)

        testObserver.assertError(exception)
        then(tokenServiceApiMock).should().paymentTokens()
        then(tokenServiceApiMock).shouldHaveNoMoreInteractions()
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
        val TEST_TOKEN = ERC20Token(Solidity.Address(BigInteger.ONE), "Hello Token", "HT", 10)
    }
}
