package pm.gnosis.heimdall.ui.safe.details.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_safe_transactions.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.base.ScrollableContainer
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class SafeTransactionsFragment : BaseFragment(), ScrollableContainer {
    @Inject
    lateinit var viewModel: SafeTransactionsContract

    @Inject
    lateinit var adapter: SafeTransactionsAdapter

    @Inject
    lateinit var layoutManager: LinearLayoutManager

    @Inject
    lateinit var eventTracker: EventTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val address = arguments!!.getString(ARGUMENT_SAFE_ADDRESS)!!.asEthereumAddress()!!
        viewModel.setup(address)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.layout_safe_transactions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout_safe_transactions_list.layoutManager = layoutManager
        layout_safe_transactions_list.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        if (userVisibleHint) eventTracker.submit(Event.ScreenView(ScreenId.SAFE_TRANSACTION_LIST))

        disposables += adapter.transactionSelectionSubject
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle {
                viewModel.transactionSelected(it)
            }
            .subscribe({
                startActivity(it)
            }, Timber::e)

        disposables += viewModel.observeTransactions()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                layout_safe_transactions_progress_bar.visibility = View.VISIBLE
            }
            .doOnNext {
                layout_safe_transactions_progress_bar.visibility = View.GONE
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(::displayTransactions, Timber::e)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (::eventTracker.isInitialized && userVisibleHint) eventTracker.submit(Event.ScreenView(ScreenId.SAFE_TRANSACTION_LIST))
    }

    override fun scrollToTop() {
        // check that the view has been created
        view ?: return
        layoutManager.scrollToPositionWithOffset(0, 0)
    }

    private fun displayTransactions(transactions: Adapter.Data<SafeTransactionsContract.AdapterEntry>) {
        // If we were at the top of the list, stay at the top
        val scrollToTop = layoutManager.findFirstVisibleItemPosition() == 0
        adapter.updateData(transactions)
        if (scrollToTop) {
            layoutManager.scrollToPositionWithOffset(0, 0)
        }
        layout_safe_transactions_empty_view.visibility = if (transactions.entries.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .applicationComponent(component)
            .viewModule(ViewModule(context!!))
            .build().inject(this)
    }


    companion object {
        private const val ARGUMENT_SAFE_ADDRESS = "argument.string.safe_address"

        fun createInstance(address: Solidity.Address) =
            SafeTransactionsFragment().withArgs(
                Bundle().apply { putString(ARGUMENT_SAFE_ADDRESS, address.asEthereumAddressString()) }
            )
    }
}
