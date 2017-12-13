package pm.gnosis.heimdall.ui.dialogs.transaction

import android.content.Context
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.mockGetString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class CreateTokenTransactionProgressViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    private lateinit var viewModel: CreateTokenTransactionProgressViewModel

    @Before
    fun setUp() {
        contextMock.mockGetString()
        viewModel = CreateTokenTransactionProgressViewModel(contextMock)
    }

    @Test
    fun loadCreateTokenTransactionNullToken() {
        val testObserver = TestObserver<Transaction>()
        viewModel.loadCreateTokenTransaction(null).subscribe(testObserver)
        testObserver.assertNoValues()
                .assertError(SimpleLocalizedException(R.string.error_invalid_token_address.toString()))
                .assertTerminated()
    }

    @Test
    fun loadCreateTokenTransactionInvalidToken() {
        val testObserver = TestObserver<Transaction>()
        viewModel.loadCreateTokenTransaction(BigInteger.valueOf(2).pow(160)).subscribe(testObserver)
        testObserver.assertNoValues()
                .assertError(SimpleLocalizedException(R.string.error_invalid_token_address.toString()))
                .assertTerminated()
    }

    @Test
    fun loadCreateTokenTransactionValidToken() {
        val testObserver = TestObserver<Transaction>()
        val testAddress = BigInteger.valueOf(1337)
        viewModel.loadCreateTokenTransaction(testAddress).subscribe(testObserver)
        // BigInteger.Zero is used as a template
        val expectedData = StandardToken.Transfer.encode(Solidity.Address(BigInteger.ZERO), Solidity.UInt256(BigInteger.ZERO))
        val expectedTx = Transaction(testAddress, data = expectedData)
        testObserver.assertNoErrors()
                .assertValue(expectedTx)
                .assertTerminated()
    }

}