package io.gnosis.safe.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.data.models.Safe
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.Adapter
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.MultiViewHolderAdapter
import io.gnosis.safe.ui.safe.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class TransactionsFragment : SafeOverviewBaseFragment<FragmentTransactionsBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS

    @Inject
    lateinit var viewModel: TransactionsViewModel

    private val adapter by lazy { MultiViewHolderAdapter(TransactionViewHolderFactory()) }
    private val noSafeFragment by lazy { NoSafeFragment.newInstance(NoSafeFragment.Position.TRANSACTIONS) }

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionsBinding =
        FragmentTransactionsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.transactions) {
            adapter = this@TransactionsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            binding.progress.visible(state.isLoading)
            state.viewAction.let { viewAction ->
                when (viewAction) {
                    is LoadTransactions -> loadTransactions(viewAction.newTransactions)
                    is NoSafeSelected -> loadNoSafeFragment()
                    is BaseStateViewModel.ViewAction.ShowEmptyState -> showEmptyState()
                    is ActiveSafeChanged -> handleActiveSafe(viewAction.activeSafe)
                    else -> binding.progress.visible(state.isLoading)
                }
            }
        })
        viewModel.load()
    }

    private fun loadNoSafeFragment() {
        with(binding) {
            transactions.visible(false)
            progress.visible(false)
            imageEmpty.visible(false)
            labelEmpty.visible(false)
            noSafe.apply {
                childFragmentManager.beginTransaction()
                    .replace(noSafe.id, noSafeFragment)
                    .commitNow()
            }
        }
    }

    private fun loadTransactions(newTransactions: List<TransactionView>) {
        with(binding) {
            progress.visible(false)
            transactions.visible(true)
            imageEmpty.visible(false)
            labelEmpty.visible(false)
        }
        adapter.updateData(Adapter.Data(entries = newTransactions))
    }

    private fun showEmptyState() {
        with(binding) {
            transactions.visible(false)
            progress.visible(false)
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
