package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAssetSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.assets.coins.CoinsAdapter
import io.gnosis.safe.ui.assets.coins.CoinsState
import io.gnosis.safe.ui.assets.coins.UpdateBalances
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.toColor
import javax.inject.Inject

class AssetSelectionFragment : BaseViewBindingFragment<FragmentAssetSelectionBinding>() {

    private val navArgs by navArgs<AssetSelectionFragmentArgs>()
    private val selectedChain by lazy { navArgs.chain }

    override fun screenId() = ScreenId.ASSETS_COINS_TRANSFER_SELECT

    override suspend fun chainId() = selectedChain.chainId

    @Inject
    lateinit var viewModel: AssetSelectionViewModel

    @Inject
    lateinit var adapter: CoinsAdapter

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAssetSelectionBinding =
        FragmentAssetSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener { findNavController().navigateUp() }
            chainRibbon.text = selectedChain.name
            chainRibbon.setTextColor(
                selectedChain.textColor.toColor(
                    requireContext(),
                    R.color.white
                )
            )
            chainRibbon.setBackgroundColor(
                selectedChain.backgroundColor.toColor(
                    requireContext(),
                    R.color.primary
                )
            )

            coins.adapter = adapter
            val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            dividerItemDecoration.setDrawable(
                AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.divider
                )!!
            )
            coins.addItemDecoration(dividerItemDecoration)
            coins.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            refresh.setOnRefreshListener { viewModel.load() }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->

            when (state) {
                is AssetSelectionState -> {
                    state.viewAction?.let { action ->
                        when (action) {
                            is UpdateAssetSelection -> {
                                binding.contentNoData.visibility = View.GONE
                                adapter.updateData(action.balances)
                            }
                        }
                    }

                }
            }
        }
    }
}
