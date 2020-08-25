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
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class CoinsFragment : BaseViewBindingFragment<FragmentCoinsBinding>() {

    @Inject
    lateinit var viewModel: CoinsViewModel

    private val adapter = CoinBalanceAdapter()

    override fun screenId() = ScreenId.ASSETS_COINS

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
            refresh.setOnRefreshListener { viewModel.load(true) }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is CoinsState -> {
                    binding.progress.visible(state.loading)
                    binding.refresh.isRefreshing = state.refreshing
                    state.viewAction?.let { action ->
                        when (action) {
                            is UpdateBalances -> adapter.setItems(action.newBalances)
                            is ShowError -> {
                                when(action.error) {
                                    is Offline -> {
                                        snackbar(requireView(), R.string.error_no_internet)
                                    }
                                    else -> {
                                        handleError(action.error)
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
        viewModel.load()
    }

    private fun handleError(throwable: Throwable) {
        snackbar(requireView(), R.string.error_loading_balances)
        Timber.e(throwable)
    }

    companion object {
        fun newInstance(): CoinsFragment = CoinsFragment()
    }
}
