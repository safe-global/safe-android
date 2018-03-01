package pm.gnosis.heimdall.ui.transactions.details.assets

import android.content.Context
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TokenTransferData
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.ui.transactions.details.assets.AssetTransferDetailsContract.FormData
import pm.gnosis.heimdall.ui.transactions.details.assets.AssetTransferDetailsContract.InputEvent
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException.Companion.AMOUNT_FIELD
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException.Companion.TOKEN_FIELD
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException.Companion.TO_FIELD
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AssetTransferDetailsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var detailsRepository: TransactionDetailsRepository

    @Mock
    lateinit var tokenRepository: TokenRepository

    private lateinit var viewModel: AssetTransferDetailsViewModel

    @Before
    fun setUp() {
        mockContext.mockGetString()
        viewModel = AssetTransferDetailsViewModel(mockContext, detailsRepository, tokenRepository)
    }

    @Test
    fun loadFormDataNoTransaction() {
        val testObserver = TestObserver<FormData>()
        viewModel.loadFormData(null, false).subscribe(testObserver)

        testObserver.assertResult(FormData())
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFormDataLoadingDetailsError() {
        val transaction = Transaction(BigInteger.ZERO)
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.error(IllegalStateException()))

        val testObserver = TestObserver<FormData>()
        viewModel.loadFormData(transaction, false).subscribe(testObserver)

        testObserver.assertResult(FormData())
        then(detailsRepository).should().loadTransactionData(transaction)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFormDataLoadingEtherTransfer() {
        val transaction = Transaction(BigInteger.ZERO, value = Wei(BigInteger.TEN))
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(None))

        val testObserver = TestObserver<FormData>()
        viewModel.loadFormData(transaction, false).subscribe(testObserver)

        testObserver.assertResult(FormData(BigInteger.ZERO, BigInteger.TEN, ETHER_TOKEN))
        then(detailsRepository).should().loadTransactionData(transaction)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFormDataLoadingGeneric() {
        val transaction = Transaction(BigInteger.ZERO)
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(None))

        val testObserver = TestObserver<FormData>()
        viewModel.loadFormData(transaction, false).subscribe(testObserver)

        testObserver.assertResult(FormData(BigInteger.ZERO, null, ETHER_TOKEN))
        then(detailsRepository).should().loadTransactionData(transaction)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFormDataLoadingTokenTransferNoData() {
        val transaction = Transaction(BigInteger.ZERO)
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(None))

        val testObserver = TestObserver<FormData>()
        viewModel.loadFormData(transaction, false).subscribe(testObserver)

        testObserver.assertResult(FormData(BigInteger.ZERO, null, ETHER_TOKEN))
        then(detailsRepository).should().loadTransactionData(transaction)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFormDataLoadingTokenTransferKnownToken() {
        val token = ERC20Token(BigInteger.ZERO, decimals = 42)
        val transaction = Transaction(token.address)
        val transferData = TokenTransferData(BigInteger.ONE, BigInteger.TEN)
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(transferData.toOptional()))
        given(tokenRepository.loadToken(token.address))
            .willReturn(Single.just(token))

        val testObserver = TestObserver<FormData>()
        viewModel.loadFormData(transaction, false).subscribe(testObserver)

        testObserver.assertResult(FormData(BigInteger.ONE, BigInteger.TEN, token))
        then(detailsRepository).should().loadTransactionData(transaction)
        then(tokenRepository).should().loadToken(token.address)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFormDataLoadingTokenTransferUnknownToken() {
        val token = ERC20Token(BigInteger.ZERO, decimals = 42)
        val transaction = Transaction(token.address)
        val transferData = TokenTransferData(BigInteger.ONE, BigInteger.TEN)
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(transferData.toOptional()))
        given(tokenRepository.loadToken(token.address))
            .willReturn(Single.error(NoSuchElementException()))

        val testObserver = TestObserver<FormData>()
        viewModel.loadFormData(transaction, false).subscribe(testObserver)

        testObserver.assertResult(FormData(BigInteger.ONE, BigInteger.TEN, null))
        then(detailsRepository).should().loadTransactionData(transaction)
        then(tokenRepository).should().loadToken(token.address)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFormDataLoadingTokenTransferKeepDefaults() {
        val token = ERC20Token(BigInteger.ZERO, decimals = 42)
        val transaction = Transaction(token.address)
        val transferData = TokenTransferData(BigInteger.ZERO, BigInteger.ZERO)
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(transferData.toOptional()))
        given(tokenRepository.loadToken(token.address))
            .willReturn(Single.error(NoSuchElementException()))

        val testObserver = TestObserver<FormData>()
        viewModel.loadFormData(transaction, false).subscribe(testObserver)

        testObserver.assertResult(FormData(BigInteger.ZERO, BigInteger.ZERO, null))
        then(detailsRepository).should().loadTransactionData(transaction)
        then(tokenRepository).should().loadToken(token.address)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun loadFormDataLoadingTokenTransferClearDefaults() {
        val token = ERC20Token(BigInteger.ZERO, decimals = 42)
        val transaction = Transaction(token.address)
        val transferData = TokenTransferData(BigInteger.ZERO, BigInteger.ZERO)
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(transferData.toOptional()))
        given(tokenRepository.loadToken(token.address))
            .willReturn(Single.error(NoSuchElementException()))

        val testObserver = TestObserver<FormData>()
        viewModel.loadFormData(transaction, true).subscribe(testObserver)

        testObserver.assertResult(FormData(null, null, null))
        then(detailsRepository).should().loadTransactionData(transaction)
        then(tokenRepository).should().loadToken(token.address)
        then(detailsRepository).shouldHaveNoMoreInteractions()
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    private fun testInputTransformer(
        inputStream: PublishSubject<InputEvent>, outputStream: TestObserver<Result<Transaction>>,
        input: InputEvent, expectedOutput: Result<Transaction>, testNo: Int
    ) {
        inputStream.onNext(input)
        outputStream.assertNoErrors().assertValueCount(testNo)
            .assertValueAt(testNo - 1, expectedOutput)
    }

    @Test
    fun inputTransformerWithOriginalTransaction() {

        val testPublisher = PublishSubject.create<InputEvent>()
        val testObserver = TestObserver<Result<Transaction>>()
        val originalTransaction = Transaction(BigInteger.TEN, nonce = BigInteger.valueOf(1337))

        testPublisher.compose(viewModel.inputTransformer(originalTransaction))
            .subscribe(testObserver)
        testObserver.assertNoValues()

        var testNo = 1
        // Valid input with change (token)
        val tentenToken = ERC20Token(BigInteger.TEN, decimals = 10)
        val transferTo = Solidity.Address(BigInteger.ZERO)
        val transferAmount = Solidity.UInt256(BigInteger.valueOf(123).multiply(BigInteger.TEN.pow(tentenToken.decimals)))
        val expectedData = StandardToken.Transfer.encode(transferTo, transferAmount)
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0" to true, "123" to false, tentenToken to false),
            DataResult(
                Transaction(
                    BigInteger.TEN, value = null,
                    data = expectedData, nonce = BigInteger.valueOf(1337)
                )
            ),
            testNo++
        )
        // Valid input with change (ether)
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0" to true, "123" to false, ERC20Token.ETHER_TOKEN to false),
            DataResult(
                Transaction(
                    BigInteger.ZERO, value = Wei(BigInteger.valueOf(123).multiply(BigInteger.TEN.pow(ERC20Token.ETHER_TOKEN.decimals))),
                    data = null, nonce = BigInteger.valueOf(1337)
                )
            ),
            testNo++
        )
        // Invalid input with change
        // Third changed (compared to last value -> valid input)
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0" to false, "123" to true, null to false),
            ErrorResult(
                TransactionInputException(
                    mockContext, TOKEN_FIELD, true
                )
            ),
            testNo++
        )
        // Second changed
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0" to false, "123y" to false, null to true),
            ErrorResult(
                TransactionInputException(
                    mockContext, AMOUNT_FIELD or TOKEN_FIELD, true
                )
            ),
            testNo++
        )
        // First changed
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0t" to false, "123y" to false, null to true),
            ErrorResult(
                TransactionInputException(
                    mockContext, TO_FIELD or AMOUNT_FIELD or TOKEN_FIELD, true
                )
            ),
            testNo++
        )
        // No change
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0t" to false, "123y" to false, null to false),
            ErrorResult(
                TransactionInputException(
                    mockContext, TO_FIELD or AMOUNT_FIELD or TOKEN_FIELD, false
                )
            ),
            testNo++
        )
    }

    @Test
    fun inputTransformerNoOriginalTransaction() {

        val testPublisher = PublishSubject.create<InputEvent>()
        val testObserver = TestObserver<Result<Transaction>>()

        testPublisher.compose(viewModel.inputTransformer(null))
            .subscribe(testObserver)
        testObserver.assertNoValues()

        var testNo = 1
        // Valid input with change (token)
        val tentenToken = ERC20Token(BigInteger.TEN, decimals = 10)
        val transferTo = Solidity.Address(BigInteger.ZERO)
        val transferAmount = Solidity.UInt256(BigInteger.valueOf(123).multiply(BigInteger.TEN.pow(tentenToken.decimals)))
        val expectedData = StandardToken.Transfer.encode(transferTo, transferAmount)
        testPublisher.onNext(InputEvent("0x0" to true, "123" to false, tentenToken to false))
        testObserver.assertNoErrors().assertValueCount(testNo)
            .assertValueAt(testNo - 1, {
                it is DataResult
                        && it.data.address == BigInteger.TEN
                        && it.data.value == null
                        && it.data.data == expectedData
                        && it.data.nonce == null
            })
        testNo++
        // Valid input with change (ether)
        testPublisher.onNext(InputEvent("0x0" to true, "123" to false, ERC20Token.ETHER_TOKEN to false))
        testObserver.assertNoErrors().assertValueCount(testNo)
            .assertValueAt(testNo - 1, {
                it is DataResult
                        && it.data.address == BigInteger.ZERO
                        && it.data.value == Wei(BigInteger.valueOf(123).multiply(BigInteger.TEN.pow(ERC20Token.ETHER_TOKEN.decimals)))
                        && it.data.data == null
                        && it.data.nonce == null
            })
        testNo++
        // Invalid input with change
        // Third changed (compared to last value -> valid input)
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0" to false, "123" to true, null to false),
            ErrorResult(
                TransactionInputException(
                    mockContext, TOKEN_FIELD, true
                )
            ),
            testNo++
        )
        // Second changed
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0" to false, "123y" to false, null to true),
            ErrorResult(
                TransactionInputException(
                    mockContext, AMOUNT_FIELD or TOKEN_FIELD, true
                )
            ),
            testNo++
        )
        // First changed
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0t" to false, "123y" to false, null to true),
            ErrorResult(
                TransactionInputException(
                    mockContext, TO_FIELD or AMOUNT_FIELD or TOKEN_FIELD, true
                )
            ),
            testNo++
        )
        // No change
        testInputTransformer(
            testPublisher, testObserver,
            InputEvent("0x0t" to false, "123y" to false, null to false),
            ErrorResult(
                TransactionInputException(
                    mockContext, TO_FIELD or AMOUNT_FIELD or TOKEN_FIELD, false
                )
            ),
            testNo++
        )
    }

    private fun testTransactionTransformer(
        inputStream: PublishSubject<Optional<Transaction>>, outputStream: TestObserver<Result<Transaction>>,
        input: Transaction?, expectedOutput: Result<Transaction>, testNo: Int
    ) {
        inputStream.onNext(input.toOptional())
        outputStream.assertNoErrors().assertValueCount(testNo)
            .assertValueAt(testNo - 1, expectedOutput)
    }

    @Test
    fun transactionTransformer() {

        val testPublisher = PublishSubject.create<Optional<Transaction>>()
        val testObserver = TestObserver<Result<Transaction>>()

        testPublisher.compose(viewModel.transactionTransformer())
            .subscribe(testObserver)
        testObserver.assertNoValues()

        var testNo = 1

        // No transaction passed
        testPublisher.onNext(None)
        testObserver.assertNoErrors().assertValueCount(testNo)
            .assertValueAt(testNo - 1, { it is ErrorResult && it.error is IllegalStateException })
        testNo++
        then(detailsRepository).shouldHaveNoMoreInteractions()
        reset(detailsRepository)

        // No asset transaction passed
        var transaction = Transaction(BigInteger.valueOf(42))
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(None))
        testTransactionTransformer(
            testPublisher, testObserver,
            transaction, ErrorResult(TransactionInputException(mockContext, AMOUNT_FIELD, true)), testNo++
        )
        then(detailsRepository).should().loadTransactionData(transaction)
        reset(detailsRepository)

        // Ether transaction action passed with no value
        transaction = Transaction(BigInteger.valueOf(42))
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(None))
        testTransactionTransformer(
            testPublisher, testObserver,
            transaction, ErrorResult(TransactionInputException(mockContext, AMOUNT_FIELD, true)), testNo++
        )
        then(detailsRepository).should().loadTransactionData(transaction)
        reset(detailsRepository)

        // Ether transaction action passed with zero value
        transaction = Transaction(BigInteger.valueOf(42), value = Wei.ZERO)
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(None))
        testTransactionTransformer(
            testPublisher, testObserver,
            transaction, ErrorResult(TransactionInputException(mockContext, AMOUNT_FIELD, true)), testNo++
        )
        then(detailsRepository).should().loadTransactionData(transaction)
        reset(detailsRepository)

        // Ether transaction action passed
        transaction = Transaction(BigInteger.valueOf(42), value = Wei(BigInteger.TEN))
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(None))
        testTransactionTransformer(
            testPublisher, testObserver,
            transaction, DataResult(transaction), testNo++
        )
        then(detailsRepository).should().loadTransactionData(transaction)
        reset(detailsRepository)

        // Token transaction action passed no data
        transaction = Transaction(BigInteger.valueOf(23))
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(None))
        testTransactionTransformer(
            testPublisher, testObserver,
            transaction, ErrorResult(TransactionInputException(mockContext, AMOUNT_FIELD, true)), testNo++
        )
        then(detailsRepository).should().loadTransactionData(transaction)
        reset(detailsRepository)

        // Token transaction action passed zero tokens
        transaction = Transaction(BigInteger.valueOf(23))
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(TokenTransferData(BigInteger.valueOf(42), BigInteger.ZERO).toOptional()))
        testTransactionTransformer(
            testPublisher, testObserver,
            transaction, ErrorResult(TransactionInputException(mockContext, AMOUNT_FIELD, true)), testNo++
        )
        then(detailsRepository).should().loadTransactionData(transaction)
        reset(detailsRepository)

        // Ether transaction action passed
        transaction = Transaction(BigInteger.valueOf(42))
        given(detailsRepository.loadTransactionData(MockUtils.any()))
            .willReturn(Single.just(TokenTransferData(BigInteger.valueOf(42), BigInteger.TEN).toOptional()))
        testTransactionTransformer(
            testPublisher, testObserver,
            transaction, DataResult(transaction), testNo++
        )
        then(detailsRepository).should().loadTransactionData(transaction)
        reset(detailsRepository)
    }

    @Test
    fun testLoadTokenInfo() {
        val testToken = ERC20Token(BigInteger.ONE, name = "Test Token", symbol = "TT", decimals = 18)
        val testObserver = TestObserver<Result<ERC20TokenWithBalance>>()
        given(tokenRepository.loadTokenBalances(MockUtils.any(), MockUtils.any())).willReturn(
            Observable.just(
                listOf(
                    testToken to BigInteger.valueOf(
                        13
                    )
                )
            )
        )

        viewModel.loadTokenInfo(BigInteger.TEN, testToken).subscribe(testObserver)

        testObserver.assertResult(DataResult(ERC20TokenWithBalance(testToken, BigInteger.valueOf(13))))
        then(tokenRepository).should().loadTokenBalances(BigInteger.TEN, listOf(testToken))
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testLoadTokenInfoError() {
        val testToken = ERC20Token(BigInteger.ONE, name = "Test Token", symbol = "TT", decimals = 18)
        val testObserver = TestObserver<Result<ERC20TokenWithBalance>>()
        val error = IllegalStateException()
        given(tokenRepository.loadTokenBalances(MockUtils.any(), MockUtils.any())).willReturn(Observable.error(error))

        viewModel.loadTokenInfo(BigInteger.TEN, testToken).subscribe(testObserver)

        testObserver.assertResult(ErrorResult(error))
        then(tokenRepository).should().loadTokenBalances(BigInteger.TEN, listOf(testToken))
        then(tokenRepository).shouldHaveNoMoreInteractions()
    }
}
