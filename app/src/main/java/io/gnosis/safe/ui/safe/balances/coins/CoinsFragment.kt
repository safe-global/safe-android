package io.gnosis.safe.ui.safe.balances.coins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.databinding.FragmentCoinsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseFragment
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.balances.ActiveSafeListener
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class CoinsFragment : BaseFragment<FragmentCoinsBinding>(), ActiveSafeListener {

    @Inject
    lateinit var viewModel: CoinsViewModel

    private val adapter = CoinBalanceAdapter()

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCoinsBinding =
        FragmentCoinsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            coins.adapter = adapter
            coins.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
            coins.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            refresh.setOnRefreshListener { viewModel.loadFor(true) }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is CoinsState -> {
                    binding.progress.visible(state.loading)
                    binding.refresh.isRefreshing = state.refreshing
                    state.viewAction?.let { action ->
                        when (action) {
                            is BaseStateViewModel.ViewAction.ShowError -> handleError(action.error)
                            is UpdateBalances -> adapter.setItems(action.newBalances)
                        }
                    }

                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadFor()
    }

    override fun onActiveSafeChanged() {
        viewModel.loadFor()
    }

    private fun handleError(throwable: Throwable) {
        snackbar(requireView(), R.string.error_loading_balances)
        throwable.printStackTrace()
        Timber.e(throwable)
//        when(throwable) { }
    }

    companion object {
        fun newInstance(): CoinsFragment = CoinsFragment()
    }
}
