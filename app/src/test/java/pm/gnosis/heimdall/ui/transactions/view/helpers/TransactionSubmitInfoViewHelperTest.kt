package pm.gnosis.heimdall.ui.transactions.view.helpers

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import io.reactivex.observers.TestObserver
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.*
import pm.gnosis.tests.utils.Asserts.assertThrow
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class TransactionSubmitInfoViewHelperTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var containerView: ViewGroup

    private lateinit var helper: TransactionSubmitInfoViewHelper

    @Before
    fun setUp() {
        given(containerView.context).willReturn(contextMock)
        helper = TransactionSubmitInfoViewHelper()
    }

    @Test
    fun resetConfirmationViews() {
        contextMock.mockGetStringWithArgs()

        val keycardGroup = mock(CardView::class.java)
        val confirmationsGroup = mock(CardView::class.java)
        val requestButton = mock(TextView::class.java)
        val confirmationsTimer = mock(TextView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_keycard_group, keycardGroup)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_group, confirmationsGroup)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_request_button, requestButton)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_timer, confirmationsTimer)

        helper.bind(containerView)
        helper.resetConfirmationViews()

        then(keycardGroup).should().visibility = View.GONE
        then(keycardGroup).shouldHaveNoMoreInteractions()

        then(confirmationsGroup).should().visibility = View.GONE
        then(confirmationsGroup).shouldHaveNoMoreInteractions()

        then(requestButton).should().isEnabled = false
        then(requestButton).shouldHaveNoMoreInteractions()

        then(confirmationsTimer).should().text = "${R.string.request_confirmation_wait_x_s}, 30"
        then(confirmationsTimer).shouldHaveNoMoreInteractions()
    }

    @Test
    fun retryEvents() {
        val retryButton = mock(TextView::class.java)
        val confirmationProgress = mock(MaterialProgressBar::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_retry_button, retryButton)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmation_progress, confirmationProgress)

        var clickListener: View.OnClickListener? = null
        given(retryButton.setOnClickListener(MockUtils.any())).will {
            clickListener = it.arguments.first() as View.OnClickListener
            Unit
        }

        helper.bind(containerView)
        val observer = TestObserver<Unit>()
        helper.retryEvents().subscribe(observer)

        observer.assertEmpty()
        then(retryButton).should().setOnClickListener(MockUtils.any())
        then(retryButton).shouldHaveZeroInteractions()
        then(confirmationProgress).shouldHaveZeroInteractions()

        clickListener!!.onClick(retryButton)

        observer.assertValues(Unit)
        then(retryButton).should().visibility = View.GONE
        then(retryButton).shouldHaveNoMoreInteractions()
        then(confirmationProgress).should().visibility = View.VISIBLE
        then(confirmationProgress).shouldHaveNoMoreInteractions()
    }

    private fun mockRejectionStateViews() =
        RejectionStateMocks(mock(MaterialProgressBar::class.java), mock(ImageView::class.java), mock(TextView::class.java)).apply {
            given(confirmationProgress.context).willReturn(contextMock)
            containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmation_progress, confirmationProgress)
            containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_image, confirmationsImage)
            containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_info, confirmationsInfo)
        }

    private fun assertRejectionState(mocks: RejectionStateMocks, rejected: Boolean) {
        then(mocks.confirmationProgress).should().isIndeterminate = !rejected
        then(mocks.confirmationProgress).should().max = 100
        then(mocks.confirmationProgress).should().progress = 100
        then(mocks.confirmationProgress).should().progressTintList = MockUtils.any()
        then(mocks.confirmationProgress).should().progressTintMode = PorterDuff.Mode.SRC_IN
        then(mocks.confirmationProgress).should().context
        then(mocks.confirmationProgress).shouldHaveNoMoreInteractions()

        then(mocks.confirmationsImage).should().setImageResource(
            if (rejected) R.drawable.ic_rejected_extension else R.drawable.ic_confirmations_waiting
        )
        then(mocks.confirmationsImage).shouldHaveNoMoreInteractions()

        then(mocks.confirmationsInfo).should().setTextColor(
            if (rejected) R.color.tomato else R.color.blue
        )
        then(mocks.confirmationsInfo).should().text =
            if (rejected) R.string.rejected_by_extension.toString() else R.string.confirm_with_extension.toString()
        then(mocks.confirmationsInfo).shouldHaveNoMoreInteractions()
    }

    @Test
    fun requestConfirmationEvents() {
        contextMock.mockGetString()
        contextMock.mockGetStringWithArgs()
        contextMock.mockGetColor()

        val requestButton = mock(TextView::class.java)
        val confirmationTimer = mock(TextView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_request_button, requestButton)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_timer, confirmationTimer)

        val rejectionStateMocks = mockRejectionStateViews()

        var clickListener: View.OnClickListener? = null
        given(requestButton.setOnClickListener(MockUtils.any())).will {
            clickListener = it.arguments.first() as View.OnClickListener
            Unit
        }

        helper.bind(containerView)
        val observer = TestObserver<Unit>()
        helper.requestConfirmationEvents().subscribe(observer)

        observer.assertEmpty()
        then(requestButton).should().setOnClickListener(MockUtils.any())
        then(requestButton).shouldHaveZeroInteractions()
        then(confirmationTimer).shouldHaveZeroInteractions()

        clickListener!!.onClick(requestButton)

        observer.assertValues(Unit)
        then(requestButton).should().isEnabled = false
        then(requestButton).shouldHaveNoMoreInteractions()
        then(confirmationTimer).should().text = "${R.string.request_confirmation_wait_x_s}, 30"
        then(confirmationTimer).shouldHaveNoMoreInteractions()

        assertRejectionState(rejectionStateMocks, false)
    }

    private fun testRejectionState(rejected: Boolean) {
        contextMock.mockGetString()
        contextMock.mockGetColor()

        val rejectionStateMocks = mockRejectionStateViews()

        helper.bind(containerView)
        helper.toggleRejectionState(rejected)

        assertRejectionState(rejectionStateMocks, rejected)
    }

    @Test
    fun toggleRejectionStateClear() {
        testRejectionState(false)
    }

    @Test
    fun toggleRejectionStateRejected() {
        testRejectionState(true)
    }

    private fun mockReadyStateViews() =
        ReadyStateMocks(mock(MaterialProgressBar::class.java), mock(TextView::class.java)).apply {
            containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmation_status, confirmationStatus)
            containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmation_progress, confirmationProgress)
        }

    private fun assertReadySate(mocks: ReadyStateMocks, isReady: Boolean, inProgressMessage: Int?) {

        then(mocks.confirmationStatus).should().text =
            if (isReady) R.string.confirmations_ready.toString() else (inProgressMessage ?: R.string.awaiting_confirmations).toString()
        then(mocks.confirmationStatus).shouldHaveNoMoreInteractions()

        then(mocks.confirmationProgress).should().isIndeterminate = !isReady
        then(mocks.confirmationProgress).should().max = 100
        then(mocks.confirmationProgress).should().progress = 100
        then(mocks.confirmationProgress).shouldHaveNoMoreInteractions()
    }

    private fun testReadyState(isReady: Boolean, inProgressMessage: Int?) {
        contextMock.mockGetString()
        val viewMocks = mockReadyStateViews()

        helper.bind(containerView)
        if (inProgressMessage != null)
            helper.toggleReadyState(isReady, inProgressMessage)
        else
            helper.toggleReadyState(isReady)

        assertReadySate(viewMocks, isReady, inProgressMessage)
    }

    @Test
    fun toggleReadyStateReady() {
        var isReady: Boolean? = null
        helper.onToggleReadyState = { isReady = it }
        testReadyState(true, 1234)
        assertEquals(true, isReady)
    }

    @Test
    fun toggleReadyStateNotReady() {
        var isReady: Boolean? = null
        helper.onToggleReadyState = { isReady = it }
        testReadyState(false, null)
        assertEquals(false, isReady)
    }

    @Test
    fun toggleReadyStateCustomMessage() {
        testReadyState(false, 1234)
    }

    @Test
    fun setupViewHolder() {
        val inflater = mock(LayoutInflater::class.java)
        val viewHolder = mock(TransactionInfoViewHolder::class.java)

        val dataContainer = mock(ConstraintLayout::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_container, dataContainer)
        helper.bind(containerView)
        helper.setupViewHolder(inflater, viewHolder)

        then(dataContainer).should().removeAllViews()
        then(dataContainer).shouldHaveNoMoreInteractions()

        then(viewHolder).should().inflate(inflater, dataContainer)
        then(viewHolder).shouldHaveNoMoreInteractions()
    }

    @Test
    fun applyUpdateUnknown() {
        val viewHolder = mock(TransactionInfoViewHolder::class.java)
        helper.bind(containerView)
        assertThrow({
            helper.applyUpdate(SubmitTransactionHelper.ViewUpdate.TransactionSubmitted("some transaction hash"))
        })
        assertThrow({
            helper.applyUpdate(SubmitTransactionHelper.ViewUpdate.TransactionInfo(viewHolder))
        })
        then(containerView).should().context
        then(containerView).shouldHaveZeroInteractions()
    }

    @Test
    fun applyUpdateEstimate() {
        contextMock.mockGetColor()

        val dataAssetBalance = mock(TextView::class.java)
        val dataAssetBalanceLabel = mock(TextView::class.java)
        val dataBalance = mock(TextView::class.java)
        val dataBalanceLabel = mock(TextView::class.java)
        val dataFees = mock(TextView::class.java)
        val dataError = mock(TextView::class.java)
        val retryButton = mock(TextView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_asset_balance_label, dataAssetBalanceLabel)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_asset_balance_value, dataAssetBalance)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_gas_token_balance_value, dataBalance)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_gas_token_balance_label, dataBalanceLabel)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_fees_value, dataFees)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_fees_error, dataError)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_retry_button, retryButton)

        val data = SubmitTransactionHelper.ViewUpdate.Estimate(
            ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, Wei.ether("1").value),
            Wei.ether("0.1").value,
            null,
            true
        )
        helper.bind(containerView)
        assertNull(helper.applyUpdate(data))

        then(dataAssetBalance).should().visibility = View.GONE
        then(dataAssetBalance).shouldHaveNoMoreInteractions()

        then(dataAssetBalanceLabel).should().visibility = View.GONE
        then(dataAssetBalanceLabel).shouldHaveNoMoreInteractions()

        then(dataBalance).should().text = "1 ETH"
        then(dataBalance).should().setTextColor(R.color.dark_grey)
        then(dataBalance).shouldHaveNoMoreInteractions()

        then(dataBalanceLabel).should().setTextColor(R.color.dark_grey)
        then(dataBalanceLabel).shouldHaveNoMoreInteractions()

        then(dataFees).should().text = "0.1 ETH"
        then(dataFees).shouldHaveNoMoreInteractions()

        then(dataError).should().visibility = View.GONE
        then(dataError).shouldHaveNoMoreInteractions()

        then(retryButton).should().visibility = View.GONE
        then(retryButton).shouldHaveNoMoreInteractions()
    }

    @Test
    fun applyUpdateEstimateNotEnoughFunds() {
        contextMock.mockGetColor()

        val dataAssetBalance = mock(TextView::class.java)
        val dataAssetBalanceLabel = mock(TextView::class.java)
        val dataBalance = mock(TextView::class.java)
        val dataBalanceLabel = mock(TextView::class.java)
        val dataFees = mock(TextView::class.java)
        val dataError = mock(TextView::class.java)
        val retryButton = mock(TextView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_asset_balance_value, dataAssetBalance)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_asset_balance_label, dataAssetBalanceLabel)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_gas_token_balance_value, dataBalance)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_gas_token_balance_label, dataBalanceLabel)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_fees_value, dataFees)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_fees_error, dataError)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_retry_button, retryButton)

        val testToken = ERC20Token("0x1337".asEthereumAddress()!!, "Test Token", "TT", 0)
        val data = SubmitTransactionHelper.ViewUpdate.Estimate(
            ERC20TokenWithBalance(ERC20Token.ETHER_TOKEN, Wei.ether("-1").value),
            Wei.ether("0.1").value,
            ERC20TokenWithBalance(testToken, BigInteger.ONE),
            false
        )
        helper.bind(containerView)
        assertNull(helper.applyUpdate(data))

        then(dataAssetBalance).should().text = "1 TT"
        then(dataAssetBalance).should().visibility = View.VISIBLE
        then(dataAssetBalance).shouldHaveNoMoreInteractions()

        then(dataAssetBalanceLabel).should().visibility = View.VISIBLE
        then(dataAssetBalanceLabel).shouldHaveNoMoreInteractions()

        then(dataBalance).should().text = "-1 ETH"
        then(dataBalance).should().setTextColor(R.color.tomato)
        then(dataBalance).shouldHaveNoMoreInteractions()

        then(dataBalanceLabel).should().setTextColor(R.color.tomato)
        then(dataBalanceLabel).shouldHaveNoMoreInteractions()

        then(dataFees).should().text = "0.1 ETH"
        then(dataFees).shouldHaveNoMoreInteractions()

        then(dataError).should().visibility = View.VISIBLE
        then(dataError).shouldHaveNoMoreInteractions()

        then(retryButton).should().visibility = View.VISIBLE
        then(retryButton).shouldHaveNoMoreInteractions()
    }

    @Test
    fun applyUpdateEstimateGasToken() {
        contextMock.mockGetColor()

        val dataAssetBalance = mock(TextView::class.java)
        val dataAssetBalanceLabel = mock(TextView::class.java)
        val dataBalance = mock(TextView::class.java)
        val dataBalanceLabel = mock(TextView::class.java)
        val dataFees = mock(TextView::class.java)
        val dataError = mock(TextView::class.java)
        val retryButton = mock(TextView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_asset_balance_label, dataAssetBalanceLabel)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_asset_balance_value, dataAssetBalance)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_gas_token_balance_value, dataBalance)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_gas_token_balance_label, dataBalanceLabel)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_fees_value, dataFees)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_data_fees_error, dataError)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_retry_button, retryButton)

        val testGasToken = ERC20Token("0x1337".asEthereumAddress()!!, "Gas Token", "GT", 0)

        val data = SubmitTransactionHelper.ViewUpdate.Estimate(
            ERC20TokenWithBalance(testGasToken, BigInteger.TEN),
            BigInteger.ONE,
            null,
            true
        )
        helper.bind(containerView)
        assertNull(helper.applyUpdate(data))

        then(dataAssetBalance).should().visibility = View.GONE
        then(dataAssetBalance).shouldHaveNoMoreInteractions()

        then(dataAssetBalanceLabel).should().visibility = View.GONE
        then(dataAssetBalanceLabel).shouldHaveNoMoreInteractions()

        then(dataBalance).should().text = "10 GT"
        then(dataBalance).should().setTextColor(R.color.dark_grey)
        then(dataBalance).shouldHaveNoMoreInteractions()

        then(dataBalanceLabel).should().setTextColor(R.color.dark_grey)
        then(dataBalanceLabel).shouldHaveNoMoreInteractions()

        then(dataFees).should().text = "1 GT"
        then(dataFees).shouldHaveNoMoreInteractions()

        then(dataError).should().visibility = View.GONE
        then(dataError).shouldHaveNoMoreInteractions()

        then(retryButton).should().visibility = View.GONE
        then(retryButton).shouldHaveNoMoreInteractions()
    }

    @Test
    fun applyUpdateEstimateError() {
        contextMock.mockGetStringWithArgs()

        val retryButton = mock(TextView::class.java)
        val confirmationsProgress = mock(MaterialProgressBar::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_retry_button, retryButton)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmation_progress, confirmationsProgress)

        val data = SubmitTransactionHelper.ViewUpdate.EstimateError
        helper.bind(containerView)
        assertNull(helper.applyUpdate(data))

        then(retryButton).should().visibility = View.VISIBLE
        then(retryButton).shouldHaveNoMoreInteractions()

        then(confirmationsProgress).should().visibility = View.GONE
        then(confirmationsProgress).shouldHaveNoMoreInteractions()
    }

    @Test
    fun applyUpdateRequireConfirmationsKeycard() {
        val keycardGroup = mock(CardView::class.java)
        val keycardButton = mock(TextView::class.java)
        val confirmationsGroup = mock(CardView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_group, confirmationsGroup)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_keycard_group, keycardGroup)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_keycard_button, keycardButton)

        val authenticatorInfo = AuthenticatorInfo(AuthenticatorInfo.Type.KEYCARD, "0xdeadbeef".asEthereumAddress()!!, 32L)
        val data = SubmitTransactionHelper.ViewUpdate.RequireConfirmations(authenticatorInfo, "somehash")
        helper.bind(containerView)
        assertNull(helper.applyUpdate(data))

        then(confirmationsGroup).should().visibility = View.GONE
        then(confirmationsGroup).shouldHaveNoMoreInteractions()
        then(keycardGroup).should().visibility = View.VISIBLE
        then(keycardGroup).shouldHaveNoMoreInteractions()
        then(keycardButton).should().setOnClickListener(MockUtils.any())
        then(keycardButton).shouldHaveNoMoreInteractions()
    }

    @Test
    fun applyUpdateRequireConfirmationsExtension() {
        val keycardGroup = mock(CardView::class.java)
        val keycardButton = mock(TextView::class.java)
        val confirmationsGroup = mock(CardView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_group, confirmationsGroup)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_keycard_group, keycardGroup)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_keycard_button, keycardButton)

        val authenticatorInfo = AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, "0xdeadbeef".asEthereumAddress()!!)
        val data = SubmitTransactionHelper.ViewUpdate.RequireConfirmations(authenticatorInfo, "somehash")
        helper.bind(containerView)
        assertNull(helper.applyUpdate(data))

        then(confirmationsGroup).should().visibility = View.VISIBLE
        then(confirmationsGroup).shouldHaveNoMoreInteractions()
        then(keycardGroup).should().visibility = View.GONE
        then(keycardGroup).shouldHaveNoMoreInteractions()
        then(keycardButton).should().setOnClickListener(null)
        then(keycardButton).shouldHaveNoMoreInteractions()
    }

    private fun testApplyUpdateConfirmations(isReady: Boolean) {
        contextMock.mockGetString()

        val viewMocks = mockReadyStateViews()
        val keycardGroup = mock(CardView::class.java)
        val confirmationsGroup = mock(CardView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_group, confirmationsGroup)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_keycard_group, keycardGroup)

        val data = SubmitTransactionHelper.ViewUpdate.Confirmations(isReady)
        helper.bind(containerView)
        assertNull(helper.applyUpdate(data))

        assertReadySate(viewMocks, isReady, null)

        if (isReady) {
            then(confirmationsGroup).should().visibility = View.GONE
            then(keycardGroup).should().visibility = View.GONE
        }
        then(confirmationsGroup).shouldHaveNoMoreInteractions()
        then(keycardGroup).shouldHaveNoMoreInteractions()
    }

    @Test
    fun applyUpdateConfirmationsReady() {
        testApplyUpdateConfirmations(true)
    }

    @Test
    fun applyUpdateConfirmationsNotReady() {
        testApplyUpdateConfirmations(false)
    }

    @Test
    fun applyUpdateConfirmationsError() {
        contextMock.mockGetStringWithArgs()

        val requestButton = mock(TextView::class.java)
        val confirmationsTimer = mock(TextView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_request_button, requestButton)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_timer, confirmationsTimer)

        val data = SubmitTransactionHelper.ViewUpdate.ConfirmationsError
        helper.bind(containerView)
        assertNull(helper.applyUpdate(data))

        then(requestButton).should().isEnabled = true
        then(requestButton).shouldHaveNoMoreInteractions()

        then(confirmationsTimer).should().text = null
        then(confirmationsTimer).shouldHaveNoMoreInteractions()
    }

    @Test
    fun applyUpdateConfirmationsRequested() {
        val testScheduler = TestScheduler()
        RxJavaPlugins.setComputationSchedulerHandler { _ -> testScheduler }
        contextMock.mockGetStringWithArgs()

        val requestButton = mock(TextView::class.java)
        val confirmationsTimer = mock(TextView::class.java)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_request_button, requestButton)
        containerView.mockFindViewById(R.id.include_transaction_submit_info_confirmations_timer, confirmationsTimer)

        val data = SubmitTransactionHelper.ViewUpdate.ConfirmationsRequested
        helper.bind(containerView)
        assertNotNull(helper.applyUpdate(data))
        (0..29).forEach {
            testScheduler.advanceTimeBy(1, TimeUnit.SECONDS)
            val time = (30 - it - 1).toString()
            then(confirmationsTimer).should().text = "${R.string.request_confirmation_wait_x_s}, $time"
            if (it < 29) {
                // On the last run we have the onComplete interactions
                then(confirmationsTimer).shouldHaveNoMoreInteractions()
            }
        }
        then(confirmationsTimer).should().text = null
        then(confirmationsTimer).shouldHaveNoMoreInteractions()
        then(requestButton).should().isEnabled = true
        then(requestButton).shouldHaveNoMoreInteractions()
    }

    @Test
    fun applyUpdateTransactionRejected() {
        contextMock.mockGetString()
        contextMock.mockGetColor()

        val viewMocks = mockRejectionStateViews()

        val data = SubmitTransactionHelper.ViewUpdate.TransactionRejected
        helper.bind(containerView)
        assertNull(helper.applyUpdate(data))

        assertRejectionState(viewMocks, true)
    }

    data class ReadyStateMocks(val confirmationProgress: MaterialProgressBar, val confirmationStatus: TextView)
    data class RejectionStateMocks(val confirmationProgress: MaterialProgressBar, val confirmationsImage: ImageView, val confirmationsInfo: TextView)
}
