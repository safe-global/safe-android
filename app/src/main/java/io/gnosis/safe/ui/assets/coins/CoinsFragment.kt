package io.gnosis.safe.ui.assets.coins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentCoinsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.Offline
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.UpdateActiveSafe
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.BalanceFormatter
import io.gnosis.safe.utils.getErrorResForException
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class CoinsFragment : BaseViewBindingFragment<FragmentCoinsBinding>() {

    @Inject
    lateinit var balanceFormatter: BalanceFormatter

    @Inject
    lateinit var viewModel: CoinsViewModel

    private lateinit var adapter: CoinBalanceAdapter

    override fun screenId() = ScreenId.ASSETS_COINS

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCoinsBinding =
        FragmentCoinsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CoinBalanceAdapter(balanceFormatter)
        with(binding) {
            coins.adapter = adapter
            coins.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
            coins.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            refresh.setOnRefreshListener { viewModel.load(true) }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is CoinsState -> {
                    binding.progress.visible(state.loading)
                    binding.refresh.isRefreshing = state.refreshing
                    state.viewAction?.let { action ->
                        when (action) {
                            is UpdateActiveSafe -> {
                                adapter.setItems(listOf())
                            }
                            is UpdateBalances -> {
                                binding.contentNoData.root.visibility = View.GONE
                                adapter.setItems(action.newBalances)
                            }
                            is ShowError -> {
                                hideLoading()
                                if (adapter.itemCount == 0) {
                                    binding.contentNoData.root.visible(true)
                                }
                                when (action.error) {
                                    is Offline -> {
                                        snackbar(requireView(), R.string.error_no_internet)
                                    }
                                    else -> {
                                        snackbar(requireView(), action.error.getErrorResForException())
                                    }
                                }
                            }
                            else -> {

                            }
                        }
                    }
                }
            }
        })
    }

    private fun hideLoading() {
        binding.progress.visible(false)
        binding.refresh.isRefreshing = false
    }

    companion object {
        fun newInstance(): CoinsFragment = CoinsFragment()
    }
}
