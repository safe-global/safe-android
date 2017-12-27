package pm.gnosis.heimdall.helpers

import android.content.Context
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import kotlinx.android.synthetic.main.include_gas_price_selection.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.remote.EthGasStationApi
import pm.gnosis.heimdall.data.remote.models.EthGasStationPrices
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.DateTimeUtils
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.models.Wei
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EtherGasStationGasPriceHelper @Inject constructor(
        @ApplicationContext context: Context,
        private val gasStationApi: EthGasStationApi
) : GasPriceHelper {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    override fun observe(view: View): Observable<Result<Wei>> {
        return Observable.combineLatest(
                Observable.merge(
                        view.include_gas_price_selection_slow_container.clicks().map { TransactionSpeed.SLOW },
                        view.include_gas_price_selection_normal_container.clicks().map { TransactionSpeed.NORMAL },
                        view.include_gas_price_selection_fast_container.clicks().map { TransactionSpeed.FAST }
                ).startWith(TransactionSpeed.NORMAL)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext { updateSelected(view, it) },

                gasStationApi.loadGasPrices()
                        .onErrorResumeNext(Function { errorHandler.observable(it) })
                        .mapToResult()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext {
                            it.handle({ updateInfo(view, it) }, { errorSnackbar(view, it) })
                        },

                BiFunction { speed: TransactionSpeed, prices: Result<EthGasStationPrices> ->
                    when (prices) {
                        is ErrorResult -> ErrorResult<Wei>(prices.error)
                        is DataResult -> {
                            val costsWithTime = when (speed) {
                                TransactionSpeed.SLOW -> convertToWei(prices.data.slowPrice)
                                TransactionSpeed.NORMAL -> convertToWei(prices.data.standardPrice)
                                TransactionSpeed.FAST -> convertToWei(prices.data.fastPrice)
                            }
                            DataResult(costsWithTime)
                        }
                    }
                }
        )
    }

    private fun updateInfo(view: View, prices: EthGasStationPrices) {
        view.include_gas_price_selection_slow_costs.text = convertToWei(prices.slowPrice).displayString(view.context)
        view.include_gas_price_selection_slow_time.text = setExecutionTimeEstimate(view.context, prices.slowWaitTime)

        view.include_gas_price_selection_normal_costs.text = convertToWei(prices.standardPrice).displayString(view.context)
        view.include_gas_price_selection_normal_time.text = setExecutionTimeEstimate(view.context, prices.standardWaitTime)

        view.include_gas_price_selection_fast_costs.text = convertToWei(prices.fastPrice).displayString(view.context)
        view.include_gas_price_selection_fast_time.text = setExecutionTimeEstimate(view.context, prices.fastWaitTime)
    }

    private fun setExecutionTimeEstimate(context: Context, waitTime: Float) =
            convertToMs(waitTime).let {
                if (it < DateTimeUtils.MINUTE_IN_MS) {
                    context.getString(R.string.transaction_execution_time_estimate_sec, (it / DateTimeUtils.SECOND_IN_MS).toString())
                } else {
                    context.getString(R.string.transaction_execution_time_estimate_min, (it / DateTimeUtils.MINUTE_IN_MS).toString())
                }
            }

    private fun updateSelected(view: View, speed: TransactionSpeed) {
        when (speed) {
            TransactionSpeed.SLOW -> {
                setBackground(view.include_gas_price_selection_slow_container, true)
                setBackground(view.include_gas_price_selection_normal_container, false)
                setBackground(view.include_gas_price_selection_fast_container, false)
            }
            TransactionSpeed.NORMAL -> {
                setBackground(view.include_gas_price_selection_slow_container, false)
                setBackground(view.include_gas_price_selection_normal_container, true)
                setBackground(view.include_gas_price_selection_fast_container, false)
            }
            TransactionSpeed.FAST -> {
                setBackground(view.include_gas_price_selection_slow_container, false)
                setBackground(view.include_gas_price_selection_normal_container, false)
                setBackground(view.include_gas_price_selection_fast_container, true)
            }
        }
    }

    private fun setBackground(view: View, selected: Boolean) {
        view.setBackgroundResource(if (selected) R.drawable.rounded_border_background else R.drawable.selectable_background)
    }

    private fun convertToWei(price: Float) =
            Wei(BigDecimal.valueOf(price.toDouble()).multiply(BigDecimal.TEN.pow(8)).toBigInteger())

    private fun convertToMs(waitTime: Float) =
            (waitTime * DateTimeUtils.MINUTE_IN_MS).toLong()
}

private enum class TransactionSpeed {
    SLOW,
    NORMAL,
    FAST
}

interface GasPriceHelper {
    /**
     *
     * Emits a DataResult with the selected price or an ErrorResult if no price could be loaded
     */
    fun observe(view: View): Observable<Result<Wei>>
}