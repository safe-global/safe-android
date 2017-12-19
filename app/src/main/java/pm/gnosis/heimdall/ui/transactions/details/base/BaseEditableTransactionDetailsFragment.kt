package pm.gnosis.heimdall.ui.transactions.details.base

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.SimpleSpinnerAdapter
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject


abstract class BaseEditableTransactionDetailsFragment: BaseTransactionDetailsFragment(), AdapterView.OnItemSelectedListener {

    @Inject
    lateinit var baseViewModel: BaseEditableTransactionDetailsContract

    private val adapter by lazy {
        // Adapter should only be created if we need it
        SafesSpinnerAdapter(context!!)
    }

    private var spinner: Spinner? = null

    protected fun setupSafeSpinner(spinner: Spinner, defaultSafe: BigInteger?) {
        this.spinner = spinner
        spinner.adapter = adapter
        disposables += baseViewModel.observeSafes(defaultSafe)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setSpinnerData, {
                    val safes = defaultSafe?.let { listOf(Safe(it)) } ?: emptyList()
                    setSpinnerData(BaseEditableTransactionDetailsContract.State(0, safes))
                })
    }

    open fun selectedSafeChanged(safe: Safe?) {}

    private fun setSpinnerData(state: BaseEditableTransactionDetailsContract.State) {
        spinner?.let {
            adapter.clear()
            adapter.addAll(state.safes)
            adapter.notifyDataSetChanged()
            it.onItemSelectedListener = this
            it.setSelection(state.selectedIndex, false)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val safe = parent.adapter.getItem(position) as Safe
        selectedSafeChanged(safe)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        selectedSafeChanged(null)
    }

    protected fun prepareInput(input: TextView): Observable<CharSequence> =
            input
                    .textChanges()
                    .debounce(INPUT_DELAY_MS, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext {
                        input.setTextColor(ContextCompat.getColor(context!!, R.color.gnosis_dark_blue))
                        input.setHintTextColor(ContextCompat.getColor(context!!, R.color.gnosis_dark_blue_alpha_70))
                    }

    protected fun setInputError(input: TextView) {
        input.setTextColor(ContextCompat.getColor(context!!, R.color.error))
        input.setHintTextColor(ContextCompat.getColor(context!!, R.color.error_hint))
    }

    private class SafesSpinnerAdapter(context: Context) : SimpleSpinnerAdapter<Safe>(context) {
        override fun title(item: Safe) =
                item.name

        override fun subTitle(item: Safe) =
                item.address.asEthereumAddressString()
    }

    companion object {
        private const val INPUT_DELAY_MS = 500L
    }
}