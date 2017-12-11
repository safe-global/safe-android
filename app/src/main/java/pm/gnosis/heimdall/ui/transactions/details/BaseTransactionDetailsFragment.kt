package pm.gnosis.heimdall.ui.transactions.details

import android.content.Context
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.gojuno.koptional.Optional
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_simple_spinner_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.base.SimpleSpinnerAdapter
import pm.gnosis.heimdall.ui.transactions.BaseTransactionActivity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject


abstract class BaseTransactionDetailsFragment : BaseFragment(), AdapterView.OnItemSelectedListener {

    @Inject
    lateinit var baseViewModel: BaseTransactionDetailsContract

    private val adapter by lazy {
        // Adapter should only be created if we need it
        SafesSpinnerAdapter(context!!)
    }

    private var spinner: Spinner? = null

    abstract fun observeTransaction(): Observable<Result<Transaction>>
    abstract fun observeSafe(): Observable<Optional<BigInteger>>
    abstract fun inputEnabled(enabled: Boolean)

    override fun onStart() {
        super.onStart()
        (activity as? BaseTransactionActivity)?.registerFragmentObservables(this)
    }

    protected fun setupSafeSpinner(spinner: Spinner, defaultSafe: BigInteger?) {
        this.spinner = spinner
        spinner.adapter = adapter
        disposables += baseViewModel.observeSafes(defaultSafe)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setSpinnerData, {
                    val safes = defaultSafe?.let { listOf(Safe(it)) } ?: emptyList()
                    setSpinnerData(BaseTransactionDetailsContract.State(0, safes))
                })
    }

    open fun selectedSafeChanged(safe: Safe?) {}

    private fun setSpinnerData(state: BaseTransactionDetailsContract.State) {
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