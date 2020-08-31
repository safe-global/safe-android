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

                binding.progress.isVisible = loadState.refresh is LoadState.Loading && adapter.itemCount == 0
                binding.refresh.isRefreshing = loadState.refresh is LoadState.Loading && adapter.itemCount != 0
                val append = loadState.append
                if (append is LoadState.Error) {
                    handleError(append.error)
                }
                val prepend = loadState.prepend
                if (prepend is LoadState.Error) {
                    handleError(prepend.error)
                }

                if (viewModel.state.value?.viewAction is LoadTransactions && loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0) {
                    showEmptyState()
                } else {
                    showList()
                }
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

            binding.contentNoData.root.visible(false)

            state.viewAction.let { viewAction ->
                when (viewAction) {
                    is LoadTransactions -> loadTransactions(viewAction.newTransactions)
                    is NoSafeSelected -> loadNoSafeFragment()
                    is ActiveSafeChanged -> {
                        handleActiveSafe(viewAction.activeSafe)
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
        binding.transactions.scrollToPosition(0)
        viewModel.load()
    }

    private fun handleError(error: Throwable) {
        when (error) {
            is Offline -> {
                snackbar(requireView(), R.string.error_no_internet)
            }
            else -> {
                snackbar(requireView(), error.getErrorResForException())
            }
        }
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
