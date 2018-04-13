package pm.gnosis.heimdall.ui.dialogs.transaction

import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class CreateTokenTransactionProgressViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var viewModel: CreateTokenTransactionProgressViewModel

    @Before
    fun setUp() {
        viewModel = CreateTokenTransactionProgressViewModel()
    }

    @Test
    fun loadCreateTokenTransactionValidToken() {
        val testObserver = TestObserver<Transaction>()
        val testAddress = Solidity.Address(BigInteger.valueOf(1337))
        viewModel.loadCreateTokenTransaction(testAddress).subscribe(testObserver)
        // BigInteger.Zero is used as a template
        val expectedData = StandardToken.Transfer.encode(Solidity.Address(BigInteger.ZERO), Solidity.UInt256(BigInteger.ZERO))
        val expectedTx = Transaction(testAddress, data = expectedData)
        testObserver.assertNoErrors()
            .assertValue(expectedTx)
            .assertTerminated()
    }
}
