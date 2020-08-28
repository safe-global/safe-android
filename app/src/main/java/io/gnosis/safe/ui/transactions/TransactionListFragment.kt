package io.gnosis.safe.ui.transactions

import android.os.Bundle
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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionListBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.Offline
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import io.gnosis.safe.ui.transactions.paging.TransactionLoadStateAdapter
import io.gnosis.safe.ui.transactions.paging.TransactionViewListAdapter
import io.gnosis.safe.utils.getErrorResForException
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class TransactionListFragment : SafeOverviewBaseFragment<FragmentTransactionListBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS

    @Inject
    lateinit var viewModel: TransactionListViewModel

    private val adapter by lazy { TransactionViewListAdapter(TransactionViewHolderFactory()) }
    private val noSafeFragment by lazy { NoSafeFragment.newInstance(NoSafeFragment.Position.TRANSACTIONS) }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionListBinding =
        FragmentTransactionListBinding.inflate(inflater, container, false)

    @ExperimentalPagingApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.addLoadStateListener { loadState ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                binding.progress.isVisible = loadState.refresh is LoadState.Loading
            }
        }
        adapter.addDataRefreshListener { isEmpty ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                // FIXME: find better solution
                // show empty state only if data was updated due to loaded transactions
                // and not reseted  due to safe switch
                if (viewModel.state.value?.viewAction is LoadTransactions && isEmpty) {
                    showEmptyState()
                } else {
                    showList()
                    binding.transactions.scrollToPosition(0)
                }

                binding.refresh.isRefreshing = false
            }
        }

        with(binding.transactions) {
            adapter = this@TransactionListFragment.adapter.withLoadStateHeaderAndFooter(
                header = TransactionLoadStateAdapter { this@TransactionListFragment.adapter.retry() },
                footer = TransactionLoadStateAdapter { this@TransactionListFragment.adapter.retry() }
            )
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
        binding.refresh.setOnRefreshListener { viewModel.load() }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            binding.progress.visible(state.isLoading)
            binding.contentNoData.root.visible(false)

            state.viewAction.let { viewAction ->
                when (viewAction) {
                    is LoadTransactions -> loadTransactions(viewAction.newTransactions)
                    is NoSafeSelected -> loadNoSafeFragment()
                    is ActiveSafeChanged -> {
                        handleActiveSafe(viewAction.activeSafe)
                    }
                    is ShowError -> {
                        binding.progress.visible(false)
                        binding.contentNoData.root.visible(true)

                        when (val errorException = viewAction.error) {
                            is Offline -> {
                                snackbar(requireView(), R.string.error_no_internet)
                            }
                            else -> {
                                snackbar(requireView(), errorException.getErrorResForException())
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
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
        with(binding) {
            transactions.visible(true)
            emptyPlaceholder.visible(false)
        }
    }

    private fun showEmptyState() {
        with(binding) {
            transactions.visible(false)
            emptyPlaceholder.visible(true)
        }
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
        if (safe != null) {
            childFragmentManager.beginTransaction().remove(noSafeFragment).commitNow()
        }
    }
}
