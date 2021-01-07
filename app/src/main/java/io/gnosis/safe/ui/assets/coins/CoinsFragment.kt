package io.gnosis.safe.ui.assets.coins

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentCoinsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.UpdateActiveSafe
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.MultiViewHolderAdapter
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.BalanceFormatter
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class CoinsFragment : BaseViewBindingFragment<FragmentCoinsBinding>() {

    @Inject
    lateinit var balanceFormatter: BalanceFormatter

    @Inject
    lateinit var viewModel: CoinsViewModel

    private val adapter by lazy { MultiViewHolderAdapter(CoinsViewHolderFactory()) }

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
            val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            dividerItemDecoration.setDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.divider)!!)
            coins.addItemDecoration(dividerItemDecoration)
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
                                adapter.updateData(Adapter.Data(null, listOf()))
                            }
                            is UpdateBalances -> {
                                binding.contentNoData.root.visibility = View.GONE
                                adapter.updateData(action.newBalances)
                            }
                            is ShowError -> {
                                hideLoading()
                                if (adapter.itemCount == 0) {
                                    binding.contentNoData.root.visible(true)
                                }
                                val error = action.error.toError()
                                errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_assets_coins))
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
