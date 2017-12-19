package pm.gnosis.heimdall.ui.transactions.details.generic

import android.content.Context
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.ui.transactions.details.generic.GenericTransactionDetailsContract.InputEvent
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException.Companion.DATA_FIELD
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException.Companion.TO_FIELD
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException.Companion.VALUE_FIELD
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger


@RunWith(MockitoJUnitRunner::class)
class GenericTransactionDetailsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var viewModel: GenericTransactionDetailsViewModel

    @Before
    fun setUp() {
        viewModel = GenericTransactionDetailsViewModel()
    }

    private fun testTransformer(inputStream: PublishSubject<InputEvent>, outputStream: TestObserver<Result<Transaction>>,
                                input: InputEvent, expectedOutput: Result<Transaction>, testNo: Int) {
        inputStream.onNext(input)
        outputStream.assertNoErrors().assertValueCount(testNo)
                .assertValueAt(testNo - 1, expectedOutput)
    }

    @Test
    fun inputTransformerWithOriginalTransaction() {
        val testPublisher = PublishSubject.create<InputEvent>()
        val testObserver = TestObserver<Result<Transaction>>()
        val mockContext = mock(Context::class.java).mockGetString()
        val originalTransaction = Transaction(BigInteger.TEN, nonce = BigInteger.valueOf(1337))

        testPublisher.compose(viewModel.inputTransformer(mockContext, originalTransaction))
                .subscribe(testObserver)
        testObserver.assertNoValues()

        var testNo = 1
        // Valid input with change
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0" to true, "123" to false, "" to false),
                DataResult(Transaction(BigInteger.ZERO, value = Wei(BigInteger.valueOf(123)),
                        data = null, nonce = BigInteger.valueOf(1337))),
                testNo++
        )
        // Invalid input with change
        // All changed (compared to last value -> valid input)
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0s" to false, "123x" to false, "üp" to false),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, true
                )),
                testNo++
        )
        // First changed
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0t" to false, "123x" to true, "üp" to false),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, true
                )),
                testNo++
        )
        // Second changed
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0t" to false, "123y" to false, "üp" to true),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, true
                )),
                testNo++
        )
        // Third changed
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0t" to false, "123y" to false, "öp" to true),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, true
                )),
                testNo++
        )
        // No change
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0t" to false, "123y" to false, "öp" to true),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, false
                )),
                testNo++
        )
    }

    @Test
    fun inputTransformerWithoutOriginalTransaction() {
        val testPublisher = PublishSubject.create<InputEvent>()
        val testObserver = TestObserver<Result<Transaction>>()
        val mockContext = mock(Context::class.java).mockGetString()

        testPublisher.compose(viewModel.inputTransformer(mockContext, null))
                .subscribe(testObserver)
        testObserver.assertNoValues()

        var testNo = 1
        // Valid input with change
        testPublisher.onNext(InputEvent("0x0" to true, "123" to false, "" to false))
        testObserver.assertNoErrors().assertValueCount(testNo)
                .assertValueAt(testNo - 1, {
                    it is DataResult
                            && it.data.address == BigInteger.ZERO
                            && it.data.value == Wei(BigInteger.valueOf(123))
                            && it.data.data == null
                            && it.data.nonce != null
                            && it.data.nonce!! <= BigInteger.valueOf(System.currentTimeMillis())
                            && it.data.nonce!! >= BigInteger.valueOf(System.currentTimeMillis() - NONCE_WINDOW)
                })
        testNo++
        // Invalid input with change
        // All changed (compared to last value -> valid input)
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0s" to false, "123x" to false, "üp" to false),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, true
                )),
                testNo++
        )
        // First changed
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0t" to false, "123x" to true, "üp" to false),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, true
                )),
                testNo++
        )
        // Second changed
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0t" to false, "123y" to false, "üp" to true),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, true
                )),
                testNo++
        )
        // Third changed
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0t" to false, "123y" to false, "öp" to true),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, true
                )),
                testNo++
        )
        // No change
        testTransformer(testPublisher, testObserver,
                InputEvent("0x0t" to false, "123y" to false, "öp" to true),
                ErrorResult(TransactionInputException(
                        mockContext, TO_FIELD or VALUE_FIELD or DATA_FIELD, false
                )),
                testNo++
        )
    }

    companion object {
        private const val NONCE_WINDOW = 10000L
    }
}