package pm.gnosis.heimdall.ui.authenticate

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.ZxingIntentIntegrator
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.security.SecurityViewModelTest
import pm.gnosis.heimdall.utils.ERC67Parser
import pm.gnosis.utils.addHexPrefix

@RunWith(MockitoJUnitRunner::class)
class AuthenticateViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var intentMock: Intent

    lateinit var viewModel: AuthenticateViewModel

    @Before
    fun setup() {
        given(contextMock.getString(Mockito.anyInt())).willReturn(SecurityViewModelTest.TEST_STRING)
        viewModel = AuthenticateViewModel(contextMock)
    }

    @Test
    fun checkResultWrongRequestCode() {
        val observer = createObserver()

        viewModel.checkResult(AuthenticateContract.ActivityResults(UNHANDLED_REQUEST_CODE, Activity.RESULT_OK, intentMock)).subscribe(observer)

        then(intentMock).shouldHaveZeroInteractions()
        observer.assertNoErrors().assertNoValues().assertComplete()
    }

    @Test
    fun checkResultNoResultOk() {
        val observer = createObserver()

        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_CANCELED, intentMock))
                .subscribe(observer)

        then(intentMock).shouldHaveZeroInteractions()
        observer.assertNoErrors().assertNoValues().assertComplete()
    }

    @Test
    fun checkResultNoIntent() {
        val observer = createObserver()

        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, null))
                .subscribe(observer)

        observer.assertNoErrors().assertNoValues().assertComplete()
    }

    @Test
    fun checkResultNoData() {
        val observer = createObserver()
        given(intentMock.hasExtra(anyString())).willReturn(false)

        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intentMock))
                .subscribe(observer)

        then(intentMock).should().hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intentMock).shouldHaveNoMoreInteractions()
        observer.assertNoErrors().assertNoValues().assertComplete()
    }

    @Test
    fun checkResultInvalidTransaction() {
        val intent = testIntent(TEST_STRING)
        val observer = createObserver()

        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)

        then(intent).should().hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).should().getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).shouldHaveNoMoreInteractions()
        observer.assertComplete().assertNoErrors().assertValue(ErrorResult(LocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.invalid_erc67)
    }

    @Test
    fun checkResultNoActionData() {
        val intent = testIntent(createTransactionString())
        val observer = createObserver()

        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)

        then(intent).should().hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).should().getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).shouldHaveNoMoreInteractions()
        observer.assertComplete().assertNoErrors().assertValue(ErrorResult(LocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.unknown_safe_action)
    }

    @Test
    fun checkResultUnknownSafeAction() {
        val intent = testIntent(createTransactionString(data = "TEST_DATA"))
        val observer = createObserver()

        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)

        then(intent).should().hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).should().getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).shouldHaveNoMoreInteractions()
        observer.assertComplete().assertNoErrors().assertValue(ErrorResult(LocalizedException(TEST_STRING)))
        then(contextMock).should().getString(R.string.unknown_safe_action)
    }

    @Test
    fun checkResultConfirmAction() {
        val intent = testIntent(createTransactionString(data = GnosisSafe.ConfirmTransaction.METHOD_ID.addHexPrefix()))
        val observer = createObserver()

        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)

        then(intent).should().hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).should().getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).shouldHaveNoMoreInteractions()
        observer.assertComplete().assertNoErrors().assertValue { it is DataResult }
    }

    @Test
    fun checkResultRevokeAction() {
        val intent = testIntent(createTransactionString(data = GnosisSafe.RevokeConfirmation.METHOD_ID.addHexPrefix()))
        val observer = createObserver()

        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)

        then(intent).should().hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).should().getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
        then(intent).shouldHaveNoMoreInteractions()
        observer.assertComplete().assertNoErrors().assertValue { it is DataResult }
    }

    private fun testIntent(result: String): Intent {
        val intent = mock(Intent::class.java)
        given(intent.hasExtra(Mockito.eq(ZxingIntentIntegrator.SCAN_RESULT_EXTRA))).willReturn(true)
        given(intent.getStringExtra(Mockito.eq(ZxingIntentIntegrator.SCAN_RESULT_EXTRA))).willReturn(result)
        return intent
    }

    private fun createTransactionString(address: String = "0000000000000000000000000000000", data: String? = null): String {
        val builder = StringBuilder()
        builder.append(ERC67Parser.SCHEMA).append("0x" + address)
        data?.let { builder.append(ERC67Parser.SEPARATOR + ERC67Parser.DATA_KEY + data) }
        return builder.toString()
    }

    private fun createObserver() = TestObserver.create<Result<Intent>>()

    companion object {
        const val UNHANDLED_REQUEST_CODE = 13187
        const val TEST_STRING = "TEST"
    }
}
