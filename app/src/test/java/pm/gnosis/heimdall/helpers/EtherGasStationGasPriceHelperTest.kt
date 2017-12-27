package pm.gnosis.heimdall.helpers

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.remote.EthGasStationApi
import pm.gnosis.heimdall.data.remote.models.EthGasStationPrices
import pm.gnosis.heimdall.utils.DateTimeUtils
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.tests.utils.mockGetStringWithArgs
import java.math.BigDecimal
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class EtherGasStationGasPriceHelperTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var ethGasStationApi: EthGasStationApi
    @Mock
    lateinit var slowCostsMock: TextView

    @Mock
    lateinit var normalCostsMock: TextView

    @Mock
    lateinit var fastCostsMock: TextView

    @Mock
    lateinit var slowTimeMock: TextView

    @Mock
    lateinit var normalTimeMock: TextView

    @Mock
    lateinit var fastTimeMock: TextView

    @Mock
    lateinit var rootViewMock: View

    private lateinit var slowContainerMock: TestLinearLayout
    private lateinit var normalContainerMock: TestLinearLayout
    private lateinit var fastContainerMock: TestLinearLayout
    private lateinit var helper: EtherGasStationGasPriceHelper

    private fun View.mockFindViewById(id: Int, mock: View) {
        given(findViewById<View>(id)).willReturn(mock)
    }

    private fun View.setupMock(): View {
        given(context).willReturn(contextMock)
        return this
    }

    @Before
    fun setup() {
        contextMock.mockGetString()
        contextMock.mockGetStringWithArgs()
        slowContainerMock = spy(TestLinearLayout(contextMock))
        normalContainerMock = spy(TestLinearLayout(contextMock))
        fastContainerMock = spy(TestLinearLayout(contextMock))

        rootViewMock.setupMock()

        rootViewMock.mockFindViewById(R.id.include_gas_price_selection_slow_container, slowContainerMock.setupMock())
        rootViewMock.mockFindViewById(R.id.include_gas_price_selection_slow_costs, slowCostsMock.setupMock())
        rootViewMock.mockFindViewById(R.id.include_gas_price_selection_slow_time, slowTimeMock.setupMock())

        rootViewMock.mockFindViewById(R.id.include_gas_price_selection_normal_container, normalContainerMock.setupMock())
        rootViewMock.mockFindViewById(R.id.include_gas_price_selection_normal_costs, normalCostsMock.setupMock())
        rootViewMock.mockFindViewById(R.id.include_gas_price_selection_normal_time, normalTimeMock.setupMock())

        rootViewMock.mockFindViewById(R.id.include_gas_price_selection_fast_container, fastContainerMock.setupMock())
        rootViewMock.mockFindViewById(R.id.include_gas_price_selection_fast_costs, fastCostsMock.setupMock())
        rootViewMock.mockFindViewById(R.id.include_gas_price_selection_fast_time, fastTimeMock.setupMock())
        helper = EtherGasStationGasPriceHelper(contextMock, ethGasStationApi)
    }

    private fun validateTextUpdate(costsView: TextView, waitTimeView: TextView, costs: Float, waitTime: Float) {
        then(costsView).should().text = convertToWei(costs).displayString(contextMock)
        then(waitTimeView).should().text = timeString(waitTime)
    }

    @Test
    fun observe() {
        val gasPriceSubject = PublishSubject.create<EthGasStationPrices>()
        given(ethGasStationApi.loadGasPrices()).willReturn(gasPriceSubject)
        val testObserver = TestObserver<Result<Wei>>()
        helper.observe(rootViewMock).subscribe(testObserver)

        // When subscribing we should setup the click listeners
        slowContainerMock.assertClickListenerNotNull()
        normalContainerMock.assertClickListenerNotNull()
        fastContainerMock.assertClickListenerNotNull()

        // No data has been emitted, so nothing should be updated
        testObserver.assertEmpty()

        // Check that the views have been updated with the returned prices
        gasPriceSubject.onNext(EthGasStationPrices(
                100f, 60f,
                200f, 1f,
                300f, 0.5f
        ))
        validateTextUpdate(slowCostsMock, slowTimeMock, 100f, 60f)
        validateTextUpdate(normalCostsMock, normalTimeMock, 200f, 1f)
        validateTextUpdate(fastCostsMock, fastTimeMock, 300f, 0.5f)

        testObserver.assertValuesOnly(DataResult(convertToWei(200f)))
        slowContainerMock.assertBackgroundRes(R.drawable.selectable_background)
        normalContainerMock.assertBackgroundRes(R.drawable.rounded_border_background)
        fastContainerMock.assertBackgroundRes(R.drawable.selectable_background)

        // If we select normal speed the selected container should change and the costs should be emitted
        normalContainerMock.callOnClick()
        testObserver.assertValuesOnly(
                DataResult(convertToWei(200f)),
                DataResult(convertToWei(200f)) // New value
        )
        slowContainerMock.assertBackgroundRes(R.drawable.selectable_background)
        normalContainerMock.assertBackgroundRes(R.drawable.rounded_border_background)
        fastContainerMock.assertBackgroundRes(R.drawable.selectable_background)

        // If we select fast speed the selected container should change and the costs should be emitted
        fastContainerMock.callOnClick()
        testObserver.assertValuesOnly(
                DataResult(convertToWei(200f)),
                DataResult(convertToWei(200f)),
                DataResult(convertToWei(300f)) // New value
        )
        slowContainerMock.assertBackgroundRes(R.drawable.selectable_background)
        normalContainerMock.assertBackgroundRes(R.drawable.selectable_background)
        fastContainerMock.assertBackgroundRes(R.drawable.rounded_border_background)

        // If we select slow speed the selected container should change and the costs should be emitted
        slowContainerMock.callOnClick()
        testObserver.assertValuesOnly(
                DataResult(convertToWei(200f)),
                DataResult(convertToWei(200f)),
                DataResult(convertToWei(300f)),
                DataResult(convertToWei(100f)) // New value
        )
        slowContainerMock.assertBackgroundRes(R.drawable.rounded_border_background)
        normalContainerMock.assertBackgroundRes(R.drawable.selectable_background)
        fastContainerMock.assertBackgroundRes(R.drawable.selectable_background)

        gasPriceSubject.onError(UnknownHostException())
        // We will get an exception when displaying the snackbar, as we don't mock static methods
        testObserver.assertFailureAndMessage(
                IllegalArgumentException::class.java,
                "No suitable parent found from the given view. Please provide a valid view.",
                DataResult(convertToWei(200f)),
                DataResult(convertToWei(200f)),
                DataResult(convertToWei(300f)),
                DataResult(convertToWei(100f))
        )

        // After the error the click listeners should be nulled
        slowContainerMock.assertClickListenerNull()
        normalContainerMock.assertClickListenerNull()
        fastContainerMock.assertClickListenerNull()

        then(slowTimeMock).shouldHaveNoMoreInteractions()
        then(slowCostsMock).shouldHaveNoMoreInteractions()

        then(normalTimeMock).shouldHaveNoMoreInteractions()
        then(normalCostsMock).shouldHaveNoMoreInteractions()

        then(fastTimeMock).shouldHaveNoMoreInteractions()
        then(fastCostsMock).shouldHaveNoMoreInteractions()

        then(ethGasStationApi).should().loadGasPrices()
        then(ethGasStationApi).shouldHaveNoMoreInteractions()
    }

    private fun convertToWei(price: Float) =
            Wei(BigDecimal.valueOf(price.toDouble()).multiply(BigDecimal.TEN.pow(8)).toBigInteger())

    private fun convertToMs(waitTime: Float) =
            (waitTime * DateTimeUtils.MINUTE_IN_MS).toLong()

    private fun timeString(waitTime: Float) =
            convertToMs(waitTime).let {
                if (it < DateTimeUtils.MINUTE_IN_MS) {
                    contextMock.getString(R.string.transaction_execution_time_estimate_sec, (it / DateTimeUtils.SECOND_IN_MS).toString())
                } else {
                    contextMock.getString(R.string.transaction_execution_time_estimate_min, (it / DateTimeUtils.MINUTE_IN_MS).toString())
                }
            }

    private class TestLinearLayout(context: Context) : LinearLayout(context) {
        private var clickListener: OnClickListener? = null
        private var backgroundResource: Int = 0

        override fun setOnClickListener(l: OnClickListener?) {
            super.setOnClickListener(l)
            clickListener = l
        }

        override fun callOnClick(): Boolean {
            clickListener?.onClick(this)
            return super.callOnClick()
        }

        override fun setBackgroundResource(resid: Int) {
            backgroundResource = resid
            super.setBackgroundResource(resid)
        }

        fun assertBackgroundRes(resid: Int) =
                apply { assertEquals("Unexpected background", resid, backgroundResource) }

        fun assertClickListenerNotNull() =
                apply { assertNotNull("Click listener should not be null!", clickListener) }

        fun assertClickListenerNull() =
                apply { assertNull("Click listener should be null!", clickListener) }
    }
}
