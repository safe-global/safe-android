package io.gnosis.safe.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.Adapter
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.BaseViewBindingFragment
import io.gnosis.safe.ui.base.MultiViewHolderAdapter
import kotlinx.android.synthetic.main.fragment_transactions.*
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class TransactionsFragment : BaseViewBindingFragment<FragmentTransactionsBinding>() {

    override fun screenId() = ScreenId.TRANSACTIONS

    @Inject
    lateinit var viewModel: TransactionsViewModel

    private val adapter by lazy { MultiViewHolderAdapter(TransactionViewHolderFactory()) }

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
                    is LoadTransactions -> adapter.updateData(Adapter.Data(entries = viewAction.newTransactions))
                    is BaseStateViewModel.ViewAction.ShowEmptyState -> showEmptyState()
                    else -> binding.progress.visible(state.isLoading)
                }
            }
        })
        viewModel.load()
    }

    private fun showEmptyState() {
        with(binding) {
            progress.visible(false)
            imageEmpty.visible(true)
            labelEmpty.visible(true)
        }
    }
}
