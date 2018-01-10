package pm.gnosis.heimdall.helpers

import android.content.Context
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import kotlinx.android.synthetic.main.include_gas_price_selection.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.remote.EthGasStationApi
import pm.gnosis.heimdall.data.remote.models.EthGasStationPrices
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.DateTimeUtils
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.models.Wei
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.data.repositories.models.Currency
import pm.gnosis.utils.stringWithNoTrailingZeroes
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@ForView
class EtherGasStationGasPriceHelper @Inject constructor(
        @ApplicationContext private val context: Context,
        private val gasStationApi: EthGasStationApi,
        private val tickerRepository: TickerRepository
) : GasPriceHelper {
    private lateinit var view: View

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    override fun setup(view: View) {
        this.view = view
    }

    override fun observe(): Observable<Result<Wei>> {
        return Observable.combineLatest(
                viewSpeedClicksObservable(),
                loadGasPrices(),
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

    private fun viewSpeedClicksObservable() = Observable
            .merge(view.include_gas_price_selection_slow_container.clicks().map { TransactionSpeed.SLOW },
                    view.include_gas_price_selection_normal_container.clicks().map { TransactionSpeed.NORMAL },
                    view.include_gas_price_selection_fast_container.clicks().map { TransactionSpeed.FAST })
            .startWith(TransactionSpeed.NORMAL)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { updateSelected(view, it) }

    private fun loadGasPrices() = gasStationApi.loadGasPrices()
            .onErrorResumeNext(Function { errorHandler.observable(it) })
            .mapToResult()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNextForResult({ updateInfo(it) }, { Timber.e(it) })
            .flatMapSingle { ethGasStationPrices ->
                when (ethGasStationPrices) {
                    is DataResult -> loadFiatPrices(ethGasStationPrices.data)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess { onFiatValues(it.first[0], it.first[1], it.first[2], it.second) }
                            .doOnError { onFiatValuesError(it) }
                            .map { ethGasStationPrices }
                            .onErrorReturn { ErrorResult(it) }
                    is ErrorResult -> Single.just(ethGasStationPrices)
                }
            }

    private fun loadFiatPrices(ethGasStationPrices: EthGasStationPrices): Single<Pair<List<BigDecimal>, Currency>> =
            Single.just(listOf(
                    convertToWei(ethGasStationPrices.slowPrice),
                    convertToWei(ethGasStationPrices.standardPrice),
                    convertToWei(ethGasStationPrices.fastPrice)))
                    .flatMap { tickerRepository.convertToFiat(it) }

    private fun onFiatValues(slow: BigDecimal,
                             normal: BigDecimal,
                             fast: BigDecimal,
                             currency: Currency) {
        val symbol = currency.getFiatSymbol()
        view.include_gas_price_selection_slow_costs_fiat.visibility = View.VISIBLE
        view.include_gas_price_selection_normal_costs_fiat.visibility = View.VISIBLE
        view.include_gas_price_selection_fast_costs_fiat.visibility = View.VISIBLE
        view.include_gas_price_selection_slow_costs_fiat.text = context.getString(R.string.fiat_approximation, slow.stringWithNoTrailingZeroes(), symbol)
        view.include_gas_price_selection_normal_costs_fiat.text = context.getString(R.string.fiat_approximation, normal.stringWithNoTrailingZeroes(), symbol)
        view.include_gas_price_selection_fast_costs_fiat.text = context.getString(R.string.fiat_approximation, fast.stringWithNoTrailingZeroes(), symbol)
    }

    private fun onFiatValuesError(throwable: Throwable) {
        Timber.e(throwable)
        view.include_gas_price_selection_slow_costs_fiat.visibility = View.GONE
        view.include_gas_price_selection_normal_costs_fiat.visibility = View.GONE
        view.include_gas_price_selection_fast_costs_fiat.visibility = View.GONE
    }

    private fun updateInfo(prices: EthGasStationPrices) {
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
    fun setup(view: View)
    /**
     *
     * Emits a DataResult with the selected price or an ErrorResult if no price could be loaded
     */
    fun observe(): Observable<Result<Wei>>
}
