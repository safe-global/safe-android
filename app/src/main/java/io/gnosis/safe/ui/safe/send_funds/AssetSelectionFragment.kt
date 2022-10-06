package io.gnosis.safe.ui.safe.send_funds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.appcompat.content.res.AppCompatResources
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAssetSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.assets.coins.CoinsAdapter
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowEmptyState
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.passcode.hideSoftKeyboard
import io.gnosis.safe.utils.toColor
import pm.gnosis.svalinn.common.utils.visible
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

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?): Boolean {

                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.load(newText ?: "")
                    return true
                }
            })

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

            coins.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    clearSearchEditFocus()
                }
            })

            refresh.setOnRefreshListener { viewModel.load(searchView.query.toString()) }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->

            when (state) {
                is AssetSelectionState -> {
                    binding.refresh.isRefreshing = state.loading
                    state.viewAction?.let { action ->
                        when (action) {
                            is UpdateAssetSelection -> {
                                binding.coins.visible(true)
                                binding.contentNoData.visible(false)
                                adapter.updateData(action.balances)
                            }
                            is ShowEmptyState -> {
                                binding.coins.visible(false)
                                binding.contentNoData.visible(true)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.isLoading()) {
            viewModel.load()
        }
    }

    private fun clearSearchEditFocus() {
        binding.searchView.clearFocus()
        binding.searchView.hideSoftKeyboard()
    }
}
