package pm.gnosis.heimdall.ui.safe.details.transactions

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.support.v7.widget.scrollEvents
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_safe_transactions.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.build
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.common.utils.withArgs
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.utils.isAtEnd
import pm.gnosis.utils.hexAsBigInteger
import timber.log.Timber
import javax.inject.Inject


class SafeTransactionsFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: SafeTransactionsContract
    @Inject
    lateinit var adapter: SafeTransactionsAdapter

    private var moreDisposable: Disposable? = null
    private var loadMore: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val address = arguments!!.getString(ARGUMENT_SAFE_ADDRESS).hexAsBigInteger()
        viewModel.setup(address)
        adapter.safeAddress = address
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = layoutInflater?.inflate(R.layout.layout_safe_transactions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout_safe_transactions_list.layoutManager = LinearLayoutManager(context)
        layout_safe_transactions_list.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        disposables += layout_safe_transactions_swipe_refresh.refreshes()
                .map { true }
                .startWith(false)
                .flatMap {
                    viewModel.initTransactions(it).doOnSubscribe { moreDisposable?.dispose() }
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe {
                                layout_safe_transactions_swipe_refresh.isRefreshing = true
                            }
                            .doAfterTerminate {
                                layout_safe_transactions_swipe_refresh.isRefreshing = false
                            }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult({ setupMoreDisposable() }, Timber::e)
    }

    override fun onStop() {
        moreDisposable?.dispose()
        super.onStop()
    }

    private fun setupMoreDisposable() {
        moreDisposable?.dispose()
        moreDisposable = viewModel.observeTransactions(
                layout_safe_transactions_list.scrollEvents()
                        .filter { loadMore && it.view().isAtEnd(LOADING_THRESHOLD) }
                        .doOnNext { loadMore = false }
                        .map { Unit }
        )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(this::displayTransactions, Timber::e)
    }

    private fun displayTransactions(transactions: SafeTransactionsContract.PaginatedTransactions) {
        loadMore = transactions.hasMore
        adapter.updateData(transactions.data)
        layout_safe_transactions_empty_view.visibility = if (transactions.data.entries.isEmpty()) {
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
        private val LOADING_THRESHOLD = 5
        private const val ARGUMENT_SAFE_ADDRESS = "argument.string.safe_address"

        fun createInstance(address: String) =
                SafeTransactionsFragment().withArgs(
                        Bundle().build { putString(ARGUMENT_SAFE_ADDRESS, address) }
                )
    }
}