package pm.gnosis.heimdall.ui.authenticate

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.common.util.ZxingIntentIntegrator
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.security.SecurityViewModelTest
import pm.gnosis.heimdall.ui.transactiondetails.TransactionDetailsActivity
import pm.gnosis.heimdall.utils.ERC67Parser
import pm.gnosis.utils.addAddressPrefix
import org.mockito.Mockito.`when` as given

@RunWith(MockitoJUnitRunner::class)
class AuthenticateViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var context: Context

    @Before
    fun setup() {
        given(context.getString(Mockito.anyInt())).thenReturn(SecurityViewModelTest.TEST_STRING)
    }

    @Test
    fun checkResultWrongRequestCode() {
        val intent = mock(Intent::class.java)

        val viewModel = createViewModel()
        val observer = createObserver()
        viewModel.checkResult(AuthenticateContract.ActivityResults(UNHANDLED_REQUEST_CODE, Activity.RESULT_OK, intent)).subscribe(observer)
        observer.assertNoErrors()
        observer.assertNoValues()
        observer.assertComplete()

        verifyNoMoreInteractions(intent)
    }

    @Test
    fun checkResultNoResultOk() {
        val intent = mock(Intent::class.java)

        val viewModel = createViewModel()
        val observer = createObserver()
        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_CANCELED, intent))
                .subscribe(observer)
        observer.assertNoErrors()
        observer.assertNoValues()
        observer.assertComplete()

        verifyNoMoreInteractions(intent)
    }

    @Test
    fun checkResultNoIntent() {
        val viewModel = createViewModel()
        val observer = createObserver()
        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, null))
                .subscribe(observer)
        observer.assertNoErrors()
        observer.assertNoValues()
        observer.assertComplete()
    }

    @Test
    fun checkResultNoData() {
        val intent = mock(Intent::class.java)

        val viewModel = createViewModel()
        val observer = createObserver()
        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)
        observer.assertNoErrors()
        observer.assertNoValues()
        observer.assertComplete()
    }

    @Test
    fun checkResultInvalidTransaction() {
        val intent = testIntent(TEST_STRING)

        val viewModel = createViewModel()
        val observer = createObserver()
        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue(Result(LocalizedException(TEST_STRING)))

        Mockito.verify(context).getString(R.string.invalid_erc67)
    }

    @Test
    fun checkResultNoActionData() {
        val intent = testIntent(createTransactionString())

        val viewModel = createViewModel()
        val observer = createObserver()
        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue(Result(LocalizedException(TEST_STRING)))

        Mockito.verify(context).getString(R.string.unknown_wallet_action)
    }

    @Test
    fun checkResultUnknownWalletAction() {
        val intent = testIntent(createTransactionString(data = "TEST_DATA"))

        val viewModel = createViewModel()
        val observer = createObserver()
        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue(Result(LocalizedException(TEST_STRING)))

        Mockito.verify(context).getString(R.string.unknown_wallet_action)
    }

    @Test
    fun checkResultConfirmAction() {
        val intent = testIntent(createTransactionString(data = MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID.addAddressPrefix()))

        val viewModel = createViewModel()
        val observer = createObserver()
        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue { it.error == null && it.data is Intent }
    }

    @Test
    fun checkResultRevokeAction() {
        val intent = testIntent(createTransactionString(data = MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID.addAddressPrefix()))

        val viewModel = createViewModel()
        val observer = createObserver()
        viewModel.checkResult(AuthenticateContract.ActivityResults(ZxingIntentIntegrator.REQUEST_CODE, Activity.RESULT_OK, intent))
                .subscribe(observer)
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue { it.error == null && it.data is Intent }
    }

    private fun testIntent(result: String): Intent {
        val intent = mock(Intent::class.java)
        given(intent.hasExtra(Mockito.eq(ZxingIntentIntegrator.SCAN_RESULT_EXTRA))).thenReturn(true)
        given(intent.getStringExtra(Mockito.eq(ZxingIntentIntegrator.SCAN_RESULT_EXTRA))).thenReturn(result)
        return intent
    }

    private fun createTransactionString(address: String = "0000000000000000000000000000000", data: String? = null): String {
        val builder = StringBuilder()
        builder.append(ERC67Parser.SCHEMA).append("0x" + address)
        data?.let { builder.append(ERC67Parser.SEPARATOR + ERC67Parser.DATA_KEY + data) }
        return builder.toString()
    }


    private fun createObserver() =
            TestObserver.create<Result<Intent>>()

    private fun createViewModel() =
            AuthenticateViewModel(context)

    companion object {
        const val UNHANDLED_REQUEST_CODE = 13187
        const val TEST_STRING = "TEST"
    }

}