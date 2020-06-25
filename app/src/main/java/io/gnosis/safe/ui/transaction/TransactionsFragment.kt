package io.gnosis.safe.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.data.models.Safe
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import io.gnosis.safe.ui.transaction.list.TransactionLoadStateAdapter
import io.gnosis.safe.ui.transaction.list.TransactionViewListAdapter
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class TransactionsFragment : SafeOverviewBaseFragment<FragmentTransactionsBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS

    @Inject
    lateinit var viewModel: TransactionsViewModel

    private val adapter by lazy { TransactionViewListAdapter(TransactionViewHolderFactory()) }
    private val noSafeFragment by lazy { NoSafeFragment.newInstance(NoSafeFragment.Position.TRANSACTIONS) }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionsBinding =
        FragmentTransactionsBinding.inflate(inflater, container, false)

    @ExperimentalPagingApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.withLoadStateHeaderAndFooter(
            header = TransactionLoadStateAdapter { adapter.retry() },
            footer = TransactionLoadStateAdapter { adapter.retry() }
        )
        adapter.addLoadStateListener { loadState ->
            binding.transactions.isVisible = loadState.refresh is LoadState.NotLoading
            binding.progress.isVisible = loadState.refresh is LoadState.Loading
        }
        adapter.addDataRefreshListener { isEmpty ->
            // FIXME: find better solution
            // show empty state only if data was updated due to loaded transactions
            // and not reseted  due to safe switch
            if (viewModel.state.value?.viewAction is LoadTransactions && isEmpty)
                showEmptyState()
            else
                showList()

            binding.refresh.isRefreshing = false
        }

        with(binding.transactions) {
            adapter = this@TransactionsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
        binding.refresh.setOnRefreshListener { viewModel.load() }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            binding.progress.visible(state.isLoading)
            state.viewAction.let { viewAction ->
                when (viewAction) {
                    is LoadTransactions -> loadTransactions(viewAction.newTransactions)
                    is NoSafeSelected -> loadNoSafeFragment()
                    is BaseStateViewModel.ViewAction.ShowEmptyState -> showEmptyState()
                    is ActiveSafeChanged -> {
                        handleActiveSafe(viewAction.activeSafe)
                        lifecycleScope.launch {
                            // if safe changes we need to reset data for recycler to be at the top of the list
                            adapter.submitData(PagingData.empty())
                        }
                    }
                    else -> {
                    }
                }
            }
        })
        viewModel.load()
    }

    private fun loadNoSafeFragment() {
        with(binding) {
            transactions.visible(false)
            imageEmpty.visible(false)
            labelEmpty.visible(false)
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
            imageEmpty.visible(false)
            labelEmpty.visible(false)
        }
    }

    private fun showEmptyState() {
        with(binding) {
            transactions.visible(false)
            imageEmpty.visible(true)
            labelEmpty.visible(true)
        }
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
        if (safe != null) {
            childFragmentManager.beginTransaction().remove(noSafeFragment).commitNow()
        }
    }
}
