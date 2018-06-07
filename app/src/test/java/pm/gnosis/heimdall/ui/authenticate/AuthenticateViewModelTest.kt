package pm.gnosis.heimdall.ui.authenticate

import android.content.Context
import android.content.Intent
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ERC20Contract
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.utils.ethereum.ERC67Parser
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AuthenticateViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    lateinit var viewModel: AuthenticateViewModel

    @Before
    fun setup() {
        given(contextMock.getString(Mockito.anyInt())).willReturn(TEST_STRING)
        viewModel = AuthenticateViewModel(contextMock)
    }

    @Test
    fun checkResultInvalidTransaction() {
        val observer = createObserver()

        viewModel.checkResult(TEST_STRING)
            .subscribe(observer)

        observer.assertComplete().assertNoErrors().assertValue(ErrorResult(SimpleLocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.invalid_erc67)
    }

    @Test
    fun checkResultTokenTransfer() {
        val data = createTransactionString(data = ERC20Contract.Transfer.encode(Solidity.Address(BigInteger.TEN), Solidity.UInt256(BigInteger.ONE)))
        val observer = createObserver()

        viewModel.checkResult(data)
            .subscribe(observer)

        observer.assertComplete().assertNoErrors().assertValue { it is DataResult }
    }

    private fun createTransactionString(address: String = "0000000000000000000000000000000", data: String? = null): String {
        val builder = StringBuilder()
        builder.append(ERC67Parser.SCHEMA).append("0x$address")
        data?.let { builder.append(ERC67Parser.SEPARATOR + ERC67Parser.DATA_KEY + data) }
        return builder.toString()
    }

    private fun createObserver() = TestObserver.create<Result<Intent>>()

    companion object {
        const val TEST_STRING = "TEST"
    }
}
