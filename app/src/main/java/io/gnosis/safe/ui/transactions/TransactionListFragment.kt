package io.gnosis.safe.ui.transactions

import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionListBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.qrscanner.nullOnThrow
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import io.gnosis.safe.ui.transactions.paging.TransactionLoadStateAdapter
import io.gnosis.safe.ui.transactions.paging.TransactionPagingSource
import io.gnosis.safe.ui.transactions.paging.TransactionViewListAdapter
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.svalinn.common.utils.withArgs
import javax.inject.Inject

class TransactionListFragment : BaseViewBindingFragment<FragmentTransactionListBinding>() {

    private val type by lazy { requireArguments()[ARGS_TYPE] as TransactionPagingSource.Type }

    override fun screenId() = when (type) {
        TransactionPagingSource.Type.QUEUE -> ScreenId.TRANSACTIONS_QUEUE
        TransactionPagingSource.Type.HISTORY -> ScreenId.TRANSACTIONS_HISTORY
    }

    @Inject
    lateinit var viewModel: TransactionListViewModel

    private val adapter by lazy { TransactionViewListAdapter(TransactionViewHolderFactory()) }
    private val noSafeFragment by lazy { NoSafeFragment.newInstance(NoSafeFragment.Position.TRANSACTIONS) }

    private var reload: Boolean = true

    private val handler = Handler(Looper.getMainLooper())
    private val intervalUpdateRunnable = object : Runnable {
        override fun run() {
            adapter.notifyDataSetChanged()
            handler.postDelayed(this, 60 * 1000)
        }
    }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionListBinding =
        FragmentTransactionListBinding.inflate(inflater, container, false)

    @ExperimentalPagingApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.addLoadStateListener { loadState ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

                binding.progress.isVisible = loadState.refresh is LoadState.Loading && adapter.itemCount == 0
                binding.refresh.isRefreshing = loadState.refresh is LoadState.Loading && adapter.itemCount != 0

                loadState.refresh.let {
                    if (it is LoadState.Error) {
                        if (adapter.itemCount == 0) {
                            binding.contentNoData.root.visible(true)
                        }
                        handleError(it.error)
                    }
                }
                loadState.append.let {
                    if (it is LoadState.Error) handleError(it.error)
                }
                loadState.prepend.let {
                    if (it is LoadState.Error) handleError(it.error)
                }

                if (viewModel.state.value?.viewAction is LoadTransactions && loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0) {
                    showEmptyState()
                } else {
                    showList()
                }
            }
        }

        binding.transactions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    val firstVisibleItem = (binding.transactions.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    reload = firstVisibleItem <= 1
                }
            }
        })

        with(binding.transactions) {
            adapter = this@TransactionListFragment.adapter.withLoadStateHeaderAndFooter(
                header = TransactionLoadStateAdapter { this@TransactionListFragment.adapter.retry() },
                footer = TransactionLoadStateAdapter { this@TransactionListFragment.adapter.retry() }
            )
            layoutManager = LinearLayoutManager(requireContext())

            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    super.getItemOffsets(outRect, view, parent, state)
                    when (val viewHolder = nullOnThrow { parent.getChildViewHolder(view) }) {
                        is ConflictViewHolder -> {
                            if (viewHolder.hasNext()) {
                                return
                            } else {
                                addItemSeparator(outRect)
                            }
                        }
                        is SectionConflictHeaderViewHolder -> return
                        else -> addItemSeparator(outRect)
                    }
                }

                private fun addItemSeparator(outRect: Rect) {
                    outRect[0, 0, 0] = resources.getDimension(R.dimen.item_separator_height).toInt()
                }
            })
        }
        binding.refresh.setOnRefreshListener { viewModel.load(type) }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->

            binding.contentNoData.root.visible(false)

            state.viewAction.let { viewAction ->
                when (viewAction) {
                    is LoadTransactions -> loadTransactions(viewAction.newTransactions)
                    is NoSafeSelected -> loadNoSafeFragment()
                    is ActiveSafeChanged -> {
                        stopElapsedIntervalsUpdate()
                        viewModel.load(type)
                        lifecycleScope.launch {
                            // if safe changes we need to reset data for recycler
                            adapter.submitData(PagingData.empty())
                        }
                    }
                    is ShowError -> {
                        binding.refresh.isRefreshing = false
                        binding.progress.visible(false)
                        if (adapter.itemCount == 0) {
                            binding.contentNoData.root.visible(true)
                        }
                        handleError(viewAction.error)
                    }
                    else -> {
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (reload) {
            viewModel.load(type)
        }
        startElapsedIntervalsUpdate()
    }

    override fun onPause() {
        super.onPause()
        stopElapsedIntervalsUpdate()
    }

    private fun startElapsedIntervalsUpdate() {
        if (type == TransactionPagingSource.Type.QUEUE) {
            handler.removeCallbacks(intervalUpdateRunnable)
            handler.post(intervalUpdateRunnable)
        }
    }

    private fun stopElapsedIntervalsUpdate() {
        if (type == TransactionPagingSource.Type.QUEUE) {
            handler.removeCallbacksAndMessages(intervalUpdateRunnable)
        }
    }

    private fun handleError(throwable: Throwable) {
        val error = throwable.toError()
        if (error.trackingRequired) {
            tracker.logException(throwable)
        }
        errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_tx_list))
    }

    private fun loadNoSafeFragment() {
        with(binding) {
            transactions.visible(false)
            emptyPlaceholder.visible(false)
            noSafe.apply {
                childFragmentManager.beginTransaction()
                    .replace(noSafe.id, noSafeFragment)
                    .commitNow()
            }
        }
    }

    private fun loadTransactions(newTransactions: PagingData<TransactionView>) {
        lifecycleScope.launch {
            adapter.submitData(newTransactions)
        }
    }

    private fun showList() {
        startElapsedIntervalsUpdate()
        with(binding) {
            transactions.visible(true)
            emptyPlaceholder.visible(false)
        }
    }

    private fun showEmptyState() {
        stopElapsedIntervalsUpdate()
        with(binding) {
            transactions.visible(false)
            emptyPlaceholder.visible(true)
        }
    }

    companion object {

        private const val ARGS_TYPE = "args.serializable.type"

        fun newQueueInstance(): TransactionListFragment {
            return TransactionListFragment().withArgs(Bundle().apply {
                putSerializable(ARGS_TYPE, TransactionPagingSource.Type.QUEUE)
            }) as TransactionListFragment
        }

        fun newHistoryInstance(): TransactionListFragment {
            return TransactionListFragment().withArgs(Bundle().apply {
                putSerializable(ARGS_TYPE, TransactionPagingSource.Type.HISTORY)
            }) as TransactionListFragment
        }
    }
}
